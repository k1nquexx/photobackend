package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final NotificationService notificationService;
    private final PricingService pricingService;
    private final RateLimiterService rateLimiterService;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          TimeSlotRepository timeSlotRepository,
                          NotificationService notificationService,
                          PricingService pricingService,
                          RateLimiterService rateLimiterService) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.notificationService = notificationService;
        this.pricingService = pricingService;
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Атомарное создание заявки на бронирование со строгой транзакционной целостностью.
     */
    @Transactional
    public Map<String, Object> createBooking(BookingCreateRequest req, String clientIp) {
        if (req == null) {
            throw new BadRequestException("Данные заявки не переданы");
        }

        // 1. Серверный расчет стоимости (Single Source of Truth)
        int calculatedPrice = pricingService.calculatePrice(
                req.getShootTypeRate(),
                req.getHours(),
                req.isAddonExpress(),
                req.isAddonStudio(),
                req.getPromoCode()
        );

        // 2. Атомарное бронирование слота на уровне БД (защита от Race Condition)
        if (req.getBookingDate() != null && req.getBookingTime() != null) {
            int updatedRows = timeSlotRepository.bookSlotAtomically(req.getBookingDate(), req.getBookingTime());
            if (updatedRows == 0) {
                throw new SlotConflictException("К сожалению, окно на " + req.getBookingTime() + 
                        " только что было забронировано! Пожалуйста, выберите другое свободное время.");
            }
        }

        // 3. Создаем сущность со строгой инициализацией полей (Mass Assignment Protection)
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String trackingToken = "TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();

        Booking booking = new Booking();
        booking.setClientName(req.getClientName());
        booking.setClientPhone(req.getClientPhone());
        booking.setDetails(req.getDetails());
        booking.setBookingDate(req.getBookingDate());
        booking.setBookingTime(req.getBookingTime());
        booking.setPromoCode(req.getPromoCode());
        booking.setTotalPrice(calculatedPrice);
        booking.setStatus(BookingStatus.NEW.getDisplayName());
        booking.setDownloadUrl(null);
        booking.setTrackingToken(trackingToken);
        booking.setCreatedAt(nowStr);
        booking.setClientIp(clientIp);

        Booking saved = bookingRepository.save(booking);

        // 4. Отправка уведомления фотографу
        try {
            notificationService.notifyNewBooking(saved);
        } catch (Exception e) {
            log.error("Ошибка отправки уведомления о новой заявке #{}: ", saved.getId(), e);
        }

        log.info("Создана новая заявка #{} (код: {}) от клиента: {} ({}), IP: {}, время: {}",
                saved.getId(), trackingToken, saved.getClientName(), saved.getClientPhone(), clientIp, nowStr);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("orderId", saved.getId());
        resp.put("trackingToken", trackingToken);
        resp.put("message", "Заявка успешно отправлена! Номер вашей заявки: #" + saved.getId());
        return resp;
    }

    /**
     * Безопасная проверка статуса заказа клиентом (по токену или ID + телефону).
     */
    public BookingStatusResponse checkStatus(String token, Long orderId, String phone, String clientIp) {
        if (!rateLimiterService.tryAcquire("status:" + clientIp, SecurityConstants.STATUS_MAX_REQUESTS, SecurityConstants.STATUS_WINDOW_SECONDS)) {
            throw new TooManyRequestsException("Слишком много запросов. Пожалуйста, подождите минуту.");
        }

        Optional<Booking> bookingOpt = Optional.empty();

        if (token != null && !token.isBlank()) {
            bookingOpt = bookingRepository.findByTrackingToken(token.trim().toUpperCase());
        } else if (orderId != null && phone != null && !phone.isBlank()) {
            bookingOpt = bookingRepository.findById(orderId)
                    .filter(b -> isPhoneMatching(phone, b.getClientPhone()));
        } else {
            throw new BadRequestException("Укажите код отслеживания или номер заявки с телефоном.");
        }

        Booking b = bookingOpt.orElseThrow(() -> new ResourceNotFoundException("Заказ с указанными данными не найден."));

        String maskedName = BookingStatusResponse.maskName(b.getClientName());
        String downloadUrl = BookingStatus.COMPLETED.getDisplayName().equalsIgnoreCase(b.getStatus()) ? b.getDownloadUrl() : null;

        return new BookingStatusResponse(
                b.getId(),
                b.getTrackingToken(),
                maskedName,
                b.getStatus(),
                b.getBookingDate(),
                b.getBookingTime(),
                downloadUrl
        );
    }

    /**
     * Получение всех заявок с пагинацией (для администратора).
     */
    public Page<BookingResponseDto> getAllBookings(Pageable pageable) {
        return bookingRepository.findAllByOrderByIdDesc(pageable)
                .map(BookingResponseDto::fromEntity);
    }

    /**
     * Быстрые агрегатные SQL-запросы статистики на уровне БД (O(1) по памяти).
     */
    public Map<String, Object> getStats() {
        long totalCount = bookingRepository.countTotalBookings();
        long totalIncome = bookingRepository.sumTotalIncome();
        long newCount = bookingRepository.countByStatusIgnoreCase(BookingStatus.NEW.getDisplayName());
        long completedCount = bookingRepository.countByStatusIgnoreCase(BookingStatus.COMPLETED.getDisplayName());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("totalIncome", totalIncome);
        stats.put("newCount", newCount);
        stats.put("completedCount", completedCount);
        return stats;
    }

    /**
     * Обновление статуса заявки с автоматическим освобождением слота при отмене.
     */
    @Transactional
    public BookingResponseDto updateStatus(Long id, String newStatus) {
        String cleanStatus = newStatus != null ? newStatus.replace("\"", "").trim() : "";
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        booking.setStatus(cleanStatus);

        // Если заказ отменен, освобождаем занятый слот в расписании в той же транзакции
        if (BookingStatus.CANCELLED.getDisplayName().equalsIgnoreCase(cleanStatus) 
                && booking.getBookingDate() != null && booking.getBookingTime() != null) {
            timeSlotRepository.findBySlotDateAndSlotTime(booking.getBookingDate(), booking.getBookingTime())
                    .ifPresent(slot -> {
                        slot.setBooked(false);
                        timeSlotRepository.save(slot);
                        log.info("Слот {} {} освобожден в связи с отменой заявки #{}", 
                                booking.getBookingDate(), booking.getBookingTime(), id);
                    });
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Статус заявки #{} изменен на '{}'", id, cleanStatus);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Обновление деталей заявки.
     */
    @Transactional
    public BookingResponseDto updateDetails(Long id, String details) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        booking.setDetails(details != null ? details.replace("\"", "").trim() : "");
        Booking saved = bookingRepository.save(booking);
        log.info("Детали заявки #{} обновлены", id);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Редактирование полей заказа администратором.
     */
    @Transactional
    public BookingResponseDto editBooking(Long id, BookingEditRequestDto updated) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        if (updated.getDetails() != null) booking.setDetails(updated.getDetails());
        if (updated.getBookingDate() != null) booking.setBookingDate(updated.getBookingDate());
        if (updated.getBookingTime() != null) booking.setBookingTime(updated.getBookingTime());
        if (updated.getTotalPrice() != null) booking.setTotalPrice(updated.getTotalPrice());

        Booking saved = bookingRepository.save(booking);
        log.info("Данные заявки #{} отредактированы", id);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Сохранение ссылки на готовые фото с валидацией протокола.
     */
    @Transactional
    public BookingResponseDto updateDownloadUrl(Long id, String url) {
        String cleanUrl = url != null ? url.replace("\"", "").trim() : "";

        // Защита от javascript:, data: Stored XSS
        if (!cleanUrl.isEmpty() && !cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            throw new BadRequestException("Недопустимый формат ссылки. Ссылка должна начинаться с https:// или http://");
        }

        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        booking.setDownloadUrl(cleanUrl);
        Booking saved = bookingRepository.save(booking);
        log.info("Ссылка на результат для заявки #{} сохранена", id);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Удаление заявки с освобождением связанного слота.
     */
    @Transactional
    public void deleteBooking(Long id) {
        Booking booking = bookingRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        // Освобождаем занятый слот в расписании (предотвращение мертвой блокировки)
        if (booking.getBookingDate() != null && booking.getBookingTime() != null) {
            timeSlotRepository.findBySlotDateAndSlotTime(booking.getBookingDate(), booking.getBookingTime())
                    .ifPresent(slot -> {
                        slot.setBooked(false);
                        timeSlotRepository.save(slot);
                        log.info("Слот {} {} освобожден при удалении заявки #{}", 
                                booking.getBookingDate(), booking.getBookingTime(), id);
                    });
        }

        bookingRepository.delete(booking);
        log.info("Заявка #{} успешно удалена", id);
    }

    private boolean isPhoneMatching(String phone1, String phone2) {
        if (phone1 == null || phone2 == null) return false;
        String digits1 = phone1.replaceAll("\\D", "");
        String digits2 = phone2.replaceAll("\\D", "");
        if (digits1.isEmpty() || digits2.isEmpty()) return false;
        String sub1 = digits1.length() >= 10 ? digits1.substring(digits1.length() - 10) : digits1;
        String sub2 = digits2.length() >= 10 ? digits2.substring(digits2.length() - 10) : digits2;
        return sub1.equals(sub2);
    }
}

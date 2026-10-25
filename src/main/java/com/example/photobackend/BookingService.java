package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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
    private final PricingService pricingService;
    private final RateLimiterService rateLimiterService;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public BookingService(BookingRepository bookingRepository,
                          TimeSlotRepository timeSlotRepository,
                          PricingService pricingService,
                          RateLimiterService rateLimiterService,
                          ApplicationEventPublisher eventPublisher) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.pricingService = pricingService;
        this.rateLimiterService = rateLimiterService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Атомарное создание заявки со строгой транзакционной целостностью и публикацией асинхронного события.
     */
    @Transactional
    public Map<String, Object> createBooking(BookingCreateRequest req, String clientIp) {
        if (req == null) {
            throw new BadRequestException("Данные заявки не переданы");
        }

        // 1. Серверный расчет стоимости на BigDecimal с банковским округлением
        BigDecimal calculatedPrice = pricingService.calculatePrice(
                req.getShootTypeRate(),
                req.getHours(),
                req.isAddonExpress(),
                req.isAddonStudio(),
                req.getPromoCode()
        );

        // 2. Атомарное бронирование слота на уровне БД (защита от Race Condition)
        TimeSlot linkedSlot = null;
        if (req.getBookingDate() != null && req.getBookingTime() != null) {
            validateFutureBooking(req.getBookingDate(), req.getBookingTime());
            int updatedRows = timeSlotRepository.bookSlotAtomically(req.getBookingDate(), req.getBookingTime());
            if (updatedRows == 0) {
                throw new SlotConflictException("К сожалению, окно на " + req.getBookingTime() + 
                        " только что было забронировано! Пожалуйста, выберите другое свободное время.");
            }
            linkedSlot = timeSlotRepository.findBySlotDateAndSlotTime(req.getBookingDate(), req.getBookingTime()).orElse(null);
        }

        // 3. Создаем сущность со строгой инициализацией полей и 128-битным UUID v4
        String nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String trackingToken = "TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        String publicId = UUID.randomUUID().toString();

        Booking booking = new Booking();
        booking.setPublicId(publicId);
        booking.setClientName(req.getClientName());
        booking.setClientPhone(req.getClientPhone());
        booking.setDetails(req.getDetails());
        booking.setBookingDate(req.getBookingDate());
        booking.setBookingTime(req.getBookingTime());
        booking.setTimeSlot(linkedSlot);
        booking.setPromoCode(req.getPromoCode());
        booking.setTotalPrice(calculatedPrice);
        booking.setStatus(BookingStatus.NEW.getDisplayName());
        booking.setDownloadUrl(null);
        booking.setTrackingToken(trackingToken);
        booking.setCreatedAt(nowStr);
        booking.setClientIp(clientIp);

        Booking saved = bookingRepository.save(booking);
        BookingResponseDto responseDto = BookingResponseDto.fromEntity(saved);

        // 4. Публикуем асинхронное событие приложения для отправки уведомлений после фиксации транзакции
        eventPublisher.publishEvent(new BookingCreatedEvent(responseDto));

        log.info("Создана новая заявка publicId: {} (код: {}) от клиента: {} ({}), IP: {}, время: {}",
                saved.getPublicId(), trackingToken, saved.getClientName(), saved.getClientPhone(), clientIp, nowStr);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("orderId", saved.getPublicId()); // Клиенту отдаем исключительно публичный UUID v4
        resp.put("publicId", saved.getPublicId());
        resp.put("trackingToken", trackingToken);
        resp.put("message", "Заявка успешно отправлена! Номер вашей заявки: #" + saved.getPublicId());
        return resp;
    }

    /**
     * Безопасная проверка статуса заказа клиентом (по токену отслеживания или UUID / телефону).
     */
    public BookingResponseDto checkStatus(String token, String orderIdentifier, String phone, String clientIp) {
        if (!rateLimiterService.tryAcquire("status:" + clientIp, SecurityConstants.STATUS_MAX_REQUESTS, SecurityConstants.STATUS_WINDOW_SECONDS)) {
            throw new TooManyRequestsException("Слишком много запросов. Пожалуйста, подождите минуту.");
        }

        Optional<Booking> bookingOpt = Optional.empty();

        if (token != null && !token.isBlank()) {
            bookingOpt = bookingRepository.findByTrackingToken(token.trim().toUpperCase());
        } else if (orderIdentifier != null && !orderIdentifier.isBlank() && phone != null && !phone.isBlank()) {
            String cleanId = orderIdentifier.trim();
            bookingOpt = bookingRepository.findByPublicId(cleanId);
            if (bookingOpt.isEmpty() && cleanId.matches("\\d+")) {
                bookingOpt = bookingRepository.findById(Long.parseLong(cleanId));
            }
            bookingOpt = bookingOpt.filter(b -> isPhoneMatching(phone, b.getClientPhone()));
        } else {
            throw new BadRequestException("Укажите код отслеживания или номер заявки с телефоном.");
        }

        Booking b = bookingOpt.orElseThrow(() -> new ResourceNotFoundException("Заказ с указанными данными не найден."));
        return BookingResponseDto.fromEntity(b);
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
        BigDecimal totalIncome = bookingRepository.sumTotalIncome();
        long newCount = bookingRepository.countByStatusIgnoreCase(BookingStatus.NEW.getDisplayName());
        long completedCount = bookingRepository.countByStatusIgnoreCase(BookingStatus.COMPLETED.getDisplayName());

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("totalIncome", totalIncome != null ? totalIncome.setScale(2, PricingConstants.BANKING_ROUNDING) : BigDecimal.ZERO);
        stats.put("newCount", newCount);
        stats.put("completedCount", completedCount);
        return stats;
    }

    /**
     * Обновление статуса заявки с автоматическим освобождением слота при отмене.
     */
    @Transactional
    public BookingResponseDto updateStatus(String identifier, String newStatus) {
        String cleanStatus = newStatus != null ? newStatus.replace("\"", "").trim() : "";
        Booking booking = findBookingByIdentifier(identifier);

        booking.setStatus(cleanStatus);

        // Если заказ отменен, освобождаем занятый слот в расписании в той же транзакции
        if (BookingStatus.CANCELLED.getDisplayName().equalsIgnoreCase(cleanStatus)) {
            if (booking.getTimeSlot() != null) {
                TimeSlot slot = booking.getTimeSlot();
                slot.setBooked(false);
                timeSlotRepository.save(slot);
                booking.setTimeSlot(null); // обнуляем ссылку при отмене: исключает коллизии внешнего ключа!
                log.info("Связанный слот освобожден в связи с отменой заявки {}", identifier);
            } else if (booking.getBookingDate() != null && booking.getBookingTime() != null) {
                timeSlotRepository.findBySlotDateAndSlotTime(booking.getBookingDate(), booking.getBookingTime())
                        .ifPresent(slot -> {
                            slot.setBooked(false);
                            timeSlotRepository.save(slot);
                            log.info("Слот {} {} освобожден в связи с отменой заявки {}", 
                                    booking.getBookingDate(), booking.getBookingTime(), identifier);
                        });
            }
        }

        Booking saved = bookingRepository.save(booking);
        log.info("Статус заявки {} изменен на '{}'", identifier, cleanStatus);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Обновление деталей заявки.
     */
    @Transactional
    public BookingResponseDto updateDetails(String identifier, String details) {
        Booking booking = findBookingByIdentifier(identifier);
        booking.setDetails(details != null ? details.replace("\"", "").trim() : "");
        Booking saved = bookingRepository.save(booking);
        log.info("Детали заявки {} обновлены", identifier);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Редактирование полей заказа администратором.
     */
    @Transactional
    public BookingResponseDto editBooking(String identifier, BookingEditRequestDto updated) {
        Booking booking = findBookingByIdentifier(identifier);

        if (updated.getDetails() != null) booking.setDetails(updated.getDetails());
        if (updated.getBookingDate() != null) booking.setBookingDate(updated.getBookingDate());
        if (updated.getBookingTime() != null) booking.setBookingTime(updated.getBookingTime());
        if (updated.getTotalPrice() != null) booking.setTotalPrice(updated.getTotalPrice());

        Booking saved = bookingRepository.save(booking);
        log.info("Данные заявки {} отредактированы", identifier);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Сохранение ссылки на готовые фото с валидацией протокола.
     */
    @Transactional
    public BookingResponseDto updateDownloadUrl(String identifier, String url) {
        String cleanUrl = url != null ? url.replace("\"", "").trim() : "";

        if (!cleanUrl.isEmpty() && !cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            throw new BadRequestException("Недопустимый формат ссылки. Ссылка должна начинаться с https:// или http://");
        }

        Booking booking = findBookingByIdentifier(identifier);
        booking.setDownloadUrl(cleanUrl);
        Booking saved = bookingRepository.save(booking);
        log.info("Ссылка на результат для заявки {} сохранена", identifier);
        return BookingResponseDto.fromEntity(saved);
    }

    /**
     * Удаление заявки с освобождением связанного слота.
     */
    @Transactional
    public void deleteBooking(String identifier) {
        Booking booking = findBookingByIdentifier(identifier);

        if (booking.getTimeSlot() != null) {
            TimeSlot slot = booking.getTimeSlot();
            slot.setBooked(false);
            timeSlotRepository.save(slot);
            booking.setTimeSlot(null);
        } else if (booking.getBookingDate() != null && booking.getBookingTime() != null) {
            timeSlotRepository.findBySlotDateAndSlotTime(booking.getBookingDate(), booking.getBookingTime())
                    .ifPresent(slot -> {
                        slot.setBooked(false);
                        timeSlotRepository.save(slot);
                        log.info("Слот {} {} освобожден при удалении заявки {}", 
                                booking.getBookingDate(), booking.getBookingTime(), identifier);
                    });
        }

        bookingRepository.delete(booking);
        log.info("Заявка {} успешно удалена", identifier);
    }

    private void validateFutureBooking(String dateStr, String timeStr) {
        try {
            java.time.LocalDate bookingDate = java.time.LocalDate.parse(dateStr.trim());
            java.time.ZonedDateTime nowStudio = java.time.ZonedDateTime.now(TimeSlotService.STUDIO_TIMEZONE);
            java.time.LocalDate today = nowStudio.toLocalDate();
            java.time.LocalTime nowTime = nowStudio.toLocalTime();

            if (bookingDate.isBefore(today)) {
                throw new BadRequestException("Невозможно оформить бронирование на прошедшую дату: " + dateStr);
            }
            if (bookingDate.isEqual(today)) {
                java.time.LocalTime startTime = TimeSlotService.extractStartTime(timeStr);
                if (startTime != null && !startTime.isAfter(nowTime)) {
                    throw new BadRequestException("Невозможно оформить бронирование на уже наступившее время: " + timeStr);
                }
            }
        } catch (java.time.format.DateTimeParseException e) {
            throw new BadRequestException("Неверный формат даты бронирования: " + dateStr);
        }
    }

    // Совместимый метод поиска по числовому Long id
    public void deleteBooking(Long id) {
        deleteBooking(String.valueOf(id));
    }

    private Booking findBookingByIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            throw new ResourceNotFoundException("Идентификатор заявки не указан");
        }
        String clean = identifier.trim();
        Optional<Booking> opt = bookingRepository.findByPublicId(clean);
        if (opt.isEmpty() && clean.matches("\\d+")) {
            opt = bookingRepository.findById(Long.parseLong(clean));
        }
        return opt.orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));
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

package com.example.photobackend;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BookingController {

    private static final Logger log = LoggerFactory.getLogger(BookingController.class);

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private RateLimiterService rateLimiterService;

    // --- ПУБЛИЧНЫЕ ЭНДПОИНТЫ ДЛЯ ЦЕН И КАЛЬКУЛЯТОРА ---

    @GetMapping("/pricing/config")
    public ResponseEntity<Map<String, Object>> getPricingConfig() {
        return ResponseEntity.ok(pricingService.getPricingConfig());
    }

    @PostMapping("/pricing/calculate")
    public ResponseEntity<Map<String, Object>> calculatePrice(@RequestBody BookingCreateRequest req) {
        int price = pricingService.calculatePrice(
                req.getShootTypeRate(),
                req.getHours(),
                req.isAddonExpress(),
                req.isAddonStudio(),
                req.getPromoCode()
        );
        Map<String, Object> resp = new HashMap<>();
        resp.put("totalPrice", price);
        return ResponseEntity.ok(resp);
    }

    // --- ВХОД И ВЫХОД АДМИНИСТРАТОРА С ТРОТТЛИНГОМ (RATE LIMITING) ---

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody String password, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        // Защита от перебора паролей: максимум 5 попыток в минуту на реальный IP TCP-сокета
        if (!rateLimiterService.tryAcquire("login:" + clientIp, 5, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Слишком много попыток входа. Пожалуйста, подождите минуту.");
        }

        String cleanPass = password.replace("\"", "").trim();
        String token = adminAuthService.login(cleanPass);
        if (token != null) {
            Map<String, String> resp = new HashMap<>();
            resp.put("token", token);
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Неверный пароль администратора");
    }

    @AdminOnly
    @PostMapping("/admin/logout")
    public ResponseEntity<String> adminLogout(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }
        adminAuthService.logout(token);
        return ResponseEntity.ok("Сессия завершена");
    }

    // --- СОЗДАНИЕ ЗАЯВКИ С АТОМАРНОСТЬЮ (@Transactional), ЛОГИРОВАНИЕМ IP И TRACKING TOKEN ---

    @Transactional
    @PostMapping("/booking")
    public ResponseEntity<Map<String, Object>> createBooking(@RequestBody BookingCreateRequest req, HttpServletRequest request) {
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
                Map<String, Object> err = new HashMap<>();
                err.put("error", "К сожалению, окно на " + req.getBookingTime() + " только что было забронировано! Пожалуйста, выберите другое свободное время.");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
            }
        }

        // 3. Создаем сущность со строгой инициализацией полей (Mass Assignment Protection)
        String clientIp = getClientIp(request);
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
        booking.setStatus("Новая");      // Клиент никогда не может выставить произвольный статус
        booking.setDownloadUrl(null);    // Клиент никогда не может внедрить javascript: URL
        booking.setTrackingToken(trackingToken);
        booking.setCreatedAt(nowStr);
        booking.setClientIp(clientIp);

        bookingRepository.save(booking);
        notificationService.notifyNewBooking(booking);

        log.info("Создана новая заявка #{} (код: {}) от клиента: {} ({}), IP: {}, время: {}",
                booking.getId(), trackingToken, booking.getClientName(), booking.getClientPhone(), clientIp, nowStr);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("orderId", booking.getId());
        resp.put("trackingToken", trackingToken);
        resp.put("message", "Заявка успешно отправлена! Номер вашей заявки: #" + booking.getId());
        return ResponseEntity.ok(resp);
    }

    // --- БЕЗОПАСНАЯ ПРОВЕРКА СТАТУСА (ПО TRACKING TOKEN ИЛИ ORDER ID + ТЕЛЕФОН) ---

    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(@RequestParam(required = false) String token,
                                         @RequestParam(required = false) Long orderId,
                                         @RequestParam(required = false) String phone,
                                         HttpServletRequest request) {
        String clientIp = getClientIp(request);
        // Защита от перебора: максимум 10 запросов в минуту на реальный IP сокета
        if (!rateLimiterService.tryAcquire("status:" + clientIp, 10, 60)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Слишком много запросов. Пожалуйста, подождите минуту.");
        }

        Optional<Booking> bookingOpt = Optional.empty();

        // 1. Поиск по уникальному коду отслеживания (максимальное удобство для клиента)
        if (token != null && !token.isBlank()) {
            bookingOpt = bookingRepository.findByTrackingToken(token.trim().toUpperCase());
        }
        // 2. Резервный поиск по номеру заказа и телефону (для клиентов, потерявших код)
        else if (orderId != null && phone != null && !phone.isBlank()) {
            bookingOpt = bookingRepository.findById(orderId)
                    .filter(b -> isPhoneMatching(phone, b.getClientPhone()));
        } else {
            return ResponseEntity.badRequest().body("Укажите код отслеживания или номер заявки с телефоном.");
        }

        if (bookingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Заказ с указанными данными не найден.");
        }

        Booking b = bookingOpt.get();
        String maskedName = BookingStatusResponse.maskName(b.getClientName());
        String downloadUrl = "Готово".equalsIgnoreCase(b.getStatus()) ? b.getDownloadUrl() : null;

        BookingStatusResponse response = new BookingStatusResponse(
                b.getId(),
                b.getTrackingToken(),
                maskedName,
                b.getStatus(),
                b.getBookingDate(),
                b.getBookingTime(),
                downloadUrl
        );

        return ResponseEntity.ok(response);
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

    // Защита от спуфинга: используем прямой IP адрес TCP-сокета без слепого доверия к X-Forwarded-For
    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }

    // --- АДМИНИСТРАТИВНЫЕ МЕТОДЫ (ЗАЩИЩЕНЫ ЧЕРЕЗ @AdminOnly И ПРОВЕРКУ ТОКЕНА) ---

    @AdminOnly
    @GetMapping("/booking")
    public ResponseEntity<?> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен: требуется токен администратора");
        }

        Page<Booking> bookingPage = bookingRepository.findAllByOrderByIdDesc(
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "id"))
        );
        return ResponseEntity.ok(bookingPage);
    }

    @AdminOnly
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен: требуется токен администратора");
        }

        // Быстрые агрегатные SQL-запросы на уровне БД (O(1) по памяти, защита от Heap OutOfMemoryError)
        long totalCount = bookingRepository.countTotalBookings();
        long totalIncome = bookingRepository.sumTotalIncome();
        long newCount = bookingRepository.countByStatusIgnoreCase("Новая");
        long completedCount = bookingRepository.countByStatusIgnoreCase("Готово");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("totalIncome", totalIncome);
        stats.put("newCount", newCount);
        stats.put("completedCount", completedCount);
        return ResponseEntity.ok(stats);
    }

    @AdminOnly
    @Transactional
    @PatchMapping("/booking/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody String newStatus,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        String cleanStatus = newStatus.replace("\"", "").trim();
        return bookingRepository.findById(id).map(b -> {
            b.setStatus(cleanStatus);
            // Если заказ отменен, освобождаем занятый слот в расписании
            if ("Отменена".equalsIgnoreCase(cleanStatus) && b.getBookingDate() != null && b.getBookingTime() != null) {
                timeSlotRepository.findBySlotDateAndSlotTime(b.getBookingDate(), b.getBookingTime())
                        .ifPresent(slot -> {
                            slot.setBooked(false);
                            timeSlotRepository.save(slot);
                        });
            }
            bookingRepository.save(b);
            return ResponseEntity.ok("Статус обновлен");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Заявка не найдена"));
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/details")
    public ResponseEntity<?> updateDetails(
            @PathVariable Long id,
            @RequestBody String details,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        return bookingRepository.findById(id).map(b -> {
            b.setDetails(details.replace("\"", "").trim());
            bookingRepository.save(b);
            return ResponseEntity.ok("Детали обновлены");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Заявка не найдена"));
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/edit")
    public ResponseEntity<?> editBooking(
            @PathVariable Long id,
            @RequestBody Booking updated,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        return bookingRepository.findById(id).map(b -> {
            if (updated.getDetails() != null) b.setDetails(updated.getDetails());
            if (updated.getBookingDate() != null) b.setBookingDate(updated.getBookingDate());
            if (updated.getBookingTime() != null) b.setBookingTime(updated.getBookingTime());
            if (updated.getTotalPrice() != null) b.setTotalPrice(updated.getTotalPrice());
            bookingRepository.save(b);
            return ResponseEntity.ok("Данные заказа обновлены");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Заявка не найдена"));
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/download-url")
    public ResponseEntity<?> updateDownloadUrl(
            @PathVariable Long id,
            @RequestBody String url,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        String cleanUrl = url.replace("\"", "").trim();

        // Валидация протокола: защита от javascript:, vbscript:, data: Stored XSS
        if (!cleanUrl.isEmpty() && !cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            return ResponseEntity.badRequest().body("Недопустимый формат ссылки. Ссылка должна начинаться с https:// или http://");
        }

        return bookingRepository.findById(id).map(b -> {
            b.setDownloadUrl(cleanUrl);
            bookingRepository.save(b);
            return ResponseEntity.ok("Ссылка сохранена");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Заявка не найдена"));
    }

    @AdminOnly
    @Transactional
    @DeleteMapping("/booking/{id}")
    public ResponseEntity<?> deleteBooking(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        Optional<Booking> opt = bookingRepository.findById(id);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Заявка не найдена");
        }

        Booking b = opt.get();
        // Освобождаем занятый слот в time_slots при удалении заказа (предотвращение "мертвой блокировки" расписания)
        if (b.getBookingDate() != null && b.getBookingTime() != null) {
            timeSlotRepository.findBySlotDateAndSlotTime(b.getBookingDate(), b.getBookingTime())
                    .ifPresent(slot -> {
                        slot.setBooked(false);
                        timeSlotRepository.save(slot);
                    });
        }

        bookingRepository.delete(b);
        return ResponseEntity.ok("Заявка удалена");
    }
}

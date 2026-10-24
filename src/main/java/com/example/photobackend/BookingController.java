package com.example.photobackend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;
    private final PricingService pricingService;
    private final AdminAuthService adminAuthService;
    private final RateLimiterService rateLimiterService;

    @Autowired
    public BookingController(BookingService bookingService,
                             PricingService pricingService,
                             AdminAuthService adminAuthService,
                             RateLimiterService rateLimiterService) {
        this.bookingService = bookingService;
        this.pricingService = pricingService;
        this.adminAuthService = adminAuthService;
        this.rateLimiterService = rateLimiterService;
    }

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

    // --- ВХОД И ВЫХОД АДМИНИСТРАТОРА С ТРОТТЛИНГОМ ---

    @PostMapping("/admin/login")
    public ResponseEntity<?> adminLogin(@RequestBody String password, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        if (!rateLimiterService.tryAcquire("login:" + clientIp, 
                SecurityConstants.LOGIN_MAX_REQUESTS, 
                SecurityConstants.LOGIN_WINDOW_SECONDS)) {
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
    public ResponseEntity<String> adminLogout(@RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {
        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }
        adminAuthService.logout(token);
        return ResponseEntity.ok("Сессия завершена");
    }

    // --- СОЗДАНИЕ ЗАЯВКИ С ВАЛИДАЦИЕЙ DTO ---

    @PostMapping("/booking")
    public ResponseEntity<Map<String, Object>> createBooking(@Valid @RequestBody BookingCreateRequest req, HttpServletRequest request) {
        String clientIp = getClientIp(request);
        Map<String, Object> response = bookingService.createBooking(req, clientIp);
        return ResponseEntity.ok(response);
    }

    // --- БЕЗОПАСНАЯ ПРОВЕРКА СТАТУСА ---

    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(@RequestParam(required = false) String token,
                                         @RequestParam(required = false) Long orderId,
                                         @RequestParam(required = false) String phone,
                                         HttpServletRequest request) {
        String clientIp = getClientIp(request);
        try {
            BookingStatusResponse response = bookingService.checkStatus(token, orderId, phone, clientIp);
            return ResponseEntity.ok(response);
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (TooManyRequestsException e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(e.getMessage());
        }
    }

    // --- АДМИНИСТРАТИВНЫЕ МЕТОДЫ (ЗАЩИЩЕНЫ ЧЕРЕЗ @AdminOnly) ---

    @AdminOnly
    @GetMapping("/booking")
    public ResponseEntity<?> getAllBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        Page<BookingResponseDto> bookingPage = bookingService.getAllBookings(
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "id"))
        );
        return ResponseEntity.ok(bookingPage);
    }

    @AdminOnly
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(@RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {
        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        return ResponseEntity.ok(bookingService.getStats());
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable Long id,
            @RequestBody String newStatus,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.updateStatus(id, newStatus);
            return ResponseEntity.ok("Статус обновлен");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/details")
    public ResponseEntity<?> updateDetails(
            @PathVariable Long id,
            @RequestBody String details,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.updateDetails(id, details);
            return ResponseEntity.ok("Детали обновлены");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/edit")
    public ResponseEntity<?> editBooking(
            @PathVariable Long id,
            @RequestBody BookingEditRequestDto updated,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.editBooking(id, updated);
            return ResponseEntity.ok("Данные заказа обновлены");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/download-url")
    public ResponseEntity<?> updateDownloadUrl(
            @PathVariable Long id,
            @RequestBody String url,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.updateDownloadUrl(id, url);
            return ResponseEntity.ok("Ссылка сохранена");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @AdminOnly
    @DeleteMapping("/booking/{id}")
    public ResponseEntity<?> deleteBooking(
            @PathVariable Long id,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.deleteBooking(id);
            return ResponseEntity.ok("Заявка удалена");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return "unknown";
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown";
    }
}

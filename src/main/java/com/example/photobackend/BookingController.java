package com.example.photobackend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Validated
public class BookingController {

    private final BookingService bookingService;
    private final PricingService pricingService;
    private final AdminAuthService adminAuthService;
    private final RateLimiterService rateLimiterService;
    private final ClientIpResolver clientIpResolver;
    private final AppConfigProperties appConfigProperties;

    @Autowired
    public BookingController(BookingService bookingService,
                             PricingService pricingService,
                             AdminAuthService adminAuthService,
                             RateLimiterService rateLimiterService,
                             ClientIpResolver clientIpResolver,
                             AppConfigProperties appConfigProperties) {
        this.bookingService = bookingService;
        this.pricingService = pricingService;
        this.adminAuthService = adminAuthService;
        this.rateLimiterService = rateLimiterService;
        this.clientIpResolver = clientIpResolver;
        this.appConfigProperties = appConfigProperties;
    }

    // --- ПУБЛИЧНЫЕ ЭНДПОИНТЫ ДЛЯ ЦЕН И КАЛЬКУЛЯТОРА ---

    @GetMapping("/pricing/config")
    public ResponseEntity<Map<String, Object>> getPricingConfig() {
        return ResponseEntity.ok(pricingService.getPricingConfig());
    }

    @PostMapping("/pricing/calculate")
    public ResponseEntity<Map<String, Object>> calculatePrice(@Valid @RequestBody BookingCreateRequest req) {
        java.math.BigDecimal price = pricingService.calculatePrice(
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
    public ResponseEntity<?> adminLogin(@RequestBody String password,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        String clientIp = clientIpResolver.resolveClientIp(request);
        if (!rateLimiterService.tryAcquire("login:" + clientIp, 
                SecurityConstants.LOGIN_MAX_REQUESTS, 
                SecurityConstants.LOGIN_WINDOW_SECONDS)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Слишком много попыток входа. Пожалуйста, подождите минуту.");
        }

        String cleanPass = password.replace("\"", "").trim();
        String token = adminAuthService.login(cleanPass);
        if (token != null) {
            boolean secure = appConfigProperties != null && appConfigProperties.getSecurity().isCookieSecure();
            ResponseCookie cookie = ResponseCookie.from("admin_token", token)
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite("Strict")
                    .path("/")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

            Map<String, String> resp = new HashMap<>();
            resp.put("token", token);
            return ResponseEntity.ok(resp);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Неверный пароль администратора");
    }

    public ResponseEntity<?> adminLogin(String password, HttpServletRequest request) {
        return adminLogin(password, request, null);
    }

    @AdminOnly
    @PostMapping("/admin/logout")
    public ResponseEntity<String> adminLogout(
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request,
            HttpServletResponse response) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        if (activeToken != null) {
            adminAuthService.logout(activeToken);
        }

        if (response != null) {
            boolean secure = appConfigProperties != null && appConfigProperties.getSecurity().isCookieSecure();
            ResponseCookie cookie = ResponseCookie.from("admin_token", "")
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite("Strict")
                    .path("/")
                    .maxAge(0)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        return ResponseEntity.ok("Сессия завершена");
    }

    public ResponseEntity<String> adminLogout(String token) {
        return adminLogout(token, null, null);
    }

    // --- СОЗДАНИЕ ЗАЯВКИ С ВАЛИДАЦИЕЙ DTO ---

    @PostMapping("/booking")
    public ResponseEntity<Map<String, Object>> createBooking(@Valid @RequestBody BookingCreateRequest req, HttpServletRequest request) {
        String clientIp = clientIpResolver.resolveClientIp(request);
        Map<String, Object> response = bookingService.createBooking(req, clientIp);
        return ResponseEntity.ok(response);
    }

    // --- БЕЗОПАСНАЯ ПРОВЕРКА СТАТУСА ---

    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(@RequestParam(required = false) String token,
                                         @RequestParam(required = false) String orderId,
                                         @RequestParam(required = false) String phone,
                                         HttpServletRequest request) {
        String clientIp = clientIpResolver.resolveClientIp(request);
        try {
            BookingResponseDto response = bookingService.checkStatus(token, orderId, phone, clientIp);
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
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        Page<BookingResponseDto> bookingPage = bookingService.getAllBookings(
                PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "id"))
        );
        return ResponseEntity.ok(bookingPage);
    }

    public ResponseEntity<?> getAllBookings(int page, int size, String token) {
        return getAllBookings(page, size, token, null);
    }

    @AdminOnly
    @GetMapping("/stats")
    public ResponseEntity<?> getStats(
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        return ResponseEntity.ok(bookingService.getStats());
    }

    public ResponseEntity<?> getStats(String token) {
        return getStats(token, null);
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/status")
    public ResponseEntity<?> updateStatus(
            @PathVariable String id,
            @RequestBody String newStatus,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.updateStatus(id, newStatus);
            return ResponseEntity.ok("Статус обновлен");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    public ResponseEntity<?> updateStatus(String id, String newStatus, String token) {
        return updateStatus(id, newStatus, token, null);
    }

    public ResponseEntity<?> updateStatus(Long id, String newStatus, String token) {
        return updateStatus(String.valueOf(id), newStatus, token, null);
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/details")
    public ResponseEntity<?> updateDetails(
            @PathVariable String id,
            @RequestBody String details,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.updateDetails(id, details);
            return ResponseEntity.ok("Детали обновлены");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    public ResponseEntity<?> updateDetails(String id, String details, String token) {
        return updateDetails(id, details, token, null);
    }

    public ResponseEntity<?> updateDetails(Long id, String details, String token) {
        return updateDetails(String.valueOf(id), details, token, null);
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/edit")
    public ResponseEntity<?> editBooking(
            @PathVariable String id,
            @Valid @RequestBody BookingEditRequestDto updated,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.editBooking(id, updated);
            return ResponseEntity.ok("Данные заказа обновлены");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    public ResponseEntity<?> editBooking(String id, BookingEditRequestDto updated, String token) {
        return editBooking(id, updated, token, null);
    }

    public ResponseEntity<?> editBooking(Long id, BookingEditRequestDto updated, String token) {
        return editBooking(String.valueOf(id), updated, token, null);
    }

    @AdminOnly
    @PatchMapping("/booking/{id}/download-url")
    public ResponseEntity<?> updateDownloadUrl(
            @PathVariable String id,
            @RequestBody String url,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
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

    public ResponseEntity<?> updateDownloadUrl(String id, String url, String token) {
        return updateDownloadUrl(id, url, token, null);
    }

    public ResponseEntity<?> updateDownloadUrl(Long id, String url, String token) {
        return updateDownloadUrl(String.valueOf(id), url, token, null);
    }

    @AdminOnly
    @DeleteMapping("/booking/{id}")
    public ResponseEntity<?> deleteBooking(
            @PathVariable String id,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        try {
            bookingService.deleteBooking(id);
            return ResponseEntity.ok("Заявка удалена");
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    public ResponseEntity<?> deleteBooking(String id, String token) {
        return deleteBooking(id, token, null);
    }

    public ResponseEntity<?> deleteBooking(Long id, String token) {
        return deleteBooking(String.valueOf(id), token, null);
    }

    private String resolveToken(HttpServletRequest request, String headerToken) {
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }
        return request != null ? AdminAuthInterceptor.extractToken(request) : null;
    }
}

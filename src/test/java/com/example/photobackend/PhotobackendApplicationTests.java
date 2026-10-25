package com.example.photobackend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PhotobackendApplicationTests {

    @Autowired
    private AdminAuthService adminAuthService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private BookingController bookingController;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private ReviewController reviewController;

    @Autowired
    private TimeSlotController timeSlotController;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private RateLimiterService rateLimiterService;

    @Autowired
    private AdminPageController adminPageController;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Test
    void contextLoads() {
    }

    @Test
    void testAdminAuthService() {
        // Неверный пароль не дает токен
        assertNull(adminAuthService.login("wrong-password"));

        // Статический дефолтный пароль 'admin2026' не подходит, если настроен безопасный пароль
        assertNull(adminAuthService.login("admin2026"));

        // Верный сконфигурированный пароль выдает токен
        String token = adminAuthService.login("testAdminPass123");
        assertNotNull(token);
        assertTrue(adminAuthService.isValid(token));

        // Проверка невалидного токена
        assertFalse(adminAuthService.isValid("fake-token"));
        assertFalse(adminAuthService.isValid("fake.token"));

        // Логаут делает токен недействительным
        adminAuthService.logout(token);
        assertFalse(adminAuthService.isValid(token));
    }

    @Test
    void testPricingService() {
        // 1000 * 2 часа = 2000
        int p1 = pricingService.calculatePrice(1000, 2, false, false, null);
        assertEquals(2000, p1);

        // 1000 * 2 + 1000 (express) + 500 (studio) = 3500
        int p2 = pricingService.calculatePrice(1000, 2, true, true, null);
        assertEquals(3500, p2);

        // Скидка 10% (START10) на 2000 = 1800
        int p3 = pricingService.calculatePrice(1000, 2, false, false, "START10");
        assertEquals(1800, p3);

        // Скидка 500 руб (FOTO500) на 2000 = 1500
        int p4 = pricingService.calculatePrice(1000, 2, false, false, "FOTO500");
        assertEquals(1500, p4);
    }

    @Test
    void testPiiMasking() {
        assertEquals("Алексей С.", BookingResponseDto.maskName("Алексей Смирнов"));
        assertEquals("Иван", BookingResponseDto.maskName("Иван"));
        assertEquals("Клиент", BookingResponseDto.maskName(null));
        assertEquals("Клиент", BookingResponseDto.maskName("   "));
    }

    @Test
    void testSecureStatusCheckRequiresMatchingPhoneAndOrderId() {
        // Создаем тестовое бронирование
        Booking booking = new Booking();
        booking.setClientName("Мария Иванова");
        booking.setClientPhone("+7 (999) 111-22-33");
        booking.setStatus("Готово");
        booking.setDownloadUrl("https://disk.yandex.ru/d/secret-photos-link");
        booking.setTrackingToken("TRK-MARIA001");
        booking = bookingRepository.save(booking);

        Long orderId = booking.getId();

        // 1. Попытка запросить с чужим номером телефона -> 404 (доступ запрещен)
        ResponseEntity<?> failedResp = bookingController.checkStatus(null, String.valueOf(orderId), "+7 (999) 000-00-00", null);
        assertEquals(HttpStatus.NOT_FOUND, failedResp.getStatusCode());

        // 2. Попытка запросить с несуществующим ID -> 404
        ResponseEntity<?> notFoundResp = bookingController.checkStatus(null, "999999", "+7 (999) 111-22-33", null);
        assertEquals(HttpStatus.NOT_FOUND, notFoundResp.getStatusCode());

        // 3. Запрос с правильным ID и телефоном (даже в другом формате 89991112233) -> 200 OK
        ResponseEntity<?> successResp = bookingController.checkStatus(null, String.valueOf(orderId), "89991112233", null);
        assertEquals(HttpStatus.OK, successResp.getStatusCode());
        assertTrue(successResp.getBody() instanceof BookingResponseDto);

        BookingResponseDto status = (BookingResponseDto) successResp.getBody();
        assertEquals("Мария И.", status.getMaskedClientName());
        assertEquals("https://disk.yandex.ru/d/secret-photos-link", status.getDownloadUrl());

        // 4. Запрос в 1 клик по trackingToken (удобство для клиента) -> 200 OK
        ResponseEntity<?> tokenResp = bookingController.checkStatus("TRK-MARIA001", null, null, null);
        assertEquals(HttpStatus.OK, tokenResp.getStatusCode());
        BookingResponseDto tokenStatus = (BookingResponseDto) tokenResp.getBody();
        assertEquals("Мария И.", tokenStatus.getMaskedClientName());
        assertEquals("TRK-MARIA001", tokenStatus.getTrackingToken());
    }

    @Test
    void testMassAssignmentAndDownloadUrlXssPrevention() {
        String token = adminAuthService.login("testAdminPass123");

        BookingCreateRequest req = new BookingCreateRequest();
        req.setClientName("Алиса Тестовая");
        req.setClientPhone("+79998887766");
        req.setDetails("Тест");
        req.setHours(2);
        req.setShootTypeRate(1000);

        var resp = bookingController.createBooking(req, null);
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().containsKey("trackingToken"));

        // Проверяем, что в БД создалась запись со статусом "Новая", downloadUrl == null, и с логированием createdAt / clientIp
        Booking saved = bookingRepository.findAll().stream()
                .filter(b -> "+79998887766".equals(b.getClientPhone()))
                .findFirst().orElseThrow();

        assertEquals("Новая", saved.getStatus());
        assertNull(saved.getDownloadUrl());
        assertNotNull(saved.getTrackingToken());
        assertTrue(saved.getTrackingToken().startsWith("TRK-"));
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getClientIp());

        // Проверяем попытку обновить downloadUrl на вредоносный javascript: -> 400 Bad Request
        ResponseEntity<?> badUrlResp = bookingController.updateDownloadUrl(saved.getId(), "javascript:alert('xss')", token);
        assertEquals(HttpStatus.BAD_REQUEST, badUrlResp.getStatusCode());

        // Проверяем валидный URL -> 200 OK
        ResponseEntity<?> goodUrlResp = bookingController.updateDownloadUrl(saved.getId(), "https://cloud.mail.ru/public/123", token);
        assertEquals(HttpStatus.OK, goodUrlResp.getStatusCode());
    }

    @Test
    void testDatabaseAggregateStats() {
        String token = adminAuthService.login("testAdminPass123");

        long total = bookingRepository.countTotalBookings();
        assertTrue(total >= 0);
        java.math.BigDecimal income = bookingRepository.sumTotalIncome();
        assertNotNull(income);
        assertTrue(income.compareTo(java.math.BigDecimal.ZERO) >= 0);
        long newCount = bookingRepository.countByStatusIgnoreCase("Новая");
        assertTrue(newCount >= 0);

        ResponseEntity<?> statsResp = bookingController.getStats(token);
        assertEquals(HttpStatus.OK, statsResp.getStatusCode());
    }

    @Test
    void testRateLimiterService() {
        RateLimiterService limiter = new RateLimiterService();
        String testIp = "test-ip-123";
        for (int i = 0; i < 5; i++) {
            assertTrue(limiter.tryAcquire(testIp, 5, 60));
        }
        // 6-й запрос блокируется
        assertFalse(limiter.tryAcquire(testIp, 5, 60));
    }

    @Test
    void testReviewCreationValidationAndMergePrevention() {
        // Создаем существующий отзыв #1
        Review initial = new Review("Сергей", 5, "Отличный фотограф!");
        initial.setApproved(true);
        initial = reviewRepository.save(initial);
        Long existingId = initial.getId();

        // Попытка передать слишком длинный текст комментария (> 2000 символов)
        ReviewCreateRequest longReq = new ReviewCreateRequest();
        longReq.setAuthorName("Спамер");
        longReq.setRating(5);
        longReq.setComment("A".repeat(2001));
        ResponseEntity<?> failResp = reviewController.createReview(longReq);
        assertEquals(HttpStatus.BAD_REQUEST, failResp.getStatusCode());

        // Создаем валидный отзыв
        ReviewCreateRequest validReq = new ReviewCreateRequest();
        validReq.setAuthorName("Елена");
        validReq.setRating(5);
        validReq.setComment("Все понравилось");
        ResponseEntity<?> okResp = reviewController.createReview(validReq);
        assertEquals(HttpStatus.OK, okResp.getStatusCode());

        // Проверяем, что исходный отзыв #1 не был перезаписан
        Review notOverwritten = reviewRepository.findById(existingId).orElseThrow();
        assertEquals("Сергей", notOverwritten.getAuthorName());
        assertEquals("Отличный фотограф!", notOverwritten.getComment());
        assertTrue(notOverwritten.isApproved());
    }

    @Test
    void testSlotReleaseOnBookingDelete() {
        String token = adminAuthService.login("testAdminPass123");

        String testDate = "2026-12-31";
        String testTime = "15:00";

        // Создаем занятый слот
        TimeSlot slot = new TimeSlot(testDate, testTime);
        slot.setBooked(true);
        slot = timeSlotRepository.save(slot);

        // Создаем заказ на эту дату и время
        Booking booking = new Booking("Тестовый Клиент", "+79990001122", "Съемка", 3000, testDate, testTime);
        booking = bookingRepository.save(booking);

        // Удаляем заказ администратором
        ResponseEntity<?> deleteResp = bookingController.deleteBooking(booking.getId(), token);
        assertEquals(HttpStatus.OK, deleteResp.getStatusCode());

        // Проверяем, что слот в time_slots освободился
        TimeSlot updatedSlot = timeSlotRepository.findById(slot.getId()).orElseThrow();
        assertFalse(updatedSlot.isBooked(), "Слот должен быть освобожден (isBooked = false) после удаления заказа");
    }

    @Autowired
    private TimeSlotService timeSlotService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private BookingService bookingService;

    @Test
    void testTimeSlotServiceAndDtoReturns() {
        String testDate = "2026-11-20";
        timeSlotRepository.save(new TimeSlot(testDate, "10:00"));
        timeSlotRepository.save(new TimeSlot(testDate, "12:00"));

        var availableSlots = timeSlotService.getAvailableSlots(testDate);
        assertNotNull(availableSlots);
        assertFalse(availableSlots.isEmpty());
        // Проверяем, что возвращаются DTO, а не JPA entities
        assertTrue(availableSlots.get(0) instanceof TimeSlotResponseDto);
        assertEquals(testDate, availableSlots.get(0).getSlotDate());

        // Проверка выборки по диапазону дат
        var rangeSlots = timeSlotService.getSlotsByDateRange("2026-11-01", "2026-11-30", false);
        assertNotNull(rangeSlots);
        assertTrue(rangeSlots.stream().anyMatch(s -> s.getSlotDate().equals(testDate)));
    }

    @Test
    void testReviewServiceReturnsDtos() {
        var reviews = reviewService.getApprovedReviews();
        assertNotNull(reviews);
        if (!reviews.isEmpty()) {
            assertTrue(reviews.get(0) instanceof ReviewResponseDto);
        }
    }

    @Test
    void testRateLimiterEvictionDoesNotCrash() {
        rateLimiterService.tryAcquire("test-evict-key", 5, 1);
        rateLimiterService.evictExpiredEntries();
        adminAuthService.evictExpiredTokens();
    }

    @Test
    void testConstantTimeTokenComparison() {
        assertTrue(AdminAuthService.constantTimeEquals("token123", "token123"));
        assertFalse(AdminAuthService.constantTimeEquals("token123", "token124"));
        assertFalse(AdminAuthService.constantTimeEquals("token123", "token1234"));
        assertFalse(AdminAuthService.constantTimeEquals(null, "token123"));
        assertFalse(AdminAuthService.constantTimeEquals("token123", null));
        assertFalse(AdminAuthService.constantTimeEquals(null, null));
    }

    @Test
    void testTimeSlotStartBeforeEndValidation() {
        String token = adminAuthService.login("testAdminPass123");
        TimeSlotRequestDto invalidSlot = new TimeSlotRequestDto("2026-12-25", "18:00 - 15:00");
        ResponseEntity<?> resp = timeSlotController.addSlot(invalidSlot, token);
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
    }

    @Test
    void testProtectedAdminPagesRequireAuthentication() {
        MockHttpServletRequest unauthReq = new MockHttpServletRequest();
        ResponseEntity<Resource> unauthPageResp = adminPageController.getAdminPage(unauthReq);
        assertEquals(HttpStatus.OK, unauthPageResp.getStatusCode());
        assertNotNull(unauthPageResp.getBody());
        assertEquals("login.html", unauthPageResp.getBody().getFilename());

        ResponseEntity<?> unauthJsResp = adminPageController.getAdminJs(unauthReq);
        assertEquals(HttpStatus.FORBIDDEN, unauthJsResp.getStatusCode());

        // Авторизованный запрос с кукой admin_token
        String token = adminAuthService.login("testAdminPass123");
        MockHttpServletRequest authReq = new MockHttpServletRequest();
        authReq.setCookies(new Cookie("admin_token", token));

        ResponseEntity<Resource> authPageResp = adminPageController.getAdminPage(authReq);
        assertEquals(HttpStatus.OK, authPageResp.getStatusCode());
        assertNotNull(authPageResp.getBody());
        assertEquals("admin.html", authPageResp.getBody().getFilename());

        ResponseEntity<?> authJsResp = adminPageController.getAdminJs(authReq);
        assertEquals(HttpStatus.OK, authJsResp.getStatusCode());
    }

    @Test
    void testAdminLoginSetsHttpOnlyCookie() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRemoteAddr("127.0.0.1");
        MockHttpServletResponse res = new MockHttpServletResponse();

        ResponseEntity<?> loginResp = bookingController.adminLogin("\"testAdminPass123\"", req, res);
        assertEquals(HttpStatus.OK, loginResp.getStatusCode());

        String setCookie = res.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("admin_token="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("SameSite=Strict"));
        assertTrue(setCookie.contains("Path=/"));
    }

    @Test
    void testTrustedProxyIpSpoofingProtection() {
        // Запрос через доверенный локальный сокет (127.0.0.1) доверяет X-Forwarded-For
        MockHttpServletRequest trustedReq = new MockHttpServletRequest();
        trustedReq.setRemoteAddr("127.0.0.1");
        trustedReq.addHeader("X-Forwarded-For", "203.0.113.195, 10.0.0.1");
        assertEquals("203.0.113.195", clientIpResolver.resolveClientIp(trustedReq));

        // Прямой запрос от недоверенного внешнего сокета игнорирует поддельный X-Forwarded-For
        MockHttpServletRequest untrustedReq = new MockHttpServletRequest();
        untrustedReq.setRemoteAddr("198.51.100.5");
        untrustedReq.addHeader("X-Forwarded-For", "1.2.3.4");
        assertEquals("198.51.100.5", clientIpResolver.resolveClientIp(untrustedReq));
    }

    @Test
    void testOverlappingSlotsDetection() {
        String token = adminAuthService.login("testAdminPass123");
        String date = "2026-11-20";

        timeSlotController.addSlot(new TimeSlotRequestDto(date, "13:00 - 15:00"), token);

        // Пересекающийся слот должен завершиться с ошибкой 409 Conflict
        ResponseEntity<?> overlapResp = timeSlotController.addSlot(new TimeSlotRequestDto(date, "14:00 - 16:00"), token);
        assertEquals(HttpStatus.CONFLICT, overlapResp.getStatusCode());
        assertTrue(overlapResp.getBody().toString().contains("пересекается"));
    }
}

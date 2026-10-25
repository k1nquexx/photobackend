package com.example.photobackend;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final AdminAuthService adminAuthService;

    @Autowired
    public ReviewController(ReviewService reviewService, AdminAuthService adminAuthService) {
        this.reviewService = reviewService;
        this.adminAuthService = adminAuthService;
    }

    /**
     * Публичный эндпоинт: возвращает DTO только одобренных отзывов для сайта.
     */
    @GetMapping
    public ResponseEntity<List<ReviewResponseDto>> getApprovedReviews() {
        return ResponseEntity.ok(reviewService.getApprovedReviews());
    }

    /**
     * Для панели администратора: закрытый список отзывов с пагинацией (возвращает Page<ReviewResponseDto>).
     */
    @AdminOnly
    @GetMapping("/all")
    public ResponseEntity<?> getAllReviewsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        Page<ReviewResponseDto> reviewPage = reviewService.getAllReviewsForAdmin(
                PageRequest.of(page, Math.min(size, 100))
        );
        return ResponseEntity.ok(reviewPage);
    }

    public ResponseEntity<?> getAllReviewsForAdmin(int page, int size, String token) {
        return getAllReviewsForAdmin(page, size, token, null);
    }

    /**
     * Добавление отзыва клиентом с автоматической валидацией DTO (@Valid).
     */
    @PostMapping
    public ResponseEntity<?> createReview(@Valid @RequestBody ReviewCreateRequest req) {
        try {
            ReviewResponseDto created = reviewService.createReview(req);
            return ResponseEntity.ok(created);
        } catch (BadRequestException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Одобрение отзыва администратором.
     */
    @AdminOnly
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveReview(
            @PathVariable Long id,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        reviewService.approveReview(id);
        return ResponseEntity.ok("Отзыв одобрен");
    }

    public ResponseEntity<?> approveReview(Long id, String token) {
        return approveReview(id, token, null);
    }

    /**
     * Отклонение / удаление отзыва администратором.
     */
    @AdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long id,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token,
            HttpServletRequest request) {

        String activeToken = resolveToken(request, token);
        if (!adminAuthService.isValid(activeToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        reviewService.deleteReview(id);
        return ResponseEntity.ok("Отзыв удален");
    }

    public ResponseEntity<?> deleteReview(Long id, String token) {
        return deleteReview(id, token, null);
    }

    private String resolveToken(HttpServletRequest request, String headerToken) {
        if (headerToken != null && !headerToken.isBlank()) {
            return headerToken.trim();
        }
        return request != null ? AdminAuthInterceptor.extractToken(request) : null;
    }
}

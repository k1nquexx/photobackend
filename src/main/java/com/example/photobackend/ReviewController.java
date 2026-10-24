package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private AdminAuthService adminAuthService;

    // Публичный эндпоинт: возвращает только одобренные отзывы для сайта
    @GetMapping
    public List<Review> getApprovedReviews() {
        return reviewRepository.findByApprovedTrue();
    }

    // Для панели администратора: закрытый список отзывов с пагинацией
    @AdminOnly
    @GetMapping("/all")
    public ResponseEntity<?> getAllReviewsForAdmin(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен: требуется токен администратора");
        }

        Page<Review> reviewPage = reviewRepository.findAllByOrderByIdDesc(
                PageRequest.of(page, Math.min(size, 100))
        );
        return ResponseEntity.ok(reviewPage);
    }

    // Добавление отзыва клиентом (DTO + валидация длины + защита от MERGE/перезаписи)
    @PostMapping
    public ResponseEntity<?> createReview(@RequestBody ReviewCreateRequest req) {
        if (req == null) {
            return ResponseEntity.badRequest().body("Данные отзыва не переданы");
        }

        String name = req.getAuthorName() != null ? req.getAuthorName().trim() : "";
        String comment = req.getComment() != null ? req.getComment().trim() : "";

        if (name.isEmpty() || name.length() > 100) {
            return ResponseEntity.badRequest().body("Имя автора должно содержать от 1 до 100 символов");
        }
        if (comment.isEmpty() || comment.length() > 2000) {
            return ResponseEntity.badRequest().body("Текст отзыва должен содержать от 1 до 2000 символов");
        }

        int rating = req.getRating() != null ? req.getRating() : 5;
        if (rating < 1) rating = 1;
        if (rating > 5) rating = 5;

        // Создаем новую запись со строгим id = null (защита от перезаписи существующих отзывов)
        Review review = new Review();
        review.setId(null);
        review.setAuthorName(name);
        review.setComment(comment);
        review.setRating(rating);
        review.setApproved(false);
        review.setCreatedAt(LocalDate.now().toString());

        reviewRepository.save(review);
        return ResponseEntity.ok(review);
    }

    // Одобрение отзыва администратором
    @AdminOnly
    @PatchMapping("/{id}/approve")
    public ResponseEntity<?> approveReview(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        return reviewRepository.findById(id).map(r -> {
            r.setApproved(true);
            reviewRepository.save(r);
            return ResponseEntity.ok("Отзыв одобрен");
        }).orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).body("Отзыв не найден"));
    }

    // Отклонение / удаление отзыва
    @AdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReview(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен");
        }

        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
            return ResponseEntity.ok("Отзыв удален");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Отзыв не найден");
    }
}

package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@CrossOrigin(origins = "*")
public class ReviewController {

    @Autowired
    private ReviewRepository reviewRepository;

    // Публичный эндпоинт: возвращает только одобренные отзывы для сайта
    @GetMapping
    public List<Review> getApprovedReviews() {
        return reviewRepository.findByApprovedTrue();
    }

    // Для панели администратора: возвращает абсолютно все отзывы (включая ожидающие)
    @GetMapping("/all")
    public List<Review> getAllReviewsForAdmin() {
        return reviewRepository.findAll();
    }

    // Добавление отзыва клиентом (по умолчанию approved = false)
    @PostMapping
    public Review createReview(@RequestBody Review review) {
        if (review.getRating() < 1) review.setRating(1);
        if (review.getRating() > 5) review.setRating(5);
        review.setApproved(false); // Требует подтверждения администратором
        return reviewRepository.save(review);
    }

    // Одобрение отзыва администратором
    @PatchMapping("/{id}/approve")
    public String approveReview(@PathVariable Long id) {
        return reviewRepository.findById(id).map(r -> {
            r.setApproved(true);
            reviewRepository.save(r);
            return "Отзыв одобрен";
        }).orElse("Отзыв не найден");
    }

    // Отклонение / удаление отзыва
    @DeleteMapping("/{id}")
    public String deleteReview(@PathVariable Long id) {
        if (reviewRepository.existsById(id)) {
            reviewRepository.deleteById(id);
            return "Отзыв удален";
        }
        return "Отзыв не найден";
    }
}

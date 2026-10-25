package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;

    @Autowired
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    /**
     * Публичный список одобренных отзывов для сайта (ограниченная выборка для защиты от OOM).
     */
    public List<ReviewResponseDto> getApprovedReviews() {
        return reviewRepository.findTop50ByApprovedTrueOrderByIdDesc()
                .stream()
                .map(ReviewResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Постраничный список отзывов для панели администратора.
     */
    public Page<ReviewResponseDto> getAllReviewsForAdmin(Pageable pageable) {
        return reviewRepository.findAllByOrderByIdDesc(pageable)
                .map(ReviewResponseDto::fromEntity);
    }

    /**
     * Создание нового отзыва клиентом с премодерацией.
     */
    @Transactional
    public ReviewResponseDto createReview(ReviewCreateRequest req) {
        if (req == null) {
            throw new BadRequestException("Данные отзыва не переданы");
        }

        String name = req.getAuthorName() != null ? req.getAuthorName().trim() : "";
        String comment = req.getComment() != null ? req.getComment().trim() : "";

        if (name.length() < 2 || name.length() > 100) {
            throw new BadRequestException("Имя автора должно содержать от 2 до 100 символов");
        }
        if (comment.length() < 3 || comment.length() > 2000) {
            throw new BadRequestException("Текст отзыва должен содержать от 3 до 2000 символов");
        }
        if (req.getRating() == null || req.getRating() < 1 || req.getRating() > 5) {
            throw new BadRequestException("Оценка должна быть в диапазоне от 1 до 5");
        }

        int rating = req.getRating();

        Review review = new Review();
        review.setId(null); // Защита от перезаписи через merge
        review.setAuthorName(name);
        review.setComment(comment);
        review.setRating(rating);
        review.setApproved(false);
        review.setCreatedAt(LocalDate.now().toString());

        Review saved = reviewRepository.save(review);
        log.info("Создан новый отзыв #{} от '{}', рейтинг: {}", saved.getId(), name, rating);
        return ReviewResponseDto.fromEntity(saved);
    }

    /**
     * Одобрение отзыва администратором.
     */
    @Transactional
    public ReviewResponseDto approveReview(Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Отзыв не найден"));

        review.setApproved(true);
        Review saved = reviewRepository.save(review);
        log.info("Отзыв #{} одобрен администратором", id);
        return ReviewResponseDto.fromEntity(saved);
    }

    /**
     * Удаление отзыва администратором.
     */
    @Transactional
    public void deleteReview(Long id) {
        if (!reviewRepository.existsById(id)) {
            throw new ResourceNotFoundException("Отзыв не найден");
        }
        reviewRepository.deleteById(id);
        log.info("Отзыв #{} удален", id);
    }
}

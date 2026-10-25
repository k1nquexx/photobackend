package com.example.photobackend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceUnitTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    void getApprovedReviews_ReturnsOnlyApprovedMappedDtos() {
        Review r1 = new Review("Алина", 5, "Великолепно!");
        r1.setId(1L);
        r1.setApproved(true);
        r1.setCreatedAt("2026-10-01");

        when(reviewRepository.findTop50ByApprovedTrueOrderByIdDesc())
                .thenReturn(List.of(r1));

        List<ReviewResponseDto> reviews = reviewService.getApprovedReviews();

        assertNotNull(reviews);
        assertEquals(1, reviews.size());
        assertEquals("Алина", reviews.get(0).getAuthorName());
        assertEquals(5, reviews.get(0).getRating());
        assertEquals("Великолепно!", reviews.get(0).getComment());
    }

    @Test
    void createReview_InitializesApprovedFalseAndSaves() {
        ReviewCreateRequest req = new ReviewCreateRequest();
        req.setAuthorName("Мария");
        req.setRating(5);
        req.setComment("Очень красивые кадры!");

        when(reviewRepository.save(any(Review.class))).thenAnswer(i -> {
            Review r = i.getArgument(0);
            r.setId(7L);
            return r;
        });

        ReviewResponseDto created = reviewService.createReview(req);

        assertNotNull(created);
        assertEquals(7L, created.getId());
        assertEquals("Мария", created.getAuthorName());
        assertFalse(created.isApproved(), "Новые отзывы должны требовать модерации (isApproved = false)");
        assertNotNull(created.getCreatedAt());
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    void createReview_InvalidInputs_ThrowsBadRequestException() {
        // Пустой автор
        ReviewCreateRequest emptyName = new ReviewCreateRequest("", 5, "Хороший отзыв");
        assertThrows(BadRequestException.class, () -> reviewService.createReview(emptyName));

        // Слишком короткий автор
        ReviewCreateRequest shortName = new ReviewCreateRequest("A", 5, "Хороший отзыв");
        assertThrows(BadRequestException.class, () -> reviewService.createReview(shortName));

        // Пустой комментарий
        ReviewCreateRequest emptyComment = new ReviewCreateRequest("Иван", 5, "   ");
        assertThrows(BadRequestException.class, () -> reviewService.createReview(emptyComment));

        // Невалидный рейтинг
        ReviewCreateRequest invalidRating = new ReviewCreateRequest("Иван", 10, "Хороший отзыв");
        assertThrows(BadRequestException.class, () -> reviewService.createReview(invalidRating));
    }

    @Test
    void approveReview_SetsApprovedTrue() {
        Review r = new Review("Петр", 4, "Хорошая работа");
        r.setId(15L);
        r.setApproved(false);

        when(reviewRepository.findById(15L)).thenReturn(Optional.of(r));

        reviewService.approveReview(15L);

        assertTrue(r.isApproved());
        verify(reviewRepository).save(r);
    }

    @Test
    void approveReview_NotFound_ThrowsResourceNotFoundException() {
        when(reviewRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> reviewService.approveReview(99L));
    }

    @Test
    void deleteReview_Success() {
        when(reviewRepository.existsById(33L)).thenReturn(true);

        reviewService.deleteReview(33L);

        verify(reviewRepository).deleteById(33L);
    }

    @Test
    void getAllReviewsForAdmin_ReturnsPagedDtos() {
        Review r = new Review("Катя", 5, "Супер");
        r.setId(1L);

        Page<Review> page = new PageImpl<>(List.of(r));
        when(reviewRepository.findAllByOrderByIdDesc(any())).thenReturn(page);

        Page<ReviewResponseDto> result = reviewService.getAllReviewsForAdmin(PageRequest.of(0, 10));

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Катя", result.getContent().get(0).getAuthorName());
    }
}

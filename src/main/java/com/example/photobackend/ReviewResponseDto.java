package com.example.photobackend;

/**
 * Неизменяемый снимок данных отзыва (Java Record).
 */
public record ReviewResponseDto(
    Long id,
    String authorName,
    int rating,
    String comment,
    String createdAt,
    boolean approved
) {
    public static ReviewResponseDto fromEntity(Review r) {
        if (r == null) return null;
        return new ReviewResponseDto(
                r.getId(),
                r.getAuthorName(),
                r.getRating(),
                r.getComment(),
                r.getCreatedAt(),
                r.isApproved()
        );
    }

    // Совместимые геттеры
    public Long getId() { return id; }
    public String getAuthorName() { return authorName; }
    public int getRating() { return rating; }
    public String getComment() { return comment; }
    public String getCreatedAt() { return createdAt; }
    public boolean isApproved() { return approved; }
}

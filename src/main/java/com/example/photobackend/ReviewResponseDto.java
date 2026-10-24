package com.example.photobackend;

public class ReviewResponseDto {

    private Long id;
    private String authorName;
    private int rating;
    private String comment;
    private String createdAt;
    private boolean approved;

    public ReviewResponseDto() {}

    public ReviewResponseDto(Long id, String authorName, int rating, String comment, String createdAt, boolean approved) {
        this.id = id;
        this.authorName = authorName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
        this.approved = approved;
    }

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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public boolean isApproved() { return approved; }
    public void setApproved(boolean approved) { this.approved = approved; }
}

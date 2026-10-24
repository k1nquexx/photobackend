package com.example.photobackend;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "reviews", indexes = {
    @Index(name = "idx_review_approved", columnList = "approved")
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String authorName;
    private int rating = 5; // от 1 до 5
    private String comment;
    private String createdAt = LocalDate.now().toString();
    private boolean approved = false; // Премодерация отзывов

    public Review() {}

    public Review(String authorName, int rating, String comment) {
        this.authorName = authorName;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = LocalDate.now().toString();
        this.approved = false;
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

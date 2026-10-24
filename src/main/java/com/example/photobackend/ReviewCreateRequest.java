package com.example.photobackend;

/**
 * DTO для добавления отзыва клиентом.
 * Предотвращает уязвимость Mass Assignment (не позволяет передать ID и перетереть чужой отзыв через Hibernate merge).
 */
public class ReviewCreateRequest {

    private String authorName;
    private Integer rating;
    private String comment;

    public ReviewCreateRequest() {}

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}

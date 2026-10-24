package com.example.photobackend;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для добавления отзыва клиентом.
 * Предотвращает уязвимость Mass Assignment (не позволяет передать ID и перетереть чужой отзыв через Hibernate merge).
 */
public class ReviewCreateRequest {

    @NotBlank(message = "Имя автора не может быть пустым")
    @Size(min = 1, max = 100, message = "Имя автора должно содержать от 1 до 100 символов")
    private String authorName;

    @Min(value = 1, message = "Минимальная оценка 1")
    @Max(value = 5, message = "Максимальная оценка 5")
    private Integer rating = 5;

    @NotBlank(message = "Текст отзыва не может быть пустым")
    @Size(min = 1, max = 2000, message = "Текст отзыва должен содержать от 1 до 2000 символов")
    private String comment;

    public ReviewCreateRequest() {}

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
}

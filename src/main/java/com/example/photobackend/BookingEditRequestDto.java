package com.example.photobackend;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class BookingEditRequestDto {

    @Size(max = 2000, message = "Детали заказа не должны превышать 2000 символов")
    private String details;

    private String bookingDate;
    private String bookingTime;

    @DecimalMin(value = "0.0", inclusive = true, message = "Стоимость не может быть отрицательной")
    private BigDecimal totalPrice;

    public BookingEditRequestDto() {}

    public BookingEditRequestDto(String details, String bookingDate, String bookingTime, BigDecimal totalPrice) {
        this.details = details;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.totalPrice = totalPrice;
    }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }
}

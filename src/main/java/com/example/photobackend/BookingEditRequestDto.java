package com.example.photobackend;

public class BookingEditRequestDto {

    private String details;
    private String bookingDate;
    private String bookingTime;
    private Integer totalPrice;

    public BookingEditRequestDto() {}

    public BookingEditRequestDto(String details, String bookingDate, String bookingTime, Integer totalPrice) {
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

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }
}

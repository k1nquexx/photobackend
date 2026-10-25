package com.example.photobackend;

public record BookingCreatedEvent(BookingResponseDto booking) {
    public BookingResponseDto getBookingDto() {
        return booking;
    }
}

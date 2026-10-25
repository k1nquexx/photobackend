package com.example.photobackend;

import java.math.BigDecimal;

/**
 * Неизменяемый снимок данных заявки (Java Record).
 * Гарантирует изоляцию от сессии JPA при асинхронной обработке событий.
 */
public record BookingResponseDto(
    String publicId,
    String id,
    String trackingToken,
    String clientName,
    String maskedClientName,
    String clientPhone,
    String details,
    BigDecimal totalPrice,
    String bookingDate,
    String bookingTime,
    String promoCode,
    String status,
    String downloadUrl,
    String createdAt,
    String clientIp
) {
    public static BookingResponseDto fromEntity(Booking b) {
        if (b == null) return null;
        String masked = maskName(b.getClientName());
        return new BookingResponseDto(
                b.getPublicId(),
                b.getPublicId(),
                b.getTrackingToken(),
                b.getClientName(),
                masked,
                b.getClientPhone(),
                b.getDetails(),
                b.getTotalPrice(),
                b.getBookingDate(),
                b.getBookingTime(),
                b.getPromoCode(),
                b.getStatus(),
                b.getDownloadUrl(),
                b.getCreatedAt(),
                b.getClientIp()
        );
    }

    public static String maskName(String fullName) {
        if (fullName == null || fullName.trim().isEmpty()) {
            return "Клиент";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0];
        }
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }

    // Совместимые геттеры
    public String getPublicId() { return publicId; }
    public String getId() { return id != null ? id : publicId; }
    public String getTrackingToken() { return trackingToken; }
    public String getClientName() { return clientName; }
    public String getMaskedClientName() { return maskedClientName; }
    public String getClientPhone() { return clientPhone; }
    public String getDetails() { return details; }
    public BigDecimal getTotalPrice() { return totalPrice; }
    public String getBookingDate() { return bookingDate; }
    public String getBookingTime() { return bookingTime; }
    public String getPromoCode() { return promoCode; }
    public String getStatus() { return status; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getCreatedAt() { return createdAt; }
    public String getClientIp() { return clientIp; }
}

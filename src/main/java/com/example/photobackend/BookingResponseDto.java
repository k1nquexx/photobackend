package com.example.photobackend;

import java.math.BigDecimal;

public class BookingResponseDto {

    private String publicId; // Криптографически стойкий публичный UUID v4
    private String id;       // Псевдоним для publicId в JSON (внутренний числовой ID БД никогда не отдается клиенту)
    private String trackingToken;
    private String clientName;
    private String maskedClientName;
    private String clientPhone;
    private String details;
    private BigDecimal totalPrice;
    private String bookingDate;
    private String bookingTime;
    private String promoCode;
    private String status;
    private String downloadUrl;
    private String createdAt;
    private String clientIp;

    public BookingResponseDto() {}

    public BookingResponseDto(String publicId, String trackingToken, String clientName, 
                              String maskedClientName, String clientPhone, String details, 
                              BigDecimal totalPrice, String bookingDate, String bookingTime, 
                              String promoCode, String status, String downloadUrl, 
                              String createdAt, String clientIp) {
        this.publicId = publicId;
        this.id = publicId;
        this.trackingToken = trackingToken;
        this.clientName = clientName;
        this.maskedClientName = maskedClientName;
        this.clientPhone = clientPhone;
        this.details = details;
        this.totalPrice = totalPrice;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.promoCode = promoCode;
        this.status = status;
        this.downloadUrl = downloadUrl;
        this.createdAt = createdAt;
        this.clientIp = clientIp;
    }

    public static BookingResponseDto fromEntity(Booking b) {
        if (b == null) return null;
        String masked = maskName(b.getClientName());
        return new BookingResponseDto(
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

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { 
        this.publicId = publicId; 
        this.id = publicId;
    }

    public String getId() { return id != null ? id : publicId; }
    public void setId(String id) { 
        this.id = id; 
        this.publicId = id;
    }

    public String getTrackingToken() { return trackingToken; }
    public void setTrackingToken(String trackingToken) { this.trackingToken = trackingToken; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getMaskedClientName() { return maskedClientName; }
    public void setMaskedClientName(String maskedClientName) { this.maskedClientName = maskedClientName; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { this.totalPrice = totalPrice; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}

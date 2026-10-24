package com.example.photobackend;

public class BookingResponseDto {

    private Long id;
    private String clientName;
    private String clientPhone;
    private String details;
    private Integer totalPrice;
    private String bookingDate;
    private String bookingTime;
    private String promoCode;
    private String status;
    private String downloadUrl;
    private String trackingToken;
    private String createdAt;
    private String clientIp;

    public BookingResponseDto() {}

    public BookingResponseDto(Long id, String clientName, String clientPhone, String details, 
                              Integer totalPrice, String bookingDate, String bookingTime, 
                              String promoCode, String status, String downloadUrl, 
                              String trackingToken, String createdAt, String clientIp) {
        this.id = id;
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.details = details;
        this.totalPrice = totalPrice;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.promoCode = promoCode;
        this.status = status;
        this.downloadUrl = downloadUrl;
        this.trackingToken = trackingToken;
        this.createdAt = createdAt;
        this.clientIp = clientIp;
    }

    public static BookingResponseDto fromEntity(Booking b) {
        if (b == null) return null;
        return new BookingResponseDto(
                b.getId(),
                b.getClientName(),
                b.getClientPhone(),
                b.getDetails(),
                b.getTotalPrice(),
                b.getBookingDate(),
                b.getBookingTime(),
                b.getPromoCode(),
                b.getStatus(),
                b.getDownloadUrl(),
                b.getTrackingToken(),
                b.getCreatedAt(),
                b.getClientIp()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public Integer getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Integer totalPrice) { this.totalPrice = totalPrice; }

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

    public String getTrackingToken() { return trackingToken; }
    public void setTrackingToken(String trackingToken) { this.trackingToken = trackingToken; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }
}

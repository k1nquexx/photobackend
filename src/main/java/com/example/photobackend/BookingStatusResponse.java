package com.example.photobackend;

public class BookingStatusResponse {

    private Long id;
    private String trackingToken;
    private String maskedClientName;
    private String status;
    private String bookingDate;
    private String bookingTime;
    private String downloadUrl;

    public BookingStatusResponse() {}

    public BookingStatusResponse(Long id, String trackingToken, String maskedClientName, String status, String bookingDate, String bookingTime, String downloadUrl) {
        this.id = id;
        this.trackingToken = trackingToken;
        this.maskedClientName = maskedClientName;
        this.status = status;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.downloadUrl = downloadUrl;
    }

    public BookingStatusResponse(Long id, String maskedClientName, String status, String bookingDate, String bookingTime, String downloadUrl) {
        this(id, null, maskedClientName, status, bookingDate, bookingTime, downloadUrl);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTrackingToken() { return trackingToken; }
    public void setTrackingToken(String trackingToken) { this.trackingToken = trackingToken; }

    public String getMaskedClientName() { return maskedClientName; }
    public void setMaskedClientName(String maskedClientName) { this.maskedClientName = maskedClientName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    // Утилита маскирования имени: "Алексей Смирнов" -> "Алексей С."
    public static String maskName(String fullName) {
        if (fullName == null || fullName.isBlank()) return "Клиент";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0];
        return parts[0] + " " + parts[1].charAt(0) + ".";
    }
}

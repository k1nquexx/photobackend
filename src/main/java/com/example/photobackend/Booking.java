package com.example.photobackend;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "bookings", indexes = {
    @Index(name = "idx_booking_public_id", columnList = "publicId", unique = true),
    @Index(name = "idx_booking_tracking_token", columnList = "trackingToken"),
    @Index(name = "idx_booking_status", columnList = "status"),
    @Index(name = "idx_booking_phone", columnList = "clientPhone"),
    @Index(name = "idx_booking_date_time", columnList = "bookingDate, bookingTime")
})
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Криптографически стойкий публичный UUID v4 (128 бит энтропии)
    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String publicId = UUID.randomUUID().toString();

    private String clientName;
    private String clientPhone;
    private String details;
    private String status = "Новая";
    private String downloadUrl; // Ссылка на облачный диск с готовыми фото

    @Column(precision = 12, scale = 2)
    private BigDecimal totalPrice; // Итоговая стоимость на BigDecimal с банковским округлением

    private String bookingDate; // Дата съемки (ГГГГ-ММ-ДД)
    private String bookingTime; // Время съемки (ЧЧ:ММ)
    private String promoCode;   // Использованный промокод
    private String trackingToken; // Случайный секретный токен для безопасной проверки статуса
    private String createdAt;   // Дата и время создания заявки (ГГГГ-ММ-ДД ЧЧ:ММ:СС)
    private String clientIp;    // IP-адрес клиента при создании заявки

    // Параметры расчета из формы калькулятора (не сохраняются как отдельные колонки БД)
    @Transient
    private BigDecimal shootTypeRate;

    @Transient
    private Integer hours;

    @Transient
    private boolean addonExpress;

    @Transient
    private boolean addonStudio;

    public Booking() {}

    public Booking(String clientName, String clientPhone, String details, BigDecimal totalPrice, String bookingDate, String bookingTime) {
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.details = details;
        this.totalPrice = totalPrice != null ? totalPrice.setScale(2, PricingConstants.BANKING_ROUNDING) : null;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
    }

    public Booking(String clientName, String clientPhone, String details, int totalPrice, String bookingDate, String bookingTime) {
        this(clientName, clientPhone, details, BigDecimal.valueOf(totalPrice), bookingDate, bookingTime);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPublicId() { return publicId; }
    public void setPublicId(String publicId) { this.publicId = publicId; }

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public BigDecimal getTotalPrice() { return totalPrice; }
    public void setTotalPrice(BigDecimal totalPrice) { 
        this.totalPrice = totalPrice != null ? totalPrice.setScale(2, PricingConstants.BANKING_ROUNDING) : null; 
    }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public String getTrackingToken() { return trackingToken; }
    public void setTrackingToken(String trackingToken) { this.trackingToken = trackingToken; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public BigDecimal getShootTypeRate() { return shootTypeRate; }
    public void setShootTypeRate(BigDecimal shootTypeRate) { this.shootTypeRate = shootTypeRate; }

    public Integer getHours() { return hours; }
    public void setHours(Integer hours) { this.hours = hours; }

    public boolean isAddonExpress() { return addonExpress; }
    public void setAddonExpress(boolean addonExpress) { this.addonExpress = addonExpress; }

    public boolean isAddonStudio() { return addonStudio; }
    public void setAddonStudio(boolean addonStudio) { this.addonStudio = addonStudio; }
}

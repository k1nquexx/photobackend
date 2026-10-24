package com.example.photobackend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO для создания заявки на съемку.
 * Предотвращает уязвимость Mass Assignment: клиент физически не может передать поля
 * status, downloadUrl или подделать totalPrice.
 */
public class BookingCreateRequest {

    @NotBlank(message = "Имя клиента обязательно для заполнения")
    @Size(max = 100, message = "Имя клиента не должно превышать 100 символов")
    private String clientName;

    @NotBlank(message = "Телефон клиента обязателен для заполнения")
    @Size(max = 30, message = "Номер телефона не должен превышать 30 символов")
    private String clientPhone;

    @Size(max = 2000, message = "Детали заказа не должны превышать 2000 символов")
    private String details;

    private String bookingDate;
    private String bookingTime;
    private Integer shootTypeRate;
    private Integer hours;
    private boolean addonExpress;
    private boolean addonStudio;
    private String promoCode;

    public BookingCreateRequest() {}

    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }

    public String getClientPhone() { return clientPhone; }
    public void setClientPhone(String clientPhone) { this.clientPhone = clientPhone; }

    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }

    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }

    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }

    public Integer getShootTypeRate() { return shootTypeRate; }
    public void setShootTypeRate(Integer shootTypeRate) { this.shootTypeRate = shootTypeRate; }

    public Integer getHours() { return hours; }
    public void setHours(Integer hours) { this.hours = hours; }

    public boolean isAddonExpress() { return addonExpress; }
    public void setAddonExpress(boolean addonExpress) { this.addonExpress = addonExpress; }

    public boolean isAddonStudio() { return addonStudio; }
    public void setAddonStudio(boolean addonStudio) { this.addonStudio = addonStudio; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
}

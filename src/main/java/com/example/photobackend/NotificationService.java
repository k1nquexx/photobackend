package com.example.photobackend;

import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    public void notifyNewBooking(Booking booking) {
        // Симуляция отправки уведомления фотографу (в Telegram / Email)
        System.out.println("==================================================");
        System.out.println("📸 [НОВАЯ ЗАЯВКА НА СЪЕМКУ]");
        System.out.println("Клиент: " + booking.getClientName());
        System.out.println("Телефон: " + booking.getClientPhone());
        System.out.println("Дата и время: " + booking.getBookingDate() + " " + (booking.getBookingTime() != null ? booking.getBookingTime() : ""));
        System.out.println("Стоимость: " + booking.getTotalPrice() + " ₽");
        System.out.println("Промокод: " + (booking.getPromoCode() != null ? booking.getPromoCode() : "нет"));
        System.out.println("Детали: " + booking.getDetails());
        System.out.println("==================================================");
    }
}

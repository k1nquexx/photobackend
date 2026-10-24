package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    public void notifyNewBooking(Booking booking) {
        log.info("📸 [НОВАЯ ЗАЯВКА НА СЪЕМКУ] Клиент: {}, Телефон: {}, Дата и время: {} {}, Стоимость: {} ₽, Промокод: {}, Детали: {}",
                booking.getClientName(),
                booking.getClientPhone(),
                booking.getBookingDate(),
                booking.getBookingTime() != null ? booking.getBookingTime() : "",
                booking.getTotalPrice(),
                booking.getPromoCode() != null ? booking.getPromoCode() : "нет",
                booking.getDetails());
    }
}

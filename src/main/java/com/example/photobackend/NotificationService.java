package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    /**
     * Асинхронная обработка событий создания заявки в фоновом пуле потоков ПОСЛЕ фиксации транзакции (@TransactionalEventListener).
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event) {
        if (event != null && event.booking() != null) {
            notifyNewBooking(event.booking());
        }
    }

    public void notifyNewBooking(BookingResponseDto booking) {
        log.info("📸 [НОВАЯ ЗАЯВКА НА СЪЕМКУ] Клиент: {}, Телефон: {}, Дата и время: {} {}, Стоимость: {} ₽, Промокод: {}, Детали: {}",
                booking.getClientName(),
                booking.getClientPhone(),
                booking.getBookingDate(),
                booking.getBookingTime() != null ? booking.getBookingTime() : "",
                booking.getTotalPrice(),
                booking.getPromoCode() != null ? booking.getPromoCode() : "нет",
                booking.getDetails());
    }

    public void notifyNewBooking(Booking booking) {
        notifyNewBooking(BookingResponseDto.fromEntity(booking));
    }
}

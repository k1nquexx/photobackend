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

    private static final int MAX_RETRIES = 3;
    private static final long INITIAL_BACKOFF_MS = 1000;

    /**
     * Асинхронная обработка событий создания заявки в фоновом пуле потоков 
     * ИСКЛЮЧИТЕЛЬНО ПОСЛЕ успешной фиксации транзакции (@TransactionalEventListener(phase = AFTER_COMMIT)).
     * Если транзакция откатилась или БД упала, уведомление клиенту отправлено НЕ БУДЕТ.
     */
    @Async("notificationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = false)
    public void onBookingCreated(BookingCreatedEvent event) {
        if (event == null || event.booking() == null) {
            return;
        }

        BookingResponseDto booking = event.booking();
        deliverNotificationWithRetry(booking);
    }

    /**
     * Отправка уведомления с механизмом повторных попыток (Retry) и экспоненциальной задержкой.
     * Предотвращает бесследную потерю исключений в фоновом пуле потоков.
     */
    public void deliverNotificationWithRetry(BookingResponseDto booking) {
        int attempt = 0;
        Exception lastError = null;

        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                sendNotification(booking);
                log.info("Уведомление для заявки {} успешно доставлено (попытка {}/{})", 
                        booking.getPublicId(), attempt, MAX_RETRIES);
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("Ошибка доставки уведомления для заявки {} (попытка {}/{}): {}", 
                        booking.getPublicId(), attempt, MAX_RETRIES, e.getMessage());

                if (attempt < MAX_RETRIES) {
                    try {
                        long backoff = INITIAL_BACKOFF_MS * (1L << (attempt - 1));
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Прервано ожидание повторной отправки уведомления для заявки {}", booking.getPublicId());
                        break;
                    }
                }
            }
        }

        // Если все попытки исчерпаны, фиксируем критическую ошибку в логах для мониторинга
        log.error("КРИТИЧЕСКИЙ СБОЙ: Не удалось доставить уведомление для заявки {} после {} попыток. " +
                  "Бронирование сохранено в БД, но клиент не оповещен. Ошибка: {}", 
                  booking.getPublicId(), MAX_RETRIES, lastError != null ? lastError.getMessage() : "неизвестно", lastError);
    }

    private void sendNotification(BookingResponseDto booking) {
        // Здесь выполняется отправка в Telegram / Email / SMS
        log.info("📸 [НОВАЯ ЗАЯВКА НА СЪЕМКУ] ID: {}, Клиент: {}, Телефон: {}, Дата и время: {} {}, Стоимость: {} ₽, Промокод: {}, Детали: {}",
                booking.getPublicId(),
                booking.getClientName(),
                booking.getClientPhone(),
                booking.getBookingDate(),
                booking.getBookingTime() != null ? booking.getBookingTime() : "",
                booking.getTotalPrice(),
                booking.getPromoCode() != null ? booking.getPromoCode() : "нет",
                booking.getDetails());
    }

    public void notifyNewBooking(BookingResponseDto booking) {
        deliverNotificationWithRetry(booking);
    }

    public void notifyNewBooking(Booking booking) {
        deliverNotificationWithRetry(BookingResponseDto.fromEntity(booking));
    }
}

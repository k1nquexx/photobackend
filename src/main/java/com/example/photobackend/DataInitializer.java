package com.example.photobackend;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import java.time.LocalDate;

@Component
public class DataInitializer implements CommandLineRunner {

    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final TimeSlotRepository timeSlotRepository;

    // Флаг генерации демо-данных (по умолчанию false, чтобы не засорять БД при каждом перезапуске)
    @Value("${app.seed-demo-data:false}")
    private boolean seedDemoData;

    public DataInitializer(BookingRepository bookingRepository, 
                           ReviewRepository reviewRepository,
                           TimeSlotRepository timeSlotRepository) {
        this.bookingRepository = bookingRepository;
        this.reviewRepository = reviewRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    @Override
    public void run(String... args) {
        // Демо-заявки создаются только если явно включен флаг app.seed-demo-data=true
        if (seedDemoData && bookingRepository.count() == 0) {
            Booking b1 = new Booking("Алексей Смирнов", "+7 (999) 111-22-33", "Индивидуальный портрет, 2 часа", 2500, "2026-10-25", "14:00");
            b1.setStatus("Готово");
            b1.setDownloadUrl("https://disk.yandex.ru/d/demo-photos");
            b1.setTrackingToken("TRK-DEMO0001");
            b1.setCreatedAt(LocalDate.now() + " 10:00:00");
            b1.setClientIp("127.0.0.1");

            Booking b2 = new Booking("Екатерина Волкова", "+7 (999) 444-55-66", "Репортаж мероприятия, 3 часа", 6500, "2026-11-02", "18:00");
            b2.setStatus("В обработке");
            b2.setTrackingToken("TRK-DEMO0002");
            b2.setCreatedAt(LocalDate.now() + " 11:30:00");
            b2.setClientIp("127.0.0.1");

            Booking b3 = new Booking("Иван Петров", "+7 (999) 777-88-99", "Студийная съемка, 1 час", 1500, "2026-11-10", "12:00");
            b3.setStatus("Новая");
            b3.setTrackingToken("TRK-DEMO0003");
            b3.setCreatedAt(LocalDate.now() + " 12:45:00");
            b3.setClientIp("127.0.0.1");

            bookingRepository.save(b1);
            bookingRepository.save(b2);
            bookingRepository.save(b3);
        }

        // Первоначальные отзывы для витрины (если база пустая)
        if (reviewRepository.count() == 0) {
            Review r1 = new Review("Анна С.", 5, "Кирилл — замечательный фотограф! С ним было очень комфортно работать, помог с позами. Фотографии получились живыми и атмосферными!");
            r1.setApproved(true);
            Review r2 = new Review("Максим и Дарья", 5, "Заказывали съемку лавстори на природе. Исходники отдал быстро, а готовая серия просто восторг! Спасибо за приятные воспоминания.");
            r2.setApproved(true);
            Review r3 = new Review("Артем Д.", 5, "Приглашали на репортаж корпоратива. Все ключевые моменты пойманы четко, фотографии отданы в срок по ссылке на диск. Рекомендую!");
            r3.setApproved(true);

            reviewRepository.save(r1);
            reviewRepository.save(r2);
            reviewRepository.save(r3);
        }

        // Первоначальные окна для записи на ближайшие 7 дней (если расписание пустое)
        if (timeSlotRepository.count() == 0) {
            LocalDate today = LocalDate.now();
            String[] times = {"11:00", "14:00", "16:30", "19:00"};

            for (int i = 0; i < 7; i++) {
                String d = today.plusDays(i).toString();
                for (String t : times) {
                    timeSlotRepository.save(new TimeSlot(d, t));
                }
            }
        }
    }
}

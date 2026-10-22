package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private NotificationService notificationService;

    @PostMapping("/booking")
    public String createBooking(@RequestBody Booking booking) {
        bookingRepository.save(booking);

        // Если указана дата и время, помечаем окно как занятое
        if (booking.getBookingDate() != null && booking.getBookingTime() != null) {
            timeSlotRepository.findBySlotDateAndSlotTime(booking.getBookingDate(), booking.getBookingTime())
                    .ifPresent(slot -> {
                        slot.setBooked(true);
                        timeSlotRepository.save(slot);
                    });
        }

        notificationService.notifyNewBooking(booking);
        return "Заявка успешно отправлена! Номер заявки: #" + booking.getId();
    }

    // Проверка статуса заказа по номеру телефона
    @GetMapping("/status")
    public ResponseEntity<?> checkStatus(@RequestParam String phone) {
        return bookingRepository.findByClientPhone(phone)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(404).body("Заказ с таким номером не найден."));
    }

    @GetMapping("/booking")
    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    // Статистика для админки
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<Booking> all = bookingRepository.findAll();
        long totalCount = all.size();
        long totalIncome = all.stream()
                .filter(b -> b.getTotalPrice() != null)
                .mapToInt(Booking::getTotalPrice)
                .sum();
        long newCount = all.stream().filter(b -> "Новая".equalsIgnoreCase(b.getStatus())).count();
        long completedCount = all.stream().filter(b -> "Готово".equalsIgnoreCase(b.getStatus())).count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("totalIncome", totalIncome);
        stats.put("newCount", newCount);
        stats.put("completedCount", completedCount);
        return stats;
    }

    // Изменение статуса заявки
    @PatchMapping("/booking/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestBody String newStatus) {
        return bookingRepository.findById(id).map(b -> {
            b.setStatus(newStatus.replace("\"", "").trim());
            bookingRepository.save(b);
            return "Статус обновлен";
        }).orElse("Заявка не найдена");
    }

    // Изменение деталей заказа
    @PatchMapping("/booking/{id}/details")
    public String updateDetails(@PathVariable Long id, @RequestBody String details) {
        return bookingRepository.findById(id).map(b -> {
            b.setDetails(details.replace("\"", "").trim());
            bookingRepository.save(b);
            return "Детали обновлены";
        }).orElse("Заявка не найдена");
    }

    // Полное редактирование заказа администратором (детали, дата, время, сумма)
    @PatchMapping("/booking/{id}/edit")
    public String editBooking(@PathVariable Long id, @RequestBody Booking updated) {
        return bookingRepository.findById(id).map(b -> {
            if (updated.getDetails() != null) b.setDetails(updated.getDetails());
            if (updated.getBookingDate() != null) b.setBookingDate(updated.getBookingDate());
            if (updated.getBookingTime() != null) b.setBookingTime(updated.getBookingTime());
            if (updated.getTotalPrice() != null) b.setTotalPrice(updated.getTotalPrice());
            bookingRepository.save(b);
            return "Данные заказа обновлены";
        }).orElse("Заявка не найдена");
    }

    // Сохранение ссылки на скачивание фото
    @PatchMapping("/booking/{id}/download-url")
    public String updateDownloadUrl(@PathVariable Long id, @RequestBody String url) {
        return bookingRepository.findById(id).map(b -> {
            b.setDownloadUrl(url.replace("\"", "").trim());
            bookingRepository.save(b);
            return "Ссылка сохранена";
        }).orElse("Заявка не найдена");
    }

    // Удаление заявки
    @DeleteMapping("/booking/{id}")
    public String deleteBooking(@PathVariable Long id) {
        if (bookingRepository.existsById(id)) {
            bookingRepository.deleteById(id);
            return "Заявка удалена";
        }
        return "Заявка не найдена";
    }

    // Простая проверка пароля администратора
    @PostMapping("/admin/login")
    public boolean adminLogin(@RequestBody String password) {
        String cleanPass = password.replace("\"", "").trim();
        return "admin123".equals(cleanPass);
    }
}

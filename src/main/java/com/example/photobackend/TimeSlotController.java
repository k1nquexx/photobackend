package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = "*")
public class TimeSlotController {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    @Autowired
    private AdminAuthService adminAuthService;

    // Публичные свободные окна для клиентов на выбранную дату
    @GetMapping
    public List<TimeSlot> getAvailableSlots(@RequestParam String date) {
        return timeSlotRepository.findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc(date);
    }

    // Все окна на дату для фотографа
    @AdminOnly
    @GetMapping("/all")
    public ResponseEntity<?> getAllSlotsByDate(
            @RequestParam(required = false) String date,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен: требуется токен администратора");
        }

        if (date != null && !date.isEmpty()) {
            return ResponseEntity.ok(timeSlotRepository.findBySlotDateOrderBySlotTimeAsc(date));
        }
        return ResponseEntity.ok(timeSlotRepository.findAll());
    }

    // Добавление окна
    @AdminOnly
    @PostMapping
    public ResponseEntity<?> addSlot(
            @RequestBody TimeSlot slot,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен: требуется токен администратора");
        }

        if (timeSlotRepository.findBySlotDateAndSlotTime(slot.getSlotDate(), slot.getSlotTime()).isPresent()) {
            return ResponseEntity.badRequest().body("Окно на это время уже существует");
        }
        timeSlotRepository.save(slot);
        return ResponseEntity.ok("Окно добавлено");
    }

    // Удаление окна
    @AdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(
            @PathVariable Long id,
            @RequestHeader(value = "X-Admin-Token", required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Доступ запрещен: требуется токен администратора");
        }

        if (timeSlotRepository.existsById(id)) {
            timeSlotRepository.deleteById(id);
            return ResponseEntity.ok("Окно удалено");
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Окно не найдено");
    }
}

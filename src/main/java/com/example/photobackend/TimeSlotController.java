package com.example.photobackend;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/slots")
@Validated
public class TimeSlotController {

    private final TimeSlotService timeSlotService;
    private final AdminAuthService adminAuthService;

    @Autowired
    public TimeSlotController(TimeSlotService timeSlotService, AdminAuthService adminAuthService) {
        this.timeSlotService = timeSlotService;
        this.adminAuthService = adminAuthService;
    }

    /**
     * Публичные свободные окна для клиентов на выбранную дату (возвращает DTO).
     */
    @GetMapping
    public ResponseEntity<List<TimeSlotResponseDto>> getAvailableSlots(@RequestParam String date) {
        return ResponseEntity.ok(timeSlotService.getAvailableSlots(date));
    }

    /**
     * Окна на дату для фотографа (администратора).
     */
    @AdminOnly
    @GetMapping("/all")
    public ResponseEntity<?> getAllSlotsByDate(
            @RequestParam(required = false) String date,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        return ResponseEntity.ok(timeSlotService.getAllSlotsByDate(date));
    }

    /**
     * Добавление окна администратором (валидация DTO через @Valid).
     */
    @AdminOnly
    @PostMapping
    public ResponseEntity<?> addSlot(
            @Valid @RequestBody TimeSlotRequestDto slotReq,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        TimeSlotResponseDto created = timeSlotService.addSlot(slotReq);
        return ResponseEntity.ok(created);
    }

    /**
     * Удаление окна администратором.
     */
    @AdminOnly
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSlot(
            @PathVariable Long id,
            @RequestHeader(value = SecurityConstants.AUTH_HEADER_NAME, required = false) String token) {

        if (!adminAuthService.isValid(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Доступ запрещен: требуется токен администратора");
        }

        timeSlotService.deleteSlot(id);
        return ResponseEntity.ok("Окно удалено");
    }
}

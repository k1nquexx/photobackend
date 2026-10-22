package com.example.photobackend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
@CrossOrigin(origins = "*")
public class TimeSlotController {

    @Autowired
    private TimeSlotRepository timeSlotRepository;

    // Свободные окна для клиентов на выбранную дату
    @GetMapping
    public List<TimeSlot> getAvailableSlots(@RequestParam String date) {
        return timeSlotRepository.findBySlotDateAndIsBookedFalse(date);
    }

    // Все окна на дату для фотографа
    @GetMapping("/all")
    public List<TimeSlot> getAllSlotsByDate(@RequestParam(required = false) String date) {
        if (date != null && !date.isEmpty()) {
            return timeSlotRepository.findBySlotDateOrderBySlotTimeAsc(date);
        }
        return timeSlotRepository.findAll();
    }

    // Добавление окна
    @PostMapping
    public String addSlot(@RequestBody TimeSlot slot) {
        if (timeSlotRepository.findBySlotDateAndSlotTime(slot.getSlotDate(), slot.getSlotTime()).isPresent()) {
            return "Окно на это время уже существует";
        }
        timeSlotRepository.save(slot);
        return "Окно добавлено";
    }

    // Быстрое переключение: если слота нет — создаем, если есть — удаляем (или меняем занятость)
    @DeleteMapping("/{id}")
    public String deleteSlot(@PathVariable Long id) {
        if (timeSlotRepository.existsById(id)) {
            timeSlotRepository.deleteById(id);
            return "Окно удалено";
        }
        return "Окно не найдено";
    }
}

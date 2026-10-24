package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    private static final Logger log = LoggerFactory.getLogger(TimeSlotService.class);

    private final TimeSlotRepository timeSlotRepository;

    @Autowired
    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * Выборка свободных слотов на конкретную дату (использует индекс).
     */
    public List<TimeSlotResponseDto> getAvailableSlots(String date) {
        if (date == null || date.isBlank()) {
            throw new BadRequestException("Параметр date обязателен для получения свободных окон");
        }
        return timeSlotRepository.findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc(date.trim())
                .stream()
                .map(TimeSlotResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Индексированная выборка слотов по диапазону дат и статусу занятости (защита от Full Table Scan).
     */
    public List<TimeSlotResponseDto> getSlotsByDateRange(String startDate, String endDate, boolean isBooked) {
        return timeSlotRepository.findBySlotDateBetweenAndIsBookedOrderBySlotDateAscSlotTimeAsc(startDate, endDate, isBooked)
                .stream()
                .map(TimeSlotResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Выборка всех слотов для администратора: на конкретную дату либо постранично.
     */
    public List<TimeSlotResponseDto> getAllSlotsByDate(String date) {
        if (date != null && !date.isBlank()) {
            return timeSlotRepository.findBySlotDateOrderBySlotTimeAsc(date.trim())
                    .stream()
                    .map(TimeSlotResponseDto::fromEntity)
                    .collect(Collectors.toList());
        }
        // Защита от OutOfMemory: возвращаем первые 100 слотов при запросе без даты
        return timeSlotRepository.findAllByOrderBySlotDateDescSlotTimeAsc(Pageable.ofSize(100))
                .stream()
                .map(TimeSlotResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    public Page<TimeSlotResponseDto> getAllSlotsPaged(Pageable pageable) {
        return timeSlotRepository.findAllByOrderBySlotDateDescSlotTimeAsc(pageable)
                .map(TimeSlotResponseDto::fromEntity);
    }

    /**
     * Создание нового слота с проверкой уникальности.
     */
    @Transactional
    public TimeSlotResponseDto addSlot(TimeSlotRequestDto req) {
        String cleanDate = req.getSlotDate().trim();
        String cleanTime = req.getSlotTime().trim();

        if (timeSlotRepository.findBySlotDateAndSlotTime(cleanDate, cleanTime).isPresent()) {
            throw new BadRequestException("Окно на это время уже существует");
        }

        TimeSlot slot = new TimeSlot(cleanDate, cleanTime);
        TimeSlot saved = timeSlotRepository.save(slot);
        log.info("Добавлен новый слот расписания: {} в {}", cleanDate, cleanTime);
        return TimeSlotResponseDto.fromEntity(saved);
    }

    /**
     * Удаление слота.
     */
    @Transactional
    public void deleteSlot(Long id) {
        if (!timeSlotRepository.existsById(id)) {
            throw new ResourceNotFoundException("Окно не найдено");
        }
        timeSlotRepository.deleteById(id);
        log.info("Удален слот расписания #{}", id);
    }
}

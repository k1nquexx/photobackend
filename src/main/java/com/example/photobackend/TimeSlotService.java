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
     * Создание нового слота с проверкой уникальности и корректности интервала времени.
     */
    @Transactional
    public TimeSlotResponseDto addSlot(TimeSlotRequestDto req) {
        if (req == null) {
            throw new BadRequestException("Данные окна не переданы");
        }
        if (req.getSlotDate() == null || req.getSlotDate().isBlank()) {
            throw new BadRequestException("Дата окна обязательна для заполнения");
        }

        String cleanDate = req.getSlotDate().trim();
        if (!cleanDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new BadRequestException("Неверный формат даты. Требуется ГГГГ-ММ-ДД");
        }

        String cleanTime = req.getResolvedSlotTime();
        if (cleanTime == null || cleanTime.isBlank()) {
            throw new BadRequestException("Время окна обязательно для заполнения");
        }

        // Проверка: время начала слота не должно быть позже или равно времени окончания
        validateTimeInterval(cleanTime, req.getStartTime(), req.getEndTime());

        if (timeSlotRepository.findBySlotDateAndSlotTime(cleanDate, cleanTime).isPresent()) {
            throw new BadRequestException("Окно на это время уже существует");
        }

        TimeSlot slot = new TimeSlot(cleanDate, cleanTime);
        TimeSlot saved = timeSlotRepository.save(slot);
        log.info("Добавлен новый слот расписания: {} в {}", cleanDate, cleanTime);
        return TimeSlotResponseDto.fromEntity(saved);
    }

    private void validateTimeInterval(String timeStr, String explicitStart, String explicitEnd) {
        String startStr = explicitStart;
        String endStr = explicitEnd;

        if ((startStr == null || endStr == null) && timeStr != null && timeStr.contains("-")) {
            String[] parts = timeStr.split("-");
            if (parts.length == 2) {
                startStr = parts[0].trim();
                endStr = parts[1].trim();
            }
        }

        if (startStr != null && endStr != null && !startStr.isBlank() && !endStr.isBlank()) {
            try {
                String normalizedStart = startStr.length() == 4 ? "0" + startStr : startStr;
                String normalizedEnd = endStr.length() == 4 ? "0" + endStr : endStr;
                java.time.LocalTime start = java.time.LocalTime.parse(normalizedStart);
                java.time.LocalTime end = java.time.LocalTime.parse(normalizedEnd);
                if (!start.isBefore(end)) {
                    throw new BadRequestException("Время начала слота (" + startStr + ") не может быть позже или равно времени окончания (" + endStr + ")");
                }
            } catch (java.time.format.DateTimeParseException e) {
                throw new BadRequestException("Неверный формат времени: " + e.getParsedString());
            }
        }
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

package com.example.photobackend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class TimeSlotService {

    private static final Logger log = LoggerFactory.getLogger(TimeSlotService.class);

    // Часовой пояс студии для строгой фильтрации прошедших слотов
    public static final ZoneId STUDIO_TIMEZONE = ZoneId.of("Europe/Moscow");

    private final TimeSlotRepository timeSlotRepository;

    @Autowired
    public TimeSlotService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    /**
     * Выборка свободных слотов на конкретную дату (использует индекс).
     * Отфильтровывает слоты в прошлом: если дата прошла или если время сегодняшнего слота уже наступило.
     */
    public List<TimeSlotResponseDto> getAvailableSlots(String date) {
        if (date == null || date.isBlank()) {
            throw new BadRequestException("Параметр date обязателен для получения свободных окон");
        }

        String cleanDate = date.trim();
        ZonedDateTime nowStudio = ZonedDateTime.now(STUDIO_TIMEZONE);
        LocalDate today = nowStudio.toLocalDate();
        LocalTime nowTime = nowStudio.toLocalTime();

        try {
            LocalDate requestedDate = LocalDate.parse(cleanDate);
            // Если запрашиваемая дата в прошлом — доступных окон нет
            if (requestedDate.isBefore(today)) {
                return List.of();
            }

            List<TimeSlot> slots = timeSlotRepository.findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc(cleanDate);

            // Если дата сегодняшняя — отфильтровываем окна, чье время старта уже наступило
            if (requestedDate.isEqual(today)) {
                slots = slots.stream()
                        .filter(s -> {
                            LocalTime startTime = extractStartTime(s.getSlotTime());
                            return startTime != null && startTime.isAfter(nowTime);
                        })
                        .collect(Collectors.toList());
            }

            return slots.stream()
                    .map(TimeSlotResponseDto::fromEntity)
                    .collect(Collectors.toList());
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Неверный формат даты. Требуется ГГГГ-ММ-ДД");
        }
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
     * Создание нового слота с проверкой уникальности, хронологии интервала и пересечения (наложения).
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

        // Парсинг и валидация интервала нового слота (start < end)
        LocalTime[] newInterval = parseSlotInterval(cleanTime, req.getStartTime(), req.getEndTime());

        // Проверка точного совпадения
        if (timeSlotRepository.findBySlotDateAndSlotTime(cleanDate, cleanTime).isPresent()) {
            throw new BadRequestException("Окно на это время уже существует");
        }

        // Проверка наложения (пересечения) с уже созданными слотами на эту дату
        List<TimeSlot> existingSlots = timeSlotRepository.findBySlotDateOrderBySlotTimeAsc(cleanDate);
        for (TimeSlot ex : existingSlots) {
            LocalTime[] exInterval = parseSlotInterval(ex.getSlotTime(), null, null);
            // Интервалы [A, B] и [C, D] пересекаются, если A < D и C < B
            boolean overlaps = newInterval[0].isBefore(exInterval[1]) && exInterval[0].isBefore(newInterval[1]);
            if (overlaps) {
                throw new SlotConflictException("Временной интервал (" + cleanTime + 
                        ") пересекается с существующим окном (" + ex.getSlotTime() + ") на дату " + cleanDate);
            }
        }

        TimeSlot slot = new TimeSlot(cleanDate, cleanTime);
        TimeSlot saved = timeSlotRepository.save(slot);
        log.info("Добавлен новый слот расписания: {} в {}", cleanDate, cleanTime);
        return TimeSlotResponseDto.fromEntity(saved);
    }

    public static LocalTime extractStartTime(String slotTime) {
        if (slotTime == null || slotTime.isBlank()) return null;
        String s = slotTime.trim();
        if (s.contains("-")) {
            s = s.split("-")[0].trim();
        }
        try {
            return LocalTime.parse(s.length() == 4 ? "0" + s : s);
        } catch (Exception e) {
            return null;
        }
    }

    public static LocalTime[] parseSlotInterval(String timeStr, String explicitStart, String explicitEnd) {
        String startStr = explicitStart;
        String endStr = explicitEnd;

        if ((startStr == null || endStr == null) && timeStr != null && timeStr.contains("-")) {
            String[] parts = timeStr.split("-");
            if (parts.length == 2) {
                startStr = parts[0].trim();
                endStr = parts[1].trim();
            }
        }

        if (startStr == null && timeStr != null) {
            startStr = timeStr.trim();
        }

        LocalTime start;
        try {
            start = LocalTime.parse(startStr.length() == 4 ? "0" + startStr : startStr);
        } catch (Exception e) {
            throw new BadRequestException("Неверный формат времени начала: " + startStr);
        }

        LocalTime end;
        if (endStr != null && !endStr.isBlank()) {
            try {
                end = LocalTime.parse(endStr.length() == 4 ? "0" + endStr : endStr);
            } catch (Exception e) {
                throw new BadRequestException("Неверный формат времени окончания: " + endStr);
            }
        } else {
            // Если длительность явно не указана, считаем стандартное часовое окно
            end = start.plusHours(1);
        }

        if (!start.isBefore(end)) {
            throw new BadRequestException("Время начала слота (" + start + ") не может быть позже или равно времени окончания (" + end + ")");
        }

        return new LocalTime[]{start, end};
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

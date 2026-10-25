package com.example.photobackend;

/**
 * Неизменяемый снимок данных временного слота (Java Record).
 */
public record TimeSlotResponseDto(
    Long id,
    String slotDate,
    String slotTime,
    boolean isBooked
) {
    public static TimeSlotResponseDto fromEntity(TimeSlot slot) {
        if (slot == null) return null;
        return new TimeSlotResponseDto(
                slot.getId(),
                slot.getSlotDate(),
                slot.getSlotTime(),
                slot.isBooked()
        );
    }

    // Совместимые геттеры
    public Long getId() { return id; }
    public String getSlotDate() { return slotDate; }
    public String getSlotTime() { return slotTime; }
    public boolean isBooked() { return isBooked; }
}

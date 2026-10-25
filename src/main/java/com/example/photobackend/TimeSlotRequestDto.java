package com.example.photobackend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TimeSlotRequestDto {

    @NotBlank(message = "Дата обязательна для заполнения")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Формат даты должен быть ГГГГ-ММ-ДД")
    private String slotDate;

    @Pattern(regexp = "^\\d{1,2}:\\d{2}(\\s*-\\s*\\d{1,2}:\\d{2})?$", message = "Формат времени должен быть ЧЧ:ММ или ЧЧ:ММ - ЧЧ:ММ")
    private String slotTime;

    @Pattern(regexp = "^\\d{1,2}:\\d{2}$", message = "Формат времени начала должен быть ЧЧ:ММ")
    private String startTime;

    @Pattern(regexp = "^\\d{1,2}:\\d{2}$", message = "Формат времени окончания должен быть ЧЧ:ММ")
    private String endTime;

    public TimeSlotRequestDto() {}

    public TimeSlotRequestDto(String slotDate, String slotTime) {
        this.slotDate = slotDate;
        this.slotTime = slotTime;
    }

    public TimeSlotRequestDto(String slotDate, String startTime, String endTime) {
        this.slotDate = slotDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.slotTime = startTime + " - " + endTime;
    }

    public String getResolvedSlotTime() {
        if (slotTime != null && !slotTime.isBlank()) {
            return slotTime.trim();
        }
        if (startTime != null && !startTime.isBlank() && endTime != null && !endTime.isBlank()) {
            return startTime.trim() + " - " + endTime.trim();
        }
        if (startTime != null && !startTime.isBlank()) {
            return startTime.trim();
        }
        return null;
    }

    public String getSlotDate() { return slotDate; }
    public void setSlotDate(String slotDate) { this.slotDate = slotDate; }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) { this.slotTime = slotTime; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}

package com.example.photobackend;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TimeSlotRequestDto {

    @NotBlank(message = "Дата обязательна для заполнения")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "Формат даты должен быть ГГГГ-ММ-ДД")
    private String slotDate;

    @NotBlank(message = "Время обязательно для заполнения")
    @Pattern(regexp = "^\\d{1,2}:\\d{2}$", message = "Формат времени должен быть ЧЧ:ММ")
    private String slotTime;

    public TimeSlotRequestDto() {}

    public TimeSlotRequestDto(String slotDate, String slotTime) {
        this.slotDate = slotDate;
        this.slotTime = slotTime;
    }

    public String getSlotDate() { return slotDate; }
    public void setSlotDate(String slotDate) { this.slotDate = slotDate; }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) { this.slotTime = slotTime; }
}

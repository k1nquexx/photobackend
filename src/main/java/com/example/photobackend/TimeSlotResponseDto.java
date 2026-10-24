package com.example.photobackend;

public class TimeSlotResponseDto {

    private Long id;
    private String slotDate;
    private String slotTime;
    private boolean isBooked;

    public TimeSlotResponseDto() {}

    public TimeSlotResponseDto(Long id, String slotDate, String slotTime, boolean isBooked) {
        this.id = id;
        this.slotDate = slotDate;
        this.slotTime = slotTime;
        this.isBooked = isBooked;
    }

    public static TimeSlotResponseDto fromEntity(TimeSlot slot) {
        if (slot == null) return null;
        return new TimeSlotResponseDto(
                slot.getId(),
                slot.getSlotDate(),
                slot.getSlotTime(),
                slot.isBooked()
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSlotDate() { return slotDate; }
    public void setSlotDate(String slotDate) { this.slotDate = slotDate; }

    public String getSlotTime() { return slotTime; }
    public void setSlotTime(String slotTime) { this.slotTime = slotTime; }

    public boolean isBooked() { return isBooked; }
    public void setBooked(boolean booked) { isBooked = booked; }
}

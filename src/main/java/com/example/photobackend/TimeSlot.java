package com.example.photobackend;

import jakarta.persistence.*;

@Entity
@Table(name = "time_slots")
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slotDate; // Дата: "2026-10-25"
    private String slotTime; // Время: "14:00"
    private boolean isBooked = false; // Занято или свободно

    public TimeSlot() {}

    public TimeSlot(String slotDate, String slotTime) {
        this.slotDate = slotDate;
        this.slotTime = slotTime;
        this.isBooked = false;
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

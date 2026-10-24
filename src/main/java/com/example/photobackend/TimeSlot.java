package com.example.photobackend;

import jakarta.persistence.*;

@Entity
@Table(name = "time_slots", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_timeslot_date_time", columnNames = {"slotDate", "slotTime"})
    },
    indexes = {
        @Index(name = "idx_timeslot_date_booked", columnList = "slotDate, isBooked"),
        @Index(name = "idx_timeslot_date_time", columnList = "slotDate, slotTime"),
        @Index(name = "idx_timeslot_booked", columnList = "isBooked")
    }
)
public class TimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String slotDate; // Дата: "2026-10-25"

    @Column(nullable = false, length = 10)
    private String slotTime; // Время: "14:00"

    @Column(nullable = false)
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

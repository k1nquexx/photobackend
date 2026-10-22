package com.example.photobackend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {
    List<TimeSlot> findBySlotDateAndIsBookedFalse(String slotDate);
    List<TimeSlot> findBySlotDateOrderBySlotTimeAsc(String slotDate);
    Optional<TimeSlot> findBySlotDateAndSlotTime(String slotDate, String slotTime);
}

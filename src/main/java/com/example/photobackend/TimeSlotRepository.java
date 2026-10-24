package com.example.photobackend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    List<TimeSlot> findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc(String slotDate);

    default List<TimeSlot> findBySlotDateAndIsBookedFalse(String slotDate) {
        return findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc(slotDate);
    }

    List<TimeSlot> findBySlotDateOrderBySlotTimeAsc(String slotDate);

    Optional<TimeSlot> findBySlotDateAndSlotTime(String slotDate, String slotTime);

    // Атомарное бронирование слота на уровне БД (защита от Race Condition)
    @Modifying
    @Transactional
    @Query("UPDATE TimeSlot t SET t.isBooked = true WHERE t.slotDate = :date AND t.slotTime = :time AND t.isBooked = false")
    int bookSlotAtomically(@Param("date") String date, @Param("time") String time);
}

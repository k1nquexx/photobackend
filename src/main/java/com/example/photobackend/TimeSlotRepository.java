package com.example.photobackend;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    // Выборка свободных слотов на конкретную дату (использует индекс idx_timeslot_date_booked)
    List<TimeSlot> findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc(String slotDate);

    default List<TimeSlot> findBySlotDateAndIsBookedFalse(String slotDate) {
        return findBySlotDateAndIsBookedFalseOrderBySlotTimeAsc(slotDate);
    }

    // Выборка слотов по диапазону дат и статусу занятости (использует индекс idx_timeslot_date_booked)
    List<TimeSlot> findBySlotDateBetweenAndIsBookedOrderBySlotDateAscSlotTimeAsc(String startDate, String endDate, boolean isBooked);

    // Выборка слотов на дату для фотографа
    List<TimeSlot> findBySlotDateOrderBySlotTimeAsc(String slotDate);

    // Постраничная выборка слотов для защиты от переполнения памяти
    Page<TimeSlot> findAllByOrderBySlotDateDescSlotTimeAsc(Pageable pageable);

    // Поиск конкретного слота по дате и времени (использует индекс idx_timeslot_date_time)
    Optional<TimeSlot> findBySlotDateAndSlotTime(String slotDate, String slotTime);

    // Выборка слота с пессимистической блокировкой на запись (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TimeSlot t WHERE t.slotDate = :date AND t.slotTime = :time")
    Optional<TimeSlot> findBySlotDateAndSlotTimeForUpdate(@Param("date") String date, @Param("time") String time);

    // Атомарное бронирование слота на уровне БД (защита от Race Condition)
    @Modifying
    @Transactional
    @Query("UPDATE TimeSlot t SET t.isBooked = true WHERE t.slotDate = :date AND t.slotTime = :time AND t.isBooked = false")
    int bookSlotAtomically(@Param("date") String date, @Param("time") String time);
}

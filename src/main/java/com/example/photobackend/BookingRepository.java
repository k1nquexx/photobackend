package com.example.photobackend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    // Поиск по публичному UUID v4 (128 бит энтропии) с жадной подгрузкой слота (без N+1)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"timeSlot"})
    Optional<Booking> findByPublicId(String publicId);

    // Поиск по секретному коду отслеживания
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"timeSlot"})
    Optional<Booking> findByTrackingToken(String trackingToken);

    // Постраничная выборка для администратора с JOIN FETCH (устраняет проблему 100 лишних запросов N+1)
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"timeSlot"})
    Page<Booking> findAllByOrderByIdDesc(Pageable pageable);

    // Быстрые агрегатные запросы O(1) памяти
    @Query("SELECT COUNT(b) FROM Booking b")
    long countTotalBookings();

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0.0) FROM Booking b")
    BigDecimal sumTotalIncome();

    long countByStatusIgnoreCase(String status);
}

package com.example.photobackend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByClientPhone(String clientPhone);

    Optional<Booking> findByIdAndClientPhone(Long id, String clientPhone);

    Optional<Booking> findByTrackingToken(String trackingToken);

    Page<Booking> findAllByOrderByIdDesc(Pageable pageable);

    // Быстрые агрегатные запросы на уровне БД (защита от OutOfMemoryError)
    @Query("SELECT COUNT(b) FROM Booking b")
    long countTotalBookings();

    @Query("SELECT COALESCE(SUM(b.totalPrice), 0) FROM Booking b")
    long sumTotalIncome();

    @Query("SELECT COUNT(b) FROM Booking b WHERE LOWER(b.status) = LOWER(:status)")
    long countByStatusIgnoreCase(@Param("status") String status);
}

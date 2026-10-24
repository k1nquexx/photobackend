package com.example.photobackend;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    // Ограниченная выборка одобренных отзывов для витрины (защита от OutOfMemory)
    List<Review> findTop50ByApprovedTrueOrderByIdDesc();

    default List<Review> findByApprovedTrue() {
        return findTop50ByApprovedTrueOrderByIdDesc();
    }

    Page<Review> findByApprovedTrueOrderByIdDesc(Pageable pageable);

    // Пагинация для администратора
    Page<Review> findAllByOrderByIdDesc(Pageable pageable);
}

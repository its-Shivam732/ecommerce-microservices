package com.example.ecommerce.product.repository;

import com.example.ecommerce.product.entity.ProcessedReservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedReservationRepository extends JpaRepository<ProcessedReservation, Long> {

    Optional<ProcessedReservation> findByIdempotencyKey(String idempotencyKey);
}

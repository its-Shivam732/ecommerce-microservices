package com.example.ecommerce.product.entity;

import com.example.ecommerce.common.dto.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * Stores the outcome of a previously processed reservation, keyed by the idempotency key
 * supplied by the caller. If the same key arrives again (e.g. order-service retried after a
 * lost response), we return the stored outcome instead of decrementing stock a second time.
 *
 * The unique constraint on idempotency_key is what actually enforces "process once" under
 * concurrency: a duplicate insert fails at the database level.
 */
@Entity
@Table(name = "processed_reservations",
        uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
public class ProcessedReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public ProcessedReservation() {
    }

    public ProcessedReservation(String idempotencyKey, Long productId, Integer quantity,
                                ReservationStatus status) {
        this.idempotencyKey = idempotencyKey;
        this.productId = productId;
        this.quantity = quantity;
        this.status = status;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

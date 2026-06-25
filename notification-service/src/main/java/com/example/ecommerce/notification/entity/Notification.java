package com.example.ecommerce.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    public enum Status {
        SENT,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private String channel;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    private Long relatedOrderId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Notification() {
    }

    public Notification(String recipient, String channel, String subject, String message,
                        Status status, Long relatedOrderId) {
        this.recipient = recipient;
        this.channel = channel;
        this.subject = subject;
        this.message = message;
        this.status = status;
        this.relatedOrderId = relatedOrderId;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getChannel() {
        return channel;
    }

    public String getSubject() {
        return subject;
    }

    public String getMessage() {
        return message;
    }

    public Status getStatus() {
        return status;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

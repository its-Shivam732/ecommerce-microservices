package com.example.ecommerce.notification.dto;

import com.example.ecommerce.notification.entity.Notification;

import java.time.Instant;

public class NotificationResponse {

    private Long id;
    private String recipient;
    private String channel;
    private String subject;
    private String message;
    private String status;
    private Long relatedOrderId;
    private Instant createdAt;

    public static NotificationResponse from(Notification n) {
        NotificationResponse r = new NotificationResponse();
        r.id = n.getId();
        r.recipient = n.getRecipient();
        r.channel = n.getChannel();
        r.subject = n.getSubject();
        r.message = n.getMessage();
        r.status = n.getStatus().name();
        r.relatedOrderId = n.getRelatedOrderId();
        r.createdAt = n.getCreatedAt();
        return r;
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

    public String getStatus() {
        return status;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

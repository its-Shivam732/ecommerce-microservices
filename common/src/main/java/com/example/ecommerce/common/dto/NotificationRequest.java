package com.example.ecommerce.common.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request to send a notification. order-service builds this when an order is placed and
 * posts it to notification-service; it is also the public contract of POST /notifications.
 */
public class NotificationRequest {

    @NotBlank
    private String recipient;

    /** e.g. EMAIL, SMS, IN_APP. Kept as a free string for simplicity. */
    @NotBlank
    private String channel;

    @NotBlank
    private String subject;

    @NotBlank
    private String message;

    /** Optional link back to the order that triggered this notification. */
    private Long relatedOrderId;

    public NotificationRequest() {
    }

    public NotificationRequest(String recipient, String channel, String subject,
                               String message, Long relatedOrderId) {
        this.recipient = recipient;
        this.channel = channel;
        this.subject = subject;
        this.message = message;
        this.relatedOrderId = relatedOrderId;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getRelatedOrderId() {
        return relatedOrderId;
    }

    public void setRelatedOrderId(Long relatedOrderId) {
        this.relatedOrderId = relatedOrderId;
    }
}

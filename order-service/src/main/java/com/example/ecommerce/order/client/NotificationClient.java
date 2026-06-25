package com.example.ecommerce.order.client;

import com.example.ecommerce.common.dto.NotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Calls notification-service. Sending a notification is best-effort: the order is already
 * committed by the time we call, so a notification failure must NOT fail the order. We
 * swallow errors and log them. In a production system this is exactly the hop you'd move
 * off the synchronous path onto an event/queue so it can be retried independently.
 */
@Component
public class NotificationClient {

    private static final Logger log = LoggerFactory.getLogger(NotificationClient.class);

    private final RestClient notificationServiceClient;

    public NotificationClient(RestClient notificationServiceClient) {
        this.notificationServiceClient = notificationServiceClient;
    }

    public void sendOrderNotification(NotificationRequest request) {
        try {
            notificationServiceClient.post()
                    .uri("/notifications")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            log.warn("Failed to send notification for order {} (continuing anyway): {}",
                    request.getRelatedOrderId(), ex.toString());
        }
    }
}

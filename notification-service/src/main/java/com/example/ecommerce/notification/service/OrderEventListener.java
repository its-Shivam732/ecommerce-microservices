package com.example.ecommerce.notification.service;

import com.example.ecommerce.common.dto.NotificationRequest;
import com.example.ecommerce.common.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);
    private final NotificationService notificationService;

    public OrderEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "order-notifications", groupId = "notification-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        log.info("Received OrderPlaced event for order {} via Kafka", event.getOrderId());
        // Tagged channel so you can tell the Kafka-originated row from the REST one in a demo.
        notificationService.send(new NotificationRequest(
                event.getCustomerEmail(), "EMAIL_KAFKA",
                "Order #" + event.getOrderId() + " confirmed (via Kafka)",
                "Hi " + event.getCustomerName() + ", your order #" + event.getOrderId()
                        + " for " + event.getTotalAmount() + " was confirmed.",
                event.getOrderId()));
    }
}
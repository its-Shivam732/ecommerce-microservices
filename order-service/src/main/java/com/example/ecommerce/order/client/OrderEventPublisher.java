package com.example.ecommerce.order.client;

import com.example.ecommerce.common.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);
    private static final String TOPIC = "order-notifications";

    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public OrderEventPublisher(KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /** Best-effort publish; a Kafka failure must not fail an already-confirmed order. */
    public void publishOrderPlaced(OrderPlacedEvent event) {
        try {
            kafkaTemplate.send(TOPIC, String.valueOf(event.getOrderId()), event);
            log.info("Published OrderPlaced event for order {}", event.getOrderId());
        } catch (Exception ex) {
            log.warn("Failed to publish OrderPlaced event for order {}: {}",
                    event.getOrderId(), ex.toString());
        }
    }
}
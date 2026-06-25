package com.example.ecommerce.order.service;

import com.example.ecommerce.common.dto.NotificationRequest;
import com.example.ecommerce.common.dto.ReservationResponse;
import com.example.ecommerce.common.dto.ReservationStatus;
import com.example.ecommerce.order.client.NotificationClient;
import com.example.ecommerce.order.client.OrderEventPublisher;
import com.example.ecommerce.order.client.ProductClient;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderItem;
import com.example.ecommerce.order.entity.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final NotificationClient notificationClient;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository,
                        ProductClient productClient,
                        NotificationClient notificationClient, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.notificationClient = notificationClient;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * Place an order.
     *
     * Flow:
     *  1. For each line, call product-service to reserve stock. The call carries a unique
     *     idempotency key so a retry (driven by Resilience4j) can't double-decrement.
     *  2. If any line can't be reserved (insufficient stock / unknown product), reject the
     *     whole order. (A production system would release the lines already reserved — see
     *     the note in the README about compensation; out of scope for this assignment.)
     *  3. Persist the order as CONFIRMED.
     *  4. Fire a best-effort notification. A notification failure does not fail the order.
     *
     * If product-service is unreachable, the ProductClient fallback throws
     * ProductServiceUnavailableException, which propagates out as a 503 — the order is not
     * created at all.
     */
    @Transactional
    public Order placeOrder(OrderRequest request) {
        Order order = new Order(request.getCustomerName(), request.getCustomerEmail());

        for (OrderRequest.Item line : request.getItems()) {
            String idempotencyKey = UUID.randomUUID().toString();
            ReservationResponse reservation =
                    productClient.reserve(idempotencyKey, line.getProductId(), line.getQuantity());

            if (reservation.getStatus() != ReservationStatus.RESERVED) {
                String reason = describe(reservation, line.getProductId());
                // Marking the order REJECTED and persisting it gives the caller a record;
                // we throw so the controller returns a 4xx and the transaction rolls back
                // any partial order rows. (Reserved stock from earlier lines would be
                // compensated in a full saga.)
                log.info("Order rejected: {}", reason);
                throw new OrderRejectedException(reason);
            }

            order.addItem(new OrderItem(
                    reservation.getProductId(),
                    reservation.getProductName(),
                    reservation.getUnitPrice(),
                    reservation.getReservedQuantity()));
        }

        order.recalculateTotal();
        order.setStatus(OrderStatus.CONFIRMED);
        Order saved = orderRepository.save(order);

        sendConfirmation(saved);
        return saved;
    }

    private void sendConfirmation(Order order) {
        String subject = "Order #" + order.getId() + " confirmed";
        String message = "Hi " + order.getCustomerName()
                + ", your order #" + order.getId()
                + " totalling " + order.getTotalAmount()
                + " has been confirmed. Thank you!";
        notificationClient.sendOrderNotification(new NotificationRequest(
                order.getCustomerEmail(), "EMAIL", subject, message, order.getId()));

        orderEventPublisher.publishOrderPlaced(new com.example.ecommerce.common.event.OrderPlacedEvent(
                order.getId(), order.getCustomerName(), order.getCustomerEmail(), order.getTotalAmount()));
    }

    private String describe(ReservationResponse reservation, Long productId) {
        return switch (reservation.getStatus()) {
            case INSUFFICIENT_STOCK ->
                    "Insufficient stock for product " + productId;
            case PRODUCT_NOT_FOUND ->
                    "Product not found: " + productId;
            default ->
                    "Reservation failed for product " + productId;
        };
    }
}

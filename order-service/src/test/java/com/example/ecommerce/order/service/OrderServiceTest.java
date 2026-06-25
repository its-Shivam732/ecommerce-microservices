package com.example.ecommerce.order.service;

import com.example.ecommerce.common.dto.ReservationResponse;
import com.example.ecommerce.order.client.NotificationClient;
import com.example.ecommerce.order.client.OrderEventPublisher;
import com.example.ecommerce.order.client.ProductClient;
import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.entity.Order;
import com.example.ecommerce.order.entity.OrderStatus;
import com.example.ecommerce.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests And Integration for the order orchestration. ProductClient and NotificationClient are mocked, so
 * no downstream services or network are involved — we test only OrderService's decisions.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductClient productClient;

    @Mock
    private NotificationClient notificationClient;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest singleItemRequest(long productId, int qty) {
        OrderRequest request = new OrderRequest();
        request.setCustomerName("Ada");
        request.setCustomerEmail("ada@example.com");
        OrderRequest.Item item = new OrderRequest.Item();
        item.setProductId(productId);
        item.setQuantity(qty);
        request.setItems(List.of(item));
        return request;
    }

    @Test
    void placeOrder_confirmsAndNotifies_whenReservationSucceeds() {
        when(productClient.reserve(anyString(), anyLong(), anyInt()))
                .thenReturn(ReservationResponse.reserved(1L, "Mouse", new BigDecimal("24.99"), 2));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order result = orderService.placeOrder(singleItemRequest(1L, 2));

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getTotalAmount()).isEqualByComparingTo("49.98");
        verify(orderRepository).save(any(Order.class));
        // Notification is fired for a confirmed order.
        verify(notificationClient).sendOrderNotification(any());
    }

    @Test
    void placeOrder_rejects_whenStockInsufficient() {
        when(productClient.reserve(anyString(), anyLong(), anyInt()))
                .thenReturn(ReservationResponse.insufficientStock(1L, "Mouse"));

        assertThatThrownBy(() -> orderService.placeOrder(singleItemRequest(1L, 999)))
                .isInstanceOf(OrderRejectedException.class);

        // No order persisted, no notification sent on rejection.
        verify(orderRepository, never()).save(any(Order.class));
        verify(notificationClient, never()).sendOrderNotification(any());
    }

    @Test
    void findById_throwsWhenMissing() {
        when(orderRepository.findById(7L)).thenReturn(java.util.Optional.empty());
        assertThatThrownBy(() -> orderService.findById(7L))
                .isInstanceOf(OrderNotFoundException.class);
    }
}

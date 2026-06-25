package com.example.ecommerce.order.controller;

import com.example.ecommerce.common.dto.ReservationResponse;
import com.example.ecommerce.order.client.NotificationClient;
import com.example.ecommerce.order.client.OrderEventPublisher;
import com.example.ecommerce.order.client.ProductClient;
import com.example.ecommerce.order.dto.OrderRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for order placement through the controller and service into H2.
 *
 * ProductClient and NotificationClient are mocked (@MockBean), so the test exercises the order
 * orchestration and persistence without needing product-service or notification-service running
 * (and without the outbound token logic). A stub JwtDecoder avoids a startup call to the issuer.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(OrderControllerIT.TestSecurityConfig.class)
class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductClient productClient;

    @MockBean
    private NotificationClient notificationClient;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        JwtDecoder jwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    private OrderRequest request(long productId, int qty) {
        OrderRequest r = new OrderRequest();
        r.setCustomerName("Ada");
        r.setCustomerEmail("ada@example.com");
        OrderRequest.Item item = new OrderRequest.Item();
        item.setProductId(productId);
        item.setQuantity(qty);
        r.setItems(List.of(item));
        return r;
    }

    @Test
    void placeOrder_requiresAuth() throws Exception {
        mockMvc.perform(post("/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request(1L, 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void placeOrder_succeeds_whenReservationOk() throws Exception {
        when(productClient.reserve(anyString(), anyLong(), anyInt()))
                .thenReturn(ReservationResponse.reserved(1L, "Mouse", new BigDecimal("24.99"), 2));

        mockMvc.perform(post("/orders").with(jwt())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request(1L, 2))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.totalAmount").value(49.98));
    }

    @Test
    void placeOrder_returns422_whenStockInsufficient() throws Exception {
        when(productClient.reserve(anyString(), anyLong(), anyInt()))
                .thenReturn(ReservationResponse.insufficientStock(1L, "Mouse"));

        mockMvc.perform(post("/orders").with(jwt())
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request(1L, 999))))
                .andExpect(status().isUnprocessableEntity());
    }
}

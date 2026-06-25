package com.example.ecommerce.order.client;

import com.example.ecommerce.common.dto.ReservationRequest;
import com.example.ecommerce.common.dto.ReservationResponse;
import com.example.ecommerce.common.dto.ReservationStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Calls product-service to reserve stock, protected by Resilience4j.
 *
 * Resilience split:
 *  - @Retry handles transient faults (a dropped connection, a brief blip). Because we send a
 *    stable Idempotency-Key, retrying the same reservation can't double-decrement stock.
 *  - @CircuitBreaker handles sustained outages: after enough failures it opens and we fail
 *    fast via the fallback instead of hammering a downed service.
 *
 * Important: a 409 (insufficient stock) or 404 (no such product) is a *valid business answer*,
 * not an infrastructure failure. We convert those into normal ReservationResponse objects so
 * they do NOT count toward the circuit breaker's failure rate. Only connection errors and 5xx
 * propagate as exceptions that trip retry/breaker.
 */
@Component
public class ProductClient {

    private static final Logger log = LoggerFactory.getLogger(ProductClient.class);

    private final RestClient productServiceClient;

    public ProductClient(RestClient productServiceClient) {
        this.productServiceClient = productServiceClient;
    }

    @CircuitBreaker(name = "productService", fallbackMethod = "reserveFallback")
    @Retry(name = "productService")
    public ReservationResponse reserve(String idempotencyKey, Long productId, int quantity) {
        try {
            return productServiceClient.post()
                    .uri("/products/reserve")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(new ReservationRequest(productId, quantity))
                    .retrieve()
                    .body(ReservationResponse.class);
        } catch (RestClientResponseException ex) {
            // 4xx responses carry a deserializable body with the business status.
            ReservationResponse body = safeBody(ex);
            if (body != null && body.getStatus() != null) {
                return body;
            }
            // Anything else (5xx without a usable body) is treated as a real failure
            // so retry/circuit-breaker can act on it.
            throw ex;
        }
    }

    /**
     * Fallback invoked when retries are exhausted or the circuit is open. We surface this as a
     * dedicated exception so the order service can reject the order cleanly with a 503.
     */
    @SuppressWarnings("unused")
    private ReservationResponse reserveFallback(String idempotencyKey, Long productId,
                                                int quantity, Throwable t) {
        log.warn("product-service unavailable for reservation (productId={}, key={}): {}",
                productId, idempotencyKey, t.toString());
        throw new ProductServiceUnavailableException(
                "Could not reach product-service to reserve stock", t);
    }

    private ReservationResponse safeBody(RestClientResponseException ex) {
        try {
            return ex.getResponseBodyAs(ReservationResponse.class);
        } catch (Exception parseError) {
            return null;
        }
    }
}

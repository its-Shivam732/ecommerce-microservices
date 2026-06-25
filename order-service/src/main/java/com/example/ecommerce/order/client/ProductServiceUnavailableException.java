package com.example.ecommerce.order.client;

/**
 * Raised by the product client's fallback when product-service can't be reached
 * (circuit breaker open, or retries exhausted). Mapped to HTTP 503 upstream.
 */
public class ProductServiceUnavailableException extends RuntimeException {

    public ProductServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}

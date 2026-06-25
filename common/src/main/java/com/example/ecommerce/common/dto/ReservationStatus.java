package com.example.ecommerce.common.dto;

/**
 * Outcome of a stock-reservation attempt in product-service,
 * shared so order-service can interpret the response without duplicating the type.
 */
public enum ReservationStatus {
    RESERVED,
    INSUFFICIENT_STOCK,
    PRODUCT_NOT_FOUND
}

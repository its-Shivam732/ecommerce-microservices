package com.example.ecommerce.order.entity;

/**
 * Order lifecycle. With the synchronous-reserve design used here, an order moves straight
 * from creation to CONFIRMED (stock reserved) or REJECTED (reservation failed). CANCELLED
 * is available for a later cancel flow.
 */
public enum OrderStatus {
    PENDING,
    CONFIRMED,
    REJECTED,
    CANCELLED
}

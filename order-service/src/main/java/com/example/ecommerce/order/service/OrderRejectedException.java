package com.example.ecommerce.order.service;

/** Thrown when an order can't be placed because reservation failed (stock or product). */
public class OrderRejectedException extends RuntimeException {

    public OrderRejectedException(String message) {
        super(message);
    }
}

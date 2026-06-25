package com.example.ecommerce.product.service;

/** Thrown when a product id does not exist; mapped to HTTP 404 by the exception handler. */
public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(Long id) {
        super("Product not found: " + id);
    }
}

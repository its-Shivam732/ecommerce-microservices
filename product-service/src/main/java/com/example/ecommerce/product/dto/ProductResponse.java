package com.example.ecommerce.product.dto;

import com.example.ecommerce.product.entity.Product;

import java.math.BigDecimal;
import java.time.Instant;

/** API view of a product. Keeps the entity out of the controller layer. */
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer availableStock;
    private Integer reservedStock;
    private Instant createdAt;
    private Instant updatedAt;

    public static ProductResponse from(Product p) {
        ProductResponse r = new ProductResponse();
        r.id = p.getId();
        r.name = p.getName();
        r.description = p.getDescription();
        r.price = p.getPrice();
        r.availableStock = p.getAvailableStock();
        r.reservedStock = p.getReservedStock();
        r.createdAt = p.getCreatedAt();
        r.updatedAt = p.getUpdatedAt();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getAvailableStock() {
        return availableStock;
    }

    public Integer getReservedStock() {
        return reservedStock;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

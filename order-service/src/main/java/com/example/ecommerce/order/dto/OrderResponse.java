package com.example.ecommerce.order.dto;

import com.example.ecommerce.order.entity.Order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponse {

    private Long id;
    private String customerName;
    private String customerEmail;
    private String status;
    private String statusReason;
    private BigDecimal totalAmount;
    private List<Item> items;
    private Instant createdAt;
    private Instant updatedAt;

    public static class Item {
        private Long productId;
        private String productName;
        private BigDecimal unitPrice;
        private Integer quantity;
        private BigDecimal lineTotal;

        public Long getProductId() {
            return productId;
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public BigDecimal getLineTotal() {
            return lineTotal;
        }
    }

    public static OrderResponse from(Order order) {
        OrderResponse r = new OrderResponse();
        r.id = order.getId();
        r.customerName = order.getCustomerName();
        r.customerEmail = order.getCustomerEmail();
        r.status = order.getStatus().name();
        r.statusReason = order.getStatusReason();
        r.totalAmount = order.getTotalAmount();
        r.createdAt = order.getCreatedAt();
        r.updatedAt = order.getUpdatedAt();
        r.items = order.getItems().stream().map(i -> {
            Item item = new Item();
            item.productId = i.getProductId();
            item.productName = i.getProductName();
            item.unitPrice = i.getUnitPrice();
            item.quantity = i.getQuantity();
            item.lineTotal = i.getLineTotal();
            return item;
        }).toList();
        return r;
    }

    public Long getId() {
        return id;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getStatus() {
        return status;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<Item> getItems() {
        return items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

package com.example.ecommerce.common.event;

import java.math.BigDecimal;

public class OrderPlacedEvent {
    private Long orderId;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;

    public OrderPlacedEvent() {}

    public OrderPlacedEvent(Long orderId, String customerName, String customerEmail, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.totalAmount = totalAmount;
    }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
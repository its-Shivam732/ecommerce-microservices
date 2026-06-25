package com.example.ecommerce.common.dto;

import java.math.BigDecimal;

/**
 * Response from product-service after a reservation attempt. Carries the status plus
 * enough product detail (name, unit price) for order-service to build the order line
 * without a second call.
 */
public class ReservationResponse {

    private ReservationStatus status;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer reservedQuantity;
    private String message;

    public ReservationResponse() {
    }

    public ReservationResponse(ReservationStatus status, Long productId, String productName,
                               BigDecimal unitPrice, Integer reservedQuantity, String message) {
        this.status = status;
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.reservedQuantity = reservedQuantity;
        this.message = message;
    }

    public static ReservationResponse reserved(Long productId, String productName,
                                               BigDecimal unitPrice, Integer quantity) {
        return new ReservationResponse(ReservationStatus.RESERVED, productId, productName,
                unitPrice, quantity, "Stock reserved");
    }

    public static ReservationResponse insufficientStock(Long productId, String productName) {
        return new ReservationResponse(ReservationStatus.INSUFFICIENT_STOCK, productId, productName,
                null, 0, "Insufficient stock");
    }

    public static ReservationResponse productNotFound(Long productId) {
        return new ReservationResponse(ReservationStatus.PRODUCT_NOT_FOUND, productId, null,
                null, 0, "Product not found");
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(Integer reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

package com.example.ecommerce.product.controller;

import com.example.ecommerce.common.dto.ReservationRequest;
import com.example.ecommerce.common.dto.ReservationResponse;
import com.example.ecommerce.common.dto.ReservationStatus;
import com.example.ecommerce.product.dto.ProductRequest;
import com.example.ecommerce.product.dto.ProductResponse;
import com.example.ecommerce.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> getAll() {
        return productService.findAll().stream().map(ProductResponse::from).toList();
    }

    @GetMapping("/{id}")
    public ProductResponse getById(@PathVariable Long id) {
        return ProductResponse.from(productService.findById(id));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        ProductResponse created = ProductResponse.from(productService.create(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id,
                                  @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(productService.update(id, request));
    }

    /**
     * Reserve stock for an order. Called by order-service. The Idempotency-Key header makes
     * the call safe to retry: the same key always yields the same outcome without
     * double-decrementing stock.
     *
     * Returns 200 with a RESERVED body on success; 409 Conflict for insufficient stock;
     * 404 for an unknown product. The body always carries the machine-readable status too.
     */
    @PostMapping("/reserve")
    public ResponseEntity<ReservationResponse> reserve(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody ReservationRequest request) {

        ReservationResponse response = productService.reserve(
                idempotencyKey, request.getProductId(), request.getQuantity());

        HttpStatus httpStatus = switch (response.getStatus()) {
            case RESERVED -> HttpStatus.OK;
            case INSUFFICIENT_STOCK -> HttpStatus.CONFLICT;
            case PRODUCT_NOT_FOUND -> HttpStatus.NOT_FOUND;
        };
        return ResponseEntity.status(httpStatus).body(response);
    }
}

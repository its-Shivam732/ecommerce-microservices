package com.example.ecommerce.product.service;

import com.example.ecommerce.common.dto.ReservationResponse;
import com.example.ecommerce.common.dto.ReservationStatus;
import com.example.ecommerce.product.dto.ProductRequest;
import com.example.ecommerce.product.entity.ProcessedReservation;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProcessedReservationRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProcessedReservationRepository processedReservationRepository;

    public ProductService(ProductRepository productRepository,
                          ProcessedReservationRepository processedReservationRepository) {
        this.productRepository = productRepository;
        this.processedReservationRepository = processedReservationRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAll() {
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional
    public Product create(ProductRequest request) {
        Product product = new Product(
                request.getName(),
                request.getDescription(),
                request.getPrice(),
                request.getAvailableStock());
        return productRepository.save(product);
    }

    @Transactional
    public Product update(Long id, ProductRequest request) {
        Product product = findById(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setAvailableStock(request.getAvailableStock());
        return productRepository.save(product);
    }

    /**
     * Reserve stock for a product, idempotently.
     *
     * The whole method runs in one local transaction, so the idempotency record and the
     * stock decrement commit together or not at all. The flow:
     *   1. If we've already processed this idempotency key, return the stored outcome
     *      (a retry must not decrement twice).
     *   2. Otherwise validate the product and stock, decrement on success, and record the
     *      outcome under the key.
     *
     * The @Version field on Product means a concurrent reservation racing on the same row
     * will fail with an optimistic-lock exception rather than silently overselling.
     */
    @Transactional
    public ReservationResponse reserve(String idempotencyKey, Long productId, int quantity) {
        // Step 1: replay a previously processed request.
        var existing = processedReservationRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // Step 2: process for the first time.
        Product product = productRepository.findById(productId).orElse(null);

        ReservationStatus status;
        if (product == null) {
            status = ReservationStatus.PRODUCT_NOT_FOUND;
        } else if (product.getAvailableStock() < quantity) {
            status = ReservationStatus.INSUFFICIENT_STOCK;
        } else {
            product.setAvailableStock(product.getAvailableStock() - quantity);
            product.setReservedStock(product.getReservedStock() + quantity);
            productRepository.save(product);
            status = ReservationStatus.RESERVED;
        }

        // Record the outcome under the idempotency key. The unique constraint guards the
        // race where two identical requests arrive concurrently: the second insert fails,
        // and we fall back to returning the already-stored result.
        try {
            processedReservationRepository.save(
                    new ProcessedReservation(idempotencyKey, productId, quantity, status));
        } catch (DataIntegrityViolationException duplicate) {
            return processedReservationRepository.findByIdempotencyKey(idempotencyKey)
                    .map(this::toResponse)
                    .orElseThrow(() -> duplicate);
        }

        return buildResponse(status, product, productId, quantity);
    }

    private ReservationResponse toResponse(ProcessedReservation record) {
        Product product = productRepository.findById(record.getProductId()).orElse(null);
        return buildResponse(record.getStatus(), product, record.getProductId(),
                record.getQuantity());
    }

    private ReservationResponse buildResponse(ReservationStatus status, Product product,
                                              Long productId, int quantity) {
        return switch (status) {
            case RESERVED -> ReservationResponse.reserved(
                    productId, product.getName(), product.getPrice(), quantity);
            case INSUFFICIENT_STOCK -> ReservationResponse.insufficientStock(
                    productId, product != null ? product.getName() : null);
            case PRODUCT_NOT_FOUND -> ReservationResponse.productNotFound(productId);
        };
    }
}

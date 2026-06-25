package com.example.ecommerce.product.service;

import com.example.ecommerce.common.dto.ReservationResponse;
import com.example.ecommerce.common.dto.ReservationStatus;
import com.example.ecommerce.product.entity.ProcessedReservation;
import com.example.ecommerce.product.entity.Product;
import com.example.ecommerce.product.repository.ProcessedReservationRepository;
import com.example.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the reservation logic in isolation. Repositories are mocked, so these are
 * fast and test only ProductService's behavior, not JPA or the database.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProcessedReservationRepository processedReservationRepository;

    @InjectMocks
    private ProductService productService;

    private Product sampleProduct() {
        Product p = new Product("Mouse", "desc", new BigDecimal("24.99"), 10);
        p.setId(1L);
        return p;
    }

    @Test
    void reserve_decrementsStock_whenAvailable() {
        when(processedReservationRepository.findByIdempotencyKey("key-1"))
                .thenReturn(Optional.empty());
        Product product = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ReservationResponse response = productService.reserve("key-1", 1L, 3);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(response.getReservedQuantity()).isEqualTo(3);
        assertThat(product.getAvailableStock()).isEqualTo(7);
        assertThat(product.getReservedStock()).isEqualTo(3);
        verify(productRepository).save(product);
        verify(processedReservationRepository).save(any(ProcessedReservation.class));
    }

    @Test
    void reserve_returnsInsufficientStock_whenNotEnough() {
        when(processedReservationRepository.findByIdempotencyKey("key-2"))
                .thenReturn(Optional.empty());
        Product product = sampleProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ReservationResponse response = productService.reserve("key-2", 1L, 999);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.INSUFFICIENT_STOCK);
        assertThat(product.getAvailableStock()).isEqualTo(10);
        // Stock not changed; save only records the (failed) idempotency outcome.
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void reserve_returnsProductNotFound_whenMissing() {
        when(processedReservationRepository.findByIdempotencyKey("key-3"))
                .thenReturn(Optional.empty());
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        ReservationResponse response = productService.reserve("key-3", 99L, 1);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PRODUCT_NOT_FOUND);
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void reserve_isIdempotent_replaysStoredOutcomeWithoutDecrementing() {
        Product product = sampleProduct();
        ProcessedReservation prior =
                new ProcessedReservation("key-dup", 1L, 3, ReservationStatus.RESERVED);
        when(processedReservationRepository.findByIdempotencyKey("key-dup"))
                .thenReturn(Optional.of(prior));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ReservationResponse response = productService.reserve("key-dup", 1L, 3);

        assertThat(response.getStatus()).isEqualTo(ReservationStatus.RESERVED);
        // Crucial: a replayed key must NOT decrement stock again.
        assertThat(product.getAvailableStock()).isEqualTo(10);
        verify(productRepository, never()).save(any(Product.class));
        verify(processedReservationRepository, never()).save(any(ProcessedReservation.class));
    }

    @Test
    void findById_throwsWhenMissing() {
        when(productRepository.findById(42L)).thenReturn(Optional.empty());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> productService.findById(42L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}

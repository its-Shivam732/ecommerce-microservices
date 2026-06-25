package com.example.ecommerce.order.controller;

import com.example.ecommerce.order.dto.OrderRequest;
import com.example.ecommerce.order.dto.OrderResponse;
import com.example.ecommerce.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderResponse> getAll() {
        return orderService.findAll().stream().map(OrderResponse::from).toList();
    }

    @GetMapping("/{id}")
    public OrderResponse getById(@PathVariable Long id) {
        return OrderResponse.from(orderService.findById(id));
    }

    @PostMapping
    public ResponseEntity<OrderResponse> place(@Valid @RequestBody OrderRequest request) {
        OrderResponse created = OrderResponse.from(orderService.placeOrder(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}

package com.ecommerce.platform.controller;

import com.ecommerce.platform.domain.Order;
import com.ecommerce.platform.dto.OrderRequest;
import com.ecommerce.platform.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order Management", description = "APIs for managing orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "Create order", description = "Create a new order with synchronous notification")
    public ResponseEntity<Order> createOrder(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrder(request.getUserId(), request.getTotalAmount());
        return ResponseEntity.ok(order);
    }

    @PostMapping("/async")
    @Operation(summary = "Create order async", description = "Create a new order with asynchronous notification")
    public ResponseEntity<Order> createOrderAsync(@Valid @RequestBody OrderRequest request) {
        Order order = orderService.createOrderAsync(request.getUserId(), request.getTotalAmount());
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/ship")
    @Operation(summary = "Ship order", description = "Mark an order as shipped")
    public ResponseEntity<Order> shipOrder(@PathVariable Long orderId) {
        Order order = orderService.shipOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderId}/deliver")
    @Operation(summary = "Deliver order", description = "Mark an order as delivered")
    public ResponseEntity<Order> deliverOrder(@PathVariable Long orderId) {
        Order order = orderService.deliverOrder(orderId);
        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get order", description = "Get order details by ID")
    public ResponseEntity<Order> getOrder(@PathVariable Long orderId) {
        Order order = orderService.getOrder(orderId);
        return ResponseEntity.ok(order);
    }
}

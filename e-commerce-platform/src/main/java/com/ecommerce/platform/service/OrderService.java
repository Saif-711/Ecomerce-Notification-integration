package com.ecommerce.platform.service;

import com.ecommerce.platform.client.NotificationClient;
import com.ecommerce.platform.domain.Order;
import com.ecommerce.platform.dto.NotificationChannel;
import com.ecommerce.platform.model.OrderStatus;
import com.ecommerce.platform.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);
    
    private final OrderRepository orderRepository;
    private final NotificationClient notificationClient;

    @Transactional
    public Order createOrder(Long userId, BigDecimal totalAmount) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order(userId, orderNumber, totalAmount);
        order = orderRepository.save(order);

        log.info("Order created: {}", orderNumber);

        // Send notification to user
        String message = String.format("Order #%s confirmed! Total: $%.2f. Thank you for your purchase.",
                orderNumber, totalAmount);
        notificationClient.sendNotification(userId, NotificationChannel.EMAIL, message);

        return order;
    }

    @Transactional
    public Order createOrderAsync(Long userId, BigDecimal totalAmount) {
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order(userId, orderNumber, totalAmount);
        order = orderRepository.save(order);

        log.info("Order created (async notification): {}", orderNumber);

        // Send notification asynchronously - doesn't block order creation
        String message = String.format("Order #%s confirmed! Total: $%.2f. Thank you for your purchase.",
                orderNumber, totalAmount);
        CompletableFuture<Void> notificationFuture = notificationClient.sendNotificationAsync(
                userId, NotificationChannel.EMAIL, message);

        // Handle async completion (optional - for logging or error handling)
        notificationFuture.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Async notification failed for order {}: {}", orderNumber, ex.getMessage());
            } else {
                log.info("Async notification sent successfully for order {}", orderNumber);
            }
        });

        return order;
    }

    @Transactional
    public Order shipOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setStatus(OrderStatus.SHIPPED);
        order.setShippedAt(LocalDateTime.now());
        order = orderRepository.save(order);
        
        log.info("Order shipped: {}", order.getOrderNumber());
        
        // Send shipping notification
        String message = String.format("Your order #%s has been shipped! Track your package for updates.", 
                order.getOrderNumber());
        notificationClient.sendNotification(order.getUserId(), NotificationChannel.EMAIL, message);
        
        return order;
    }

    @Transactional
    public Order deliverOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
        
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order = orderRepository.save(order);
        
        log.info("Order delivered: {}", order.getOrderNumber());
        
        // Send delivery notification
        String message = String.format("Your order #%s has been delivered! We hope you enjoy your purchase.", 
                order.getOrderNumber());
        notificationClient.sendNotification(order.getUserId(), NotificationChannel.SMS, message);
        
        return order;
    }

    public Order getOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));
    }
}

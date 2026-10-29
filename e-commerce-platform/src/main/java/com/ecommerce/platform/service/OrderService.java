package com.ecommerce.platform.service;

import com.ecommerce.platform.client.NotificationClient;
import com.ecommerce.platform.domain.NotificationStatusRecord;
import com.ecommerce.platform.domain.Order;
import com.ecommerce.platform.dto.NotificationChannel;
import com.ecommerce.platform.model.NotificationStatus;
import com.ecommerce.platform.model.OrderStatus;
import com.ecommerce.platform.repository.NotificationStatusRepository;
import com.ecommerce.platform.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final NotificationClient notificationClient;
    private final NotificationStatusRepository notificationStatusRepository;

    @Transactional
    public Order createOrder(Long userId, BigDecimal totalAmount, String idempotencyKey, String recipientEmail) {
        // Check if order with same idempotency key already exists
        if (idempotencyKey != null && !idempotencyKey.isEmpty()) {
            Order existingOrder = orderRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existingOrder != null) {
                log.info("Order already exists with idempotency key: {}, returning existing order: {}", idempotencyKey, existingOrder.getOrderNumber());
                return existingOrder;
            }
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = new Order(userId, orderNumber, totalAmount);
        order.setIdempotencyKey(idempotencyKey);
        order = orderRepository.save(order);

        // Create notification status record as PENDING
        NotificationStatusRecord statusRecord = new NotificationStatusRecord();
        statusRecord.setOrderId(order.getId());
        statusRecord.setStatus(NotificationStatus.PENDING);
        notificationStatusRepository.save(statusRecord);

        log.info("Order created: {}", orderNumber);

        // Send notification asynchronously - doesn't block order creation
        String message = String.format("Order #%s confirmed! Total: $%.2f. Thank you for your purchase.",
                orderNumber, totalAmount);
        CompletableFuture<Void> notificationFuture = notificationClient.sendNotificationAsync(
                userId, NotificationChannel.EMAIL, message, recipientEmail);

        // Handle async completion (optional - for logging or error handling)
        notificationFuture.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Async notification failed for order {}: {}", orderNumber, ex.getMessage());
                // Update notification status to FAILED
                statusRecord.setStatus(NotificationStatus.FAILED);
                statusRecord.setErrorMessage(ex.getMessage());
                notificationStatusRepository.save(statusRecord);
            } else {
                log.info("Async notification sent successfully for order {}", orderNumber);
                // Update notification status to SENT
                statusRecord.setStatus(NotificationStatus.SENT);
                notificationStatusRepository.save(statusRecord);
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

    public NotificationStatusRecord getNotificationStatus(Long orderId) {
        return notificationStatusRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Notification status not found for order: " + orderId));
    }
}

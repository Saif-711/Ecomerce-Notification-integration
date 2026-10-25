package com.ecommerce.platform.domain;

import com.ecommerce.platform.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String orderNumber;

    @Column(unique = true)
    private String idempotencyKey;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime shippedAt;

    @Column
    private LocalDateTime deliveredAt;

    public Order(Long userId, String orderNumber, BigDecimal totalAmount) {
        this.userId = userId;
        this.orderNumber = orderNumber;
        this.totalAmount = totalAmount;
        this.status = OrderStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }
}

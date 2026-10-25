package com.ecommerce.platform.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequest {

    @NotNull
    private Long userId;

    @NotNull
    private BigDecimal totalAmount;

    private String idempotencyKey;
}

package com.ecommerce.platform.controller;

import com.ecommerce.platform.domain.Payment;
import com.ecommerce.platform.dto.PaymentRequest;
import com.ecommerce.platform.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payment Management", description = "APIs for managing payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Create payment", description = "Create a new payment")
    public ResponseEntity<Payment> createPayment(@Valid @RequestBody PaymentRequest request) {
        Payment payment = paymentService.createPayment(
                request.getUserId(),
                request.getOrderId(),
                request.getAmount(),
                request.getPaymentMethod()
        );
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/{paymentId}")
    @Operation(summary = "Get payment", description = "Get payment details by ID")
    public ResponseEntity<Payment> getPayment(@PathVariable Long paymentId) {
        Payment payment = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(payment);
    }
}

package com.microcommerce.paymentservice.controller;

import com.microcommerce.paymentservice.dto.PaymentRequest;
import com.microcommerce.paymentservice.model.Payment;
import com.microcommerce.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    // X-User-* headers are set by the API gateway from the verified JWT.
    @PostMapping
    public ResponseEntity<Payment> processPayment(@RequestHeader("X-User-Id") Long userId,
                                                  @Valid @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(paymentService.processPayment(request.orderId(), userId));
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable Long orderId,
                                                       @RequestHeader("X-User-Id") Long userId,
                                                       @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(paymentService.getPaymentByOrderId(orderId, userId, "ADMIN".equals(role)));
    }
}

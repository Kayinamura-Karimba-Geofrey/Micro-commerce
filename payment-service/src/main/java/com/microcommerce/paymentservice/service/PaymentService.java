package com.microcommerce.paymentservice.service;

import com.microcommerce.paymentservice.model.Payment;
import com.microcommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    public Payment processPayment(Payment payment) {
        // Mocking payment processing
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setStatus("SUCCESS");
        payment.setPaymentDate(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment getPaymentByOrderId(Long orderId) {
        // Simple mock search
        return paymentRepository.findAll().stream()
                .filter(p -> p.getOrderId().equals(orderId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }
}

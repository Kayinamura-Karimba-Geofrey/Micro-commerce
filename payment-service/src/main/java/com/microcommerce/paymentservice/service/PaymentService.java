package com.microcommerce.paymentservice.service;

import com.microcommerce.paymentservice.model.Payment;
import com.microcommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;

    /**
     * Settles the pending payment created for the caller's order. The amount
     * always comes from the order event, never from the client.
     */
    @Transactional
    public Payment processPayment(Long orderId, Long userId) {
        Payment payment = getPaymentByOrderId(orderId, userId, false);
        if (!"PENDING".equals(payment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Payment already processed");
        }
        // Mocking payment processing
        payment.setStatus("SUCCESS");
        payment.setCreatedAt(LocalDateTime.now());
        return paymentRepository.save(payment);
    }

    public Payment getPaymentByOrderId(Long orderId, Long userId, boolean admin) {
        // Respond 404 for other users' payments so order ids cannot be probed.
        return paymentRepository.findByOrderId(orderId)
                .filter(payment -> admin || userId.equals(payment.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found"));
    }
}

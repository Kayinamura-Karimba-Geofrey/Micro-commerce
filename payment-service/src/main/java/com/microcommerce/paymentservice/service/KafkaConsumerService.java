package com.microcommerce.paymentservice.service;

import com.microcommerce.paymentservice.event.OrderPlacedEvent;
import com.microcommerce.paymentservice.model.Payment;
import com.microcommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {
    private final PaymentRepository paymentRepository;

    @KafkaListener(topics = "order-placed", groupId = "payment-group")
    public void handleOrderPlacedEvent(OrderPlacedEvent event) {
        log.info("Received OrderPlacedEvent: {}", event);
        
        // Auto-create a pending payment record for the new order
        Payment payment = new Payment();
        payment.setOrderId(0L); // We don't have the internal ID here, usually orderNumber is used instead
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setAmount(event.getTotalAmount());
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());
        
        paymentRepository.save(payment);
        log.info("Created pending payment for order: {}", event.getOrderNumber());
    }
}

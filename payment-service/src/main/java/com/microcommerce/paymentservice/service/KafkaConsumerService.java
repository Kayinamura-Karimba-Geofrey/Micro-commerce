package com.microcommerce.paymentservice.service;

import com.microcommerce.common.event.OrderPlacedEvent;
import com.microcommerce.paymentservice.model.Payment;
import com.microcommerce.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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

        // Kafka can redeliver an event; there must only ever be one payment per order.
        if (paymentRepository.existsByOrderId(event.getOrderId())) {
            log.info("Payment for order {} already exists, ignoring duplicate event", event.getOrderNumber());
            return;
        }

        // Auto-create a pending payment record for the new order
        Payment payment = new Payment();
        payment.setOrderId(event.getOrderId());
        payment.setUserId(event.getUserId());
        payment.setTransactionId(UUID.randomUUID().toString());
        payment.setAmount(event.getTotalAmount());
        payment.setStatus("PENDING");
        payment.setCreatedAt(LocalDateTime.now());

        try {
            paymentRepository.save(payment);
        } catch (DataIntegrityViolationException e) {
            // A concurrent delivery won the race on the unique order_id index.
            log.info("Payment for order {} already exists, ignoring duplicate event", event.getOrderNumber());
            return;
        }
        log.info("Created pending payment for order: {}", event.getOrderNumber());
    }
}

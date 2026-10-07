package com.microcommerce.paymentservice.service;

import com.microcommerce.common.event.OrderPlacedEvent;
import com.microcommerce.paymentservice.model.Payment;
import com.microcommerce.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaConsumerServiceTest {

    private static final OrderPlacedEvent EVENT =
            new OrderPlacedEvent(1L, 5L, "ord-1", "a@example.com", new BigDecimal("12.00"));

    @Mock
    private PaymentRepository paymentRepository;
    @InjectMocks
    private KafkaConsumerService consumer;

    @Test
    void createsPendingPaymentFromEvent() {
        consumer.handleOrderPlacedEvent(EVENT);

        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getOrderId()).isEqualTo(1L);
        assertThat(saved.getValue().getUserId()).isEqualTo(5L);
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("12.00");
        assertThat(saved.getValue().getStatus()).isEqualTo("PENDING");
    }

    @Test
    void ignoresRedeliveredEvent() {
        when(paymentRepository.existsByOrderId(1L)).thenReturn(true);

        consumer.handleOrderPlacedEvent(EVENT);

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void ignoresConcurrentDuplicateRejectedByUniqueIndex() {
        when(paymentRepository.save(any())).thenThrow(new DataIntegrityViolationException("ux_payments_order_id"));

        assertThatCode(() -> consumer.handleOrderPlacedEvent(EVENT)).doesNotThrowAnyException();
    }
}

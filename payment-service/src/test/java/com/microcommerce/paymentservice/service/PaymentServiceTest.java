package com.microcommerce.paymentservice.service;

import com.microcommerce.paymentservice.model.Payment;
import com.microcommerce.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;
    @InjectMocks
    private PaymentService paymentService;

    @Test
    void settlesOwnPendingPayment() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment(5L, "PENDING")));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Payment payment = paymentService.processPayment(1L, 5L);

        assertThat(payment.getStatus()).isEqualTo("SUCCESS");
        assertThat(payment.getAmount()).isEqualByComparingTo("12.00");
    }

    @Test
    void cannotPayAnotherUsersOrder() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment(5L, "PENDING")));

        assertThatThrownBy(() -> paymentService.processPayment(1L, 6L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void cannotPayTwice() {
        when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.of(payment(5L, "SUCCESS")));

        assertThatThrownBy(() -> paymentService.processPayment(1L, 5L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        verify(paymentRepository, never()).save(any());
    }

    private static Payment payment(Long userId, String status) {
        return Payment.builder().orderId(1L).userId(userId).amount(new BigDecimal("12.00")).status(status).build();
    }
}

package com.microcommerce.paymentservice.dto;

import jakarta.validation.constraints.NotNull;

public record PaymentRequest(@NotNull Long orderId) {
}

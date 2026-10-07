package com.microcommerce.productservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Quantity to reserve or release, sent by order-service. */
public record StockRequest(@NotNull @Min(1) @Max(1000) Integer quantity) {
}

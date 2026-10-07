package com.microcommerce.orderservice.dto;

/** Body of product-service's stock reserve/release endpoints. */
public record StockRequest(Integer quantity) {
}

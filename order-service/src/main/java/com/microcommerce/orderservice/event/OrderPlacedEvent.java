package com.microcommerce.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    // Internal order id so downstream services can link payments to the order.
    private Long orderId;
    private String orderNumber;
    private String customerEmail;
    private BigDecimal totalAmount;
}

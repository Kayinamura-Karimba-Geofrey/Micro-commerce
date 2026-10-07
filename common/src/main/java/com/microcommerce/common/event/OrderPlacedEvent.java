package com.microcommerce.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Published by order-service on the "order-placed" topic, consumed by payment-service. */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    // Internal order id so downstream services can link payments to the order.
    private Long orderId;
    private Long userId;
    private String orderNumber;
    private String customerEmail;
    private BigDecimal totalAmount;
}

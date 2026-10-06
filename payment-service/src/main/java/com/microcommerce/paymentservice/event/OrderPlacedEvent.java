package com.microcommerce.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPlacedEvent {
    // Must mirror com.microcommerce.orderservice.event.OrderPlacedEvent.
    private Long orderId;
    private Long userId;
    private String orderNumber;
    private String customerEmail;
    private BigDecimal totalAmount;
}

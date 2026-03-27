package com.microcommerce.orderservice.service;

import com.microcommerce.orderservice.client.ProductClient;
import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;

    public Order placeOrder(Order order) {
        // Simple validation and calculation
        BigDecimal total = BigDecimal.ZERO;
        for (var item : order.getOrderItems()) {
            BigDecimal price = productClient.getProductPrice(item.getProductId());
            item.setPrice(price);
            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        order.setTotalAmount(total);
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setStatus("PLACED");
        order.setCreatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }
}

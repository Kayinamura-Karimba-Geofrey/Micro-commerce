package com.microcommerce.orderservice.service;

import com.microcommerce.orderservice.client.ProductClient;
import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
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
    private final KafkaProducerService kafkaProducerService;

    @CircuitBreaker(name = "productService", fallbackMethod = "placeOrderFallback")
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
        Order savedOrder = orderRepository.save(order);
        
        // Publish event to Kafka
        kafkaProducerService.sendOrderPlacedEvent(new OrderPlacedEvent(
            savedOrder.getOrderNumber(),
            "customer@example.com", // Mock email for now
            savedOrder.getTotalAmount()
        ));

        return savedOrder;
    }

    public Order placeOrderFallback(Order order, Exception e) {
        order.setStatus("FAILED - PRODUCT SERVICE DOWN");
        return order;
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }
}

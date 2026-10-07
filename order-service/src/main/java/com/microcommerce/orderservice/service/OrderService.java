package com.microcommerce.orderservice.service;

import com.microcommerce.orderservice.client.ProductClient;
import com.microcommerce.common.event.OrderPlacedEvent;
import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.model.OrderItem;
import com.microcommerce.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaProducerService kafkaProducerService;

    @CircuitBreaker(name = "productService", fallbackMethod = "placeOrderFallback")
    public Order placeOrder(Order request, Long userId, String userEmail) {
        // Build a fresh order from the client's product ids and quantities only.
        // Copying the request entity as-is would let clients set the id (and
        // overwrite another order), the owner, the status or the item prices.
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        for (var requested : request.getOrderItems()) {
            BigDecimal price = productClient.getProductPrice(requested.getProductId());
            items.add(OrderItem.builder()
                    .productId(requested.getProductId())
                    .quantity(requested.getQuantity())
                    .price(price)
                    .build());
            total = total.add(price.multiply(BigDecimal.valueOf(requested.getQuantity())));
        }
        Order order = Order.builder()
                .orderItems(items)
                .userId(userId)
                .totalAmount(total)
                .orderNumber(UUID.randomUUID().toString())
                .status("PLACED")
                .createdAt(LocalDateTime.now())
                .build();
        Order savedOrder = orderRepository.save(order);
        
        // Publish event to Kafka
        kafkaProducerService.sendOrderPlacedEvent(new OrderPlacedEvent(
            savedOrder.getId(),
            savedOrder.getUserId(),
            savedOrder.getOrderNumber(),
            userEmail,
            savedOrder.getTotalAmount()
        ));

        return savedOrder;
    }

    public Order placeOrderFallback(Order request, Long userId, String userEmail, Exception e) {
        return Order.builder()
                .userId(userId)
                .status("FAILED - PRODUCT SERVICE DOWN")
                .build();
    }

    public Order getOrderById(Long id, Long userId, boolean admin) {
        // Respond 404 for other users' orders so ids cannot be probed.
        return orderRepository.findById(id)
                .filter(order -> admin || userId.equals(order.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }
}

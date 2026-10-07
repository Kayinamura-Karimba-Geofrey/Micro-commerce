package com.microcommerce.orderservice.service;

import com.microcommerce.common.event.OrderPlacedEvent;
import com.microcommerce.orderservice.client.ProductClient;
import com.microcommerce.orderservice.dto.StockRequest;
import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.model.OrderItem;
import com.microcommerce.orderservice.repository.OrderRepository;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OrderService {
    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final KafkaProducerService kafkaProducerService;

    // ResponseStatusExceptions are client errors (unknown product, no stock) and
    // are excluded from the circuit breaker in application.properties.
    @CircuitBreaker(name = "productService", fallbackMethod = "placeOrderFallback")
    public Order placeOrder(Order request, Long userId, String userEmail) {
        // Build a fresh order from the client's product ids and quantities only.
        // Copying the request entity as-is would let clients set the id (and
        // overwrite another order), the owner, the status or the item prices.
        BigDecimal total = BigDecimal.ZERO;
        List<OrderItem> items = new ArrayList<>();
        Order savedOrder;
        try {
            for (var requested : request.getOrderItems()) {
                BigDecimal price = reserveStock(requested.getProductId(), requested.getQuantity());
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
            savedOrder = orderRepository.save(order);
        } catch (RuntimeException e) {
            // Give back what was already reserved so a failed order holds no stock.
            items.forEach(this::releaseStock);
            throw e;
        }

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
        if (e instanceof ResponseStatusException clientError) {
            throw clientError;
        }
        log.warn("Placing order failed because product-service is unavailable", e);
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Product service is unavailable, please try again later");
    }

    public Order getOrderById(Long id, Long userId, boolean admin) {
        // Respond 404 for other users' orders so ids cannot be probed.
        return orderRepository.findById(id)
                .filter(order -> admin || userId.equals(order.getUserId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
    }

    private BigDecimal reserveStock(Long productId, int quantity) {
        try {
            return productClient.reserveStock(productId, new StockRequest(quantity));
        } catch (FeignException.NotFound e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Product " + productId + " does not exist");
        } catch (FeignException.Conflict e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Insufficient stock for product " + productId);
        }
    }

    private void releaseStock(OrderItem item) {
        try {
            productClient.releaseStock(item.getProductId(), new StockRequest(item.getQuantity()));
        } catch (RuntimeException e) {
            log.error("Could not release {} units of product {}", item.getQuantity(), item.getProductId(), e);
        }
    }
}

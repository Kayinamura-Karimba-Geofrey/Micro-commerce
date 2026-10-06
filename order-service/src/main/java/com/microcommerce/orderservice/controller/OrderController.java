package com.microcommerce.orderservice.controller;

import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    // X-User-* headers are set by the API gateway from the verified JWT.
    @PostMapping
    public ResponseEntity<Order> placeOrder(@RequestHeader("X-User-Id") Long userId,
                                            @RequestHeader("X-User-Email") String userEmail,
                                            @Valid @RequestBody Order order) {
        return ResponseEntity.ok(orderService.placeOrder(order, userId, userEmail));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrderById(@PathVariable Long id,
                                              @RequestHeader("X-User-Id") Long userId,
                                              @RequestHeader("X-User-Role") String role) {
        return ResponseEntity.ok(orderService.getOrderById(id, userId, "ADMIN".equals(role)));
    }
}

package com.microcommerce.orderservice.client;

import com.microcommerce.orderservice.dto.StockRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "product-service")
public interface ProductClient {
    /** Takes the quantity out of stock and returns the unit price; 404 unknown product, 409 not enough stock. */
    @PostMapping("/api/v1/products/{id}/reserve")
    BigDecimal reserveStock(@PathVariable("id") Long id, @RequestBody StockRequest request);

    @PostMapping("/api/v1/products/{id}/release")
    void releaseStock(@PathVariable("id") Long id, @RequestBody StockRequest request);
}

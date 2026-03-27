package com.microcommerce.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.math.BigDecimal;

@FeignClient(name = "product-service")
public interface ProductClient {
    @GetMapping("/api/v1/products/{id}/price")
    BigDecimal getProductPrice(@PathVariable("id") Long id);

    @GetMapping("/api/v1/products/{id}/stock")
    Integer getProductStock(@PathVariable("id") Long id);
}

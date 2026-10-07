package com.microcommerce.productservice.controller;

import com.microcommerce.productservice.dto.StockRequest;
import com.microcommerce.productservice.model.Product;
import com.microcommerce.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @PostMapping
    public Product createProduct(@Valid @RequestBody Product product) {
        // Always create: a client-supplied id would otherwise overwrite an existing product.
        product.setId(null);
        return productService.saveProduct(product);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/price")
    public BigDecimal getProductPrice(@PathVariable Long id) {
        return productService.getProductById(id).getPrice();
    }

    @GetMapping("/{id}/stock")
    public Integer getProductStock(@PathVariable Long id) {
        return productService.getProductById(id).getStockLevel();
    }

    // Called by order-service. Through the gateway these POSTs require the ADMIN role.
    @PostMapping("/{id}/reserve")
    public BigDecimal reserveStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return productService.reserveStock(id, request.quantity());
    }

    @PostMapping("/{id}/release")
    public ResponseEntity<Void> releaseStock(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        productService.releaseStock(id, request.quantity());
        return ResponseEntity.noContent().build();
    }
}

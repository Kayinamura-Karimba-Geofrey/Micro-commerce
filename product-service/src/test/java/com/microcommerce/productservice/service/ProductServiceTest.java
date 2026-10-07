package com.microcommerce.productservice.service;

import com.microcommerce.productservice.model.Product;
import com.microcommerce.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;
    @InjectMocks
    private ProductService productService;

    @Test
    void reserveReturnsPriceWhenStockIsAvailable() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));
        when(productRepository.decrementStock(1L, 3)).thenReturn(1);

        assertThat(productService.reserveStock(1L, 3)).isEqualByComparingTo("9.99");
    }

    @Test
    void reserveFailsWithConflictWhenStockIsInsufficient() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product()));
        when(productRepository.decrementStock(1L, 3)).thenReturn(0);

        assertThatThrownBy(() -> productService.reserveStock(1L, 3))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void reserveFailsWithNotFoundForUnknownProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.reserveStock(1L, 3))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verify(productRepository, never()).decrementStock(anyLong(), anyInt());
    }

    @Test
    void unknownProductIsNotFound() {
        when(productRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static Product product() {
        return Product.builder().id(1L).name("Mug").price(new BigDecimal("9.99")).stockLevel(5).build();
    }
}

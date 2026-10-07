package com.microcommerce.orderservice.service;

import com.microcommerce.common.event.OrderPlacedEvent;
import com.microcommerce.orderservice.client.ProductClient;
import com.microcommerce.orderservice.dto.StockRequest;
import com.microcommerce.orderservice.model.Order;
import com.microcommerce.orderservice.model.OrderItem;
import com.microcommerce.orderservice.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductClient productClient;
    @Mock
    private KafkaProducerService kafkaProducerService;
    @InjectMocks
    private OrderService orderService;

    @Test
    void placeOrderUsesServerSidePricesAndIgnoresClientFields() {
        when(productClient.reserveStock(eq(1L), any())).thenReturn(new BigDecimal("10.00"));
        when(productClient.reserveStock(eq(2L), any())).thenReturn(new BigDecimal("2.50"));
        when(orderRepository.save(any())).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(42L);
            return order;
        });
        Order request = Order.builder()
                .id(7L).userId(999L).status("PAID").totalAmount(BigDecimal.ONE)
                .orderItems(List.of(item(1L, 2, "0.01"), item(2L, 4, null)))
                .build();

        Order order = orderService.placeOrder(request, 5L, "a@example.com");

        assertThat(order.getId()).isEqualTo(42L);
        assertThat(order.getUserId()).isEqualTo(5L);
        assertThat(order.getStatus()).isEqualTo("PLACED");
        assertThat(order.getTotalAmount()).isEqualByComparingTo("30.00");
        assertThat(order.getOrderItems()).extracting(OrderItem::getPrice)
                .usingElementComparator(BigDecimal::compareTo)
                .containsExactly(new BigDecimal("10.00"), new BigDecimal("2.50"));

        ArgumentCaptor<OrderPlacedEvent> event = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaProducerService).sendOrderPlacedEvent(event.capture());
        assertThat(event.getValue().getOrderId()).isEqualTo(42L);
        assertThat(event.getValue().getUserId()).isEqualTo(5L);
        assertThat(event.getValue().getTotalAmount()).isEqualByComparingTo("30.00");
    }

    @Test
    void outOfStockReleasesEarlierReservationsAndReturnsConflict() {
        when(productClient.reserveStock(eq(1L), any())).thenReturn(BigDecimal.TEN);
        when(productClient.reserveStock(eq(2L), any())).thenThrow(feignError(409));
        Order request = Order.builder().orderItems(List.of(item(1L, 3, null), item(2L, 1, null))).build();

        assertThatThrownBy(() -> orderService.placeOrder(request, 5L, "a@example.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        verify(productClient).releaseStock(1L, new StockRequest(3));
        verify(productClient, never()).releaseStock(eq(2L), any());
        verify(orderRepository, never()).save(any());
        verify(kafkaProducerService, never()).sendOrderPlacedEvent(any());
    }

    @Test
    void unknownProductIsABadRequest() {
        when(productClient.reserveStock(eq(1L), any())).thenThrow(feignError(404));
        Order request = Order.builder().orderItems(List.of(item(1L, 1, null))).build();

        assertThatThrownBy(() -> orderService.placeOrder(request, 5L, "a@example.com"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void fallbackKeepsClientErrors() {
        ResponseStatusException conflict = new ResponseStatusException(HttpStatus.CONFLICT);

        assertThatThrownBy(() -> orderService.placeOrderFallback(new Order(), 5L, "a@example.com", conflict))
                .isSameAs(conflict);
    }

    @Test
    void fallbackReportsServiceUnavailableInsteadOfAFailedOrder() {
        assertThatThrownBy(() -> orderService.placeOrderFallback(new Order(), 5L, "a@example.com",
                new ConnectException("refused")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));
    }

    @Test
    void otherUsersOrdersAreNotFound() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(Order.builder().id(1L).userId(5L).build()));

        assertThatThrownBy(() -> orderService.getOrderById(1L, 6L, false))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThat(orderService.getOrderById(1L, 6L, true).getUserId()).isEqualTo(5L);
    }

    private static OrderItem item(Long productId, int quantity, String price) {
        return OrderItem.builder()
                .productId(productId)
                .quantity(quantity)
                .price(price == null ? null : new BigDecimal(price))
                .build();
    }

    private static FeignException feignError(int status) {
        Request request = Request.create(Request.HttpMethod.POST, "/api/v1/products/x/reserve",
                new HashMap<>(), null, StandardCharsets.UTF_8, null);
        return FeignException.errorStatus("ProductClient#reserveStock",
                feign.Response.builder().status(status).request(request).headers(new HashMap<>()).build());
    }
}

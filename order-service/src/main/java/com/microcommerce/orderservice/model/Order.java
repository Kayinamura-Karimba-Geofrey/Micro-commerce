package com.microcommerce.orderservice.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderNumber;

    @OneToMany(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_id")
    @NotEmpty
    @Size(max = 100)
    private List<@Valid OrderItem> orderItems;

    private Long userId;

    private BigDecimal totalAmount;

    private String status;

    private LocalDateTime createdAt;
}

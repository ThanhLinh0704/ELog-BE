package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 50)
    private String sku;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal unitWeightKg;

    @Column(name = "unit_volume_m3", nullable = false, precision = 10, scale = 6)
    private BigDecimal unitVolumeM3;

    @Column(name = "line_weight_kg", nullable = false, precision = 10, scale = 3)
    private BigDecimal lineWeightKg;

    @Column(name = "line_volume_m3", nullable = false, precision = 12, scale = 6)
    private BigDecimal lineVolumeM3;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}

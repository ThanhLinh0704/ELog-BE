package com.elog.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 3)
    private BigDecimal weightKg;

    @Column(name = "length_m", nullable = false, precision = 8, scale = 4)
    private BigDecimal lengthM;

    @Column(name = "width_m", nullable = false, precision = 8, scale = 4)
    private BigDecimal widthM;

    @Column(name = "height_m", nullable = false, precision = 8, scale = 4)
    private BigDecimal heightM;

    @Column(name = "volume_m3", nullable = false, precision = 10, scale = 6)
    private BigDecimal volumeM3;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "shape", length = 100)
    private String shape;

    @Column(name = "is_fragile", nullable = false)
    @Builder.Default
    private Boolean isFragile = false;

    @Column(name = "package_image_url", length = 512)
    private String packageImageUrl;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}

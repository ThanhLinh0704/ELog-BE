package com.elog.dto.response.product;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String sku;
    private String productName;
    private String brand;
    private String productGroup;
    private String productType;
    private BigDecimal capacityValue;
    private BigDecimal weightKg;
    private BigDecimal lengthM;
    private BigDecimal widthM;
    private BigDecimal heightM;
    private BigDecimal volumeM3;
    private Boolean isActive;
    private String shape;
    private Boolean isFragile;
    private String packageImageUrl;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

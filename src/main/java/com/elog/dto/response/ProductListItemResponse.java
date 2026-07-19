package com.elog.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductListItemResponse {

    private Long id;
    private String sku;
    private String productName;
    private BigDecimal weightKg;
    private BigDecimal volumeM3;
    private Boolean isActive;
    private String shape;
    private Boolean isFragile;
    private String packageImageUrl;
    private String description;
}

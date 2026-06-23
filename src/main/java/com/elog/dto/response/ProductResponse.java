package com.elog.dto.response;

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
    private BigDecimal weightKg;
    private BigDecimal lengthM;
    private BigDecimal widthM;
    private BigDecimal heightM;
    private BigDecimal volumeM3;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

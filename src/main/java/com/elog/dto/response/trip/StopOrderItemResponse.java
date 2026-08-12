package com.elog.dto.response.trip;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StopOrderItemResponse {
    private Long orderId;
    private String orderRef;
    private String sku;
    private String productName;
    private Integer quantity;
    private BigDecimal weightKg;
    private BigDecimal volumeM3;
}

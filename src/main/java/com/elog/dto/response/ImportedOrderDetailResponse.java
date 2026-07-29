package com.elog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ImportedOrderDetailResponse {
    private String orderRef;
    private LocalDate deliveryDate;
    private String storeCode;
    private String storeName;
    private String sku;
    private String productName;
    private Integer quantity;
    private BigDecimal weightKg;
    private BigDecimal volumeM3;
    private String deliveryTimeWindow;
    private String recipientName;
    private String recipientPhone;
    private String notes;
}

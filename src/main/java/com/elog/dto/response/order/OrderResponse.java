package com.elog.dto.response.order;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderResponse {

    private Long id;
    private Long batchId;
    private String orderRef;
    private LocalDate deliveryDate;
    private String status;

    private RouteSummaryDto route;
    private StoreSummaryDto store;

    private String recipientName;
    private String recipientPhone;
    private String deliveryTimeWindow;
    private String notes;

    private Integer totalItems;
    private Integer totalQuantity;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeM3;

    private List<OrderItemDetailDto> items;
    private Long tripDraftId;

    private LocalDateTime createdAt;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RouteSummaryDto {
        private Long id;
        private String code;
        private String name;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class StoreSummaryDto {
        private Long id;
        private String code;
        private String name;
        private String address;
        private String provinceCode;
        private String districtCode;
        private String wardCode;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderItemDetailDto {
        private Long id;
        private Long productId;
        private String sku;
        private String productName;
        private Integer quantity;
        private BigDecimal unitWeightKg;
        private BigDecimal unitVolumeM3;
        private BigDecimal lineWeightKg;
        private BigDecimal lineVolumeM3;
    }
}

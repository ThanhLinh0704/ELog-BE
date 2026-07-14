package com.elog.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStopResponse {
    private Long tripStopId;
    private Long routeStopId;
    private Long tripDraftStopId;
    private Integer sequenceOrder;
    private String storeCode;
    private String storeName;
    private LocalDateTime plannedEta;
    private String status;
    private BigDecimal stopWeightKg;
    private BigDecimal stopVolumeM3;
    private String notes;
}


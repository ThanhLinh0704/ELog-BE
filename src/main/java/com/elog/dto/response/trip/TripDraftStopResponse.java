package com.elog.dto.response.trip;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripDraftStopResponse {
    private Long tripDraftStopId;
    private Integer sequenceNo;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private Boolean isActive;
    private Integer orderCount;
    private LocalDateTime plannedEta;
    private String overrideNote;
    private Long routeStopId;
    private BigDecimal stopVolumeM3;
    private BigDecimal stopWeightKg;
    private BigDecimal distanceFromPrevKm;
    private Integer travelTimeFromPrevMin;
    private BigDecimal estimatedDistanceKm;
    private Integer estimatedTravelMin;
}



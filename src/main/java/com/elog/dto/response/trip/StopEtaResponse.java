package com.elog.dto.response.trip;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StopEtaResponse {
    private Long tripDraftStopId;
    private Integer sequenceNo;
    private String storeCode;
    private LocalDateTime plannedEta;
    private java.math.BigDecimal distanceFromPrevKm;
    private Integer travelTimeFromPrevMin;
    private java.math.BigDecimal estimatedDistanceKm;
    private Integer estimatedTravelMin;
}



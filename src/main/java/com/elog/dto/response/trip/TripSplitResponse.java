package com.elog.dto.response.trip;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSplitResponse {
    private Long tripDraftId;
    private Integer tripsCreated;
    private List<TripSummary> trips;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TripSummary {
        private Long tripId;
        private String plateNumber;
        private String vehicleType;
        private Integer stopCount;
        private java.math.BigDecimal totalVolumeM3;
        private java.math.BigDecimal totalWeightKg;
    }
}

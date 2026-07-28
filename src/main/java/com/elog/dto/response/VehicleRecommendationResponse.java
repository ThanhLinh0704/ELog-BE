package com.elog.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRecommendationResponse {

    /**
     * "SINGLE_VEHICLE" or "TWO_VEHICLE"
     */
    private String planType;

    /**
     * 1 vehicle for single-vehicle plan, 2 for two-vehicle plan.
     */
    private List<RecommendedVehicleDto> vehicles;

    /**
     * Stop allocation per vehicle (only used for TWO_VEHICLE plans).
     */
    private List<SubTripDto> subTrips;

    /**
     * Overall score of this recommendation plan.
     */
    private BigDecimal totalScore;

    /**
     * Human-readable explanation of why this plan was recommended.
     */
    private String explanation;

    // ── Inner DTOs ────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendedVehicleDto {
        private Long vehicleId;
        private String vehicleCode;
        private String plateNumber;
        private String vehicleType;
        private BigDecimal payloadKg;
        private BigDecimal maxVolumeM3;
        private BigDecimal costPerKm;
        private BigDecimal averageSpeedKmh;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubTripDto {
        private String label; // e.g. "Sub-trip A", "Sub-trip B"
        private Long vehicleId;
        private List<Integer> stopSequenceNos; // which stops are assigned
        private BigDecimal subTotalVolumeM3;
        private BigDecimal subTotalWeightKg;
        private BigDecimal subScore;
    }
}

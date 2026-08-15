package com.elog.dto.response.trip;

import com.elog.dto.response.vehicle.VehicleRecommendationResponse;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResultResponse {

    private Long tripDraftId;

    /**
     * "SINGLE_VEHICLE", "TWO_VEHICLE", or "NO_PLAN"
     */
    private String planType;

    /**
     * Top-3 recommended plans (may be empty if no feasible plan).
     */
    private List<VehicleRecommendationResponse> recommendations;

    /**
     * Summary message or reason when no plan is available.
     */
    private String message;

    /**
     * Violated constraints when no plan is feasible.
     */
    private List<String> violatedConstraints;
}

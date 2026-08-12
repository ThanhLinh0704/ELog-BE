package com.elog.service;

import com.elog.dto.response.trip.RecommendationResultResponse;

/**
 * Recommendation Engine — suggests Top-3 vehicle plans for a TripDraft.
 * Tries single-vehicle first (FT-06); falls back to two-vehicle (FT-07) if needed.
 */
public interface RecommendationService {

    /**
     * Generate Top-3 vehicle recommendation plans for a given TripDraft.
     *
     * @param tripDraftId ID of the TripDraft (must have active stops with ETA calculated)
     * @return Top-3 recommendations (single or two-vehicle), or NO_PLAN if infeasible
     */
    RecommendationResultResponse recommendTop3(Long tripDraftId);

    /**
     * Lightweight feasibility check — reuses the two-vehicle algorithm from recommendTop3(),
     * but does NOT write Planning History and does NOT build full DTOs.
     * Used by CapacityValidationService to decide whether to promote status to VALIDATED
     * when no single vehicle can accommodate the load.
     */
    boolean isTwoVehicleFeasible(Long tripDraftId);
}

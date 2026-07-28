package com.elog.service;

import com.elog.dto.response.RecommendationResultResponse;

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
}

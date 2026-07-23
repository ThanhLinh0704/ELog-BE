package com.elog.service;

import java.util.Map;

public interface DepartureAdjustmentService {
    /**
     * Calculates recommended departure time from warehouse to minimize/eliminate time window violations.
     * @param tripDraftId ID of the TripDraft
     * @return Map containing "suggestedDepartureTime", "reason", and "hasViolations".
     */
    Map<String, Object> calculateOptimalDepartureTime(Long tripDraftId);
}

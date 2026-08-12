package com.elog.service;

import com.elog.dto.response.trip.StopEtaResponse;

import java.time.LocalTime;
import java.util.List;

/**
 * ETA calculation contract — BE-1 and BE-2 共同定义.
 * Calculates sequential linear ETA for active stops using Haversine GPS distance.
 */
public interface EtaCalculationService {

    /**
     * Calculate ETA for all active stops of a TripDraft.
     * Persists results to trip_draft_stops.planned_eta.
     *
     * @param tripDraftId   ID of the TripDraft (status must be DRAFT)
     * @param departureTime planned departure time from warehouse
     * @return list of stop ETA results
     */
    List<StopEtaResponse> calculateAndPersist(Long tripDraftId, LocalTime departureTime);
}

package com.elog.service;

import com.elog.dto.response.trip.TripOutcomeResponse;

import java.util.List;

public interface TripOutcomeService {

    /**
     * Get list of submitted trip outcomes awaiting Dispatcher validation.
     */
    List<TripOutcomeResponse> getSubmittedOutcomes();

    /**
     * Dispatcher validates a submitted trip outcome (SUBMITTED -> VALIDATED).
     * Only VALIDATED outcomes enter historical datasets.
     */
    TripOutcomeResponse validateOutcome(Long outcomeId, String dispatcherUsername);

    /**
     * Dispatcher amends an outcome with audit reason (version incremented).
     */
    TripOutcomeResponse amendOutcome(Long outcomeId, String amendmentReason, String dispatcherUsername);
}

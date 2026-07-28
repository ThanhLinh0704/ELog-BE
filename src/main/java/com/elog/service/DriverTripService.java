package com.elog.service;

import com.elog.dto.request.UpdateOrderResultRequest;
import com.elog.dto.response.DriverTripResponse;
import com.elog.dto.response.TripOutcomeResponse;

public interface DriverTripService {

    /**
     * Get current active trip assigned to driver (including LIFO guidance).
     */
    DriverTripResponse getActiveTrip(String driverUsername);

    /**
     * Driver starts the assigned trip (ASSIGNED -> IN_PROGRESS).
     */
    DriverTripResponse startTrip(Long executionId, String driverUsername);

    /**
     * Update order delivery result at a stop (PENDING -> DELIVERED / FAILED / PARTIAL).
     * Enforces mandatory reason for FAILED and PARTIALLY_DELIVERED.
     */
    DriverTripResponse updateOrderResult(Long executionId, Long orderId, UpdateOrderResultRequest request, String driverUsername);

    /**
     * Driver completes trip after all orders reach terminal state.
     * Generates a SUBMITTED TripOutcome.
     */
    TripOutcomeResponse completeTrip(Long executionId, String driverUsername);
}

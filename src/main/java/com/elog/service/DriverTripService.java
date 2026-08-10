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
     * Driver arrives at a delivery stop.
     */
    DriverTripResponse arriveAtStop(Long executionId, Long stopId, String driverUsername);

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

    /**
     * Driver confirms returning to warehouse. Releases vehicle back to AVAILABLE.
     */
    DriverTripResponse returnToWarehouse(Long executionId, String driverUsername);

    /**
     * Get completed trips for driver that are waiting for return to warehouse confirmation.
     */
    java.util.List<DriverTripResponse> getPendingReturnTrips(String driverUsername);

    /**
     * Admin / Dispatcher override for a trip execution (FORCE_RETURN or FORCE_COMPLETE_AND_RETURN).
     */
    DriverTripResponse adminOverrideTripExecution(Long executionId, com.elog.dto.request.AdminTripOverrideRequest request, String adminUsername);
}

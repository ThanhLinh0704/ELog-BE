package com.elog.service;

import com.elog.dto.request.trip.CancelTripRequest;
import com.elog.dto.request.trip.TripAssignmentPatchRequest;
import com.elog.dto.request.trip.TripAssignRequest;
import com.elog.dto.request.trip.TripSplitAssignRequest;
import com.elog.dto.response.trip.TripResponse;
import com.elog.dto.response.trip.TripSplitResponse;
import com.elog.dto.response.user.AvailableDriverResponse;
import com.elog.dto.response.user.DriverTripCalendarDayResponse;
import com.elog.dto.response.vehicle.EligibleVehiclesResponse;
import com.elog.dto.response.vehicle.FleetCapacityCheckResponse;

import java.time.LocalDate;
import java.util.List;

public interface TripService {

    // US-15 TASK-02 — Vehicle Assignment
    EligibleVehiclesResponse getEligibleVehicles(Long tripDraftId);

    EligibleVehiclesResponse getEligibleVehiclesForStops(Long tripDraftId, List<Long> stopIds);

    List<AvailableDriverResponse> getAvailableDrivers(LocalDate date);

    TripResponse assignVehicleAndDriver(Long tripDraftId, TripAssignRequest request, String currentUsername);

    List<TripResponse> getTripsByTripDraftId(Long tripDraftId);

    // US-15 TASK-03 — Trip Splitting
    TripSplitResponse assignSplit(Long tripDraftId, TripSplitAssignRequest request, String currentUsername);

    // US-15 TASK-03 — Fleet Shortfall Check (BR-08)
    FleetCapacityCheckResponse checkFleetCapacity(LocalDate date);

    // US-16 TASK-02 — Dispatch
    TripResponse dispatchTrip(Long tripId, String currentUsername);

    // Cancel a DISPATCHED trip that hasn't started yet — releases vehicle/driver immediately.
    TripResponse cancelTrip(Long tripId, CancelTripRequest request, String currentUsername);

    String getHandoverSlipHtml(Long tripId);

    // US-16 — Driver view
    List<TripResponse> getDriverTrips(String username, LocalDate date, String status);

    TripResponse getTripById(Long tripId);

    TripResponse updateAssignment(Long tripId, TripAssignmentPatchRequest request, String currentUsername);

    List<DriverTripCalendarDayResponse> getDriverTripCalendar(String username, java.time.YearMonth month);
}


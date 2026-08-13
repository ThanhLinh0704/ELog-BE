package com.elog.service;

import com.elog.dto.request.TripAssignRequest;
import com.elog.dto.request.TripSplitAssignRequest;
import com.elog.dto.request.TripAssignmentPatchRequest;
import com.elog.dto.response.*;

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

    String getHandoverSlipHtml(Long tripId);

    // US-16 — Driver view
    List<TripResponse> getDriverTrips(String username, LocalDate date, String status);

    TripResponse getTripById(Long tripId);

    TripResponse updateAssignment(Long tripId, TripAssignmentPatchRequest request, String currentUsername);

    List<DriverTripCalendarDayResponse> getDriverTripCalendar(String username, java.time.YearMonth month);
}


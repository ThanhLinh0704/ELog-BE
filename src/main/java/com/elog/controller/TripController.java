package com.elog.controller;

import com.elog.dto.request.TripAssignRequest;
import com.elog.dto.request.TripSplitAssignRequest;
import com.elog.dto.request.TripAssignmentPatchRequest;
import com.elog.dto.response.*;
import com.elog.service.TripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Trips", description = "US-15 Vehicle Assignment & US-16 Dispatch APIs")
public class TripController {

    private final TripService tripService;

    @GetMapping("/api/v1/trip-drafts/{id}/eligible-vehicles")
    @Operation(summary = "Get eligible and ineligible vehicles for a validated trip draft")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<EligibleVehiclesResponse>> getEligibleVehicles(
            @PathVariable Long id) {
        EligibleVehiclesResponse response = tripService.getEligibleVehicles(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/trip-drafts/{id}/eligible-vehicles-for-stops")
    @Operation(summary = "Tính xe đủ tải theo 1 nhóm điểm dừng con — dùng khi Dispatcher tự tách chuyến thủ công (BR-07)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<EligibleVehiclesResponse>> getEligibleVehiclesForStops(
            @PathVariable Long id,
            @RequestParam List<Long> stopIds) {
        EligibleVehiclesResponse response = tripService.getEligibleVehiclesForStops(id, stopIds);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/api/v1/drivers/available")
    @Operation(summary = "Get available drivers for a date")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<List<AvailableDriverResponse>>> getAvailableDrivers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AvailableDriverResponse> drivers = tripService.getAvailableDrivers(date);
        return ResponseEntity.ok(ApiResponse.success(drivers));
    }

    @PostMapping("/api/v1/trip-drafts/{id}/assign")
    @Operation(summary = "Assign vehicle + driver to trip draft → create Trip")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<TripResponse>> assignVehicleAndDriver(
            @PathVariable Long id,
            @Valid @RequestBody TripAssignRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripService.assignVehicleAndDriver(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/api/v1/trip-drafts/{id}/assign-split")
    @Operation(summary = "Split-assign trip draft into multiple trips (BR-07)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<TripSplitResponse>> assignSplit(
            @PathVariable Long id,
            @Valid @RequestBody TripSplitAssignRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripSplitResponse response = tripService.assignSplit(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/api/v1/trips")
    @Operation(summary = "Get trips by trip draft ID")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByTripDraftId(
            @RequestParam Long tripDraftId) {
        List<TripResponse> trips = tripService.getTripsByTripDraftId(tripDraftId);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    @GetMapping("/api/v1/fleet/capacity-check")
    @Operation(summary = "Check fleet capacity for a delivery date (BR-08)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<FleetCapacityCheckResponse>> checkFleetCapacity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        FleetCapacityCheckResponse response = tripService.checkFleetCapacity(date);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/api/v1/trips/{id}/dispatch")
    @Operation(summary = "Dispatch trip — lock and generate handover slip")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<TripResponse>> dispatchTrip(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripService.dispatchTrip(id, username);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping(value = "/api/v1/trips/{id}/handover-slip", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Get handover slip as HTML for printing")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<String> getHandoverSlip(@PathVariable Long id) {
        String html = tripService.getHandoverSlipHtml(id);
        return ResponseEntity.ok(html);
    }

    @GetMapping("/api/v1/trips/{tripId}")
    @Operation(summary = "Get trip detail by trip ID")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<TripResponse>> getTripById(@PathVariable Long tripId) {
        TripResponse response = tripService.getTripById(tripId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/api/v1/trips/{id}/assignment")
    @Operation(summary = "Update vehicle and driver assignment for a validated trip before dispatch")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<TripResponse>> updateAssignment(
            @PathVariable Long id,
            @Valid @RequestBody TripAssignmentPatchRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripService.updateAssignment(id, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Assignment updated successfully."));
    }

    @GetMapping("/api/v1/trips/my-trips")
    @Operation(summary = "Driver view: get trips assigned to current driver")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TripResponse> trips = tripService.getDriverTrips(username, date, status);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    @GetMapping("/api/v1/trips/my-trips/calendar")
    @Operation(summary = "Driver view: tổng hợp trạng thái chuyến theo tháng, phục vụ chấm đỏ/xanh trên lịch")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<List<DriverTripCalendarDayResponse>>> getMyTripsCalendar(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") java.time.YearMonth month) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<DriverTripCalendarDayResponse> response = tripService.getDriverTripCalendar(username, month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.elog.controller;

import com.elog.dto.request.TripAssignRequest;
import com.elog.dto.request.TripSplitAssignRequest;
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

    // ── US-15 TASK-02 — Vehicle Assignment ─────────────────────────

    @GetMapping("/api/trip-drafts/{id}/eligible-vehicles")
    @Operation(summary = "Get eligible vehicles for a validated trip draft")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<List<EligibleVehicleDto>>> getEligibleVehicles(
            @PathVariable Long id) {
        List<EligibleVehicleDto> vehicles = tripService.getEligibleVehicles(id);
        return ResponseEntity.ok(ApiResponse.success(vehicles));
    }

    @GetMapping("/api/drivers/available")
    @Operation(summary = "Get available drivers for a date")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<List<AvailableDriverResponse>>> getAvailableDrivers(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AvailableDriverResponse> drivers = tripService.getAvailableDrivers(date);
        return ResponseEntity.ok(ApiResponse.success(drivers));
    }

    @PostMapping("/api/trip-drafts/{id}/assign")
    @Operation(summary = "Assign vehicle + driver to trip draft → create Trip")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<TripResponse>> assignVehicleAndDriver(
            @PathVariable Long id,
            @Valid @RequestBody TripAssignRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripService.assignVehicleAndDriver(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }

    @PostMapping("/api/trip-drafts/{id}/assign-split")
    @Operation(summary = "Split-assign trip draft into multiple trips (BR-07)")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<TripSplitResponse>> assignSplit(
            @PathVariable Long id,
            @Valid @RequestBody TripSplitAssignRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripSplitResponse response = tripService.assignSplit(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/api/trips")
    @Operation(summary = "Get trips by trip draft ID")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getTripsByTripDraftId(
            @RequestParam Long tripDraftId) {
        List<TripResponse> trips = tripService.getTripsByTripDraftId(tripDraftId);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    // ── US-15 TASK-03 — Fleet Capacity Check (BR-08) ──────────────

    @GetMapping("/api/fleet/capacity-check")
    @Operation(summary = "Check fleet capacity for a delivery date (BR-08)")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<FleetCapacityCheckResponse>> checkFleetCapacity(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        FleetCapacityCheckResponse response = tripService.checkFleetCapacity(date);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    // ── US-16 TASK-02 — Dispatch ──────────────────────────────────

    @PostMapping("/api/trips/{id}/dispatch")
    @Operation(summary = "Dispatch trip — lock and generate handover slip")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<TripResponse>> dispatchTrip(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripResponse response = tripService.dispatchTrip(id, username);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping(value = "/api/trips/{id}/handover-slip", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Get handover slip as HTML for printing")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<String> getHandoverSlip(@PathVariable Long id) {
        String html = tripService.getHandoverSlipHtml(id);
        return ResponseEntity.ok(html);
    }

    // ── US-16 — Driver view ───────────────────────────────────────

    @GetMapping("/api/trips/my-trips")
    @Operation(summary = "Driver view: get trips assigned to current driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<List<TripResponse>>> getMyTrips(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) String status) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        List<TripResponse> trips = tripService.getDriverTrips(username, date, status);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }
}


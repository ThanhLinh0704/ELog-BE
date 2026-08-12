package com.elog.controller;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.StopArriveResponse;
import com.elog.dto.response.trip.StopCompleteResponse;
import com.elog.dto.response.trip.TripStartResponse;
import com.elog.service.TripMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "Trip Monitoring — Driver", description = "US-17 Driver status update APIs (replaces Android App)")
public class TripMonitoringController {

    private final TripMonitoringService tripMonitoringService;

    /**
     * Driver bấm "BẮT ĐẦU CHUYẾN"
     * Guard: Trip.driver == currentUser, Trip.status == DISPATCHED
     */
    @PostMapping("/api/v1/trips/{id}/start")
    @Operation(summary = "Driver starts trip: DISPATCHED → IN_PROGRESS")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<TripStartResponse>> startTrip(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripStartResponse response = tripMonitoringService.startTrip(id, username);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    /**
     * Driver bấm "ĐÃ ĐẾN" tại stop
     * Guard: stop thuộc trip của driver, stop.status == PENDING, sequential order
     * Side-effect: tạo TIME_EXCEPTION nếu delay > threshold
     */
    @PostMapping("/api/v1/trip-stops/{id}/arrive")
    @Operation(summary = "Driver arrives at stop: PENDING → IN_PROGRESS. Auto-flags TIME_EXCEPTION if late (BR-09)")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<StopArriveResponse>> arriveAtStop(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        StopArriveResponse response = tripMonitoringService.arriveAtStop(id, username);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    /**
     * Driver bấm "HOÀN THÀNH stop"
     * Guard: stop.status == IN_PROGRESS, parent trip.driver == currentUser
     * Side-effect: nếu stop cuối → Trip auto-COMPLETED
     */
    @PostMapping("/api/v1/trip-stops/{id}/complete")
    @Operation(summary = "Driver completes stop: IN_PROGRESS → COMPLETED. Trip auto-completes if last stop done")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<StopCompleteResponse>> completeStop(@PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        StopCompleteResponse response = tripMonitoringService.completeStop(id, username);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }
}

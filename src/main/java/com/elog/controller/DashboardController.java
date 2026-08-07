package com.elog.controller;

import com.elog.dto.response.*;
import com.elog.service.TripMonitoringService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "US-17 Dispatcher/Manager monitoring dashboard APIs")
public class DashboardController {

    private final TripMonitoringService tripMonitoringService;

    /**
     * Dashboard chính: danh sách tất cả active trips của ngày
     * Default: ngày hôm nay. Cho phép truyền ?date= để xem ngày khác
     */
    @GetMapping("/api/v1/dashboard/active-trips")
    @Operation(summary = "Get all active trips for a date (DISPATCHED/IN_PROGRESS/COMPLETED)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<ActiveTripsResponse>> getActiveTrips(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        LocalDate queryDate = (date != null) ? date : LocalDate.now();
        ActiveTripsResponse response = tripMonitoringService.getActiveTripsDashboard(queryDate);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Chi tiết tiến độ từng stop của 1 trip
     */
    @GetMapping("/api/v1/trips/{id}/progress")
    @Operation(summary = "Get detailed stop-by-stop progress of a trip")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<TripProgressResponse>> getTripProgress(@PathVariable Long id) {
        TripProgressResponse response = tripMonitoringService.getTripProgress(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

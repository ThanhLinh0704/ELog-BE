package com.elog.controller;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.TripOutcomeEventResponse;
import com.elog.entity.TripOutcomeEventType;
import com.elog.service.TripOutcomeHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Trip Outcome History", description = "Audit trail cho kết quả thực hiện chuyến giao (BR-OUT-01..08)")
public class TripOutcomeHistoryController {

    private final TripOutcomeHistoryService tripOutcomeHistoryService;

    @GetMapping("/api/trip-outcome-events")
    @Operation(summary = "Search trip outcome history events with filters (default sort ASC)")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<List<TripOutcomeEventResponse>>> search(
            @RequestParam(required = false) Long tripId,
            @RequestParam(required = false) String driverUsername,
            @RequestParam(required = false) String routeCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(required = false) String storeCode,
            @RequestParam(required = false) String deliveryResult,
            @RequestParam(required = false) TripOutcomeEventType eventType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.ASC) Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isDriver = auth.getAuthorities().stream().anyMatch(a -> "ROLE_DRIVER".equals(a.getAuthority()) || "DRIVER".equals(a.getAuthority()));

        var filter = new TripOutcomeHistoryService.OutcomeHistoryFilter(
                tripId, driverUsername, routeCode, deliveryDate, storeCode, deliveryResult, eventType, fromDate, toDate);

        return ResponseEntity.ok(tripOutcomeHistoryService.search(filter, pageable, currentUsername, isDriver));
    }

    @GetMapping("/api/trips/{tripId}/outcome-history")
    @Operation(summary = "Get outcome history for a specific trip (AC-08, default sort ASC)")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<List<TripOutcomeEventResponse>>> getTripOutcomeHistory(
            @PathVariable Long tripId,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.ASC) Pageable pageable) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isDriver = auth.getAuthorities().stream().anyMatch(a -> "ROLE_DRIVER".equals(a.getAuthority()) || "DRIVER".equals(a.getAuthority()));

        var filter = new TripOutcomeHistoryService.OutcomeHistoryFilter(
                tripId, null, null, null, null, null, null, null, null);

        return ResponseEntity.ok(tripOutcomeHistoryService.search(filter, pageable, currentUsername, isDriver));
    }
}

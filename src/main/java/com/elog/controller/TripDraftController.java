package com.elog.controller;

import com.elog.dto.request.ConsolidateRequest;
import com.elog.dto.request.RecalculateEtaRequest;
import com.elog.dto.request.StopUpdateRequest;
import com.elog.dto.response.*;
import com.elog.service.TripDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trip-drafts")
@RequiredArgsConstructor
@Tag(name = "Trip Drafts", description = "US-10 Route Consolidation & US-11 Trip Draft Review APIs")
public class TripDraftController {

    private final TripDraftService tripDraftService;

    // ── US-10 endpoints ──────────────────────────────────────────

    @PostMapping("/consolidate")
    @Operation(summary = "Trigger consolidation for all routes on a delivery date")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ConsolidateResponse>> consolidate(
            @Valid @RequestBody ConsolidateRequest request) {
        ConsolidateResponse response = tripDraftService.consolidate(request.getDeliveryDate());
        return ResponseEntity.ok(ApiResponse.success(response, "Consolidation hoàn tất"));
    }

    @GetMapping
    @Operation(summary = "Get trip drafts by delivery date (paginated)")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ApiResponse<List<TripDraftResponse>>> getTripDrafts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @PageableDefault(size = 20) Pageable pageable) {
        ApiResponse<List<TripDraftResponse>> response =
                tripDraftService.getTripDrafts(deliveryDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trip draft detail with active/skipped stop list")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ApiResponse<TripDraftResponse>> getTripDraftById(
            @PathVariable Long id) {
        TripDraftResponse response = tripDraftService.getTripDraftById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── US-11 endpoints ──────────────────────────────────────────

    @GetMapping("/{id}/stops")
    @Operation(summary = "Get all stops (active + skipped) for trip draft review")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ApiResponse<TripDraftResponse>> getStopsForReview(
            @PathVariable Long id) {
        TripDraftResponse response = tripDraftService.getStopsForReview(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/stops/{stopId}")
    @Operation(summary = "Toggle active/skipped status for a stop")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<TripDraftStopResponse>> updateStop(
            @PathVariable Long id,
            @PathVariable Long stopId,
            @Valid @RequestBody StopUpdateRequest request) {
        TripDraftStopResponse response = tripDraftService.updateStop(id, stopId, request);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Stop updated. ETA recalculation required — call POST /recalculate-eta."));
    }

    @PostMapping("/{id}/recalculate-eta")
    @Operation(summary = "Recalculate ETA for all active stops using Haversine")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<RecalculateEtaResponse>> recalculateEta(
            @PathVariable Long id,
            @Valid @RequestBody RecalculateEtaRequest request) {
        RecalculateEtaResponse response = tripDraftService.recalculateEta(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm trip draft — DC-07 gate (DRAFT → PLANNED)")
    @PreAuthorize("hasRole('DISPATCHER')")
    public ResponseEntity<ApiResponse<ConfirmResponse>> confirmTripDraft(
            @PathVariable Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        ConfirmResponse response = tripDraftService.confirmTripDraft(id, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}


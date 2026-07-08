package com.elog.controller;

import com.elog.dto.request.ConsolidateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ConsolidateResponse;
import com.elog.dto.response.TripDraftResponse;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trip-drafts")
@RequiredArgsConstructor
@Tag(name = "Trip Drafts", description = "US-10 Route Consolidation APIs")
public class TripDraftController {

    private final TripDraftService tripDraftService;

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
}

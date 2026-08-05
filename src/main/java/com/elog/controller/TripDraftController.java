package com.elog.controller;

import com.elog.dto.request.ConsolidateRequest;
import com.elog.dto.request.RecalculateEtaRequest;
import com.elog.dto.request.StopUpdateRequest;
import com.elog.dto.response.*;
import com.elog.service.CapacityValidationService;
import com.elog.service.ManifestService;
import com.elog.service.TripDraftService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/trip-drafts")
@RequiredArgsConstructor
@Tag(name = "Trip Drafts", description = "US-10 Route Consolidation, US-11 Trip Draft Review, US-12 Capacity Validation & US-13 LIFO Manifest APIs")
public class TripDraftController {

    private final TripDraftService tripDraftService;
    private final CapacityValidationService capacityValidationService;
    private final ManifestService manifestService;
    private final com.elog.service.DepartureAdjustmentService departureAdjustmentService;
    private final com.elog.service.RecommendationService recommendationService;

    // ── US-10 endpoints ──────────────────────────────────────────

    @PostMapping("/consolidate")
    @Operation(summary = "Trigger consolidation for all routes on a delivery date")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<ConsolidateResponse>> consolidate(
            @Valid @RequestBody ConsolidateRequest request) {
        ConsolidateResponse response = tripDraftService.consolidate(request.getDeliveryDate());
        return ResponseEntity.ok(ApiResponse.success(response, "Consolidation hoàn tất"));
    }

    @GetMapping
    @Operation(summary = "Get trip drafts by delivery date (paginated)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<List<TripDraftResponse>>> getTripDrafts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @PageableDefault(size = 20) Pageable pageable) {
        ApiResponse<List<TripDraftResponse>> response = tripDraftService.getTripDrafts(deliveryDate, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get trip draft detail with active/skipped stop list")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<TripDraftResponse>> getTripDraftById(
            @PathVariable Long id) {
        TripDraftResponse response = tripDraftService.getTripDraftById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── US-11 endpoints ──────────────────────────────────────────

    @GetMapping("/{id}/stops")
    @Operation(summary = "Get all stops (active + skipped) for trip draft review")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<TripDraftResponse>> getStopsForReview(
            @PathVariable Long id) {
        TripDraftResponse response = tripDraftService.getStopsForReview(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/stops/{stopId}")
    @Operation(summary = "Toggle active/skipped status for a stop")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<TripDraftStopResponse>> updateStop(
            @PathVariable Long id,
            @PathVariable Long stopId,
            @Valid @RequestBody StopUpdateRequest request) {
        TripDraftStopResponse response = tripDraftService.updateStop(id, stopId, request);
        return ResponseEntity.ok(ApiResponse.success(response,
                "Stop updated. ETA recalculation required — call POST /recalculate-eta."));
    }

    @GetMapping("/{id}/stops/{stopId}/order-items")
    @Operation(summary = "Get order items detail for a specific trip draft stop")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<List<StopOrderItemResponse>>> getStopOrderItems(
            @PathVariable Long id,
            @PathVariable Long stopId) {
        List<StopOrderItemResponse> response = tripDraftService.getStopOrderItems(id, stopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/recalculate-eta")
    @Operation(summary = "Recalculate ETA for all active stops using Haversine")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<RecalculateEtaResponse>> recalculateEta(
            @PathVariable Long id,
            @Valid @RequestBody RecalculateEtaRequest request) {
        RecalculateEtaResponse response = tripDraftService.recalculateEta(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/confirm")
    @Operation(summary = "Confirm trip draft — DC-07 gate (DRAFT → PLANNED)")
    @PreAuthorize("hasAuthority('trip:confirm')")
    public ResponseEntity<ApiResponse<ConfirmResponse>> confirmTripDraft(
            @PathVariable Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        ConfirmResponse response = tripDraftService.confirmTripDraft(id, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/revert")
    @Operation(summary = "Revert trip draft — (PLANNED/VALIDATED → DRAFT)")
    @PreAuthorize("hasAuthority('trip:confirm')")
    public ResponseEntity<ApiResponse<Void>> revertToDraft(
            @PathVariable Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        tripDraftService.revertToDraft(id, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(null, "Trip draft reverted to DRAFT successfully."));
    }

    // ── US-12 endpoints ──────────────────────────────────────────

    @PostMapping("/{id}/validate-capacity")
    @Operation(summary = "Trigger capacity validation for a trip draft against the vehicle fleet")
    @PreAuthorize("hasAuthority('trip:confirm')")
    public ResponseEntity<ApiResponse<CapacityValidationResultResponse>> validateCapacity(
            @PathVariable Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        CapacityValidationResultResponse response = capacityValidationService.validate(id, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/validation-result")
    @Operation(summary = "Get stored capacity validation result")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<CapacityValidationResultResponse>> getValidationResult(
            @PathVariable Long id) {
        CapacityValidationResultResponse response = capacityValidationService.getValidationResult(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── US-13 endpoints ──────────────────────────────────────────

    @PostMapping("/{id}/generate-manifest")
    @Operation(summary = "Generate LIFO loading manifest for a validated trip draft")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<ManifestResponse>> generateManifest(
            @PathVariable Long id) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        ManifestResponse response = manifestService.generateManifest(id, currentUsername);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/{id}/manifest")
    @Operation(summary = "Get LIFO manifest flat list for a trip draft")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<ManifestResponse>> getManifest(
            @PathVariable Long id) {
        ManifestResponse response = manifestService.getManifest(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}/manifest/by-stop")
    @Operation(summary = "Get LIFO manifest grouped by stop (for Warehouse Staff)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<ManifestByStopResponse>> getManifestByStop(
            @PathVariable Long id) {
        ManifestByStopResponse response = manifestService.getManifestByStop(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/optimal-departure")
    @Operation(summary = "Tính toán gợi ý giờ xuất phát tối ưu tránh vi phạm Time Window (Smart Departure Adjustment)")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> getOptimalDeparture(
            @PathVariable Long id) {
        java.util.Map<String, Object> response = departureAdjustmentService.calculateOptimalDepartureTime(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/adjust-departure-time")
    @Operation(summary = "Điều chỉnh giờ xuất phát của chuyến và tự động tính lại ETA")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<TripDraftResponse>> adjustDepartureTime(
            @PathVariable Long id,
            @Valid @RequestBody com.elog.dto.request.AdjustDepartureTimeRequest request) {
        TripDraftResponse response = tripDraftService.adjustDepartureTime(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã điều chỉnh giờ xuất phát thành công"));
    }

    @PostMapping("/{id}/orders/{orderId}/settle-delay")
    @Operation(summary = "Ghi nhận dàn xếp thành công nhận trễ với người nhận đơn hàng")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<Void>> settleDelay(
            @PathVariable Long id,
            @PathVariable Long orderId,
            @Valid @RequestBody com.elog.dto.request.SettleDelayRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        tripDraftService.settleDelay(id, orderId, request, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(null, "Ghi nhận dàn xếp giao trễ thành công"));
    }

    @PostMapping("/{id}/orders/{orderId}/exclude")
    @Operation(summary = "Tách đơn hàng vi phạm ra khỏi chuyến đưa về hàng chờ ngoại lệ")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<Void>> excludeOrder(
            @PathVariable Long id,
            @PathVariable Long orderId) {
        tripDraftService.excludeOrder(id, orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã tách đơn hàng khỏi chuyến thành công"));
    }

    @PostMapping("/{id}/orders/{orderId}/re-include")
    @Operation(summary = "Thêm lại đơn hàng từ hàng chờ ngoại lệ vào chuyến draft")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<Void>> reIncludeOrder(
            @PathVariable Long id,
            @PathVariable Long orderId) {
        tripDraftService.reIncludeOrder(id, orderId);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã thêm lại đơn hàng vào chuyến thành công"));
    }

    @GetMapping("/{id}/excluded-orders")
    @Operation(summary = "Lấy danh sách các đơn hàng bị tách (UNASSIGNED) thuộc tuyến của đợt gom")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<List<StopOrderItemResponse>>> getExcludedOrders(
            @PathVariable Long id) {
        List<StopOrderItemResponse> response = tripDraftService.getExcludedOrders(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private final com.elog.service.PlanningHistoryService planningHistoryService;

    // ── Recommendation Engine endpoints ───────────────────────────

    @GetMapping("/{id}/recommendations")
    @Operation(summary = "Get Top-3 vehicle recommendations for a Trip Draft (FT-06/FT-07)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<RecommendationResultResponse>> getRecommendations(
            @PathVariable Long id) {
        RecommendationResultResponse result = recommendationService.recommendTop3(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Get planning history for a specific Trip Draft")
    @PreAuthorize("hasAuthority('planning-history:read')")
    public ResponseEntity<ApiResponse<List<com.elog.dto.response.PlanningEventResponse>>> getTripDraftHistory(
            @PathVariable Long id,
            @org.springframework.data.web.PageableDefault(size = 20, sort = "occurredAt", direction = org.springframework.data.domain.Sort.Direction.DESC) org.springframework.data.domain.Pageable pageable) {
        var filter = new com.elog.service.PlanningHistoryService.PlanningHistoryFilter(
                id, null, null, null, null, null, null, null, null);
        return ResponseEntity.ok(planningHistoryService.search(filter, pageable));
    }
}

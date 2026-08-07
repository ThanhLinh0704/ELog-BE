package com.elog.controller;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.TripOutcomeResponse;
import com.elog.service.TripOutcomeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/trip-outcomes")
@RequiredArgsConstructor
@Tag(name = "Trip Outcome Management", description = "FT-09 Dispatcher Outcome Validation & Audit Correction APIs")
public class TripOutcomeController {

    private final TripOutcomeService tripOutcomeService;

    @GetMapping
    @Operation(summary = "Lấy danh sách các Trip Outcome đang chờ Dispatcher nghiệm thu (SUBMITTED)")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<List<TripOutcomeResponse>>> getSubmittedOutcomes() {
        List<TripOutcomeResponse> list = tripOutcomeService.getSubmittedOutcomes();
        return ResponseEntity.ok(ApiResponse.success(list));
    }

    @PostMapping("/{id}/validate")
    @Operation(summary = "Dispatcher nghiệm thu kết quả chuyến xe (SUBMITTED -> VALIDATED)")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<TripOutcomeResponse>> validateOutcome(
            @PathVariable Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripOutcomeResponse response = tripOutcomeService.validateOutcome(id, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã nghiệm thu kết quả chuyến xe thành công"));
    }

    @PostMapping("/{id}/amend")
    @Operation(summary = "Dispatcher yêu cầu điều chỉnh kết quả nghiệm thu (Lưu vết audit version)")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<TripOutcomeResponse>> amendOutcome(
            @PathVariable Long id,
            @RequestParam String amendmentReason) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripOutcomeResponse response = tripOutcomeService.amendOutcome(id, amendmentReason, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã gửi yêu cầu điều chỉnh nghiệm thu"));
    }
}

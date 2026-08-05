package com.elog.controller;

import com.elog.dto.request.DriverStatusUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.DriverResponse;
import com.elog.dto.response.DriverStatusHistoryResponse;
import com.elog.service.DriverStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Tag(name = "Driver Status", description = "BR-DRV-01..06 Driver Active/Inactive operational status management")
public class DriverStatusController {

    private final DriverStatusService driverStatusService;

    @GetMapping
    @Operation(summary = "List drivers with operational status (paginated, filter by status/keyword)")
    @PreAuthorize("hasAuthority('driver:read')")
    public ResponseEntity<ApiResponse<List<DriverResponse>>> getDrivers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(driverStatusService.getDrivers(keyword, status, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get driver detail with current operational status")
    @PreAuthorize("hasAuthority('driver:read')")
    public ResponseEntity<ApiResponse<DriverResponse>> getDriverById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(driverStatusService.getDriverById(id)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Change driver Active/Inactive status (Admin only, BR-DRV-01)")
    @PreAuthorize("hasAuthority('driver:write')")
    public ResponseEntity<ApiResponse<DriverResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody DriverStatusUpdateRequest request) {
        String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverResponse response = driverStatusService.updateStatus(id, request, currentUsername);
        return ResponseEntity.ok(ApiResponse.success(response, "Driver status updated successfully"));
    }

    @GetMapping("/{id}/status-history")
    @Operation(summary = "Get status change history for a driver (BR-DRV-06 — full history retained)")
    @PreAuthorize("hasAuthority('driver:read')")
    public ResponseEntity<ApiResponse<List<DriverStatusHistoryResponse>>> getStatusHistory(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(driverStatusService.getStatusHistory(id, pageable));
    }
}

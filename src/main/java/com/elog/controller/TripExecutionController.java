package com.elog.controller;

import com.elog.dto.request.AdminTripOverrideRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.DriverTripResponse;
import com.elog.service.DriverTripService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/trip-executions")
@RequiredArgsConstructor
@Tag(name = "Trip Execution Admin Override", description = "Web Management APIs for Force Return / Force Complete Admin Overrides")
public class TripExecutionController {

    private final DriverTripService driverTripService;

    @PostMapping("/{id}/admin-override")
    @Operation(summary = "Quản lý Web can thiệp kết thúc chuyến xe hoặc xác nhận xe về kho khẩn cấp (Admin Override)")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<DriverTripResponse>> adminOverrideTripExecution(
            @PathVariable Long id,
            @Valid @RequestBody AdminTripOverrideRequest request) {
        String adminUsername = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverTripResponse response = driverTripService.adminOverrideTripExecution(id, request, adminUsername);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã thực hiện can thiệp Admin Override chuyến xe thành công"));
    }
}

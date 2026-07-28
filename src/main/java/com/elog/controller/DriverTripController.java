package com.elog.controller;

import com.elog.dto.request.UpdateOrderResultRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.DriverTripResponse;
import com.elog.dto.response.TripOutcomeResponse;
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
@RequestMapping("/api/driver/trips")
@RequiredArgsConstructor
@Tag(name = "Driver App Trip Execution", description = "FT-09 Supporting Driver Trip Updates & LIFO Guidance APIs")
public class DriverTripController {

    private final DriverTripService driverTripService;

    @GetMapping("/active")
    @Operation(summary = "Lấy thông tin chuyến xe đang phân công cho Tài xế hiện tại (kèm Hướng dẫn LIFO)")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<DriverTripResponse>> getActiveTrip() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverTripResponse response = driverTripService.getActiveTrip(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{executionId}/start")
    @Operation(summary = "Tài xế bấm Bắt đầu chuyến xe (ASSIGNED -> IN_PROGRESS)")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<DriverTripResponse>> startTrip(
            @PathVariable Long executionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverTripResponse response = driverTripService.startTrip(executionId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã bắt đầu chuyến xe thành công"));
    }

    @PutMapping("/{executionId}/orders/{orderId}/result")
    @Operation(summary = "Cập nhật kết quả giao hàng từng đơn (DELIVERED, PARTIALLY_DELIVERED, FAILED)")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<DriverTripResponse>> updateOrderResult(
            @PathVariable Long executionId,
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderResultRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverTripResponse response = driverTripService.updateOrderResult(executionId, orderId, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã cập nhật trạng thái đơn hàng"));
    }

    @PostMapping("/{executionId}/complete")
    @Operation(summary = "Hoàn tất chuyến xe sau khi tất cả đơn hàng có trạng thái terminal (Tạo TripOutcome SUBMITTED)")
    @PreAuthorize("hasAuthority('trip:write')")
    public ResponseEntity<ApiResponse<TripOutcomeResponse>> completeTrip(
            @PathVariable Long executionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripOutcomeResponse response = driverTripService.completeTrip(executionId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã hoàn tất chuyến xe và gửi kết quả nghiệm thu"));
    }
}

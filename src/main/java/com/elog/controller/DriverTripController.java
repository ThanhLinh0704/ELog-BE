package com.elog.controller;

import com.elog.dto.request.trip.UpdateOrderResultRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.TripOutcomeResponse;
import com.elog.dto.response.user.DriverTripResponse;
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
@RequestMapping("/api/v1/driver/trips")
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

    @GetMapping("/pending-return")
    @Operation(summary = "Lấy các chuyến đã hoàn thành nhưng tài xế chưa xác nhận xe về kho")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<java.util.List<DriverTripResponse>>> getPendingReturnTrips() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        java.util.List<DriverTripResponse> response = driverTripService.getPendingReturnTrips(username);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{executionId}/start")
    @Operation(summary = "Tài xế bấm Bắt đầu chuyến xe (ASSIGNED -> IN_PROGRESS)")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<DriverTripResponse>> startTrip(
            @PathVariable Long executionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverTripResponse response = driverTripService.startTrip(executionId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã bắt đầu chuyến xe thành công"));
    }

    @PostMapping("/{executionId}/stops/{stopId}/arrive")
    @Operation(summary = "Tài xế bấm Đã đến điểm giao")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<DriverTripResponse>> arriveAtStop(
            @PathVariable Long executionId,
            @PathVariable Long stopId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverTripResponse response = driverTripService.arriveAtStop(executionId, stopId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã ghi nhận đến điểm giao"));
    }

    @PutMapping("/{executionId}/orders/{orderId}/result")
    @Operation(summary = "Cập nhật kết quả giao hàng từng đơn (DELIVERED, PARTIALLY_DELIVERED, FAILED)")
    @PreAuthorize("hasAuthority('trip:execute')")
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
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<TripOutcomeResponse>> completeTrip(
            @PathVariable Long executionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        TripOutcomeResponse response = driverTripService.completeTrip(executionId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã hoàn tất chuyến xe và gửi kết quả nghiệm thu"));
    }

    @PostMapping("/{executionId}/return-to-warehouse")
    @Operation(summary = "Tài xế bấm Xác nhận xe đã về tới kho (Giải phóng xe IN_USE -> AVAILABLE)")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<DriverTripResponse>> returnToWarehouse(
            @PathVariable Long executionId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DriverTripResponse response = driverTripService.returnToWarehouse(executionId, username);
        return ResponseEntity.ok(ApiResponse.success(response, "Đã xác nhận xe về tới kho thành công"));
    }
}

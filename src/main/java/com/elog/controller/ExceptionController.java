package com.elog.controller;

import com.elog.dto.request.exception.RejectStopRequest;
import com.elog.dto.request.exception.ResolveExceptionRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.exception.DeliveryExceptionResponse;
import com.elog.dto.response.exception.ExceptionListResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.service.ExceptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@Tag(name = "Exception Management", description = "US-18 — DELIVERY_REJECTION + TIME_EXCEPTION")
public class ExceptionController {

    private final ExceptionService exceptionService;

    @PostMapping("/api/v1/trip-stops/{id}/reject")
    @Operation(summary = "Driver ghi nhận cửa hàng từ chối nhận hàng — DELIVERY_REJECTION (BR-10)")
    @PreAuthorize("hasAuthority('trip:execute')")
    public ResponseEntity<ApiResponse<DeliveryExceptionResponse>> rejectStop(
            @PathVariable Long id,
            @Valid @RequestBody RejectStopRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DeliveryExceptionResponse response = exceptionService.rejectStop(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/api/v1/exceptions")
    @Operation(summary = "Danh sách exception — filter theo ngày (hoặc khoảng ngày), loại, trạng thái resolve")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<ExceptionListResponse>> listExceptions(
            @Parameter(description = "yyyy-MM-dd, mặc định hôm nay — bỏ qua nếu truyền fromDate/toDate") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "yyyy-MM-dd — cùng với toDate để lọc theo khoảng ngày thay vì 1 ngày") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "yyyy-MM-dd — bắt buộc đi kèm fromDate") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "ALL | TIME_EXCEPTION | DELIVERY_REJECTION") @RequestParam(required = false, defaultValue = "ALL") String type,
            @Parameter(description = "true | false | all") @RequestParam(required = false, defaultValue = "false") String resolved) {
        if (fromDate != null && toDate != null) {
            if (fromDate.isAfter(toDate)) {
                throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
                        "fromDate must not be after toDate", HttpStatus.BAD_REQUEST);
            }
            ExceptionListResponse response = exceptionService.listExceptionsInRange(fromDate, toDate, type, resolved);
            return ResponseEntity.ok(ApiResponse.success(response, null));
        }
        LocalDate effectiveDate = (date != null) ? date : LocalDate.now();
        ExceptionListResponse response = exceptionService.listExceptions(effectiveDate, type, resolved);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    @GetMapping("/api/v1/exceptions/{id}")
    @Operation(summary = "Chi tiết 1 exception")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<DeliveryExceptionResponse>> getException(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(exceptionService.getException(id), null));
    }

    @PatchMapping("/api/v1/exceptions/{id}/resolve")
    @Operation(summary = "Dispatcher/Manager đóng exception")
    @PreAuthorize("hasAuthority('trip:coordinate')")
    public ResponseEntity<ApiResponse<DeliveryExceptionResponse>> resolveException(
            @PathVariable Long id,
            @RequestBody ResolveExceptionRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DeliveryExceptionResponse response = exceptionService.resolveException(id, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/api/v1/exceptions/violations")
    @Operation(summary = "Danh sách tổng hợp các vi phạm Time Window và Tải trọng xe cho Dispatcher Dashboard")
    @PreAuthorize("hasAuthority('trip:read')")
    public ResponseEntity<ApiResponse<ExceptionListResponse>> listViolations(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate effectiveDate = (date != null) ? date : LocalDate.now();
        ExceptionListResponse response = exceptionService.listExceptions(effectiveDate, "TIME_EXCEPTION", "false");
        return ResponseEntity.ok(ApiResponse.success(response, "Danh sách vi phạm vận hành"));
    }
}

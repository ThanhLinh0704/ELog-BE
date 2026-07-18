package com.elog.controller;

import com.elog.dto.request.RejectStopRequest;
import com.elog.dto.request.ResolveExceptionRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.DeliveryExceptionResponse;
import com.elog.dto.response.ExceptionListResponse;
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

    @PostMapping("/api/trip-stops/{id}/reject")
    @Operation(summary = "Driver ghi nhận cửa hàng từ chối nhận hàng — DELIVERY_REJECTION (BR-10)")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<ApiResponse<DeliveryExceptionResponse>> rejectStop(
            @PathVariable Long id,
            @Valid @RequestBody RejectStopRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DeliveryExceptionResponse response = exceptionService.rejectStop(id, request, username);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, response.getMessage()));
    }

    @GetMapping("/api/exceptions")
    @Operation(summary = "Danh sách exception — filter theo ngày, loại, trạng thái resolve")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<ExceptionListResponse>> listExceptions(
            @Parameter(description = "yyyy-MM-dd, mặc định hôm nay")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "ALL | TIME_EXCEPTION | DELIVERY_REJECTION")
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @Parameter(description = "true | false | all")
            @RequestParam(required = false, defaultValue = "false") String resolved) {
        LocalDate effectiveDate = (date != null) ? date : LocalDate.now();
        ExceptionListResponse response = exceptionService.listExceptions(effectiveDate, type, resolved);
        return ResponseEntity.ok(ApiResponse.success(response, null));
    }

    @GetMapping("/api/exceptions/{id}")
    @Operation(summary = "Chi tiết 1 exception")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryExceptionResponse>> getException(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(exceptionService.getException(id), null));
    }

    @PatchMapping("/api/exceptions/{id}/resolve")
    @Operation(summary = "Dispatcher/Manager đóng exception")
    @PreAuthorize("hasAnyRole('DISPATCHER', 'LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<DeliveryExceptionResponse>> resolveException(
            @PathVariable Long id,
            @RequestBody ResolveExceptionRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        DeliveryExceptionResponse response = exceptionService.resolveException(id, request, username);
        return ResponseEntity.ok(ApiResponse.success(response, response.getMessage()));
    }
}

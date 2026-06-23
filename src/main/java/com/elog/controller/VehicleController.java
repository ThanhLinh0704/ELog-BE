package com.elog.controller;

import com.elog.dto.request.VehicleCreateRequest;
import com.elog.dto.request.VehicleStatusUpdateRequest;
import com.elog.dto.request.VehicleUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.VehicleFleetCapacityResponse;
import com.elog.dto.response.VehicleListItemResponse;
import com.elog.dto.response.VehicleResponse;
import com.elog.service.VehicleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
@RequiredArgsConstructor
@Tag(name = "Vehicles", description = "Vehicle fleet management APIs")
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping
    @Operation(summary = "Register a new vehicle")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody VehicleCreateRequest request) {
        VehicleResponse response = vehicleService.createVehicle(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Vehicle created successfully"));
    }

    @GetMapping("/fleet-capacity")
    @Operation(summary = "Get total active fleet capacity")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DISPATCHER', 'LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleFleetCapacityResponse>> getFleetCapacity() {
        VehicleFleetCapacityResponse response = vehicleService.getFleetCapacity();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get vehicle detail by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DISPATCHER', 'LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(@PathVariable Long id) {
        VehicleResponse response = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get paginated and filtered vehicle list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DISPATCHER', 'LOGISTICS_MANAGER')")
    public ResponseEntity<ApiResponse<List<VehicleListItemResponse>>> getAllVehicles(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) BigDecimal minWeightKg,
            @RequestParam(required = false) BigDecimal minVolumeM3,
            @PageableDefault(size = 20) Pageable pageable) {
        ApiResponse<List<VehicleListItemResponse>> response =
                vehicleService.getAllVehicles(keyword, isActive, minWeightKg, minVolumeM3, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update vehicle information (plate number is immutable)")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicle(
            @PathVariable Long id,
            @Valid @RequestBody VehicleUpdateRequest request) {
        VehicleResponse response = vehicleService.updateVehicle(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a vehicle")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<VehicleResponse>> updateVehicleStatus(
            @PathVariable Long id,
            @Valid @RequestBody VehicleStatusUpdateRequest request) {
        VehicleResponse response = vehicleService.updateVehicleStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Vehicle status updated"));
    }
}

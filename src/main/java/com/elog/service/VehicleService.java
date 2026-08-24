package com.elog.service;

import com.elog.dto.request.vehicle.VehicleCreateRequest;
import com.elog.dto.request.vehicle.VehicleStatusUpdateRequest;
import com.elog.dto.request.vehicle.VehicleUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.vehicle.VehicleFleetCapacityResponse;
import com.elog.dto.response.vehicle.VehicleListItemResponse;
import com.elog.dto.response.vehicle.VehicleResponse;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface VehicleService {

    VehicleResponse createVehicle(VehicleCreateRequest request);

    VehicleResponse getVehicleById(Long id);

    /**
     * @param date optional — when present, {@code currentTrip} on each item reflects that
     *             vehicle's trip status for that specific delivery date (including a
     *             COMPLETED_RETURNED phase) instead of "whatever trip is active right now".
     *             See filemd/FLEET-STATUS-DASHBOARD-ADJUSTED-SPEC.md mục 0.
     */
    ApiResponse<List<VehicleListItemResponse>> getAllVehicles(
            String keyword,
            Boolean isActive,
            BigDecimal minWeightKg,
            BigDecimal minVolumeM3,
            Pageable pageable,
            LocalDate date);

    VehicleFleetCapacityResponse getFleetCapacity();

    VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request);

    VehicleResponse updateVehicleStatus(Long id, VehicleStatusUpdateRequest request);

    List<VehicleResponse> findAvailableVehiclesForTrip(Long tripId);
}

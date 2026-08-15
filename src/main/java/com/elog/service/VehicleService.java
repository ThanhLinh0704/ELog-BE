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
import java.util.List;

public interface VehicleService {

    VehicleResponse createVehicle(VehicleCreateRequest request);

    VehicleResponse getVehicleById(Long id);

    ApiResponse<List<VehicleListItemResponse>> getAllVehicles(
            String keyword,
            Boolean isActive,
            BigDecimal minWeightKg,
            BigDecimal minVolumeM3,
            Pageable pageable);

    VehicleFleetCapacityResponse getFleetCapacity();

    VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request);

    VehicleResponse updateVehicleStatus(Long id, VehicleStatusUpdateRequest request);

    List<VehicleResponse> findAvailableVehiclesForTrip(Long tripId);
}

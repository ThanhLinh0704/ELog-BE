package com.elog.service;

import com.elog.dto.request.VehicleCreateRequest;
import com.elog.dto.request.VehicleStatusUpdateRequest;
import com.elog.dto.request.VehicleUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.VehicleFleetCapacityResponse;
import com.elog.dto.response.VehicleListItemResponse;
import com.elog.dto.response.VehicleResponse;
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
}

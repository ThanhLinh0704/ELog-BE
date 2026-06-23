package com.elog.service.impl;

import com.elog.dto.request.VehicleCreateRequest;
import com.elog.dto.request.VehicleStatusUpdateRequest;
import com.elog.dto.request.VehicleUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.VehicleFleetCapacityResponse;
import com.elog.dto.response.VehicleListItemResponse;
import com.elog.dto.response.VehicleResponse;
import com.elog.entity.Vehicle;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.VehicleMapper;
import com.elog.repository.VehicleRepository;
import com.elog.repository.specification.VehicleSpecification;
import com.elog.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleMapper vehicleMapper;

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleCreateRequest request) {
        String normalizedPlate = vehicleMapper.normalizePlate(request.getPlateNumber());
        if (vehicleRepository.existsByPlateNumber(normalizedPlate)) {
            throw new BusinessException(ErrorCode.VEHICLE_PLATE_DUPLICATE,
                    "Vehicle plate already exists: " + normalizedPlate, HttpStatus.CONFLICT);
        }

        Vehicle vehicle = vehicleMapper.toEntity(request);
        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = findOrThrow(id);
        return vehicleMapper.toResponse(vehicle);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<VehicleListItemResponse>> getAllVehicles(
            String keyword,
            Boolean isActive,
            BigDecimal minWeightKg,
            BigDecimal minVolumeM3,
            Pageable pageable) {

        Specification<Vehicle> spec = Specification
                .where(VehicleSpecification.hasKeyword(keyword))
                .and(VehicleSpecification.hasActiveStatus(isActive))
                .and(VehicleSpecification.hasMinimumWeight(minWeightKg))
                .and(VehicleSpecification.hasMinimumVolume(minVolumeM3));

        Page<Vehicle> page = vehicleRepository.findAll(spec, pageable);

        List<VehicleListItemResponse> items = page.getContent().stream()
                .map(vehicleMapper::toListItem)
                .toList();

        return ApiResponse.<List<VehicleListItemResponse>>builder()
                .success(true)
                .data(items)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleFleetCapacityResponse getFleetCapacity() {
        return VehicleFleetCapacityResponse.builder()
                .activeVehicleCount(vehicleRepository.countByIsActiveTrue())
                .totalMaxWeightKg(zeroIfNull(vehicleRepository.sumActiveMaxWeightKg()))
                .totalMaxVolumeM3(zeroIfNull(vehicleRepository.sumActiveMaxVolumeM3()))
                .build();
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(Long id, VehicleUpdateRequest request) {
        Vehicle vehicle = findOrThrow(id);

        // plateNumber is intentionally immutable. If a real plate changes, deactivate
        // the old vehicle and create a new vehicle record to preserve trip history.
        vehicle.setVehicleType(request.getVehicleType());
        vehicle.setMaxWeightKg(request.getMaxWeightKg());
        vehicle.setMaxVolumeM3(request.getMaxVolumeM3());

        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicleStatus(Long id, VehicleStatusUpdateRequest request) {
        Vehicle vehicle = findOrThrow(id);

        // Trip-in-use check is deferred until Trip assignment is implemented.
        vehicle.setIsActive(request.getIsActive());

        Vehicle saved = vehicleRepository.save(vehicle);
        return vehicleMapper.toResponse(saved);
    }

    private Vehicle findOrThrow(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND,
                        "Vehicle not found with id: " + id, HttpStatus.NOT_FOUND));
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

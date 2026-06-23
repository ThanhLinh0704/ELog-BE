package com.elog.mapper;

import com.elog.dto.request.VehicleCreateRequest;
import com.elog.dto.response.VehicleListItemResponse;
import com.elog.dto.response.VehicleResponse;
import com.elog.entity.Vehicle;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public Vehicle toEntity(VehicleCreateRequest request) {
        return Vehicle.builder()
                .plateNumber(normalizePlate(request.getPlateNumber()))
                .vehicleType(request.getVehicleType())
                .maxWeightKg(request.getMaxWeightKg())
                .maxVolumeM3(request.getMaxVolumeM3())
                .isActive(true)
                .build();
    }

    public VehicleResponse toResponse(Vehicle vehicle) {
        return VehicleResponse.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .vehicleType(vehicle.getVehicleType())
                .maxWeightKg(vehicle.getMaxWeightKg())
                .maxVolumeM3(vehicle.getMaxVolumeM3())
                .isActive(vehicle.getIsActive())
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    public VehicleListItemResponse toListItem(Vehicle vehicle) {
        return VehicleListItemResponse.builder()
                .id(vehicle.getId())
                .plateNumber(vehicle.getPlateNumber())
                .vehicleType(vehicle.getVehicleType())
                .maxWeightKg(vehicle.getMaxWeightKg())
                .maxVolumeM3(vehicle.getMaxVolumeM3())
                .isActive(vehicle.getIsActive())
                .build();
    }

    public String normalizePlate(String plateNumber) {
        if (plateNumber == null) return null;
        return plateNumber.trim().toUpperCase();
    }
}

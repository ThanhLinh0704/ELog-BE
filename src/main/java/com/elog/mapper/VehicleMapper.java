package com.elog.mapper;

import com.elog.dto.request.VehicleCreateRequest;
import com.elog.dto.response.VehicleListItemResponse;
import com.elog.dto.response.VehicleResponse;
import com.elog.entity.Vehicle;
import com.elog.entity.VehicleStatus;
import org.springframework.stereotype.Component;

@Component
public class VehicleMapper {

    public Vehicle toEntity(VehicleCreateRequest request) {
        return Vehicle.builder()
                .vehicleCode(request.getVehicleCode() != null ? request.getVehicleCode().trim() : null)
                .plateNumber(normalizePlate(request.getPlateNumber()))
                .vehicleType(request.getVehicleType())
                .vehicleClass(request.getVehicleClass())
                .payloadKg(request.getPayloadKg())
                .grossVehicleWeightKg(request.getGrossVehicleWeightKg())
                .requiredLicense(request.getRequiredLicense())
                .maxVolumeM3(request.getMaxVolumeM3())
                .cargoLengthMm(request.getCargoLengthMm())
                .cargoWidthMm(request.getCargoWidthMm())
                .cargoHeightMm(request.getCargoHeightMm())
                .averageSpeedKmh(request.getAverageSpeedKmh())
                .costPerKm(request.getCostPerKm())
                .status(request.getStatus() != null ? request.getStatus() : VehicleStatus.AVAILABLE)
                .isActive(true)
                .imageUrl(request.getImageUrl())
                .permitInfo(request.getPermitInfo())
                .description(request.getDescription())
                .build();
    }

    public VehicleResponse toResponse(Vehicle vehicle) {
        Long driverId = vehicle.getAssignedDriver() != null ? vehicle.getAssignedDriver().getId() : null;
        String driverName = vehicle.getAssignedDriver() != null ? vehicle.getAssignedDriver().getFullName() : null;
        LicenseClass driverLicense = vehicle.getAssignedDriver() != null ? vehicle.getAssignedDriver().getLicenseClass() : null;

        return VehicleResponse.builder()
                .id(vehicle.getId())
                .vehicleCode(vehicle.getVehicleCode())
                .plateNumber(vehicle.getPlateNumber())
                .vehicleType(vehicle.getVehicleType())
                .vehicleClass(vehicle.getVehicleClass())
                .payloadKg(vehicle.getPayloadKg())
                .grossVehicleWeightKg(vehicle.getGrossVehicleWeightKg())
                .requiredLicense(vehicle.getRequiredLicense())
                .maxVolumeM3(vehicle.getMaxVolumeM3())
                .cargoLengthMm(vehicle.getCargoLengthMm())
                .cargoWidthMm(vehicle.getCargoWidthMm())
                .cargoHeightMm(vehicle.getCargoHeightMm())
                .averageSpeedKmh(vehicle.getAverageSpeedKmh())
                .costPerKm(vehicle.getCostPerKm())
                .status(vehicle.getStatus())
                .isActive(vehicle.getIsActive())
                .imageUrl(vehicle.getImageUrl())
                .permitInfo(vehicle.getPermitInfo())
                .description(vehicle.getDescription())
                .assignedDriverId(driverId)
                .assignedDriverName(driverName)
                .assignedDriverLicenseClass(driverLicense)
                .createdAt(vehicle.getCreatedAt())
                .updatedAt(vehicle.getUpdatedAt())
                .build();
    }

    public VehicleListItemResponse toListItem(Vehicle vehicle) {
        return VehicleListItemResponse.builder()
                .id(vehicle.getId())
                .vehicleCode(vehicle.getVehicleCode())
                .plateNumber(vehicle.getPlateNumber())
                .vehicleType(vehicle.getVehicleType())
                .vehicleClass(vehicle.getVehicleClass())
                .payloadKg(vehicle.getPayloadKg())
                .grossVehicleWeightKg(vehicle.getGrossVehicleWeightKg())
                .requiredLicense(vehicle.getRequiredLicense())
                .maxVolumeM3(vehicle.getMaxVolumeM3())
                .cargoLengthMm(vehicle.getCargoLengthMm())
                .cargoWidthMm(vehicle.getCargoWidthMm())
                .cargoHeightMm(vehicle.getCargoHeightMm())
                .averageSpeedKmh(vehicle.getAverageSpeedKmh())
                .costPerKm(vehicle.getCostPerKm())
                .status(vehicle.getStatus())
                .isActive(vehicle.getIsActive())
                .imageUrl(vehicle.getImageUrl())
                .permitInfo(vehicle.getPermitInfo())
                .description(vehicle.getDescription())
                .build();
    }

    public String normalizePlate(String plateNumber) {
        if (plateNumber == null) return null;
        return plateNumber.trim().toUpperCase();
    }
}

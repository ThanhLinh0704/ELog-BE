package com.elog.dto.response;

import com.elog.entity.LicenseClass;
import com.elog.entity.VehicleStatus;
import lombok.*;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleListItemResponse {

    private Long id;
    private String vehicleCode;
    private String plateNumber;
    private String vehicleType;
    private String vehicleClass;
    private BigDecimal payloadKg;
    private BigDecimal grossVehicleWeightKg;
    private LicenseClass requiredLicense;
    private BigDecimal maxVolumeM3;
    private Integer cargoLengthMm;
    private Integer cargoWidthMm;
    private Integer cargoHeightMm;
    private BigDecimal averageSpeedKmh;
    private BigDecimal costPerKm;
    private VehicleStatus status;
    private Boolean isActive;
    private String imageUrl;
    private String permitInfo;
    private String description;
    private Long assignedDriverId;
    private String assignedDriverName;
    private String assignedDriverPhone;
    private LicenseClass assignedDriverLicenseClass;
}

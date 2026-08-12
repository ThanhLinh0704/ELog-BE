package com.elog.dto.request.vehicle;

import com.elog.entity.LicenseClass;
import com.elog.entity.VehicleStatus;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleUpdateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 50, message = "INVALID_SIZE")
    private String vehicleType;

    @Size(max = 20, message = "INVALID_SIZE")
    private String vehicleClass;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "99999.00", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 5, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal payloadKg;

    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "99999.00", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 5, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal grossVehicleWeightKg;

    @NotNull(message = "FIELD_REQUIRED")
    private LicenseClass requiredLicense;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.001", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "999.000", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 3, fraction = 3, message = "INVALID_FORMAT")
    private BigDecimal maxVolumeM3;

    @Min(value = 1, message = "INVALID_FORMAT")
    private Integer cargoLengthMm;

    @Min(value = 1, message = "INVALID_FORMAT")
    private Integer cargoWidthMm;

    @Min(value = 1, message = "INVALID_FORMAT")
    private Integer cargoHeightMm;

    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "200.00", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 3, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal averageSpeedKmh;

    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "999999.00", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 6, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal costPerKm;

    private VehicleStatus status;

    @Size(max = 512, message = "INVALID_SIZE")
    private String imageUrl;

    private String permitInfo;

    private String description;

    private Long assignedDriverId;
}

package com.elog.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 6, max = 12, message = "INVALID_SIZE")
    @Pattern(regexp = "^[A-Za-z0-9]+(-[A-Za-z0-9]+)*$",
            message = "INVALID_FORMAT")
    private String plateNumber;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 50, message = "INVALID_SIZE")
    private String vehicleType;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "99999.00", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 5, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal maxWeightKg;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.001", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "999.000", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 3, fraction = 3, message = "INVALID_FORMAT")
    private BigDecimal maxVolumeM3;
}

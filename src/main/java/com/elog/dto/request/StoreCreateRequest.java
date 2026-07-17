package com.elog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreCreateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 3, max = 20, message = "INVALID_SIZE")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "INVALID_FORMAT")
    private String storeCode;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 100, message = "INVALID_SIZE")
    private String storeName;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(max = 20, message = "INVALID_SIZE")
    private String provinceCode;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(max = 20, message = "INVALID_SIZE")
    private String districtCode;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(max = 20, message = "INVALID_SIZE")
    private String wardCode;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 5, max = 255, message = "INVALID_SIZE")
    private String addressDetail;

    @Size(max = 100, message = "INVALID_SIZE")
    private String contactName;

    @Pattern(regexp = "^0\\d{9}$", message = "INVALID_FORMAT")
    private String contactPhone;

    @DecimalMin(value = "-90.0", message = "INVALID_FORMAT")
    @DecimalMax(value = "90.0", message = "INVALID_FORMAT")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "INVALID_FORMAT")
    @DecimalMax(value = "180.0", message = "INVALID_FORMAT")
    private Double longitude;

    private String allowedDeliveryHours;

    @DecimalMin(value = "0.0", message = "INVALID_FORMAT")
    private java.math.BigDecimal maxAllowedVehicleWeight;

    @Size(max = 512, message = "INVALID_SIZE")
    private String imageUrl;
}

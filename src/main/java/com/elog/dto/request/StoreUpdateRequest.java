package com.elog.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreUpdateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 100, message = "INVALID_SIZE")
    private String storeName;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 5, max = 255, message = "INVALID_SIZE")
    private String address;

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
}

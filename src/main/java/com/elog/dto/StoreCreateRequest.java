package com.elog.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StoreCreateRequest {

    @NotBlank(message = "Store code is required")
    @Size(min = 3, max = 20, message = "Store code must be 3-20 characters")
    @Pattern(regexp = "^[A-Za-z0-9-]+$", message = "Store code may only contain letters, digits, and hyphens")
    private String storeCode;

    @NotBlank(message = "Store name is required")
    @Size(min = 2, max = 100, message = "Store name must be 2-100 characters")
    private String storeName;

    @NotBlank(message = "Address is required")
    @Size(min = 5, max = 255, message = "Address must be 5-255 characters")
    private String address;

    @Size(max = 100)
    private String contactName;

    @Pattern(regexp = "^0\\d{9}$", message = "Phone must be 10 digits starting with 0")
    private String contactPhone;

    @DecimalMin(value = "-90.0", message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", message = "Latitude must be <= 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", message = "Longitude must be <= 180")
    private Double longitude;
}

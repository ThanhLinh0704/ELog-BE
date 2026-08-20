package com.elog.dto.request.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductCreateRequest {

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 3, max = 50, message = "INVALID_SIZE")
    @Pattern(regexp = "^[A-Z0-9-]{3,50}$", message = "INVALID_FORMAT")
    private String sku;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 100, message = "INVALID_SIZE")
    private String productName;

    @Size(max = 100, message = "INVALID_SIZE")
    private String brand;

    @Size(max = 100, message = "INVALID_SIZE")
    private String productGroup;

    @Size(max = 100, message = "INVALID_SIZE")
    private String productType;

    @DecimalMin(value = "0", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 8, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal capacityValue;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.001", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 5, fraction = 3, message = "INVALID_FORMAT")
    private BigDecimal weightKg;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.0001", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 4, fraction = 4, message = "INVALID_FORMAT")
    private BigDecimal lengthM;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.0001", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 4, fraction = 4, message = "INVALID_FORMAT")
    private BigDecimal widthM;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.0001", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 4, fraction = 4, message = "INVALID_FORMAT")
    private BigDecimal heightM;

    private String shape;

    private Boolean isFragile;

    @Size(max = 512, message = "INVALID_SIZE")
    private String packageImageUrl;

    private String description;
}

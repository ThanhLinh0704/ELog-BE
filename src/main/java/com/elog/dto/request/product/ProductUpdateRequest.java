package com.elog.dto.request.product;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductUpdateRequest {

    // Optional: if provided and differs from the existing SKU → 400 PRODUCT_SKU_IMMUTABLE
    private String sku;

    @NotBlank(message = "FIELD_REQUIRED")
    @Size(min = 2, max = 100, message = "INVALID_SIZE")
    private String productName;

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

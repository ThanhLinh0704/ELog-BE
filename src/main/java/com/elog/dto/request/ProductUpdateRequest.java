package com.elog.dto.request;

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
    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 6, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal lengthCm;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 6, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal widthCm;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.01", inclusive = true, message = "INVALID_FORMAT")
    @Digits(integer = 6, fraction = 2, message = "INVALID_FORMAT")
    private BigDecimal heightCm;
}

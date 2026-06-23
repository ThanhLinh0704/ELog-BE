package com.elog.exception;

import lombok.Getter;

/**
 * Application-level error codes — map 1:1 to the codes in
 * docs/api-conventions.md.
 * Used in ApiErrorResponse.code field so frontend can handle errors
 * programmatically.
 */
@Getter
public enum ErrorCode {

    // Resource errors
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
    ACCESS_DENIED("ACCESS_DENIED"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS"),
    TOKEN_EXPIRED("TOKEN_EXPIRED"),
    TOKEN_INVALID("TOKEN_INVALID"),
    ACCOUNT_DISABLED("ACCOUNT_DISABLED"),

    // Business rule violations
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND"),
    PRODUCT_SKU_DUPLICATE("PRODUCT_SKU_DUPLICATE"),
    PRODUCT_SKU_IMMUTABLE("PRODUCT_SKU_IMMUTABLE"),

    STORE_NOT_FOUND("STORE_NOT_FOUND"),
    STORE_CODE_DUPLICATE("STORE_CODE_DUPLICATE"),
    STORE_ACTIVE_ROUTE("STORE_ACTIVE_ROUTE"),
    INVALID_COORDINATES("INVALID_COORDINATES"),
    STORE_NOT_ASSIGNABLE("STORE_NOT_ASSIGNABLE"),       // BR-02

    // Route business rules
    ROUTE_NOT_FOUND("ROUTE_NOT_FOUND"),
    ROUTE_CODE_DUPLICATE("ROUTE_CODE_DUPLICATE"),
    ROUTE_STOP_DUPLICATE("ROUTE_STOP_DUPLICATE"),
    ROUTE_INSUFFICIENT_STOPS("ROUTE_INSUFFICIENT_STOPS"),
    ROUTE_STOP_NOT_FOUND("ROUTE_STOP_NOT_FOUND"),
    ROUTE_STOP_REORDER_INVALID("ROUTE_STOP_REORDER_INVALID"),
    STORE_INACTIVE("STORE_INACTIVE"),

    CAPACITY_EXCEEDED("CAPACITY_EXCEEDED"),             // BR-03
    FLEET_CAPACITY_INSUFFICIENT("FLEET_CAPACITY_INSUFFICIENT"), // BR-08
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION"), // DC-01
    EPOD_REQUIRED("EPOD_REQUIRED"), // BR-11

    // Import
    EXCEL_PARSE_ERROR("EXCEL_PARSE_ERROR"),

    // Validation
    FIELD_REQUIRED("Field cannot be blank"),
    INVALID_FORMAT("Invalid format"),
    INVALID_SIZE("Invalid size"),
    USERNAME_INVALID("Username must be 3-50 characters and contain only letters, numbers, and underscores"),
    PASSWORD_INVALID(
            "Password must be at least 8 characters, containing at least 1 uppercase letter, 1 number, and 1 special character"),

    // Generic
    VALIDATION_FAILED("VALIDATION_FAILED"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }
}

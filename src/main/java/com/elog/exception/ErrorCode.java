package com.elog.exception;

import lombok.Getter;

/**
 * Application-level error codes — map 1:1 to the codes in docs/api-conventions.md.
 * Used in ApiErrorResponse.code field so frontend can handle errors programmatically.
 */
@Getter
public enum ErrorCode {

    // Resource errors
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND"),
    ACCESS_DENIED("ACCESS_DENIED"),
    INVALID_CREDENTIALS("INVALID_CREDENTIALS"),
    TOKEN_EXPIRED("TOKEN_EXPIRED"),
    TOKEN_INVALID("TOKEN_INVALID"),

    // Business rule violations
    STORE_NOT_FOUND("STORE_NOT_FOUND"),
    STORE_CODE_DUPLICATE("STORE_CODE_DUPLICATE"),
    STORE_ACTIVE_ROUTE("STORE_ACTIVE_ROUTE"),
    INVALID_COORDINATES("INVALID_COORDINATES"),
    STORE_NOT_ASSIGNABLE("STORE_NOT_ASSIGNABLE"),       // BR-02
    CAPACITY_EXCEEDED("CAPACITY_EXCEEDED"),             // BR-03
    FLEET_CAPACITY_INSUFFICIENT("FLEET_CAPACITY_INSUFFICIENT"), // BR-08
    INVALID_STATE_TRANSITION("INVALID_STATE_TRANSITION"), // DC-01
    EPOD_REQUIRED("EPOD_REQUIRED"),                     // BR-11

    // Import
    EXCEL_PARSE_ERROR("EXCEL_PARSE_ERROR"),

    // Generic
    VALIDATION_FAILED("VALIDATION_FAILED"),
    INTERNAL_ERROR("INTERNAL_ERROR");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }
}

package com.elog.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Business exception — thrown from service layer when a business rule is violated.
 * Caught by GlobalExceptionHandler and returned as a structured error response.
 *
 * Usage:
 *   throw new BusinessException(ErrorCode.CAPACITY_EXCEEDED, "Volume exceeds vehicle limit");
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = HttpStatus.UNPROCESSABLE_ENTITY; // 422 default
    }

    public BusinessException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}

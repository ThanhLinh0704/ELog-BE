package com.elog.entity;

/**
 * Loại ngoại lệ vận hành trong delivery_exceptions
 * TIME_EXCEPTION       — Hệ thống auto-flag khi stop quá ETA (BR-09, US-17)
 * DELIVERY_REJECTION   — Driver báo cửa hàng từ chối nhận hàng (BR-10, US-18)
 */
public enum ExceptionType {
    TIME_EXCEPTION,
    DELIVERY_REJECTION
}

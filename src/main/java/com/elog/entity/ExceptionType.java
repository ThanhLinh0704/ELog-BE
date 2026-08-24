package com.elog.entity;

/**
 * Loại ngoại lệ vận hành trong delivery_exceptions
 * TIME_EXCEPTION              — Hệ thống auto-flag khi stop quá ETA (BR-09, US-17)
 * DELIVERY_REJECTION          — Driver báo cửa hàng từ chối nhận hàng (BR-10, US-18)
 * TRIP_STALE_UNSTARTED        — Hệ thống auto-flag chuyến DISPATCHED quá hạn N ngày mà chưa bắt đầu
 * TRIP_START_DEADLINE_EXCEEDED — Hệ thống auto-flag chuyến DISPATCHED quá hạn N phút kể từ lúc gán xe
 *                                mà tài xế chưa bấm "Bắt đầu chuyến" (nút bị khoá phía server)
 */
public enum ExceptionType {
    TIME_EXCEPTION,
    DELIVERY_REJECTION,
    TRIP_STALE_UNSTARTED,
    TRIP_START_DEADLINE_EXCEEDED
}

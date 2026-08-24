package com.elog.service;

import com.elog.entity.Trip;
import com.elog.entity.TripStatus;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * US-16 TASK-01 — Trip State Machine (DC-01 Guards).
 *
 * Valid transitions:
 *   VALIDATED   → DISPATCHED
 *   DISPATCHED  → IN_PROGRESS
 *   IN_PROGRESS → COMPLETED
 *
 * All other transitions are rejected with HTTP 409.
 */
@Component
public class TripStateMachine {

    public void transition(Trip trip, TripStatus newStatus, Long actorId) {
        TripStatus current = trip.getStatus();

        if (!isValidTransition(current, newStatus)) {
            if (current == TripStatus.COMPLETED) {
                throw new BusinessException(ErrorCode.TRIP_COMPLETED,
                        "Trip " + trip.getTripId() + " is completed. No further transitions allowed.",
                        HttpStatus.CONFLICT);
            }
            if (current == TripStatus.DISPATCHED && newStatus == TripStatus.VALIDATED) {
                throw new BusinessException(ErrorCode.TRIP_LOCKED,
                        "Trip " + trip.getTripId() + " is already dispatched (locked at " + trip.getLockedAt() + ").",
                        HttpStatus.CONFLICT);
            }
            throw new BusinessException(ErrorCode.INVALID_TRIP_TRANSITION,
                    "Cannot transition trip " + trip.getTripId()
                            + " from " + current + " to " + newStatus + ".",
                    HttpStatus.CONFLICT);
        }

        // Apply side effects
        switch (newStatus) {
            case DISPATCHED -> {
                trip.setLockedAt(LocalDateTime.now());
                // Xe đã bị khoá vào chuyến này (dispatched = sẵn sàng lăn bánh, chỉ chờ tài xế
                // bấm bắt đầu) — phải coi là bận ngay từ đây, không phải AVAILABLE. Toàn bộ phần
                // còn lại của hệ thống (mọi busyStatuses check) đã coi DISPATCHED là bận; dòng
                // này trước đây set ngược lại khiến xe hiện "Sẵn sàng" suốt khoảng thời gian giữa
                // lúc dispatch và lúc tài xế thực sự bấm bắt đầu chuyến trên app — có thể bị gán
                // nhầm cho chuyến khác trong lúc đó.
                if (trip.getVehicle() != null) {
                    trip.getVehicle().setStatus(com.elog.entity.VehicleStatus.IN_USE);
                }
            }
            case IN_PROGRESS -> {
                trip.setActualDepartureTime(LocalDateTime.now());
                if (trip.getVehicle() != null) {
                    trip.getVehicle().setStatus(com.elog.entity.VehicleStatus.IN_USE);
                }
            }
            case COMPLETED -> {
                trip.setCompletedAt(LocalDateTime.now());
                // Note: Vehicle remains IN_USE until driver confirms returnToWarehouse
            }
            case CANCELLED -> {
                trip.setCancelledAt(LocalDateTime.now());
                // DISPATCHED -> CANCELLED is the only valid path here (see isValidTransition) — the
                // trip was never started (driver never left the warehouse), so unlike COMPLETED the
                // vehicle can be released immediately without waiting for a return-to-warehouse
                // confirmation.
                if (trip.getVehicle() != null) {
                    trip.getVehicle().setStatus(com.elog.entity.VehicleStatus.AVAILABLE);
                }
            }
            default -> { /* VALIDATED has no side effects */ }
        }

        trip.setStatus(newStatus);
    }

    private boolean isValidTransition(TripStatus from, TripStatus to) {
        return switch (from) {
            case VALIDATED   -> to == TripStatus.DISPATCHED;
            case DISPATCHED  -> to == TripStatus.IN_PROGRESS || to == TripStatus.CANCELLED;
            case IN_PROGRESS -> to == TripStatus.COMPLETED;
            case COMPLETED   -> false;
            case CANCELLED   -> false;
        };
    }
}

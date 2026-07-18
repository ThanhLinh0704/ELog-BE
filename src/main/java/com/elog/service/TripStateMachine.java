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
                // lockedBy is set by caller (need User entity)
            }
            case IN_PROGRESS -> {
                trip.setActualDepartureTime(LocalDateTime.now());
            }
            case COMPLETED -> {
                trip.setCompletedAt(LocalDateTime.now());
            }
            default -> { /* VALIDATED has no side effects */ }
        }

        trip.setStatus(newStatus);
    }

    private boolean isValidTransition(TripStatus from, TripStatus to) {
        return switch (from) {
            case VALIDATED   -> to == TripStatus.DISPATCHED;
            case DISPATCHED  -> to == TripStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == TripStatus.COMPLETED;
            case COMPLETED   -> false;
        };
    }
}

package com.elog.service.impl;

import com.elog.entity.DeliveryException;
import com.elog.entity.ExceptionType;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.repository.DeliveryExceptionRepository;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Shared deadline math + dispatcher-flagging for the "chặn bắt đầu chuyến quá hạn" rule: driver
 * must tap "Bắt đầu chuyến" within TRIP_START_DEADLINE_MINUTES of vehicle assignment
 * (trip.lockedAt), or the start action is rejected and the trip is flagged for the dispatcher.
 * Used by both {@link DriverTripServiceImpl#startTrip} (blocks the attempt, flags immediately)
 * and {@link TripStartDeadlineSweepJob} (catches trips whose driver never opened the app at all).
 * See filemd/new-bug.md.
 */
@Component
@RequiredArgsConstructor
public class TripStartDeadlineService {

    private static final Long SYSTEM_USER_ID = 1L;
    private static final String CONFIG_KEY = "TRIP_START_DEADLINE_MINUTES";
    private static final int DEFAULT_DEADLINE_MINUTES = 15;

    private final SystemConfigRepository systemConfigRepo;
    private final DeliveryExceptionRepository deliveryExceptionRepo;
    private final TripExecutionRepository tripExecutionRepo;

    public int getDeadlineMinutes() {
        return systemConfigRepo.findByConfigKey(CONFIG_KEY)
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(DEFAULT_DEADLINE_MINUTES);
    }

    public boolean isDeadlineExceeded(Trip trip) {
        if (trip.getLockedAt() == null) {
            return false;
        }
        return LocalDateTime.now().isAfter(trip.getLockedAt().plusMinutes(getDeadlineMinutes()));
    }

    /**
     * Creates a dispatcher-facing exception flag for this overdue trip, unless one is already
     * open. Returns true if a new flag was created.
     */
    @Transactional
    public boolean flagIfNeeded(Trip trip) {
        Optional<TripExecution> executionOpt = tripExecutionRepo.findByTripId(trip.getTripId());
        if (executionOpt.isEmpty()) {
            return false;
        }
        Long executionId = executionOpt.get().getId();

        boolean alreadyFlagged = deliveryExceptionRepo
                .existsByTripExecutionIdAndExceptionTypeAndResolvedAtIsNull(
                        executionId, ExceptionType.TRIP_START_DEADLINE_EXCEEDED);
        if (alreadyFlagged) {
            return false;
        }

        DeliveryException ex = DeliveryException.builder()
                .tripExecutionId(executionId)
                .exceptionType(ExceptionType.TRIP_START_DEADLINE_EXCEEDED)
                .reportedBy(SYSTEM_USER_ID)
                .description(String.format(
                        "Trip %d (route %s) exceeded the %d-minute start deadline since vehicle "
                                + "assignment at %s and the driver still has not started it.",
                        trip.getTripId(),
                        trip.getRoute() != null ? trip.getRoute().getCode() : "?",
                        getDeadlineMinutes(), trip.getLockedAt()))
                .build();
        deliveryExceptionRepo.save(ex);
        return true;
    }
}

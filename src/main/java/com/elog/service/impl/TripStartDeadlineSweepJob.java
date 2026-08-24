package com.elog.service.impl;

import com.elog.entity.Trip;
import com.elog.entity.TripStatus;
import com.elog.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Safety-net sweep for the "chặn bắt đầu chuyến quá hạn" rule (10-15 min). The button-side guard
 * in {@link DriverTripServiceImpl#startTrip} only fires when the driver actually opens the app
 * and taps "Bắt đầu chuyến" — if the driver never opens the app at all, nothing triggers that
 * check. This job runs every few minutes (deadline window is short, unlike
 * {@link TripStaleDetectionJob}'s once-a-day sweep) so a silent no-show still gets caught and
 * flagged for the dispatcher within a few minutes, not the next morning.
 * See filemd/new-bug.md.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TripStartDeadlineSweepJob {

    private final TripRepository tripRepository;
    private final TripStartDeadlineService deadlineService;

    @Scheduled(fixedRate = 2 * 60 * 1000)
    @Transactional
    public void sweep() {
        int deadlineMinutes = deadlineService.getDeadlineMinutes();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(deadlineMinutes);

        List<Trip> overdueTrips = tripRepository.findByStatusAndLockedAtBefore(TripStatus.DISPATCHED, cutoff);
        if (overdueTrips.isEmpty()) {
            log.debug("TripStartDeadlineSweepJob: no overdue trips (threshold {} min).", deadlineMinutes);
            return;
        }

        int flagged = 0;
        for (Trip trip : overdueTrips) {
            if (deadlineService.flagIfNeeded(trip)) {
                flagged++;
            }
        }

        log.info("TripStartDeadlineSweepJob: checked {} overdue trip(s), flagged {} new (threshold {} min).",
                overdueTrips.size(), flagged, deadlineMinutes);
    }
}

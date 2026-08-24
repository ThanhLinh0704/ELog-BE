package com.elog.service.impl;

import com.elog.entity.DeliveryException;
import com.elog.entity.ExceptionType;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.entity.TripStatus;
import com.elog.repository.DeliveryExceptionRepository;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripExecutionRepository;
import com.elog.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Phát hiện chuyến DISPATCHED quá hạn N ngày kể từ deliveryDate mà vẫn chưa bắt đầu (driver chưa
 * bấm "Bắt đầu chuyến"). Chạy 1 lần/ngày — không cần dày như TimeExceptionDetectionJob (check theo
 * phút cho ETA từng điểm dừng), vì đây là cảnh báo ở cấp độ ngày.
 * Xem filemd/BE_FIX_CANCEL_TRIP_AND_STALE_WARNING_2026-08-23.md.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TripStaleDetectionJob {

    private static final Long SYSTEM_USER_ID = 1L;

    private final TripRepository tripRepository;
    private final SystemConfigRepository systemConfigRepo;
    private final DeliveryExceptionRepository deliveryExceptionRepo;
    private final TripExecutionRepository tripExecutionRepo;

    @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void detectStaleTrips() {
        int thresholdDays = getStaleThresholdDays();
        LocalDate cutoff = LocalDate.now().minusDays(thresholdDays);

        List<Trip> staleTrips = tripRepository.findByStatusAndDeliveryDateBefore(
                TripStatus.DISPATCHED, cutoff);

        if (staleTrips.isEmpty()) {
            log.debug("TripStaleDetectionJob: no stale trips found (threshold {} days).", thresholdDays);
            return;
        }

        int flagged = 0;
        for (Trip trip : staleTrips) {
            Optional<TripExecution> executionOpt = tripExecutionRepo.findByTripId(trip.getTripId());
            if (executionOpt.isEmpty()) {
                continue;
            }
            Long executionId = executionOpt.get().getId();

            boolean alreadyFlagged = deliveryExceptionRepo
                    .existsByTripExecutionIdAndExceptionTypeAndResolvedAtIsNull(
                            executionId, ExceptionType.TRIP_STALE_UNSTARTED);
            if (alreadyFlagged) {
                continue;
            }

            long daysOverdue = ChronoUnit.DAYS.between(trip.getDeliveryDate(), LocalDate.now());

            DeliveryException ex = DeliveryException.builder()
                    .tripExecutionId(executionId)
                    .exceptionType(ExceptionType.TRIP_STALE_UNSTARTED)
                    .reportedBy(SYSTEM_USER_ID)
                    .description(String.format(
                            "Trip %d (route %s) is %d days past its delivery date (%s) and still not started. "
                                    + "Threshold: %d days.",
                            trip.getTripId(),
                            trip.getRoute() != null ? trip.getRoute().getCode() : "?",
                            daysOverdue, trip.getDeliveryDate(), thresholdDays))
                    .build();
            deliveryExceptionRepo.save(ex);
            flagged++;
        }

        log.info("TripStaleDetectionJob: flagged {}/{} stale trip(s) (threshold {} days).",
                flagged, staleTrips.size(), thresholdDays);
    }

    private int getStaleThresholdDays() {
        return systemConfigRepo.findByConfigKey("TRIP_STALE_THRESHOLD_DAYS")
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(3);
    }
}

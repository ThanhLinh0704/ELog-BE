package com.elog.service.impl;

import com.elog.entity.DeliveryException;
import com.elog.entity.ExceptionType;
import com.elog.entity.TripStop;
import com.elog.entity.TripStopStatus;
import com.elog.repository.DeliveryExceptionRepository;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripStopRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Phát hiện TIME_EXCEPTION tự động .
 * Chạy mỗi 5 phút trong giờ vận hành (7h–20h).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TimeExceptionDetectionJob {

    private static final Long SYSTEM_USER_ID = 1L;

    private final TripStopRepository tripStopRepo;
    private final SystemConfigRepository systemConfigRepo;
    private final DeliveryExceptionRepository deliveryExceptionRepo;

    @Scheduled(cron = "0 */5 7-20 * * *")
    @Transactional
    public void detectTimeExceptions() {
        int thresholdMinutes = getEtaThresholdMinutes();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cutoff = now.minusMinutes(thresholdMinutes);

        log.debug("TimeExceptionDetectionJob running. Threshold: {} min, Cutoff: {}", thresholdMinutes, cutoff);

        List<TripStop> overdueStops = tripStopRepo.findOverdueStops(cutoff);

        if (overdueStops.isEmpty()) {
            log.debug("TimeExceptionDetectionJob: no overdue stops found.");
            return;
        }

        log.info("TimeExceptionDetectionJob: found {} overdue stop(s).", overdueStops.size());

        for (TripStop stop : overdueStops) {
            long delayMinutes = ChronoUnit.MINUTES.between(stop.getPlannedEta(), now);

            String storeCode = stop.getRouteStop() != null && stop.getRouteStop().getStore() != null
                    ? stop.getRouteStop().getStore().getCode()
                    : "STOP-" + stop.getTripStopId();

            DeliveryException ex = DeliveryException.builder()
                    .tripStopId(stop.getTripStopId())
                    .exceptionType(ExceptionType.TIME_EXCEPTION)
                    .reportedBy(SYSTEM_USER_ID)
                    .description(String.format(
                            "Stop %s is %d minutes past planned ETA (%s). Threshold: %d min.",
                            storeCode, delayMinutes, stop.getPlannedEta(), thresholdMinutes))
                    .build();
            deliveryExceptionRepo.save(ex);

            stop.setStatus(TripStopStatus.EXCEPTION);
            tripStopRepo.save(stop);

            log.info("[TimeExceptionJob] TripStop {} ({}) flagged EXCEPTION — {} min late.",
                    stop.getTripStopId(), storeCode, delayMinutes);
        }
    }

    private int getEtaThresholdMinutes() {
        return systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES")
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(15);
    }
}

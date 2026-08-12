package com.elog.service.impl;

import com.elog.dto.response.trip.StopEtaResponse;
import com.elog.entity.Store;
import com.elog.entity.TripDraft;
import com.elog.entity.TripDraftStop;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.TripDraftStopRepository;
import com.elog.service.EtaCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Haversine-based ETA calculator.
 * Computes sequential linear ETA for active stops using GPS coordinates,
 * average speed from SystemConfig, and per-stop service time.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HaversineEtaCalculator implements EtaCalculationService {

    private static final double EARTH_RADIUS_KM = 6371.0;

    private final SystemConfigRepository configRepo;
    private final TripDraftRepository tripDraftRepo;
    private final TripDraftStopRepository stopRepo;

    @Override
    @Transactional
    public List<StopEtaResponse> calculateAndPersist(Long tripDraftId, LocalTime departureTime) {
        // 1. Load config
        double avgSpeedKmh = getConfigDouble("AVG_SPEED_KMH");
        double warehouseLat = getConfigDouble("WAREHOUSE_LAT");
        double warehouseLng = getConfigDouble("WAREHOUSE_LNG");

        // 2. Load TripDraft
        TripDraft draft = tripDraftRepo.findById(tripDraftId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found with id: " + tripDraftId,
                        HttpStatus.NOT_FOUND));

        if ("CONFIRMED".equals(draft.getStatus()) || "CANCELLED".equals(draft.getStatus())) {
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_LOCKED,
                    "Trip Draft already confirmed or cancelled (status=" + draft.getStatus() + "). Cannot recalculate ETA.",
                    HttpStatus.CONFLICT);
        }

        // 3. Load active stops in order
        List<TripDraftStop> activeStops = stopRepo
                .findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);

        if (activeStops.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.NO_ACTIVE_STOP,
                    "Trip Draft " + tripDraftId + " has no active stops. Cannot calculate ETA.",
                    HttpStatus.BAD_REQUEST);
        }

        // 4. Calculate sequential ETA
        LocalDate deliveryDate = draft.getDeliveryDate();
        LocalDateTime currentEta = LocalDateTime.of(deliveryDate, departureTime);
        double prevLat = warehouseLat;
        double prevLng = warehouseLng;
        List<StopEtaResponse> results = new ArrayList<>();

        for (int i = 0; i < activeStops.size(); i++) {
            TripDraftStop stop = activeStops.get(i);
            Store store = stop.getStore();

            if (store.getLatitude() == null || store.getLongitude() == null) {
                throw new BusinessException(
                        ErrorCode.ETA_MISSING_COORDINATES,
                        "Store " + store.getCode() + " is missing GPS coordinates. Cannot calculate ETA.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }

            double stopLat = store.getLatitude();
            double stopLng = store.getLongitude();

            // Add service time of PREVIOUS stop (not for the first stop)
            if (i > 0) {
                TripDraftStop prevStop = activeStops.get(i - 1);
                int prevServiceMin = (prevStop.getRouteStop() != null && prevStop.getRouteStop().getAvgServiceTimeMin() != null)
                        ? prevStop.getRouteStop().getAvgServiceTimeMin()
                        : 15;
                currentEta = currentEta.plusMinutes(prevServiceMin);
            }

            // Calculate travel time
            double distanceKm = haversine(prevLat, prevLng, stopLat, stopLng);
            long travelMinutes = Math.round((distanceKm / avgSpeedKmh) * 60);
            currentEta = currentEta.plusMinutes(travelMinutes);

            java.math.BigDecimal distKmBd = java.math.BigDecimal.valueOf(distanceKm).setScale(2, java.math.RoundingMode.HALF_UP);
            int travelMinInt = (int) travelMinutes;

            stop.setDistanceFromPrevKm(distKmBd);
            stop.setTravelTimeFromPrevMin(travelMinInt);

            // Time window calculation & waiting time check

            LocalTime twStart = store.getTimeWindowStart();
            LocalTime twEnd = store.getTimeWindowEnd();

            // Fallback: Parse allowedDeliveryHours e.g. "10:00-12:00" if timeWindowStart is null
            if (twStart == null && store.getAllowedDeliveryHours() != null 
                    && !store.getAllowedDeliveryHours().equalsIgnoreCase("All")) {
                try {
                    String[] parts = store.getAllowedDeliveryHours().split("-");
                    if (parts.length == 2) {
                        twStart = LocalTime.parse(parts[0].trim());
                        twEnd = LocalTime.parse(parts[1].trim());
                    }
                } catch (Exception e) {
                    log.warn("Failed to parse allowedDeliveryHours for store {}: {}", store.getCode(), store.getAllowedDeliveryHours());
                }
            }

            LocalTime arrivalTime = currentEta.toLocalTime();
            if (twStart != null && arrivalTime.isBefore(twStart)) {
                long waitMin = java.time.temporal.ChronoUnit.MINUTES.between(arrivalTime, twStart);
                stop.setPlannedWaitingTimeMin((int) waitMin);
                if (waitMin <= 30) {
                    // Accept waiting time, adjust departure from stop using twStart
                    currentEta = LocalDateTime.of(currentEta.toLocalDate(), twStart);
                    stop.setViolationCode(null);
                } else {
                    stop.setViolationCode("TIME_WINDOW_EARLY");
                }
            } else if (twEnd != null && arrivalTime.isAfter(twEnd)) {
                stop.setPlannedWaitingTimeMin(0);
                stop.setViolationCode("TIME_WINDOW_LATE");
            } else {
                stop.setPlannedWaitingTimeMin(0);
                stop.setViolationCode(null);
            }

            // Set planned ETA
            stop.setPlannedEta(currentEta);

            results.add(StopEtaResponse.builder()
                    .tripDraftStopId(stop.getId())
                    .sequenceNo(stop.getSequenceNo())
                    .storeCode(store.getCode())
                    .plannedEta(currentEta)
                    .distanceFromPrevKm(distKmBd)
                    .travelTimeFromPrevMin(travelMinInt)
                    .estimatedDistanceKm(distKmBd)
                    .estimatedTravelMin(travelMinInt)
                    .build());


            prevLat = stopLat;
            prevLng = stopLng;
        }

        // 5. Calculate return leg distance (last stop to warehouse) and update totalDistanceKm
        double returnDistKm = haversine(prevLat, prevLng, warehouseLat, warehouseLng);
        java.math.BigDecimal returnKmBd = java.math.BigDecimal.valueOf(returnDistKm).setScale(2, java.math.RoundingMode.HALF_UP);
        draft.setReturnDistanceKm(returnKmBd);

        java.math.BigDecimal totalKm = activeStops.stream()
                .map(TripDraftStop::getDistanceFromPrevKm)
                .filter(Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add)
                .add(returnKmBd);
        draft.setTotalDistanceKm(totalKm);

        // 6. Persist all at once
        stopRepo.saveAll(activeStops);

        // 7. Update departure time on TripDraft
        draft.setPlannedDepartureTime(departureTime);
        tripDraftRepo.save(draft);

        log.info("US-11: ETA calculated for TripDraft id={}, {} active stops", tripDraftId, results.size());
        return results;
    }

    /**
     * Haversine formula — great-circle distance between two GPS points.
     */
    double haversine(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private double getConfigDouble(String key) {
        return configRepo.findByConfigKey(key)
                .map(config -> Double.parseDouble(config.getConfigValue()))
                .orElseThrow(() -> {
                    log.error("Missing config key: {}", key);
                    return new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            "Missing system config key: " + key,
                            HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }
}

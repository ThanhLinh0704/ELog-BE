package com.elog.service.impl;

import com.elog.dto.response.goong.GoongDirectionsResponse;
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
import com.elog.service.GoongMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Primary ETA Calculator using Goong.io REST APIs.
 * Calculates sequential road driving distance and duration between stops,
 * updating planned_eta, planned_waiting_time_min, violation_code, total_distance_km,
 * and route_polyline.
 *
 * Fallbacks to HaversineEtaCalculator if Goong API is unavailable or returns an error.
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class GoongEtaCalculator implements EtaCalculationService {

    private final GoongMapService goongMapService;
    private final HaversineEtaCalculator haversineEtaCalculator;
    private final SystemConfigRepository configRepo;
    private final TripDraftRepository tripDraftRepo;
    private final TripDraftStopRepository stopRepo;

    @Override
    @Transactional
    public List<StopEtaResponse> calculateAndPersist(Long tripDraftId, LocalTime departureTime) {
        // 1. Check if Goong is configured; if not, fallback immediately
        if (!goongMapService.isConfigured()) {
            log.warn("Goong API is not configured. Falling back to Haversine ETA Calculator for tripDraftId={}", tripDraftId);
            return haversineEtaCalculator.calculateAndPersist(tripDraftId, departureTime);
        }

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

        // 3. Load active stops in sequence
        List<TripDraftStop> activeStops = stopRepo
                .findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);

        if (activeStops.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.NO_ACTIVE_STOP,
                    "Trip Draft " + tripDraftId + " has no active stops. Cannot calculate ETA.",
                    HttpStatus.BAD_REQUEST);
        }

        // 4. Validate coordinates
        double avgSpeedKmh = getConfigDouble("AVG_SPEED_KMH");
        double warehouseLat = getConfigDouble("WAREHOUSE_LAT");
        double warehouseLng = getConfigDouble("WAREHOUSE_LNG");

        for (TripDraftStop stop : activeStops) {
            Store store = stop.getStore();
            if (store.getLatitude() == null || store.getLongitude() == null) {
                throw new BusinessException(
                        ErrorCode.ETA_MISSING_COORDINATES,
                        "Store " + store.getCode() + " is missing GPS coordinates. Cannot calculate ETA.",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        // 5. Construct list of coordinates (warehouse -> stop1 -> stop2 -> ... -> stopN -> warehouse)
        List<double[]> coords = new ArrayList<>();
        coords.add(new double[]{warehouseLat, warehouseLng});
        for (TripDraftStop stop : activeStops) {
            coords.add(new double[]{stop.getStore().getLatitude(), stop.getStore().getLongitude()});
        }
        coords.add(new double[]{warehouseLat, warehouseLng});

        // 6. Call Goong Directions API with single request using waypoints
        String origin = coords.get(0)[0] + "," + coords.get(0)[1];
        String destination = coords.get(coords.size() - 1)[0] + "," + coords.get(coords.size() - 1)[1];

        String waypoints = null;
        if (coords.size() > 2) {
            waypoints = coords.subList(1, coords.size() - 1).stream()
                    .map(pt -> pt[0] + "," + pt[1])
                    .collect(Collectors.joining("|"));
        }

        GoongDirectionsResponse response = goongMapService.getDirections(origin, destination, waypoints);
        if (response == null || response.getRoutes() == null || response.getRoutes().isEmpty()
                || response.getRoutes().get(0).getLegs() == null
                || response.getRoutes().get(0).getLegs().size() < coords.size() - 1) {
            log.warn("Goong directions call failed for tripDraftId={}. Falling back to Haversine.", tripDraftId);
            return haversineEtaCalculator.calculateAndPersist(tripDraftId, departureTime);
        }

        GoongDirectionsResponse.Route route = response.getRoutes().get(0);
        List<GoongDirectionsResponse.Leg> legs = route.getLegs();
        String fullPolyline = (route.getOverviewPolyline() != null && route.getOverviewPolyline().getPoints() != null)
                ? route.getOverviewPolyline().getPoints()
                : null;

        // 7. Calculate sequential ETA using Goong leg travel durations and distances
        LocalDate deliveryDate = draft.getDeliveryDate();
        LocalDateTime currentEta = LocalDateTime.of(deliveryDate, departureTime);
        double totalDistanceMeters = 0;

        List<StopEtaResponse> results = new ArrayList<>();

        for (int i = 0; i < activeStops.size(); i++) {
            TripDraftStop stop = activeStops.get(i);
            Store store = stop.getStore();
            GoongDirectionsResponse.Leg leg = legs.get(i);

            long legMeters = (leg.getDistance() != null && leg.getDistance().getValue() != null)
                    ? leg.getDistance().getValue() : 0L;

            totalDistanceMeters += legMeters;

            BigDecimal legDistanceKm = BigDecimal.valueOf(legMeters / 1000.0).setScale(2, RoundingMode.HALF_UP);
            int legTravelMinutes = (int) Math.round((legDistanceKm.doubleValue() / avgSpeedKmh) * 60.0);

            stop.setDistanceFromPrevKm(legDistanceKm);
            stop.setTravelTimeFromPrevMin(legTravelMinutes);

            // Add service time of PREVIOUS stop (not for the first stop)
            if (i > 0) {
                TripDraftStop prevStop = activeStops.get(i - 1);
                int prevServiceMin = (prevStop.getRouteStop() != null && prevStop.getRouteStop().getAvgServiceTimeMin() != null)
                        ? prevStop.getRouteStop().getAvgServiceTimeMin()
                        : 15;
                currentEta = currentEta.plusMinutes(prevServiceMin);
            }

            // Add travel time for this leg
            currentEta = currentEta.plusMinutes(legTravelMinutes);

            // Time window calculation & waiting time check
            LocalTime twStart = store.getTimeWindowStart();
            LocalTime twEnd = store.getTimeWindowEnd();

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

            stop.setPlannedEta(currentEta);

            results.add(StopEtaResponse.builder()
                    .tripDraftStopId(stop.getId())
                    .sequenceNo(stop.getSequenceNo())
                    .storeCode(store.getCode())
                    .plannedEta(currentEta)
                    .distanceFromPrevKm(legDistanceKm)
                    .travelTimeFromPrevMin(legTravelMinutes)
                    .estimatedDistanceKm(legDistanceKm)
                    .estimatedTravelMin(legTravelMinutes)
                    .build());

        }

        // 8. Set return distance, total distance and polyline on TripDraft
        if (legs.size() > activeStops.size()) {
            GoongDirectionsResponse.Leg returnLeg = legs.get(activeStops.size());
            long returnLegMeters = (returnLeg.getDistance() != null && returnLeg.getDistance().getValue() != null)
                    ? returnLeg.getDistance().getValue() : 0L;
            totalDistanceMeters += returnLegMeters;
            BigDecimal returnKm = BigDecimal.valueOf(returnLegMeters / 1000.0).setScale(2, RoundingMode.HALF_UP);
            draft.setReturnDistanceKm(returnKm);
        }

        BigDecimal totalKm = BigDecimal.valueOf(totalDistanceMeters / 1000.0).setScale(2, RoundingMode.HALF_UP);
        draft.setTotalDistanceKm(totalKm);

        if (fullPolyline != null) {
            draft.setRoutePolyline(fullPolyline);
        }

        draft.setPlannedDepartureTime(departureTime);

        // 9. Persist all
        stopRepo.saveAll(activeStops);
        tripDraftRepo.save(draft);

        log.info("GoongEtaCalculator: Calculated ETA for TripDraft id={}, {} stops, totalDistance={} km",
                tripDraftId, results.size(), totalKm);

        return results;
    }

    private double getConfigDouble(String key) {
        return configRepo.findByConfigKey(key)
                .map(config -> Double.parseDouble(config.getConfigValue()))
                .orElseThrow(() -> {
                    log.error("Missing system config key: {}", key);
                    return new BusinessException(
                            ErrorCode.INTERNAL_ERROR,
                            "Missing system config key: " + key,
                            HttpStatus.INTERNAL_SERVER_ERROR);
                });
    }
}

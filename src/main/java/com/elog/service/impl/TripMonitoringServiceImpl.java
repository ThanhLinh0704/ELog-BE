package com.elog.service.impl;

import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.TripMonitoringService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TripMonitoringServiceImpl implements TripMonitoringService {

    private static final String GPS_NOTE = "GPS tracking not available in this phase";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final Long SYSTEM_USER_ID = 1L;

    private final TripRepository tripRepo;
    private final TripStopRepository tripStopRepo;
    private final UserRepository userRepo;
    private final SystemConfigRepository systemConfigRepo;
    private final DeliveryExceptionRepository deliveryExceptionRepo;
    private final com.elog.service.GoongMapService goongMapService;


    @Override
    public TripStartResponse startTrip(Long tripId, String currentUsername) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND,
                        "Trip " + tripId + " not found", HttpStatus.NOT_FOUND));

        if (!trip.getDriver().getUsername().equals(currentUsername)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_TRIP,
                    "You are not assigned to Trip " + tripId, HttpStatus.FORBIDDEN);
        }

        validateDeliveryDate(trip);

        if (trip.getStatus() != TripStatus.DISPATCHED) {
            throw new BusinessException(ErrorCode.INVALID_TRIP_TRANSITION,
                    "Trip " + tripId + " is not DISPATCHED. Current status: " + trip.getStatus(),
                    HttpStatus.CONFLICT);
        }

        trip.setStatus(TripStatus.IN_PROGRESS);
        trip.setActualDepartureTime(LocalDateTime.now());
        tripRepo.save(trip);
        log.info("Trip {} started by driver {}", tripId, currentUsername);

        List<TripStop> stops = tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(tripId);
        TripStop firstStop = stops.isEmpty() ? null : stops.get(0);

        return TripStartResponse.builder()
                .tripId(trip.getTripId())
                .status("IN_PROGRESS")
                .actualDepartureTime(trip.getActualDepartureTime().format(DT_FMT))
                .firstStopCode(firstStop != null ? getStoreCode(firstStop) : null)
                .firstStopEta(firstStop != null && firstStop.getPlannedEta() != null
                        ? firstStop.getPlannedEta().format(DT_FMT) : null)
                .message(buildStartMessage(firstStop))
                .build();
    }

    @Override
    public StopArriveResponse arriveAtStop(Long tripStopId, String currentUsername) {
        TripStop stop = tripStopRepo.findById(tripStopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_STOP_NOT_FOUND,
                        "TripStop " + tripStopId + " not found", HttpStatus.NOT_FOUND));

        Trip trip = stop.getTrip();

        if (!trip.getDriver().getUsername().equals(currentUsername)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_TRIP,
                    "TripStop " + tripStopId + " does not belong to your trip", HttpStatus.FORBIDDEN);
        }

        validateDeliveryDate(trip);

        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TRIP_COMPLETED,
                    "Trip " + trip.getTripId() + " is already COMPLETED", HttpStatus.CONFLICT);
        }

        if (stop.getStatus() != TripStopStatus.PENDING) {
            if (stop.getStatus() == TripStopStatus.COMPLETED || stop.getStatus() == TripStopStatus.EXCEPTION) {
                throw new BusinessException(ErrorCode.STOP_ALREADY_DONE,
                        "Stop " + tripStopId + " is already " + stop.getStatus(), HttpStatus.CONFLICT);
            }
            throw new BusinessException(ErrorCode.STOP_NOT_PENDING,
                    "Stop " + tripStopId + " is not PENDING. Current: " + stop.getStatus(), HttpStatus.CONFLICT);
        }

        List<TripStop> remainingStops = tripStopRepo.findRemainingStopsOrdered(trip.getTripId());
        if (!remainingStops.isEmpty() && !remainingStops.get(0).getTripStopId().equals(tripStopId)) {
            TripStop expectedNext = remainingStops.get(0);
            throw new BusinessException(ErrorCode.PREVIOUS_STOP_NOT_DONE,
                    "Must complete stop " + getStoreCode(expectedNext) +
                    " (seq " + expectedNext.getSequenceOrder() + ") first", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        stop.setStatus(TripStopStatus.IN_PROGRESS);
        stop.setActualArrivalTime(now);
        tripStopRepo.save(stop);

        long delayMinutes = 0;
        boolean exceptionFlagged = false;
        Long exceptionId = null;

        if (stop.getPlannedEta() != null) {
            delayMinutes = ChronoUnit.MINUTES.between(stop.getPlannedEta(), now);
            if (delayMinutes < 0) delayMinutes = 0;

            int threshold = getEtaThresholdMinutes();
            if (delayMinutes > threshold) {
                exceptionId = createTimeException(stop, delayMinutes, threshold, now);
                exceptionFlagged = true;
                log.info("TIME_EXCEPTION flagged for TripStop {} — delay {} min", tripStopId, delayMinutes);
            }
        }

        String message = buildArriveMessage(stop, delayMinutes, exceptionFlagged, getEtaThresholdMinutes());
        return StopArriveResponse.builder()
                .tripStopId(stop.getTripStopId())
                .storeCode(getStoreCode(stop))
                .status(stop.getStatus().name())
                .actualArrivalTime(now.format(DT_FMT))
                .plannedEta(stop.getPlannedEta() != null ? stop.getPlannedEta().format(DT_FMT) : null)
                .delayMinutes(delayMinutes)
                .timeExceptionFlagged(exceptionFlagged)
                .exceptionId(exceptionId)
                .message(message)
                .build();
    }

    @Override
    public StopCompleteResponse completeStop(Long tripStopId, String currentUsername) {
        TripStop stop = tripStopRepo.findById(tripStopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_STOP_NOT_FOUND,
                        "TripStop " + tripStopId + " not found", HttpStatus.NOT_FOUND));

        Trip trip = stop.getTrip();

        if (!trip.getDriver().getUsername().equals(currentUsername)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_TRIP,
                    "TripStop " + tripStopId + " does not belong to your trip", HttpStatus.FORBIDDEN);
        }

        validateDeliveryDate(trip);

        if (stop.getStatus() != TripStopStatus.IN_PROGRESS) {
            if (stop.getStatus() == TripStopStatus.COMPLETED) {
                throw new BusinessException(ErrorCode.STOP_ALREADY_DONE,
                        "Stop " + tripStopId + " is already COMPLETED", HttpStatus.CONFLICT);
            }
            throw new BusinessException(ErrorCode.STOP_NOT_IN_PROGRESS,
                    "Stop " + tripStopId + " is not IN_PROGRESS. Current: " + stop.getStatus(), HttpStatus.CONFLICT);
        }

        if (trip.getStatus() == TripStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.TRIP_COMPLETED,
                    "Trip " + trip.getTripId() + " is already COMPLETED", HttpStatus.CONFLICT);
        }

        LocalDateTime now = LocalDateTime.now();
        stop.setStatus(TripStopStatus.COMPLETED);
        stop.setActualDepartureTime(now);
        tripStopRepo.save(stop);

        boolean tripCompleted = false;
        if (allStopsDone(trip)) {
            trip.setStatus(TripStatus.COMPLETED);
            trip.setCompletedAt(now);
            tripRepo.save(trip);
            tripCompleted = true;
            log.info("Trip {} auto-completed — all stops done", trip.getTripId());
        }

        StopCompleteResponse.NextStopInfo nextStop = null;
        if (!tripCompleted) {
            List<TripStop> remaining = tripStopRepo.findRemainingStopsOrdered(trip.getTripId());
            if (!remaining.isEmpty()) {
                TripStop next = remaining.get(0);
                nextStop = StopCompleteResponse.NextStopInfo.builder()
                        .tripStopId(next.getTripStopId())
                        .storeCode(getStoreCode(next))
                        .plannedEta(next.getPlannedEta() != null ? next.getPlannedEta().format(DT_FMT) : null)
                        .sequenceOrder(next.getSequenceOrder())
                        .build();
            }
        }

        return StopCompleteResponse.builder()
                .tripStopId(stop.getTripStopId())
                .storeCode(getStoreCode(stop))
                .status("COMPLETED")
                .actualDepartureTime(now.format(DT_FMT))
                .tripCompleted(tripCompleted)
                .tripStatus(tripCompleted ? "COMPLETED" : trip.getStatus().name())
                .nextStop(nextStop)
                .message(tripCompleted
                        ? "All stops completed. Trip " + trip.getTripId() + " is now COMPLETED."
                        : "Stop completed. Proceed to next stop.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public ActiveTripsResponse getActiveTripsDashboard(LocalDate date) {
        List<TripStatus> activeStatuses = List.of(
                TripStatus.DISPATCHED, TripStatus.IN_PROGRESS, TripStatus.COMPLETED);

        List<Trip> trips = tripRepo.findActiveTripsByDate(date, activeStatuses);

        List<Long> allStopIds = trips.stream()
                .flatMap(t -> t.getStops().stream())
                .map(TripStop::getTripStopId)
                .toList();
        Map<Long, List<DeliveryException>> exByStop = allStopIds.isEmpty() ? Map.of()
                : deliveryExceptionRepo.findByTripStopIdInOrderByCreatedAtDesc(allStopIds)
                        .stream().collect(Collectors.groupingBy(DeliveryException::getTripStopId));

        List<ActiveTripsResponse.TripSummary> summaries = trips.stream()
                .map(trip -> buildTripSummary(trip, exByStop))
                .toList();

        return ActiveTripsResponse.builder()
                .date(date.toString())
                .totalActiveTrips((int) trips.stream()
                        .filter(t -> t.getStatus() != TripStatus.COMPLETED)
                        .count())
                .trips(summaries)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TripProgressResponse getTripProgress(Long tripId) {
        Trip trip = tripRepo.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND,
                        "Trip " + tripId + " not found", HttpStatus.NOT_FOUND));

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            boolean isDriver = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> "ROLE_DRIVER".equals(a.getAuthority()) || "trip:execute".equals(a.getAuthority()));

            if (isDriver && currentUsername != null && !"anonymousUser".equals(currentUsername)) {
                if (trip.getDriver() == null || !currentUsername.equals(trip.getDriver().getUsername())) {
                    throw new BusinessException(ErrorCode.NOT_YOUR_TRIP,
                            "Bạn không có quyền xem tiến độ chuyến xe của tài xế khác", HttpStatus.FORBIDDEN);
                }
            }
        }

        List<TripStop> stops = tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(tripId);

        List<Long> stopIds = stops.stream().map(TripStop::getTripStopId).toList();
        Map<Long, List<DeliveryException>> exByStop = stopIds.isEmpty() ? Map.of()
                : deliveryExceptionRepo.findByTripStopIdInOrderByCreatedAtDesc(stopIds)
                        .stream().collect(Collectors.groupingBy(DeliveryException::getTripStopId));

        List<TripProgressResponse.StopProgress> stopProgresses = stops.stream()
                .map(stop -> buildStopProgress(stop, exByStop))
                .toList();

        String routePolyline = trip.getRoutePolyline();
        java.math.BigDecimal totalDistanceKm = trip.getTotalDistanceKm();

        if (!stopProgresses.isEmpty() && goongMapService != null) {
            try {
                double whLat = systemConfigRepo.findByConfigKey("WAREHOUSE_LAT")
                        .map(c -> Double.parseDouble(c.getConfigValue())).orElse(21.028512);
                double whLng = systemConfigRepo.findByConfigKey("WAREHOUSE_LNG")
                        .map(c -> Double.parseDouble(c.getConfigValue())).orElse(105.854211);

                List<TripProgressResponse.StopProgress> validStops = stopProgresses.stream()
                        .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                        .sorted(java.util.Comparator.comparingInt(TripProgressResponse.StopProgress::getSequenceOrder))
                        .toList();

                if (!validStops.isEmpty()) {
                    List<String> pointStrs = new java.util.ArrayList<>();
                    pointStrs.add(whLat + "," + whLng); // Kho
                    for (var stop : validStops) {
                        pointStrs.add(stop.getLatitude() + "," + stop.getLongitude());
                    }

                    List<String> legPolylines = new java.util.ArrayList<>();
                    long totalMeters = 0;

                    for (int i = 0; i < pointStrs.size() - 1; i++) {
                        String p1 = pointStrs.get(i);
                        String p2 = pointStrs.get(i + 1);

                        var resp = goongMapService.getDirections(p1, p2, null);
                        if (resp != null && resp.getRoutes() != null && !resp.getRoutes().isEmpty()) {
                            var route = resp.getRoutes().get(0);
                            if (route.getOverviewPolyline() != null && route.getOverviewPolyline().getPoints() != null) {
                                legPolylines.add(route.getOverviewPolyline().getPoints());
                            }
                            if (route.getLegs() != null) {
                                for (var leg : route.getLegs()) {
                                    if (leg.getDistance() != null && leg.getDistance().getValue() != null) {
                                        totalMeters += leg.getDistance().getValue();
                                    }
                                }
                            }
                        }
                    }

                    if (!legPolylines.isEmpty()) {
                        routePolyline = String.join(";", legPolylines);
                        trip.setRoutePolyline(routePolyline);
                        totalDistanceKm = java.math.BigDecimal.valueOf(totalMeters / 1000.0).setScale(2, java.math.RoundingMode.HALF_UP);
                        trip.setTotalDistanceKm(totalDistanceKm);
                        tripRepo.save(trip);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to calculate Goong leg-by-leg route for trip {}: {}", tripId, e.getMessage());
            }
        }



        Vehicle v = trip.getVehicle();
        User d = trip.getDriver();

        return TripProgressResponse.builder()
                .tripId(trip.getTripId())
                .fixedRouteCode(trip.getRoute().getCode())
                .deliveryDate(trip.getDeliveryDate().toString())
                .status(trip.getStatus().name())
                .totalDistanceKm(totalDistanceKm)
                .routePolyline(routePolyline)
                .vehicle(TripProgressResponse.VehicleInfo.builder()
                        .vehicleCode(v.getPlateNumber())
                        .plateNumber(v.getPlateNumber())
                        .build())
                .driver(TripProgressResponse.DriverInfo.builder()
                        .userId(d.getId())
                        .fullName(d.getFullName())
                        .phone(null)
                        .build())
                .stops(stopProgresses)
                .gpsLocation(null)
                .gpsNote(GPS_NOTE)
                .build();
    }


    private void validateDeliveryDate(Trip trip) {
        if (trip != null && trip.getDeliveryDate() != null && trip.getDeliveryDate().isAfter(LocalDate.now())) {
            String formattedDate = trip.getDeliveryDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Chưa đến ngày giao hàng (ngày giao: " + formattedDate + "). Không thể thực hiện chuyến xe trước ngày giao.",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private String getStoreCode(TripStop stop) {
        return stop.getRouteStop() != null && stop.getRouteStop().getStore() != null
                ? stop.getRouteStop().getStore().getCode()
                : "STOP-" + stop.getTripStopId();
    }

    private String getStoreName(TripStop stop) {
        return stop.getRouteStop() != null && stop.getRouteStop().getStore() != null
                ? stop.getRouteStop().getStore().getName()
                : null;
    }

    private Double getStoreLat(TripStop stop) {
        return stop.getRouteStop() != null && stop.getRouteStop().getStore() != null
                ? stop.getRouteStop().getStore().getLatitude()
                : null;
    }

    private Double getStoreLng(TripStop stop) {
        return stop.getRouteStop() != null && stop.getRouteStop().getStore() != null
                ? stop.getRouteStop().getStore().getLongitude()
                : null;
    }

    private int getEtaThresholdMinutes() {
        return systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES")
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(15);
    }

    /** Idempotent — bỏ qua nếu TIME_EXCEPTION đã tồn tại cho stop này */
    private Long createTimeException(TripStop stop, long delayMinutes, int threshold, LocalDateTime now) {
        if (deliveryExceptionRepo.existsByTripStopIdAndExceptionType(
                stop.getTripStopId(), ExceptionType.TIME_EXCEPTION)) {
            log.debug("TIME_EXCEPTION already exists for TripStop {} — skip", stop.getTripStopId());
            return null;
        }

        DeliveryException ex = DeliveryException.builder()
                .tripStopId(stop.getTripStopId())
                .exceptionType(ExceptionType.TIME_EXCEPTION)
                .reportedBy(SYSTEM_USER_ID)
                .description(String.format(
                        "Stop %s is %d minutes past planned ETA (%s). Threshold: %d min.",
                        getStoreCode(stop), delayMinutes, stop.getPlannedEta(), threshold))
                .build();
        DeliveryException saved = deliveryExceptionRepo.save(ex);

        stop.setStatus(TripStopStatus.EXCEPTION);
        tripStopRepo.save(stop);

        log.info("TIME_EXCEPTION created (id={}) for TripStop {} — {} min late",
                saved.getExceptionId(), stop.getTripStopId(), delayMinutes);
        return saved.getExceptionId();
    }

    private boolean allStopsDone(Trip trip) {
        return trip.getStops().stream()
                .allMatch(s -> s.getStatus() == TripStopStatus.COMPLETED
                            || s.getStatus() == TripStopStatus.EXCEPTION);
    }

    private ActiveTripsResponse.TripSummary buildTripSummary(
            Trip trip, Map<Long, List<DeliveryException>> exByStop) {
        List<TripStop> stops = trip.getStops();
        int total     = stops.size();
        int completed = (int) stops.stream().filter(s -> s.getStatus() == TripStopStatus.COMPLETED).count();
        int pending   = (int) stops.stream().filter(s -> s.getStatus() == TripStopStatus.PENDING).count();
        int exception = (int) stops.stream().filter(s -> s.getStatus() == TripStopStatus.EXCEPTION).count();
        int progress  = total == 0 ? 0 : (completed * 100 / total);

        List<ActiveTripsResponse.ExceptionSummary> exSummaries = stops.stream()
                .flatMap(stop -> exByStop.getOrDefault(stop.getTripStopId(), List.of()).stream()
                        .map(ex -> ActiveTripsResponse.ExceptionSummary.builder()
                                .exceptionId(ex.getExceptionId())
                                .type(ex.getExceptionType().name())
                                .storeCode(getStoreCode(stop))
                                .description(ex.getDescription())
                                .resolvedAt(ex.getResolvedAt() != null ? ex.getResolvedAt().format(DT_FMT) : null)
                                .build()))
                .toList();

        return ActiveTripsResponse.TripSummary.builder()
                .tripId(trip.getTripId())
                .fixedRouteCode(trip.getRoute().getCode())
                .vehicleCode(trip.getVehicle().getPlateNumber())
                .driverName(trip.getDriver().getFullName())
                .status(trip.getStatus().name())
                .plannedDepartureTime(trip.getPlannedDepartureTime() != null
                        ? trip.getPlannedDepartureTime().toString() : null)
                .actualDepartureTime(trip.getActualDepartureTime() != null
                        ? trip.getActualDepartureTime().format(DT_FMT) : null)
                .totalStops(total)
                .completedStops(completed)
                .pendingStops(pending)
                .exceptionStops(exception)
                .progressPercent(progress)
                .hasUnresolvedExceptions(exSummaries.stream().anyMatch(e -> e.getResolvedAt() == null))
                .exceptions(exSummaries)
                .gpsLocation(null)
                .gpsNote(GPS_NOTE)
                .build();
    }

    private TripProgressResponse.StopProgress buildStopProgress(
            TripStop stop, Map<Long, List<DeliveryException>> exByStop) {
        long delayMin = 0;
        if (stop.getActualArrivalTime() != null && stop.getPlannedEta() != null) {
            delayMin = ChronoUnit.MINUTES.between(stop.getPlannedEta(), stop.getActualArrivalTime());
            if (delayMin < 0) delayMin = 0;
        }

        List<TripProgressResponse.ExceptionDetail> exDetails =
                exByStop.getOrDefault(stop.getTripStopId(), List.of()).stream()
                        .map(ex -> TripProgressResponse.ExceptionDetail.builder()
                                .exceptionId(ex.getExceptionId())
                                .type(ex.getExceptionType().name())
                                .description(ex.getDescription())
                                .createdAt(ex.getCreatedAt().format(DT_FMT))
                                .resolvedAt(ex.getResolvedAt() != null ? ex.getResolvedAt().format(DT_FMT) : null)
                                .build())
                        .toList();

        return TripProgressResponse.StopProgress.builder()
                .tripStopId(stop.getTripStopId())
                .sequenceOrder(stop.getSequenceOrder())
                .storeCode(getStoreCode(stop))
                .storeName(getStoreName(stop))
                .status(stop.getStatus().name())
                .plannedEta(stop.getPlannedEta() != null ? stop.getPlannedEta().format(DT_FMT) : null)
                .actualArrivalTime(stop.getActualArrivalTime() != null
                        ? stop.getActualArrivalTime().format(DT_FMT) : null)
                .actualDepartureTime(stop.getActualDepartureTime() != null
                        ? stop.getActualDepartureTime().format(DT_FMT) : null)
                .delayMinutes(stop.getActualArrivalTime() != null ? delayMin : null)
                .hasException(stop.getStatus() == TripStopStatus.EXCEPTION)
                .exceptions(exDetails)
                .latitude(getStoreLat(stop))
                .longitude(getStoreLng(stop))
                .build();
    }

    private String buildStartMessage(TripStop firstStop) {
        if (firstStop == null) return "Trip started.";
        String eta = firstStop.getPlannedEta() != null
                ? " (ETA " + firstStop.getPlannedEta().toLocalTime() + ")" : "";
        return "Trip started. Proceed to first stop: " + getStoreCode(firstStop) + eta + ".";
    }

    private String buildArriveMessage(TripStop stop, long delayMin, boolean excepted, int threshold) {
        if (stop.getPlannedEta() == null) return "Arrived at " + getStoreCode(stop) + ".";
        if (excepted) {
            return String.format("⚠️ TIME EXCEPTION flagged: %d minutes behind ETA.", delayMin);
        }
        if (delayMin > 0) {
            return String.format("Arrived at %s. %d minutes behind ETA (threshold: %d min).",
                    getStoreCode(stop), delayMin, threshold);
        }
        return "Arrived at " + getStoreCode(stop) + " on time.";
    }
}

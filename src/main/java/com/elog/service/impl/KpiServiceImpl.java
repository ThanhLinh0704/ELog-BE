package com.elog.service.impl;

import com.elog.dto.response.kpi.KpiByDriverResponse.DriverKpi;
import com.elog.dto.response.kpi.KpiByRouteResponse.RouteKpi;
import com.elog.dto.response.kpi.KpiByVehicleResponse.VehicleKpi;
import com.elog.dto.response.kpi.KpiDailyTrendResponse.DailyDataPoint;
import com.elog.dto.response.kpi.KpiSummaryResponse.ExceptionKpi;
import com.elog.dto.response.kpi.KpiSummaryResponse.FleetUtilizationKpi;
import com.elog.dto.response.kpi.KpiSummaryResponse.OnTimeDeliveryKpi;
import com.elog.dto.response.kpi.KpiSummaryResponse.PeriodInfo;
import com.elog.dto.response.kpi.KpiSummaryResponse.TripCompletionKpi;
import com.elog.dto.response.kpi.KpiByDriverResponse;
import com.elog.dto.response.kpi.KpiByRouteResponse;
import com.elog.dto.response.kpi.KpiByVehicleResponse;
import com.elog.dto.response.kpi.KpiDailyTrendResponse;
import com.elog.dto.response.kpi.KpiSummaryResponse;
import com.elog.entity.*;
import com.elog.repository.*;
import com.elog.service.KpiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class KpiServiceImpl implements KpiService {

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final DeliveryExceptionRepository deliveryExceptionRepository;
    private final SystemConfigRepository systemConfigRepository;

    private static final String ETA_THRESHOLD_KEY = "ETA_THRESHOLD_MINUTES";
    private static final int DEFAULT_ETA_THRESHOLD = 15;

    // ── Public API ────────────────────────────────────────────────────

    @Override
    public KpiSummaryResponse calculate(LocalDate startDate, LocalDate endDate) {
        int threshold = getEtaThreshold();

        // K1 + K5: stop-level metrics
        List<TripStop> processedStops = tripStopRepository.findProcessedStopsInDateRange(startDate, endDate);
        List<DeliveryException> exceptions = deliveryExceptionRepository.findExceptionsInDateRange(startDate, endDate);
        long unresolvedCount = deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(startDate, endDate);

        OnTimeDeliveryKpi onTimeKpi = calculateOnTimeDelivery(processedStops, threshold);
        ExceptionKpi exceptionKpi = calculateExceptionMetrics(processedStops, exceptions, (int) unresolvedCount);

        // K2 + K3 + K4: trip-level metrics
        List<Trip> trips = tripRepository.findTripsWithVehicleInDateRange(startDate, endDate);
        FleetUtilizationKpi fleetKpi = calculateFleetUtilization(trips);
        TripCompletionKpi completionKpi = calculateTripCompletion(trips);

        double totalFleetKmSum = trips.stream()
                .map(Trip::getTotalDistanceKm)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .sum();
        Double totalFleetKm = trips.isEmpty() ? null : round(totalFleetKmSum);

        return KpiSummaryResponse.builder()
                .period(PeriodInfo.builder().startDate(startDate).endDate(endDate).build())
                .generatedAt(LocalDateTime.now())
                .onTimeDelivery(onTimeKpi)
                .fleetUtilization(fleetKpi)
                .tripCompletion(completionKpi)
                .exceptions(exceptionKpi)
                .totalFleetDistanceKm(totalFleetKm)
                .build();
    }

    @Override
    public KpiDailyTrendResponse getDailyTrend(LocalDate startDate, LocalDate endDate) {
        int threshold = getEtaThreshold();

        List<TripStop> processedStops = tripStopRepository.findProcessedStopsInDateRange(startDate, endDate);
        List<Trip> trips = tripRepository.findTripsWithVehicleInDateRange(startDate, endDate);

        // Group stops by delivery date
        Map<LocalDate, List<TripStop>> stopsByDate = processedStops.stream()
                .collect(Collectors.groupingBy(ts -> ts.getTrip().getDeliveryDate()));

        // Group trips by delivery date (for trip count & volume util)
        Map<LocalDate, List<Trip>> tripsByDate = trips.stream()
                .collect(Collectors.groupingBy(Trip::getDeliveryDate));

        // Build data points for each date in range (fill gaps)
        List<DailyDataPoint> dataPoints = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            List<Trip> dayTrips = tripsByDate.getOrDefault(date, Collections.emptyList());
            List<TripStop> dayStops = stopsByDate.getOrDefault(date, Collections.emptyList());

            int tripCount = dayTrips.isEmpty() ? 0 : (int) dayTrips.stream()
                    .map(Trip::getTripId)
                    .distinct()
                    .count();

            Double onTimeRate = null;
            Double volumeUtil = null;

            if (!dayStops.isEmpty()) {
                int onTime = countOnTimeStops(dayStops, threshold);
                onTimeRate = round((double) onTime / dayStops.size() * 100);
            }

            if (!dayTrips.isEmpty()) {
                volumeUtil = computeAvgVolumeUtil(dayTrips);
            }

            dataPoints.add(DailyDataPoint.builder()
                    .date(date)
                    .tripCount(tripCount)
                    .onTimeRatePct(onTimeRate)
                    .volumeUtilPct(volumeUtil)
                    .build());
        }

        return KpiDailyTrendResponse.builder()
                .period(PeriodInfo.builder().startDate(startDate).endDate(endDate).build())
                .data(dataPoints)
                .build();
    }

    @Override
    public KpiByRouteResponse getByRoute(LocalDate startDate, LocalDate endDate) {
        int threshold = getEtaThreshold();

        List<Trip> trips = tripRepository.findTripsWithVehicleAndRouteInDateRange(startDate, endDate);
        List<TripStop> processedStops = tripStopRepository.findProcessedStopsInDateRange(startDate, endDate);
        List<DeliveryException> exceptions = deliveryExceptionRepository.findExceptionsInDateRange(startDate, endDate);

        // Index exceptions by tripStopId
        Map<Long, List<DeliveryException>> exceptionsByStopId = exceptions.stream()
                .filter(de -> de.getTripStopId() != null)
                .collect(Collectors.groupingBy(DeliveryException::getTripStopId));

        // Group trips by route
        Map<Long, List<Trip>> tripsByRoute = trips.stream()
                .collect(Collectors.groupingBy(t -> t.getRoute().getId()));

        // Group stops by route (via trip)
        Map<Long, List<TripStop>> stopsByRoute = processedStops.stream()
                .collect(Collectors.groupingBy(ts -> ts.getTrip().getRoute().getId()));

        List<KpiByRouteResponse.RouteKpi> routeKpis = new ArrayList<>();
        for (Map.Entry<Long, List<Trip>> entry : tripsByRoute.entrySet()) {
            Long routeId = entry.getKey();
            List<Trip> routeTrips = entry.getValue();
            Route route = routeTrips.get(0).getRoute();

            List<TripStop> routeStops = stopsByRoute.getOrDefault(routeId, Collections.emptyList());

            int onTime = countOnTimeStops(routeStops, threshold);
            Double onTimeRate = routeStops.isEmpty() ? null
                    : round((double) onTime / routeStops.size() * 100);
            Double volumeUtil = computeAvgVolumeUtil(routeTrips);

            // Count exceptions for this route's stops
            Set<Long> routeStopIds = routeStops.stream()
                    .map(TripStop::getTripStopId)
                    .collect(Collectors.toSet());
            int totalExceptions = 0;
            int totalRejections = 0;
            for (Long stopId : routeStopIds) {
                List<DeliveryException> stopExceptions = exceptionsByStopId.getOrDefault(stopId, Collections.emptyList());
                if (!stopExceptions.isEmpty()) {
                    totalExceptions += stopExceptions.size();
                    totalRejections += (int) stopExceptions.stream()
                            .filter(de -> de.getExceptionType() == ExceptionType.DELIVERY_REJECTION)
                            .count();
                }
            }

            routeKpis.add(KpiByRouteResponse.RouteKpi.builder()
                    .routeCode(route.getCode())
                    .routeName(route.getName())
                    .totalTrips(routeTrips.size())
                    .onTimeRatePct(onTimeRate)
                    .avgVolumeUtilPct(volumeUtil)
                    .totalExceptions(totalExceptions)
                    .totalRejections(totalRejections)
                    .build());
        }

        // Sort by on-time rate ASC (worst routes first), null at end
        routeKpis.sort(Comparator.comparing(KpiByRouteResponse.RouteKpi::getOnTimeRatePct,
                Comparator.nullsLast(Comparator.naturalOrder())));

        return KpiByRouteResponse.builder()
                .period(PeriodInfo.builder().startDate(startDate).endDate(endDate).build())
                .routes(routeKpis)
                .build();
    }

    @Override
    public KpiByVehicleResponse getByVehicle(LocalDate startDate, LocalDate endDate) {
        int threshold = getEtaThreshold();

        List<Trip> trips = tripRepository.findTripsWithVehicleAndRouteInDateRange(startDate, endDate);
        List<TripStop> processedStops = tripStopRepository.findProcessedStopsInDateRange(startDate, endDate);
        List<DeliveryException> exceptions = deliveryExceptionRepository.findExceptionsInDateRange(startDate, endDate);

        Map<Long, List<DeliveryException>> exceptionsByStopId = exceptions.stream()
                .filter(de -> de.getTripStopId() != null)
                .collect(Collectors.groupingBy(DeliveryException::getTripStopId));

        Map<Long, List<Trip>> tripsByVehicle = trips.stream()
                .filter(t -> t.getVehicle() != null)
                .collect(Collectors.groupingBy(t -> t.getVehicle().getId()));

        Map<Long, List<TripStop>> stopsByVehicle = processedStops.stream()
                .filter(ts -> ts.getTrip() != null && ts.getTrip().getVehicle() != null)
                .collect(Collectors.groupingBy(ts -> ts.getTrip().getVehicle().getId()));

        List<KpiByVehicleResponse.VehicleKpi> vehicleKpis = new ArrayList<>();
        for (Map.Entry<Long, List<Trip>> entry : tripsByVehicle.entrySet()) {
            Long vehicleId = entry.getKey();
            List<Trip> vehicleTrips = entry.getValue();
            Vehicle vehicle = vehicleTrips.get(0).getVehicle();

            List<TripStop> vehicleStops = stopsByVehicle.getOrDefault(vehicleId, Collections.emptyList());

            int onTime = countOnTimeStops(vehicleStops, threshold);
            Double onTimeRate = vehicleStops.isEmpty() ? null
                    : round((double) onTime / vehicleStops.size() * 100);

            Double volumeUtil = computeAvgVolumeUtil(vehicleTrips);
            Double weightUtil = computeAvgWeightUtil(vehicleTrips);

            double totalDistance = vehicleTrips.stream()
                    .map(Trip::getTotalDistanceKm)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            Set<Long> vehicleStopIds = vehicleStops.stream()
                    .map(TripStop::getTripStopId)
                    .collect(Collectors.toSet());

            int totalExceptions = 0;
            for (Long stopId : vehicleStopIds) {
                totalExceptions += exceptionsByStopId.getOrDefault(stopId, Collections.emptyList()).size();
            }

            vehicleKpis.add(KpiByVehicleResponse.VehicleKpi.builder()
                    .vehicleId(vehicle.getId())
                    .licensePlate(vehicle.getPlateNumber())
                    .vehicleType(vehicle.getVehicleType())
                    .payloadKg(vehicle.getPayloadKg())
                    .maxVolumeM3(vehicle.getMaxVolumeM3())
                    .totalTrips(vehicleTrips.size())
                    .totalDistanceKm(round(totalDistance))
                    .avgVolumeUtilPct(volumeUtil)
                    .avgWeightUtilPct(weightUtil)
                    .onTimeRatePct(onTimeRate)
                    .totalExceptions(totalExceptions)
                    .build());
        }

        vehicleKpis.sort(Comparator.comparing(KpiByVehicleResponse.VehicleKpi::getTotalTrips, Comparator.reverseOrder()));

        return KpiByVehicleResponse.builder()
                .period(PeriodInfo.builder().startDate(startDate).endDate(endDate).build())
                .vehicles(vehicleKpis)
                .build();
    }

    @Override
    public KpiByDriverResponse getByDriver(LocalDate startDate, LocalDate endDate) {
        int threshold = getEtaThreshold();

        List<Trip> trips = tripRepository.findTripsWithVehicleAndRouteInDateRange(startDate, endDate);
        List<TripStop> processedStops = tripStopRepository.findProcessedStopsInDateRange(startDate, endDate);
        List<DeliveryException> exceptions = deliveryExceptionRepository.findExceptionsInDateRange(startDate, endDate);

        Map<Long, List<DeliveryException>> exceptionsByStopId = exceptions.stream()
                .filter(de -> de.getTripStopId() != null)
                .collect(Collectors.groupingBy(DeliveryException::getTripStopId));

        Map<Long, List<Trip>> tripsByDriver = trips.stream()
                .filter(t -> t.getDriver() != null)
                .collect(Collectors.groupingBy(t -> t.getDriver().getId()));

        Map<Long, List<TripStop>> stopsByDriver = processedStops.stream()
                .filter(ts -> ts.getTrip() != null && ts.getTrip().getDriver() != null)
                .collect(Collectors.groupingBy(ts -> ts.getTrip().getDriver().getId()));

        List<KpiByDriverResponse.DriverKpi> driverKpis = new ArrayList<>();
        for (Map.Entry<Long, List<Trip>> entry : tripsByDriver.entrySet()) {
            Long driverId = entry.getKey();
            List<Trip> driverTrips = entry.getValue();
            User driver = driverTrips.get(0).getDriver();

            List<TripStop> driverStops = stopsByDriver.getOrDefault(driverId, Collections.emptyList());

            int onTime = countOnTimeStops(driverStops, threshold);
            Double onTimeRate = driverStops.isEmpty() ? null
                    : round((double) onTime / driverStops.size() * 100);

            double totalDistance = driverTrips.stream()
                    .map(Trip::getTotalDistanceKm)
                    .filter(Objects::nonNull)
                    .mapToDouble(BigDecimal::doubleValue)
                    .sum();

            Set<Long> driverStopIds = driverStops.stream()
                    .map(TripStop::getTripStopId)
                    .collect(Collectors.toSet());

            int totalExceptions = 0;
            for (Long stopId : driverStopIds) {
                totalExceptions += exceptionsByStopId.getOrDefault(stopId, Collections.emptyList()).size();
            }

            driverKpis.add(KpiByDriverResponse.DriverKpi.builder()
                    .driverId(driver.getId())
                    .driverCode("DRV-" + driver.getId())
                    .fullName(driver.getFullName() != null ? driver.getFullName() : driver.getUsername())
                    .phoneNumber(driver.getPhoneNumber())
                    .totalTrips(driverTrips.size())
                    .totalDistanceKm(round(totalDistance))
                    .onTimeRatePct(onTimeRate)
                    .totalExceptions(totalExceptions)
                    .build());
        }

        driverKpis.sort(Comparator.comparing(KpiByDriverResponse.DriverKpi::getTotalTrips, Comparator.reverseOrder()));

        return KpiByDriverResponse.builder()
                .period(PeriodInfo.builder().startDate(startDate).endDate(endDate).build())
                .drivers(driverKpis)
                .build();
    }

    // ── Private calculation helpers ───────────────────────────────────

    private OnTimeDeliveryKpi calculateOnTimeDelivery(List<TripStop> processedStops, int threshold) {
        if (processedStops.isEmpty()) {
            return OnTimeDeliveryKpi.builder()
                    .rate(null)
                    .onTimeStops(0)
                    .totalProcessedStops(0)
                    .build();
        }

        int onTime = countOnTimeStops(processedStops, threshold);
        double rate = round((double) onTime / processedStops.size() * 100);

        return OnTimeDeliveryKpi.builder()
                .rate(rate)
                .onTimeStops(onTime)
                .totalProcessedStops(processedStops.size())
                .build();
    }

    private FleetUtilizationKpi calculateFleetUtilization(List<Trip> trips) {
        if (trips.isEmpty()) {
            return FleetUtilizationKpi.builder()
                    .avgVolumeUtilizationPct(null)
                    .avgWeightUtilizationPct(null)
                    .build();
        }

        return FleetUtilizationKpi.builder()
                .avgVolumeUtilizationPct(computeAvgVolumeUtil(trips))
                .avgWeightUtilizationPct(computeAvgWeightUtil(trips))
                .build();
    }

    private TripCompletionKpi calculateTripCompletion(List<Trip> trips) {
        if (trips.isEmpty()) {
            return TripCompletionKpi.builder()
                    .rate(null)
                    .completedTrips(0)
                    .totalTrips(0)
                    .build();
        }

        int completed = (int) trips.stream()
                .filter(t -> t.getStatus() == TripStatus.COMPLETED)
                .count();
        double rate = round((double) completed / trips.size() * 100);

        return TripCompletionKpi.builder()
                .rate(rate)
                .completedTrips(completed)
                .totalTrips(trips.size())
                .build();
    }

    private ExceptionKpi calculateExceptionMetrics(List<TripStop> processedStops,
                                                    List<DeliveryException> exceptions,
                                                    int unresolvedCount) {
        if (processedStops.isEmpty()) {
            return ExceptionKpi.builder()
                    .exceptionRate(null)
                    .totalExceptions(0)
                    .totalTimeExceptions(0)
                    .totalRejections(0)
                    .unresolvedCount(unresolvedCount)
                    .build();
        }

        // Group exceptions by stopId to count distinct stops with exceptions
        Map<Long, List<DeliveryException>> byStopId = exceptions.stream()
                .filter(de -> de.getTripStopId() != null)
                .collect(Collectors.groupingBy(DeliveryException::getTripStopId));

        // Only count stops that are in the processed set
        Set<Long> processedStopIds = processedStops.stream()
                .map(TripStop::getTripStopId)
                .collect(Collectors.toSet());

        long stopsWithException = byStopId.keySet().stream()
                .filter(processedStopIds::contains)
                .count();

        int totalTimeExceptions = (int) exceptions.stream()
                .filter(de -> de.getExceptionType() == ExceptionType.TIME_EXCEPTION)
                .count();
        int totalRejections = (int) exceptions.stream()
                .filter(de -> de.getExceptionType() == ExceptionType.DELIVERY_REJECTION)
                .count();

        double exceptionRate = round((double) stopsWithException / processedStops.size() * 100);

        return ExceptionKpi.builder()
                .exceptionRate(exceptionRate)
                .totalExceptions(exceptions.size())
                .totalTimeExceptions(totalTimeExceptions)
                .totalRejections(totalRejections)
                .unresolvedCount(unresolvedCount)
                .build();
    }

    // ── Utility methods ──────────────────────────────────────────────

    private int countOnTimeStops(List<TripStop> stops, int thresholdMinutes) {
        int count = 0;
        for (TripStop ts : stops) {
            // actualArrivalTime <= plannedEta + threshold → on-time
            if (!ts.getActualArrivalTime().isAfter(ts.getPlannedEta().plusMinutes(thresholdMinutes))) {
                count++;
            }
        }
        return count;
    }

    private Double computeAvgVolumeUtil(List<Trip> trips) {
        BigDecimal sum = BigDecimal.ZERO;
        int validCount = 0;
        for (Trip t : trips) {
            Vehicle v = t.getVehicle();
            if (v.getMaxVolumeM3() != null && v.getMaxVolumeM3().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal util = t.getTotalVolumeM3()
                        .divide(v.getMaxVolumeM3(), 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                sum = sum.add(util);
                validCount++;
            }
        }
        return validCount == 0 ? null : round(sum.doubleValue() / validCount);
    }

    private Double computeAvgWeightUtil(List<Trip> trips) {
        BigDecimal sum = BigDecimal.ZERO;
        int validCount = 0;
        for (Trip t : trips) {
            Vehicle v = t.getVehicle();
            if (v.getPayloadKg() != null && v.getPayloadKg().compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal util = t.getTotalWeightKg()
                        .divide(v.getPayloadKg(), 6, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("100"));
                sum = sum.add(util);
                validCount++;
            }
        }
        return validCount == 0 ? null : round(sum.doubleValue() / validCount);
    }

    private int getEtaThreshold() {
        return systemConfigRepository.findByConfigKey(ETA_THRESHOLD_KEY)
                .map(sc -> {
                    try {
                        return Integer.parseInt(sc.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid ETA_THRESHOLD_MINUTES config value '{}', using default {}",
                                sc.getConfigValue(), DEFAULT_ETA_THRESHOLD);
                        return DEFAULT_ETA_THRESHOLD;
                    }
                })
                .orElse(DEFAULT_ETA_THRESHOLD);
    }

    private double round(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

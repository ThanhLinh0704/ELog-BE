package com.elog.service;

import com.elog.dto.response.*;
import com.elog.dto.response.KpiDailyTrendResponse.DailyDataPoint;
import com.elog.entity.*;
import com.elog.repository.*;
import com.elog.service.impl.KpiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiServiceImplTest {

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripStopRepository tripStopRepository;

    @Mock
    private DeliveryExceptionRepository deliveryExceptionRepository;

    @Mock
    private SystemConfigRepository systemConfigRepository;

    @InjectMocks
    private KpiServiceImpl kpiService;

    private static final LocalDate START = LocalDate.of(2026, 8, 1);
    private static final LocalDate END = LocalDate.of(2026, 8, 7);
    private static final int THRESHOLD = 15;

    @BeforeEach
    void setupThreshold() {
        SystemConfig config = new SystemConfig();
        config.setConfigKey("ETA_THRESHOLD_MINUTES");
        config.setConfigValue("15");
        when(systemConfigRepository.findByConfigKey("ETA_THRESHOLD_MINUTES"))
                .thenReturn(Optional.of(config));
    }

    // ── Helper builders ──────────────────────────────────────────────

    private Vehicle buildVehicle(double maxVolume, double payloadKg) {
        return Vehicle.builder()
                .id(1L)
                .maxVolumeM3(BigDecimal.valueOf(maxVolume))
                .payloadKg(BigDecimal.valueOf(payloadKg))
                .build();
    }

    private Route buildRoute(Long id, String code, String name) {
        return Route.builder().id(id).code(code).name(name).build();
    }

    private Trip buildTrip(Long id, LocalDate date, TripStatus status,
                           double volumeM3, double weightKg, Vehicle vehicle, Route route) {
        return Trip.builder()
                .tripId(id)
                .deliveryDate(date)
                .status(status)
                .totalVolumeM3(BigDecimal.valueOf(volumeM3))
                .totalWeightKg(BigDecimal.valueOf(weightKg))
                .vehicle(vehicle)
                .route(route)
                .build();
    }

    private TripStop buildStop(Long id, Trip trip, TripStopStatus status,
                                LocalDateTime eta, LocalDateTime actual) {
        return TripStop.builder()
                .tripStopId(id)
                .trip(trip)
                .status(status)
                .plannedEta(eta)
                .actualArrivalTime(actual)
                .build();
    }

    private DeliveryException buildException(Long stopId, ExceptionType type, boolean resolved) {
        return DeliveryException.builder()
                .exceptionId(stopId * 100)
                .tripStopId(stopId)
                .exceptionType(type)
                .reportedBy(1L)
                .resolvedAt(resolved ? LocalDateTime.now() : null)
                .build();
    }

    // ── Tests ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("calculate() — KPI Summary")
    class CalculateTests {

        @Test
        @DisplayName("TC-01: K1 On-Time Rate — 60/70 = 85.7%")
        void onTimeRate_correctCalculation() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            // 60 on-time + 10 late = 70 total processed stops
            List<TripStop> stops = new ArrayList<>();
            LocalDateTime eta = LocalDateTime.of(2026, 8, 1, 8, 0);
            for (int i = 0; i < 60; i++) {
                stops.add(buildStop((long) (i + 1), trip, TripStopStatus.COMPLETED,
                        eta, eta.plusMinutes(10))); // within 15min → on-time
            }
            for (int i = 60; i < 70; i++) {
                stops.add(buildStop((long) (i + 1), trip, TripStopStatus.COMPLETED,
                        eta, eta.plusMinutes(20))); // 20 > 15 → late
            }

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(stops);
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getOnTimeDelivery().getRate()).isEqualTo(85.7);
            assertThat(result.getOnTimeDelivery().getOnTimeStops()).isEqualTo(60);
            assertThat(result.getOnTimeDelivery().getTotalProcessedStops()).isEqualTo(70);
        }

        @Test
        @DisplayName("TC-02: K1 boundary — arrival exactly at ETA + threshold → on-time")
        void onTimeRate_exactBoundary_isOnTime() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            LocalDateTime eta = LocalDateTime.of(2026, 8, 1, 8, 0);
            // Exactly at threshold: 8:15 == eta + 15min → on-time (≤)
            TripStop stop = buildStop(1L, trip, TripStopStatus.COMPLETED,
                    eta, eta.plusMinutes(THRESHOLD));

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(List.of(stop));
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getOnTimeDelivery().getRate()).isEqualTo(100.0);
            assertThat(result.getOnTimeDelivery().getOnTimeStops()).isEqualTo(1);
        }

        @Test
        @DisplayName("TC-03: K1 boundary — arrival at ETA + threshold + 1min → late")
        void onTimeRate_oneMinuteOver_isLate() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            LocalDateTime eta = LocalDateTime.of(2026, 8, 1, 8, 0);
            TripStop stop = buildStop(1L, trip, TripStopStatus.COMPLETED,
                    eta, eta.plusMinutes(THRESHOLD + 1));

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(List.of(stop));
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getOnTimeDelivery().getRate()).isEqualTo(0.0);
            assertThat(result.getOnTimeDelivery().getOnTimeStops()).isEqualTo(0);
        }

        @Test
        @DisplayName("TC-04: K2 Volume Utilization — 4.0/6.0 = 66.7%")
        void volumeUtilization_correct() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            // 4.0 / 6.0 * 100 = 66.666... → rounded to 66.7
            assertThat(result.getFleetUtilization().getAvgVolumeUtilizationPct()).isEqualTo(66.7);
        }

        @Test
        @DisplayName("TC-05: K3 Weight Utilization — 600/800 = 75.0%")
        void weightUtilization_correct() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getFleetUtilization().getAvgWeightUtilizationPct()).isEqualTo(75.0);
        }

        @Test
        @DisplayName("TC-06: K4 Trip Completion Rate — 3/4 = 75.0%")
        void tripCompletionRate_correct() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");

            List<Trip> trips = List.of(
                    buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r),
                    buildTrip(2L, START, TripStatus.COMPLETED, 4.0, 600, v, r),
                    buildTrip(3L, START, TripStatus.COMPLETED, 4.0, 600, v, r),
                    buildTrip(4L, START, TripStatus.IN_PROGRESS, 4.0, 600, v, r)
            );

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(trips);

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getTripCompletion().getRate()).isEqualTo(75.0);
            assertThat(result.getTripCompletion().getCompletedTrips()).isEqualTo(3);
            assertThat(result.getTripCompletion().getTotalTrips()).isEqualTo(4);
        }

        @Test
        @DisplayName("TC-07: K5 Exception Rate — 2 stops with exceptions / 10 total = 20.0%")
        void exceptionRate_correct() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            LocalDateTime eta = LocalDateTime.of(2026, 8, 1, 8, 0);
            List<TripStop> stops = new ArrayList<>();
            for (int i = 1; i <= 10; i++) {
                stops.add(buildStop((long) i, trip, TripStopStatus.COMPLETED, eta, eta.plusMinutes(5)));
            }

            // 2 stops have exceptions: stop 1 (TIME_EXCEPTION) and stop 2 (DELIVERY_REJECTION)
            List<DeliveryException> exceptions = List.of(
                    buildException(1L, ExceptionType.TIME_EXCEPTION, false),
                    buildException(2L, ExceptionType.DELIVERY_REJECTION, false)
            );

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(stops);
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(exceptions);
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(2L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getExceptions().getExceptionRate()).isEqualTo(20.0);
            assertThat(result.getExceptions().getTotalExceptions()).isEqualTo(2);
            assertThat(result.getExceptions().getTotalTimeExceptions()).isEqualTo(1);
            assertThat(result.getExceptions().getTotalRejections()).isEqualTo(1);
            assertThat(result.getExceptions().getUnresolvedCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("TC-08: No trips in period — all rates null, counts 0")
        void noTrips_allNull() {
            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(Collections.emptyList());

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getOnTimeDelivery().getRate()).isNull();
            assertThat(result.getOnTimeDelivery().getTotalProcessedStops()).isZero();
            assertThat(result.getFleetUtilization().getAvgVolumeUtilizationPct()).isNull();
            assertThat(result.getFleetUtilization().getAvgWeightUtilizationPct()).isNull();
            assertThat(result.getTripCompletion().getRate()).isNull();
            assertThat(result.getTripCompletion().getTotalTrips()).isZero();
            assertThat(result.getExceptions().getExceptionRate()).isNull();
        }

        @Test
        @DisplayName("TC-10: Vehicle with payloadKg = 0 — K3 skip, no ArithmeticException")
        void vehicleZeroPayload_noException() {
            Vehicle v = buildVehicle(6.0, 0.0); // payloadKg = 0
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            // Volume should still work (6.0 > 0), weight should be null (skipped)
            assertThat(result.getFleetUtilization().getAvgVolumeUtilizationPct()).isNotNull();
            assertThat(result.getFleetUtilization().getAvgWeightUtilizationPct()).isNull();
        }

        @Test
        @DisplayName("TC-11: Vehicle with maxVolumeM3 = 0 — K2 skip, no ArithmeticException")
        void vehicleZeroVolume_noException() {
            Vehicle v = buildVehicle(0.0, 800); // maxVolumeM3 = 0
            Route r = buildRoute(1L, "RT-01", "Route 1");
            Trip trip = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r);

            when(tripStopRepository.findProcessedStopsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END)).thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.countUnresolvedExceptionsInDateRange(START, END)).thenReturn(0L);
            when(tripRepository.findTripsWithVehicleInDateRange(START, END)).thenReturn(List.of(trip));

            KpiSummaryResponse result = kpiService.calculate(START, END);

            assertThat(result.getFleetUtilization().getAvgVolumeUtilizationPct()).isNull();
            assertThat(result.getFleetUtilization().getAvgWeightUtilizationPct()).isNotNull();
        }
    }

    @Nested
    @DisplayName("getDailyTrend()")
    class DailyTrendTests {

        @Test
        @DisplayName("TC-09: 7-day range with gaps — all dates present, empty days have tripCount=0")
        void dailyTrend_fillsGaps() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r = buildRoute(1L, "RT-01", "Route 1");

            // Only day 1 and day 3 have trips
            LocalDate day1 = START;
            LocalDate day3 = START.plusDays(2);
            Trip t1 = buildTrip(1L, day1, TripStatus.COMPLETED, 4.0, 600, v, r);
            Trip t3 = buildTrip(2L, day3, TripStatus.COMPLETED, 3.0, 500, v, r);

            LocalDateTime eta = LocalDateTime.of(2026, 8, 1, 8, 0);
            TripStop stop1 = buildStop(1L, t1, TripStopStatus.COMPLETED, eta, eta.plusMinutes(5));

            LocalDateTime eta3 = LocalDateTime.of(2026, 8, 3, 8, 0);
            TripStop stop3 = buildStop(2L, t3, TripStopStatus.COMPLETED, eta3, eta3.plusMinutes(5));

            when(tripStopRepository.findProcessedStopsInDateRange(START, END))
                    .thenReturn(List.of(stop1, stop3));
            when(tripRepository.findTripsWithVehicleInDateRange(START, END))
                    .thenReturn(List.of(t1, t3));

            KpiDailyTrendResponse result = kpiService.getDailyTrend(START, END);

            // Should have 7 data points (Aug 1 to Aug 7)
            assertThat(result.getData()).hasSize(7);

            // Day 1: has data
            DailyDataPoint d1 = result.getData().get(0);
            assertThat(d1.getDate()).isEqualTo(day1);
            assertThat(d1.getTripCount()).isEqualTo(1);
            assertThat(d1.getOnTimeRatePct()).isNotNull();

            // Day 2: no data
            DailyDataPoint d2 = result.getData().get(1);
            assertThat(d2.getDate()).isEqualTo(START.plusDays(1));
            assertThat(d2.getTripCount()).isEqualTo(0);
            assertThat(d2.getOnTimeRatePct()).isNull();
            assertThat(d2.getVolumeUtilPct()).isNull();
        }
    }

    @Nested
    @DisplayName("getByRoute()")
    class ByRouteTests {

        @Test
        @DisplayName("By-route: sorted by on-time rate ASC (worst first)")
        void byRoute_sortedAsc() {
            Vehicle v = buildVehicle(6.0, 800);
            Route r1 = buildRoute(1L, "RT-Q1", "Quận 1");
            Route r2 = buildRoute(2L, "RT-Q2", "Quận 2");

            Trip t1 = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v, r1);
            Trip t2 = buildTrip(2L, START, TripStatus.COMPLETED, 3.0, 500, v, r2);

            LocalDateTime eta = LocalDateTime.of(2026, 8, 1, 8, 0);
            // Route 1: 1 on-time stop → 100%
            TripStop s1 = buildStop(1L, t1, TripStopStatus.COMPLETED, eta, eta.plusMinutes(5));
            // Route 2: 1 late stop → 0%
            TripStop s2 = buildStop(2L, t2, TripStopStatus.COMPLETED, eta, eta.plusMinutes(20));

            when(tripRepository.findTripsWithVehicleAndRouteInDateRange(START, END))
                    .thenReturn(List.of(t1, t2));
            when(tripStopRepository.findProcessedStopsInDateRange(START, END))
                    .thenReturn(List.of(s1, s2));
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END))
                    .thenReturn(Collections.emptyList());

            KpiByRouteResponse result = kpiService.getByRoute(START, END);

            assertThat(result.getRoutes()).hasSize(2);
            // RT-Q2 (0%) should come first (worst)
            assertThat(result.getRoutes().get(0).getRouteCode()).isEqualTo("RT-Q2");
            assertThat(result.getRoutes().get(0).getOnTimeRatePct()).isEqualTo(0.0);
            // RT-Q1 (100%) second
            assertThat(result.getRoutes().get(1).getRouteCode()).isEqualTo("RT-Q1");
            assertThat(result.getRoutes().get(1).getOnTimeRatePct()).isEqualTo(100.0);
        }
    }
}

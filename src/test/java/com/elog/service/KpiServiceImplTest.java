package com.elog.service;

import com.elog.dto.response.kpi.*;
import com.elog.entity.*;
import com.elog.repository.*;
import com.elog.service.impl.KpiServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KpiServiceImplTest {
    @Mock TripRepository tripRepo;
    @Mock TripStopRepository tripStopRepo;
    @Mock DeliveryExceptionRepository deliveryExceptionRepo;
    @Mock SystemConfigRepository systemConfigRepo;

    KpiServiceImpl service;
    LocalDate start = LocalDate.of(2026, 8, 1);
    LocalDate end = LocalDate.of(2026, 8, 14);

    @BeforeEach
    void setUp() {
        service = new KpiServiceImpl(tripRepo, tripStopRepo, deliveryExceptionRepo, systemConfigRepo);
    }

    @Test
    @DisplayName("[L1-KPI-01] calculate returns non-null KPI summary metrics")
    void calculateReturnsSummary() {
        Vehicle vehicle = Vehicle.builder().id(1L).maxVolumeM3(new BigDecimal("10")).payloadKg(new BigDecimal("2000")).build();
        Trip trip = Trip.builder().tripId(10L).deliveryDate(start).status(TripStatus.COMPLETED).totalVolumeM3(new BigDecimal("8")).totalWeightKg(new BigDecimal("1500")).totalDistanceKm(new BigDecimal("120")).vehicle(vehicle).build();
        TripStop stop = TripStop.builder().tripStopId(100L).trip(trip).status(TripStopStatus.COMPLETED).plannedEta(LocalDateTime.of(2026, 8, 1, 9, 0)).actualArrivalTime(LocalDateTime.of(2026, 8, 1, 9, 5)).build();

        when(tripStopRepo.findProcessedStopsInDateRange(start, end)).thenReturn(List.of(stop));
        when(deliveryExceptionRepo.findExceptionsInDateRange(start, end)).thenReturn(List.of());
        when(deliveryExceptionRepo.countUnresolvedExceptionsInDateRange(start, end)).thenReturn(0L);
        when(tripRepo.findTripsWithVehicleInDateRange(start, end)).thenReturn(List.of(trip));

        KpiSummaryResponse summary = service.calculate(start, end);

        assertAll(
                () -> assertNotNull(summary),
                () -> assertEquals(start, summary.getPeriod().getStartDate()),
                () -> assertEquals(end, summary.getPeriod().getEndDate()),
                () -> assertNotNull(summary.getOnTimeDelivery()),
                () -> assertNotNull(summary.getFleetUtilization()),
                () -> assertNotNull(summary.getTripCompletion())
        );
    }

    @Test
    @DisplayName("[L1-KPI-02] getDailyTrend returns trend data points for date range")
    void getDailyTrendReturnsPoints() {
        Vehicle vehicle = Vehicle.builder().id(1L).maxVolumeM3(new BigDecimal("10")).payloadKg(new BigDecimal("2000")).build();
        Trip trip = Trip.builder().tripId(10L).deliveryDate(start).status(TripStatus.COMPLETED).totalVolumeM3(new BigDecimal("8")).totalWeightKg(new BigDecimal("1500")).totalDistanceKm(new BigDecimal("120")).vehicle(vehicle).build();
        TripStop stop = TripStop.builder().tripStopId(100L).trip(trip).status(TripStopStatus.COMPLETED).plannedEta(LocalDateTime.of(2026, 8, 1, 9, 0)).actualArrivalTime(LocalDateTime.of(2026, 8, 1, 9, 5)).build();

        when(tripStopRepo.findProcessedStopsInDateRange(start, end)).thenReturn(List.of(stop));
        when(tripRepo.findTripsWithVehicleInDateRange(start, end)).thenReturn(List.of(trip));

        KpiDailyTrendResponse trend = service.getDailyTrend(start, end);

        assertAll(
                () -> assertNotNull(trend),
                () -> assertFalse(trend.getData().isEmpty())
        );
    }

    @Test
    @DisplayName("[L1-KPI-03] getByRoute returns KPI breakdown by route")
    void getByRouteReturnsData() {
        Route route = Route.builder().id(1L).code("R1").name("Route 1").build();
        Vehicle vehicle = Vehicle.builder().id(1L).maxVolumeM3(new BigDecimal("10")).payloadKg(new BigDecimal("2000")).build();
        Trip trip = Trip.builder().tripId(10L).deliveryDate(start).status(TripStatus.COMPLETED).totalVolumeM3(new BigDecimal("8")).totalWeightKg(new BigDecimal("1500")).totalDistanceKm(new BigDecimal("120")).route(route).vehicle(vehicle).build();
        TripStop stop = TripStop.builder().tripStopId(100L).trip(trip).status(TripStopStatus.COMPLETED).plannedEta(LocalDateTime.of(2026, 8, 1, 9, 0)).actualArrivalTime(LocalDateTime.of(2026, 8, 1, 9, 5)).build();

        when(tripRepo.findTripsWithVehicleAndRouteInDateRange(start, end)).thenReturn(List.of(trip));
        when(tripStopRepo.findProcessedStopsInDateRange(start, end)).thenReturn(List.of(stop));
        when(deliveryExceptionRepo.findExceptionsInDateRange(start, end)).thenReturn(List.of());

        KpiByRouteResponse resp = service.getByRoute(start, end);

        assertAll(
                () -> assertNotNull(resp),
                () -> assertEquals(1, resp.getRoutes().size()),
                () -> assertEquals("R1", resp.getRoutes().getFirst().getRouteCode())
        );
    }

    @Test
    @DisplayName("[L1-KPI-04] getByVehicle returns KPI breakdown by vehicle")
    void getByVehicleReturnsData() {
        Route route = Route.builder().id(1L).code("R1").name("Route 1").build();
        Vehicle vehicle = Vehicle.builder().id(1L).plateNumber("29A-12345").maxVolumeM3(new BigDecimal("10")).payloadKg(new BigDecimal("2000")).build();
        Trip trip = Trip.builder().tripId(10L).deliveryDate(start).status(TripStatus.COMPLETED).totalVolumeM3(new BigDecimal("8")).totalWeightKg(new BigDecimal("1500")).totalDistanceKm(new BigDecimal("120")).route(route).vehicle(vehicle).build();
        TripStop stop = TripStop.builder().tripStopId(100L).trip(trip).status(TripStopStatus.COMPLETED).plannedEta(LocalDateTime.of(2026, 8, 1, 9, 0)).actualArrivalTime(LocalDateTime.of(2026, 8, 1, 9, 5)).build();

        when(tripRepo.findTripsWithVehicleAndRouteInDateRange(start, end)).thenReturn(List.of(trip));
        when(tripStopRepo.findProcessedStopsInDateRange(start, end)).thenReturn(List.of(stop));
        when(deliveryExceptionRepo.findExceptionsInDateRange(start, end)).thenReturn(List.of());

        KpiByVehicleResponse resp = service.getByVehicle(start, end);

        assertAll(
                () -> assertNotNull(resp),
                () -> assertEquals(1, resp.getVehicles().size()),
                () -> assertEquals("29A-12345", resp.getVehicles().getFirst().getLicensePlate())
        );
    }

    @Test
    @DisplayName("[L1-KPI-05] getByDriver returns KPI breakdown by driver")
    void getByDriverReturnsData() {
        User driver = User.builder().id(5L).username("driver01").fullName("Le Van Driver").build();
        Vehicle vehicle = Vehicle.builder().id(1L).plateNumber("29A-12345").build();
        Trip trip = Trip.builder().tripId(10L).deliveryDate(start).status(TripStatus.COMPLETED).driver(driver).vehicle(vehicle).totalDistanceKm(new BigDecimal("50")).build();
        TripStop stop = TripStop.builder().tripStopId(100L).trip(trip).status(TripStopStatus.COMPLETED).plannedEta(LocalDateTime.of(2026, 8, 1, 9, 0)).actualArrivalTime(LocalDateTime.of(2026, 8, 1, 9, 5)).build();

        when(tripRepo.findTripsWithVehicleAndRouteInDateRange(start, end)).thenReturn(List.of(trip));
        when(tripStopRepo.findProcessedStopsInDateRange(start, end)).thenReturn(List.of(stop));
        when(deliveryExceptionRepo.findExceptionsInDateRange(start, end)).thenReturn(List.of());

        KpiByDriverResponse resp = service.getByDriver(start, end);

        assertAll(
                () -> assertNotNull(resp),
                () -> assertEquals(1, resp.getDrivers().size()),
                () -> assertEquals("Le Van Driver", resp.getDrivers().getFirst().getFullName())
        );
    }
}

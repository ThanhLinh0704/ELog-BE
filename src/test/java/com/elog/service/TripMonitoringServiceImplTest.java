package com.elog.service;

import com.elog.dto.response.trip.*;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.TripMonitoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripMonitoringServiceImplTest {
    @Mock TripRepository tripRepo;
    @Mock TripStopRepository tripStopRepo;
    @Mock SystemConfigRepository systemConfigRepo;
    @Mock DeliveryExceptionRepository deliveryExceptionRepo;
    @Mock GoongMapService goongMapService;

    TripMonitoringServiceImpl service;
    User driver;
    Trip trip;
    TripStop stop;

    @BeforeEach
    void setUp() {
        service = new TripMonitoringServiceImpl(tripRepo, tripStopRepo, systemConfigRepo, deliveryExceptionRepo, goongMapService);
        driver = User.builder().id(2L).username("driver").fullName("Driver User").build();
        Route route = Route.builder().id(1L).code("R1").name("Route 1").build();
        Vehicle vehicle = Vehicle.builder().id(1L).plateNumber("29A-12345").build();
        trip = Trip.builder().tripId(10L).driver(driver).route(route).vehicle(vehicle).deliveryDate(LocalDate.now()).status(TripStatus.DISPATCHED).stops(new ArrayList<>()).build();
        stop = TripStop.builder().tripStopId(100L).trip(trip).status(TripStopStatus.PENDING).plannedEta(LocalDateTime.now().plusHours(1)).build();
        trip.getStops().add(stop);
    }

    @Test
    @DisplayName("[L1-MON-01] startTrip transitions trip status to IN_PROGRESS")
    void startTripSuccess() {
        when(tripRepo.findById(10L)).thenReturn(Optional.of(trip));
        when(tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(10L)).thenReturn(List.of(stop));

        TripStartResponse resp = service.startTrip(10L, "driver");

        assertAll(
                () -> assertEquals(10L, resp.getTripId()),
                () -> assertEquals("IN_PROGRESS", resp.getStatus()),
                () -> verify(tripRepo).save(trip)
        );
    }

    @Test
    @DisplayName("[L1-MON-02] startTrip rejects unauthorized driver")
    void startTripUnauthorizedDriver() {
        when(tripRepo.findById(10L)).thenReturn(Optional.of(trip));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.startTrip(10L, "intruder"));
        assertEquals(ErrorCode.NOT_YOUR_TRIP, ex.getErrorCode());
    }

    @Test
    @DisplayName("[L1-MON-03] arriveAtStop updates stop status to IN_PROGRESS")
    void arriveAtStopSuccess() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        when(tripStopRepo.findRemainingStopsOrdered(10L)).thenReturn(List.of(stop));
        when(systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES")).thenReturn(Optional.empty());

        StopArriveResponse resp = service.arriveAtStop(100L, "driver");

        assertAll(
                () -> assertEquals(100L, resp.getTripStopId()),
                () -> assertEquals("IN_PROGRESS", resp.getStatus()),
                () -> verify(tripStopRepo).save(stop)
        );
    }

    @Test
    @DisplayName("[L1-MON-04] completeStop marks stop completed and auto-completes trip when all stops done")
    void completeStopAutoCompletesTrip() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        stop.setStatus(TripStopStatus.IN_PROGRESS);
        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));

        StopCompleteResponse resp = service.completeStop(100L, "driver");

        assertAll(
                () -> assertEquals(100L, resp.getTripStopId()),
                () -> assertEquals("COMPLETED", resp.getStatus()),
                () -> assertTrue(resp.isTripCompleted()),
                () -> assertEquals("COMPLETED", resp.getTripStatus())
        );
    }

    @Test
    @DisplayName("[L1-MON-05] getActiveTripsDashboard returns active trips overview")
    void getActiveTripsDashboardSuccess() {
        LocalDate today = LocalDate.now();
        when(tripRepo.findActiveTripsByDate(eq(today), any())).thenReturn(List.of(trip));

        ActiveTripsResponse resp = service.getActiveTripsDashboard(today);

        assertAll(
                () -> assertNotNull(resp),
                () -> assertEquals(1, resp.getTotalActiveTrips()),
                () -> assertEquals(1, resp.getTrips().size())
        );
    }

    // ── getTripProgress tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("[L1-MON-06] getTripProgress returns full trip progress and stops")
    void getTripProgressSuccess() {
        Store store = Store.builder().id(5L).code("ST-05").name("Store 5").latitude(21.01).longitude(105.82).build();
        RouteStop routeStop = RouteStop.builder().id(1L).store(store).build();
        stop.setRouteStop(routeStop);
        stop.setPlannedEta(LocalDateTime.now().minusMinutes(10));
        stop.setActualArrivalTime(LocalDateTime.now().minusMinutes(5));
        stop.setActualDepartureTime(LocalDateTime.now());

        when(tripRepo.findById(10L)).thenReturn(Optional.of(trip));
        when(tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(10L)).thenReturn(List.of(stop));
        when(deliveryExceptionRepo.findByTripStopIdInOrderByCreatedAtDesc(anyList())).thenReturn(List.of());

        TripProgressResponse resp = service.getTripProgress(10L);

        assertNotNull(resp);
        assertEquals(10L, resp.getTripId());
        assertEquals("DISPATCHED", resp.getStatus());
        assertEquals(1, resp.getStops().size());
        assertEquals("ST-05", resp.getStops().get(0).getStoreCode());
    }

    @Test
    @DisplayName("[L1-MON-07] getTripProgress trip not found throws NotFound")
    void getTripProgressNotFound() {
        when(tripRepo.findById(999L)).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, () -> service.getTripProgress(999L));
        assertEquals(ErrorCode.TRIP_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    @DisplayName("[L1-MON-08] arriveAtStop with delay creates TIME_EXCEPTION")
    void arriveAtStopWithDelayCreatesTimeException() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        stop.setPlannedEta(LocalDateTime.now().minusMinutes(45)); // 45 min overdue

        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        when(tripStopRepo.findRemainingStopsOrdered(10L)).thenReturn(List.of(stop));
        when(systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES"))
                .thenReturn(Optional.of(SystemConfig.builder().configKey("ETA_THRESHOLD_MINUTES").configValue("15").build()));
        when(deliveryExceptionRepo.existsByTripStopIdAndExceptionType(100L, ExceptionType.TIME_EXCEPTION)).thenReturn(false);
        when(deliveryExceptionRepo.save(any())).thenAnswer(inv -> {
            DeliveryException de = inv.getArgument(0);
            de.setExceptionId(501L);
            return de;
        });

        StopArriveResponse resp = service.arriveAtStop(100L, "driver");

        assertNotNull(resp);
        assertTrue(resp.isTimeExceptionFlagged());
        assertEquals(501L, resp.getExceptionId());
        assertEquals("EXCEPTION", stop.getStatus().name());
        verify(deliveryExceptionRepo).save(any());
    }

    @Test
    @DisplayName("[L1-MON-09] arriveAtStop out of order throws PREVIOUS_STOP_NOT_DONE")
    void arriveAtStopOutOfOrder() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        TripStop firstStop = TripStop.builder().tripStopId(99L).sequenceOrder(1).trip(trip).status(TripStopStatus.PENDING).build();
        stop.setSequenceOrder(2);

        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        when(tripStopRepo.findRemainingStopsOrdered(10L)).thenReturn(List.of(firstStop, stop));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.arriveAtStop(100L, "driver"));
        assertEquals(ErrorCode.PREVIOUS_STOP_NOT_DONE, ex.getErrorCode());
    }

    @Test
    @DisplayName("[L1-MON-10] completeStop with remaining stops returns next stop info")
    void completeStopWithRemainingStops() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        stop.setStatus(TripStopStatus.IN_PROGRESS);

        Store store2 = Store.builder().id(6L).code("ST-06").build();
        RouteStop routeStop2 = RouteStop.builder().id(2L).store(store2).build();
        TripStop nextStop = TripStop.builder().tripStopId(101L).sequenceOrder(2).trip(trip).status(TripStopStatus.PENDING).routeStop(routeStop2).build();
        trip.getStops().add(nextStop);

        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        when(tripStopRepo.findRemainingStopsOrdered(10L)).thenReturn(List.of(nextStop));

        StopCompleteResponse resp = service.completeStop(100L, "driver");

        assertNotNull(resp);
        assertFalse(resp.isTripCompleted());
        assertNotNull(resp.getNextStop());
        assertEquals(101L, resp.getNextStop().getTripStopId());
        assertEquals("ST-06", resp.getNextStop().getStoreCode());
    }

    @Test
    @DisplayName("[L1-MON-11] startTrip future delivery date throws VALIDATION_FAILED")
    void startTripFutureDateThrows() {
        trip.setDeliveryDate(LocalDate.now().plusDays(2));
        when(tripRepo.findById(10L)).thenReturn(Optional.of(trip));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.startTrip(10L, "driver"));
        assertEquals(ErrorCode.VALIDATION_FAILED, ex.getErrorCode());
    }
}

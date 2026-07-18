package com.elog.service;

import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.TripMonitoringServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripMonitoringServiceImplTest {

    @Mock
    private TripRepository tripRepo;

    @Mock
    private TripStopRepository tripStopRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private SystemConfigRepository systemConfigRepo;

    @Mock
    private DeliveryExceptionRepository deliveryExceptionRepo;

    @InjectMocks
    private TripMonitoringServiceImpl monitoringService;

    private Trip trip;
    private User driver;
    private User dispatcher;
    private TripStop stop1;
    private TripStop stop2;
    private Store store1;
    private Store store2;
    private RouteStop routeStop1;
    private RouteStop routeStop2;

    @BeforeEach
    void setUp() {
        driver = User.builder().id(30L).username("driver01").fullName("Driver One").isActive(true).build();
        dispatcher = User.builder().id(2L).username("dispatcher01").fullName("Dispatcher One").build();

        store1 = Store.builder().id(10L).code("ST-001").name("Store One").build();
        store2 = Store.builder().id(11L).code("ST-002").name("Store Two").build();

        routeStop1 = RouteStop.builder().id(15L).store(store1).sequenceOrder(1).build();
        routeStop2 = RouteStop.builder().id(16L).store(store2).sequenceOrder(2).build();

        trip = Trip.builder()
                .tripId(1L)
                .driver(driver)
                .status(TripStatus.DISPATCHED)
                .deliveryDate(LocalDate.now())
                .route(Route.builder().id(1L).code("RT-001").build())
                .vehicle(Vehicle.builder().id(10L).plateNumber("29A-12345").build())
                .build();

        stop1 = TripStop.builder()
                .tripStopId(50L)
                .trip(trip)
                .routeStop(routeStop1)
                .sequenceOrder(1)
                .status(TripStopStatus.PENDING)
                .plannedEta(LocalDateTime.now().minusHours(1)) // 1 hour ago
                .build();

        stop2 = TripStop.builder()
                .tripStopId(51L)
                .trip(trip)
                .routeStop(routeStop2)
                .sequenceOrder(2)
                .status(TripStopStatus.PENDING)
                .plannedEta(LocalDateTime.now().plusHours(1))
                .build();

        trip.setStops(Arrays.asList(stop1, stop2));
    }

    @Test
    void startTrip_tripNotFound_throwsException() {
        when(tripRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> monitoringService.startTrip(999L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_NOT_FOUND);
    }

    @Test
    void startTrip_notAssignedDriver_throwsException() {
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> monitoringService.startTrip(1L, "another_driver"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_YOUR_TRIP)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
    }

    @Test
    void startTrip_statusNotDispatched_throwsException() {
        trip.setStatus(TripStatus.IN_PROGRESS);
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> monitoringService.startTrip(1L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRIP_TRANSITION);
    }

    @Test
    void startTrip_success() {
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(1L)).thenReturn(Arrays.asList(stop1, stop2));

        TripStartResponse response = monitoringService.startTrip(1L, "driver01");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        verify(tripRepo).save(trip);
    }

    @Test
    void arriveAtStop_tripStopNotFound_throwsException() {
        when(tripStopRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> monitoringService.arriveAtStop(999L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_STOP_NOT_FOUND);
    }

    @Test
    void arriveAtStop_notAssignedDriver_throwsException() {
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));

        assertThatThrownBy(() -> monitoringService.arriveAtStop(50L, "another_driver"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_YOUR_TRIP);
    }

    @Test
    void arriveAtStop_tripAlreadyCompleted_throwsException() {
        trip.setStatus(TripStatus.COMPLETED);
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));

        assertThatThrownBy(() -> monitoringService.arriveAtStop(50L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_COMPLETED);
    }

    @Test
    void arriveAtStop_stopAlreadyDone_throwsException() {
        stop1.setStatus(TripStopStatus.COMPLETED);
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));

        assertThatThrownBy(() -> monitoringService.arriveAtStop(50L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOP_ALREADY_DONE);
    }

    @Test
    void arriveAtStop_stopNotInProgressAndNotPending_throwsException() {
        stop1.setStatus(TripStopStatus.IN_PROGRESS);
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));

        assertThatThrownBy(() -> monitoringService.arriveAtStop(50L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOP_NOT_PENDING);
    }

    @Test
    void arriveAtStop_outOfSequence_throwsException() {
        // Driver tries to arrive at stop2 before stop1 is completed
        when(tripStopRepo.findById(51L)).thenReturn(Optional.of(stop2));
        when(tripStopRepo.findRemainingStopsOrdered(1L)).thenReturn(Arrays.asList(stop1, stop2));

        assertThatThrownBy(() -> monitoringService.arriveAtStop(51L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.PREVIOUS_STOP_NOT_DONE)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void arriveAtStop_success_noException_withinThreshold() {
        // ETA is 1 hour in future, so no delay
        stop1.setPlannedEta(LocalDateTime.now().plusHours(1));

        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));
        when(tripStopRepo.findRemainingStopsOrdered(1L)).thenReturn(Arrays.asList(stop1, stop2));
        when(systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES")).thenReturn(Optional.empty()); // defaults to 15

        StopArriveResponse response = monitoringService.arriveAtStop(50L, "driver01");

        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(response.isTimeExceptionFlagged()).isFalse();
        assertThat(stop1.getStatus()).isEqualTo(TripStopStatus.IN_PROGRESS);
    }

    @Test
    void arriveAtStop_success_timeExceptionFlagged_delayExceedsThreshold() {
        // Planned ETA was 1 hour ago, actual arrival is now (60 minutes delay). Threshold = 15.
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));
        when(tripStopRepo.findRemainingStopsOrdered(1L)).thenReturn(Arrays.asList(stop1, stop2));
        when(systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES")).thenReturn(Optional.empty()); // default 15
        when(deliveryExceptionRepo.existsByTripStopIdAndExceptionType(50L, ExceptionType.TIME_EXCEPTION)).thenReturn(false);

        DeliveryException exception = DeliveryException.builder().exceptionId(999L).build();
        when(deliveryExceptionRepo.save(any(DeliveryException.class))).thenReturn(exception);

        StopArriveResponse response = monitoringService.arriveAtStop(50L, "driver01");

        assertThat(response).isNotNull();
        assertThat(response.isTimeExceptionFlagged()).isTrue();
        assertThat(response.getExceptionId()).isEqualTo(999L);
        assertThat(stop1.getStatus()).isEqualTo(TripStopStatus.EXCEPTION);
        verify(deliveryExceptionRepo).save(any(DeliveryException.class));
    }

    @Test
    void completeStop_tripStopNotFound_throwsException() {
        when(tripStopRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> monitoringService.completeStop(999L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_STOP_NOT_FOUND);
    }

    @Test
    void completeStop_notAssignedDriver_throwsException() {
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));
        assertThatThrownBy(() -> monitoringService.completeStop(50L, "another_driver"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_YOUR_TRIP);
    }

    @Test
    void completeStop_stopNotInProgress_throwsException() {
        stop1.setStatus(TripStopStatus.PENDING);
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));
        assertThatThrownBy(() -> monitoringService.completeStop(50L, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOP_NOT_IN_PROGRESS);
    }

    @Test
    void completeStop_success_notLastStop() {
        stop1.setStatus(TripStopStatus.IN_PROGRESS);
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop1));
        when(tripStopRepo.findRemainingStopsOrdered(1L)).thenReturn(Collections.singletonList(stop2));

        StopCompleteResponse response = monitoringService.completeStop(50L, "driver01");

        assertThat(response).isNotNull();
        assertThat(response.isTripCompleted()).isFalse();
        assertThat(stop1.getStatus()).isEqualTo(TripStopStatus.COMPLETED);
        assertThat(response.getNextStop().getTripStopId()).isEqualTo(51L);
    }

    @Test
    void completeStop_success_lastStopCompletesTrip() {
        stop1.setStatus(TripStopStatus.COMPLETED);
        stop2.setStatus(TripStopStatus.IN_PROGRESS);

        when(tripStopRepo.findById(51L)).thenReturn(Optional.of(stop2));

        StopCompleteResponse response = monitoringService.completeStop(51L, "driver01");

        assertThat(response).isNotNull();
        assertThat(response.isTripCompleted()).isTrue();
        assertThat(response.getTripStatus()).isEqualTo("COMPLETED");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
        verify(tripRepo).save(trip);
    }

    @Test
    void getTripProgress_tripNotFound_throwsException() {
        when(tripRepo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> monitoringService.getTripProgress(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_NOT_FOUND);
    }

    @Test
    void getTripProgress_success() {
        when(tripRepo.findById(1L)).thenReturn(Optional.of(trip));
        when(tripStopRepo.findByTripTripIdOrderBySequenceOrderAsc(1L)).thenReturn(Arrays.asList(stop1, stop2));

        TripProgressResponse response = monitoringService.getTripProgress(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTripId()).isEqualTo(1L);
        assertThat(response.getStops()).hasSize(2);
    }

    @Test
    void getActiveTripsDashboard_success() {
        when(tripRepo.findActiveTripsByDate(any(LocalDate.class), anyList())).thenReturn(Collections.singletonList(trip));
        when(deliveryExceptionRepo.findByTripStopIdInOrderByCreatedAtDesc(anyList())).thenReturn(Collections.emptyList());

        ActiveTripsResponse response = monitoringService.getActiveTripsDashboard(LocalDate.now());

        assertThat(response).isNotNull();
        assertThat(response.getTrips()).hasSize(1);
    }
}

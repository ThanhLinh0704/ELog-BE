package com.elog.service;

import com.elog.dto.request.RejectStopRequest;
import com.elog.dto.request.ResolveExceptionRequest;
import com.elog.dto.response.DeliveryExceptionResponse;
import com.elog.dto.response.ExceptionListResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.ExceptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExceptionServiceImplTest {

    @Mock
    private TripStopRepository tripStopRepo;

    @Mock
    private TripRepository tripRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private DeliveryExceptionRepository deliveryExceptionRepo;

    @InjectMocks
    private ExceptionServiceImpl exceptionService;

    private Trip trip;
    private User driver;
    private User manager;
    private TripStop stop;
    private RouteStop routeStop;
    private Store store;

    @BeforeEach
    void setUp() {
        driver = User.builder().id(30L).username("driver01").fullName("Driver One").build();
        manager = User.builder().id(2L).username("dispatcher01").fullName("Dispatcher One").build();

        store = Store.builder().id(10L).code("ST-001").name("Store One").build();
        routeStop = RouteStop.builder().id(15L).store(store).sequenceOrder(1).build();

        trip = Trip.builder()
                .tripId(1L)
                .driver(driver)
                .status(TripStatus.IN_PROGRESS)
                .deliveryDate(LocalDate.now())
                .route(Route.builder().id(1L).code("RT-001").build())
                .vehicle(Vehicle.builder().id(10L).plateNumber("29A-12345").build())
                .build();

        stop = TripStop.builder()
                .tripStopId(50L)
                .trip(trip)
                .routeStop(routeStop)
                .sequenceOrder(1)
                .status(TripStopStatus.IN_PROGRESS)
                .plannedEta(LocalDateTime.now())
                .build();

        trip.setStops(Collections.singletonList(stop));
    }

    @Test
    void rejectStop_tripStopNotFound_throwsException() {
        when(tripStopRepo.findById(999L)).thenReturn(Optional.empty());
        RejectStopRequest req = new RejectStopRequest();

        assertThatThrownBy(() -> exceptionService.rejectStop(999L, req, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_STOP_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void rejectStop_notAssignedDriver_throwsException() {
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));

        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("STORE_CLOSED");

        assertThatThrownBy(() -> exceptionService.rejectStop(50L, req, "another_driver"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_YOUR_TRIP)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.FORBIDDEN);
    }

    @Test
    void rejectStop_stopNotInProgress_throwsException() {
        stop.setStatus(TripStopStatus.PENDING);
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));

        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("STORE_CLOSED");

        assertThatThrownBy(() -> exceptionService.rejectStop(50L, req, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STOP_NOT_IN_PROGRESS)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void rejectStop_alreadyRecorded_throwsException() {
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));
        when(deliveryExceptionRepo.existsByTripStopIdAndExceptionTypeAndResolvedAtIsNull(
                50L, ExceptionType.DELIVERY_REJECTION)).thenReturn(true);
        
        DeliveryException existing = DeliveryException.builder().exceptionId(101L).exceptionType(ExceptionType.DELIVERY_REJECTION).build();
        when(deliveryExceptionRepo.findByTripStopIdOrderByCreatedAtDesc(50L)).thenReturn(Collections.singletonList(existing));

        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("STORE_CLOSED");

        assertThatThrownBy(() -> exceptionService.rejectStop(50L, req, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.REJECTION_ALREADY_RECORDED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void rejectStop_otherRejectionTypeWithoutDescription_throwsException() {
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));
        when(deliveryExceptionRepo.existsByTripStopIdAndExceptionTypeAndResolvedAtIsNull(
                50L, ExceptionType.DELIVERY_REJECTION)).thenReturn(false);

        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("OTHER");
        req.setDescription(""); // blank description

        assertThatThrownBy(() -> exceptionService.rejectStop(50L, req, "driver01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void rejectStop_success_withOtherTypeAndDescription() {
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));
        when(deliveryExceptionRepo.existsByTripStopIdAndExceptionTypeAndResolvedAtIsNull(
                50L, ExceptionType.DELIVERY_REJECTION)).thenReturn(false);
        when(userRepo.findByUsername("driver01")).thenReturn(Optional.of(driver));

        DeliveryException exSaved = DeliveryException.builder()
                .exceptionId(500L)
                .tripStopId(50L)
                .exceptionType(ExceptionType.DELIVERY_REJECTION)
                .reportedBy(30L)
                .description("[OTHER] Broken gate")
                .createdAt(LocalDateTime.now())
                .build();
        when(deliveryExceptionRepo.save(any(DeliveryException.class))).thenReturn(exSaved);

        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("OTHER");
        req.setDescription("Broken gate");

        DeliveryExceptionResponse response = exceptionService.rejectStop(50L, req, "driver01");

        assertThat(response).isNotNull();
        assertThat(response.getExceptionId()).isEqualTo(500L);
        assertThat(response.getDescription()).isEqualTo("Broken gate");
    }

    @Test
    void rejectStop_success_withPredefinedType() {
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));
        when(deliveryExceptionRepo.existsByTripStopIdAndExceptionTypeAndResolvedAtIsNull(
                50L, ExceptionType.DELIVERY_REJECTION)).thenReturn(false);
        when(userRepo.findByUsername("driver01")).thenReturn(Optional.of(driver));

        DeliveryException exSaved = DeliveryException.builder()
                .exceptionId(500L)
                .tripStopId(50L)
                .exceptionType(ExceptionType.DELIVERY_REJECTION)
                .reportedBy(30L)
                .description("[STORE_CLOSED]")
                .createdAt(LocalDateTime.now())
                .build();
        when(deliveryExceptionRepo.save(any(DeliveryException.class))).thenReturn(exSaved);

        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("STORE_CLOSED");

        DeliveryExceptionResponse response = exceptionService.rejectStop(50L, req, "driver01");

        assertThat(response).isNotNull();
        assertThat(response.getExceptionId()).isEqualTo(500L);
        assertThat(response.getTripStopStatus()).isEqualTo("EXCEPTION");
        assertThat(stop.getStatus()).isEqualTo(TripStopStatus.EXCEPTION);
        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED); // because it is the only stop in the trip
        verify(deliveryExceptionRepo).save(any(DeliveryException.class));
    }

    @Test
    void resolveException_alreadyResolved_throwsException() {
        DeliveryException ex = DeliveryException.builder()
                .exceptionId(500L)
                .resolvedAt(LocalDateTime.now())
                .build();

        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(ex));

        ResolveExceptionRequest req = new ResolveExceptionRequest();
        req.setResolutionNotes("Solved");

        assertThatThrownBy(() -> exceptionService.resolveException(500L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEPTION_ALREADY_RESOLVED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void resolveException_exceptionNotFound_throwsException() {
        when(deliveryExceptionRepo.findById(999L)).thenReturn(Optional.empty());
        ResolveExceptionRequest req = new ResolveExceptionRequest();

        assertThatThrownBy(() -> exceptionService.resolveException(999L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEPTION_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void resolveException_userNotFound_throwsException() {
        DeliveryException ex = DeliveryException.builder().exceptionId(500L).build();
        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(ex));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.empty());
        ResolveExceptionRequest req = new ResolveExceptionRequest();

        assertThatThrownBy(() -> exceptionService.resolveException(500L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void resolveException_tripStopNotFound_throwsException() {
        DeliveryException ex = DeliveryException.builder()
                .exceptionId(500L)
                .tripStopId(50L)
                .reportedBy(30L)
                .build();
        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(ex));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.of(manager));
        when(tripStopRepo.findById(50L)).thenReturn(Optional.empty());
        ResolveExceptionRequest req = new ResolveExceptionRequest();

        assertThatThrownBy(() -> exceptionService.resolveException(500L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_STOP_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void resolveException_success() {
        DeliveryException ex = DeliveryException.builder()
                .exceptionId(500L)
                .tripStopId(50L)
                .exceptionType(ExceptionType.DELIVERY_REJECTION)
                .reportedBy(30L)
                .description("[STORE_CLOSED]")
                .createdAt(LocalDateTime.now())
                .build();

        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(ex));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.of(manager));
        when(userRepo.findById(30L)).thenReturn(Optional.of(driver));
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));

        ResolveExceptionRequest req = new ResolveExceptionRequest();
        req.setResolutionNotes("Rescheduled to tomorrow");

        DeliveryExceptionResponse response = exceptionService.resolveException(500L, req, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(response.getResolvedAt()).isNotNull();
        assertThat(response.getResolutionNotes()).isEqualTo("Rescheduled to tomorrow");
        verify(deliveryExceptionRepo).save(ex);
    }

    @Test
    void listExceptions_invalidTypeFilter_throwsException() {
        assertThatThrownBy(() -> exceptionService.listExceptions(LocalDate.now(), "INVALID_TYPE", "all"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void listExceptions_invalidResolvedFilter_throwsException() {
        assertThatThrownBy(() -> exceptionService.listExceptions(LocalDate.now(), "ALL", "invalid_resolved"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAILED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void listExceptions_success() {
        DeliveryException ex = DeliveryException.builder()
                .exceptionId(500L)
                .tripStopId(50L)
                .exceptionType(ExceptionType.DELIVERY_REJECTION)
                .reportedBy(30L)
                .description("[STORE_CLOSED]")
                .createdAt(LocalDateTime.now())
                .build();

        when(deliveryExceptionRepo.findByFilters(any(LocalDate.class), any(), any()))
                .thenReturn(Collections.singletonList(ex));
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));
        when(userRepo.findById(30L)).thenReturn(Optional.of(driver));

        ExceptionListResponse response = exceptionService.listExceptions(LocalDate.now(), "ALL", "all");

        assertThat(response).isNotNull();
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getExceptions().get(0).getExceptionId()).isEqualTo(500L);
    }

    @Test
    void getException_notFound_throwsException() {
        when(deliveryExceptionRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exceptionService.getException(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EXCEPTION_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void getException_tripStopNotFound_throwsException() {
        DeliveryException ex = DeliveryException.builder().exceptionId(500L).tripStopId(50L).build();
        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(ex));
        when(tripStopRepo.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> exceptionService.getException(500L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_STOP_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void getException_success() {
        DeliveryException ex = DeliveryException.builder()
                .exceptionId(500L)
                .tripStopId(50L)
                .exceptionType(ExceptionType.DELIVERY_REJECTION)
                .reportedBy(30L)
                .description("[STORE_CLOSED]")
                .createdAt(LocalDateTime.now())
                .build();

        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(ex));
        when(tripStopRepo.findById(50L)).thenReturn(Optional.of(stop));
        when(userRepo.findById(30L)).thenReturn(Optional.of(driver));

        DeliveryExceptionResponse response = exceptionService.getException(500L);

        assertThat(response).isNotNull();
        assertThat(response.getExceptionId()).isEqualTo(500L);
        assertThat(response.getStoreCode()).isEqualTo("ST-001");
    }
}

package com.elog.service;

import com.elog.dto.request.exception.RejectStopRequest;
import com.elog.dto.request.exception.ResolveExceptionRequest;
import com.elog.dto.response.exception.DeliveryExceptionResponse;
import com.elog.dto.response.exception.ExceptionListResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.ExceptionServiceImpl;
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
class ExceptionServiceImplTest {
    @Mock TripStopRepository tripStopRepo;
    @Mock TripRepository tripRepo;
    @Mock UserRepository userRepo;
    @Mock DeliveryExceptionRepository deliveryExceptionRepo;
    @Mock TripExecutionRepository tripExecutionRepo;
    @Mock DeliveryOrderResultRepository deliveryOrderResultRepo;

    ExceptionServiceImpl service;
    User driver, manager;
    Trip trip;
    TripStop stop;
    DeliveryException exception;

    @BeforeEach
    void setUp() {
        service = new ExceptionServiceImpl(tripStopRepo, tripRepo, userRepo, deliveryExceptionRepo, tripExecutionRepo, deliveryOrderResultRepo);
        driver = User.builder().id(2L).username("driver").fullName("Driver User").build();
        manager = User.builder().id(3L).username("manager").fullName("Manager User").build();
        trip = Trip.builder().tripId(10L).driver(driver).status(TripStatus.IN_PROGRESS).stops(new ArrayList<>()).build();
        stop = TripStop.builder().tripStopId(100L).trip(trip).status(TripStopStatus.IN_PROGRESS).build();
        trip.getStops().add(stop);
        exception = DeliveryException.builder().exceptionId(500L).tripStopId(100L).exceptionType(ExceptionType.DELIVERY_REJECTION).reportedBy(2L).description("[STORE_CLOSED] Closed early").createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("[L1-EX-01] rejectStop records rejection exception and sets stop to EXCEPTION status")
    void rejectStopSuccess() {
        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        when(deliveryExceptionRepo.existsByTripStopIdAndExceptionTypeAndResolvedAtIsNull(100L, ExceptionType.DELIVERY_REJECTION)).thenReturn(false);
        when(userRepo.findByUsername("driver")).thenReturn(Optional.of(driver));
        when(deliveryExceptionRepo.save(any())).thenAnswer(i -> {
            DeliveryException ex = i.getArgument(0);
            ex.setExceptionId(500L);
            ex.setCreatedAt(LocalDateTime.now());
            return ex;
        });

        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("STORE_CLOSED");
        req.setDescription("Closed early");

        DeliveryExceptionResponse resp = service.rejectStop(100L, req, "driver");

        assertAll(
                () -> assertEquals(500L, resp.getExceptionId()),
                () -> assertEquals(TripStopStatus.EXCEPTION, stop.getStatus()),
                () -> verify(tripStopRepo).save(stop)
        );
    }

    @Test
    @DisplayName("[L1-EX-02] rejectStop rejects unauthorized driver")
    void rejectStopUnauthorizedDriver() {
        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("STORE_CLOSED");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.rejectStop(100L, req, "intruder"));
        assertEquals(ErrorCode.NOT_YOUR_TRIP, ex.getErrorCode());
    }

    @Test
    @DisplayName("[L1-EX-03] rejectStop rejects stop not IN_PROGRESS")
    void rejectStopNotInProgress() {
        stop.setStatus(TripStopStatus.PENDING);
        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        RejectStopRequest req = new RejectStopRequest();
        req.setRejectionType("STORE_CLOSED");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.rejectStop(100L, req, "driver"));
        assertEquals(ErrorCode.STOP_NOT_IN_PROGRESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("[L1-EX-04] listExceptions returns filtered exception list")
    void listExceptionsReturnsList() {
        when(deliveryExceptionRepo.findByFilters(eq(LocalDate.now()), any(), any())).thenReturn(List.of(exception));

        ExceptionListResponse resp = service.listExceptions(LocalDate.now(), "ALL", "all");

        assertAll(
                () -> assertEquals(1, resp.getTotalCount()),
                () -> assertEquals(1, resp.getUnresolvedCount()),
                () -> assertEquals(1, resp.getExceptions().size())
        );
    }

    @Test
    @DisplayName("[L1-EX-05] getException returns detailed exception info")
    void getExceptionReturnsDetail() {
        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(exception));
        when(tripStopRepo.findById(100L)).thenReturn(Optional.of(stop));
        when(userRepo.findById(2L)).thenReturn(Optional.of(driver));

        DeliveryExceptionResponse resp = service.getException(500L);

        assertAll(
                () -> assertEquals(500L, resp.getExceptionId()),
                () -> assertEquals("DELIVERY_REJECTION", resp.getExceptionType())
        );
    }

    @Test
    @DisplayName("[L1-EX-06] resolveException updates resolvedAt and resolvedBy")
    void resolveExceptionSuccess() {
        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(exception));
        when(userRepo.findByUsername("manager")).thenReturn(Optional.of(manager));
        when(userRepo.findById(2L)).thenReturn(Optional.of(driver));
        when(deliveryExceptionRepo.save(any())).thenAnswer(i -> i.getArgument(0));

        ResolveExceptionRequest req = new ResolveExceptionRequest();
        req.setResolutionNotes("Resolved with customer");

        DeliveryExceptionResponse resp = service.resolveException(500L, req, "manager");

        assertAll(
                () -> assertNotNull(exception.getResolvedAt()),
                () -> assertEquals(3L, exception.getResolvedBy()),
                () -> assertEquals("Resolved with customer", exception.getResolutionNotes())
        );
    }

    @Test
    @DisplayName("[L1-EX-07] resolveException rejects already resolved exception")
    void resolveAlreadyResolved() {
        exception.setResolvedAt(LocalDateTime.now());
        when(deliveryExceptionRepo.findById(500L)).thenReturn(Optional.of(exception));

        ResolveExceptionRequest req = new ResolveExceptionRequest();
        req.setResolutionNotes("Again");

        BusinessException ex = assertThrows(BusinessException.class, () -> service.resolveException(500L, req, "manager"));
        assertEquals(ErrorCode.EXCEPTION_ALREADY_RESOLVED, ex.getErrorCode());
    }
}

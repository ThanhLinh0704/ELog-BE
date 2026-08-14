package com.elog.service;

import com.elog.dto.request.UpdateOrderResultRequest;
import com.elog.entity.DeliveryOrderResult;
import com.elog.entity.Order;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.entity.TripOutcome;
import com.elog.entity.TripStatus;
import com.elog.entity.User;
import com.elog.entity.Vehicle;
import com.elog.entity.VehicleStatus;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.DeliveryExceptionRepository;
import com.elog.repository.DeliveryOrderResultRepository;
import com.elog.repository.OrderRepository;
import com.elog.repository.TripDraftStopRepository;
import com.elog.repository.TripExecutionRepository;
import com.elog.repository.TripOutcomeRepository;
import com.elog.repository.TripRepository;
import com.elog.repository.TripStopRepository;
import com.elog.repository.UserRepository;
import com.elog.repository.VehicleRepository;
import com.elog.service.impl.DriverTripServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DriverTripServiceImplReport5Test {
    private TripExecutionRepository executions;
    private DeliveryOrderResultRepository results;
    private TripOutcomeRepository outcomes;
    private TripRepository trips;
    private DeliveryExceptionRepository exceptions;
    private TripOutcomeHistoryService history;
    private DriverTripServiceImpl service;
    private TripExecution execution;
    private Trip trip;

    @BeforeEach
    void setUp() {
        executions = mock(TripExecutionRepository.class);
        results = mock(DeliveryOrderResultRepository.class);
        outcomes = mock(TripOutcomeRepository.class);
        trips = mock(TripRepository.class);
        exceptions = mock(DeliveryExceptionRepository.class);
        history = mock(TripOutcomeHistoryService.class);
        service = new DriverTripServiceImpl(executions, results, outcomes, trips,
                mock(OrderRepository.class), mock(TripDraftStopRepository.class), mock(UserRepository.class),
                mock(TripStopRepository.class), exceptions, mock(VehicleRepository.class), history);

        User driver = User.builder().id(7L).username("driver").fullName("Driver Seven").build();
        Vehicle vehicle = Vehicle.builder().id(8L).vehicleCode("V-08").plateNumber("51A-00008")
                .status(VehicleStatus.AVAILABLE).build();
        trip = Trip.builder().tripId(10L).deliveryDate(LocalDate.now()).driver(driver).vehicle(vehicle)
                .status(TripStatus.DISPATCHED).build();
        execution = TripExecution.builder().id(20L).trip(trip).driver(driver).status("ASSIGNED").build();
        when(executions.findById(20L)).thenReturn(Optional.of(execution));
    }

    @Test
    @DisplayName("[L1-DR-04] assigned execution starts and synchronizes execution trip and vehicle")
    void startsAssignedExecution() {
        LocalDateTime before = LocalDateTime.now();
        service.startTrip(20L, "driver");
        assertAll(
                () -> assertEquals("IN_PROGRESS", execution.getStatus()),
                () -> assertFalse(execution.getStartedAt().isBefore(before)),
                () -> assertEquals(TripStatus.IN_PROGRESS, trip.getStatus()),
                () -> assertEquals(VehicleStatus.IN_USE, trip.getVehicle().getStatus()),
                () -> verify(executions).save(execution),
                () -> verify(trips).save(trip));
    }

    @Test
    @DisplayName("[L1-DR-05] start rejects an execution outside ASSIGNED")
    void startRejectsWrongState() {
        execution.setStatus("IN_PROGRESS");
        BusinessException error = assertThrows(BusinessException.class, () -> service.startTrip(20L, "driver"));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> verify(executions, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-06] start rejects a different driver")
    void startRejectsWrongDriver() {
        BusinessException error = assertThrows(BusinessException.class, () -> service.startTrip(20L, "intruder"));
        assertAll(() -> assertEquals(ErrorCode.UNAUTHORIZED_ACCESS, error.getErrorCode()),
                () -> verify(executions, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-07] start rejects a future delivery date")
    void startRejectsFutureDate() {
        trip.setDeliveryDate(LocalDate.now().plusDays(1));
        BusinessException error = assertThrows(BusinessException.class, () -> service.startTrip(20L, "driver"));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> verify(executions, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-08] delivered result updates the existing order result")
    void recordsDeliveredResult() {
        DeliveryOrderResult result = result("PENDING");
        updateFixture(result);
        service.updateOrderResult(20L, 30L, request("DELIVERED", null), "driver");
        assertAll(() -> assertEquals("DELIVERED", result.getStatus()),
                () -> assertNotNull(result.getUpdatedAt()),
                () -> verify(results).save(result));
    }

    @Test
    @DisplayName("[L1-DR-09] failed result persists status and mandatory reason")
    void recordsFailedResult() {
        DeliveryOrderResult result = result("PENDING");
        updateFixture(result);
        when(exceptions.existsByOrderIdAndExceptionTypeAndResolvedAtIsNull(any(), any())).thenReturn(true);
        service.updateOrderResult(20L, 30L, request("FAILED", "CUSTOMER_REJECTED"), "driver");
        assertAll(() -> assertEquals("FAILED", result.getStatus()),
                () -> assertEquals("CUSTOMER_REJECTED", result.getReasonCode()),
                () -> verify(results).save(result));
    }

    @Test
    @DisplayName("[L1-DR-10] partially delivered result persists status and reason")
    void recordsPartialResult() {
        DeliveryOrderResult result = result("PENDING");
        updateFixture(result);
        when(exceptions.existsByOrderIdAndExceptionTypeAndResolvedAtIsNull(any(), any())).thenReturn(true);
        service.updateOrderResult(20L, 30L, request("PARTIALLY_DELIVERED", "SHORTAGE"), "driver");
        assertAll(() -> assertEquals("PARTIALLY_DELIVERED", result.getStatus()),
                () -> assertEquals("SHORTAGE", result.getReasonCode()),
                () -> verify(results).save(result));
    }

    @Test
    @DisplayName("[L1-DR-11] update rejects an order absent from the execution")
    void updateRejectsUnknownOrder() {
        execution.setStatus("IN_PROGRESS");
        when(results.findByTripExecutionIdAndOrderId(20L, 999L)).thenReturn(Optional.empty());
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.updateOrderResult(20L, 999L, request("DELIVERED", null), "driver"));
        assertAll(() -> assertEquals(ErrorCode.RESOURCE_NOT_FOUND, error.getErrorCode()),
                () -> verify(results, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-12] update rejects execution outside IN_PROGRESS")
    void updateRejectsWrongExecutionState() {
        execution.setStatus("ASSIGNED");
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.updateOrderResult(20L, 30L, request("DELIVERED", null), "driver"));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> verify(results, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-13] failed update rejects a blank reason")
    void failedUpdateRequiresReason() {
        execution.setStatus("IN_PROGRESS");
        BusinessException error = assertThrows(BusinessException.class,
                () -> service.updateOrderResult(20L, 30L, request("FAILED", "  "), "driver"));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> verify(results, never()).findByTripExecutionIdAndOrderId(any(), any()),
                () -> verify(results, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-14] completion with terminal orders creates a submitted outcome")
    void completesAllTerminalOrders() {
        stubCompletionCounts(3, 2, 1, 0);
        service.completeTrip(20L, "driver");
        TripOutcome saved = captureOutcome();
        assertAll(() -> assertEquals("COMPLETED_WITH_EXCEPTIONS", execution.getStatus()),
                () -> assertEquals(TripStatus.COMPLETED, trip.getStatus()),
                () -> assertEquals("SUBMITTED", saved.getStatus()),
                () -> assertEquals(3, saved.getTotalOrders()),
                () -> assertEquals(2, saved.getDeliveredCount()),
                () -> assertEquals(1, saved.getFailedCount()));
    }

    @Test
    @DisplayName("[L1-DR-15] completion rejects while any order remains pending")
    void completionRejectsPendingOrders() {
        execution.setStatus("IN_PROGRESS");
        when(results.countByTripExecutionIdAndStatus(20L, "PENDING")).thenReturn(1L);
        BusinessException error = assertThrows(BusinessException.class, () -> service.completeTrip(20L, "driver"));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> verify(outcomes, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-16] completion rejects execution outside IN_PROGRESS")
    void completionRejectsWrongState() {
        execution.setStatus("ASSIGNED");
        BusinessException error = assertThrows(BusinessException.class, () -> service.completeTrip(20L, "driver"));
        assertAll(() -> assertEquals(ErrorCode.VALIDATION_FAILED, error.getErrorCode()),
                () -> verify(results, never()).countByTripExecutionIdAndStatus(any(), any()),
                () -> verify(outcomes, never()).save(any()));
    }

    @Test
    @DisplayName("[L1-DR-17] completion records completedAt at the current operation time")
    void completionRecordsCurrentTimestamp() {
        stubCompletionCounts(1, 1, 0, 0);
        LocalDateTime before = LocalDateTime.now();
        service.completeTrip(20L, "driver");
        LocalDateTime after = LocalDateTime.now();
        assertAll(() -> assertNotNull(execution.getCompletedAt()),
                () -> assertTrue(!execution.getCompletedAt().isBefore(before)
                        && !execution.getCompletedAt().isAfter(after)),
                () -> verify(executions).save(execution));
    }

    private DeliveryOrderResult result(String status) {
        return DeliveryOrderResult.builder().id(40L).tripExecution(execution)
                .order(Order.builder().id(30L).orderRef("ORD-30").build()).status(status).build();
    }

    private void updateFixture(DeliveryOrderResult result) {
        execution.setStatus("IN_PROGRESS");
        when(results.findByTripExecutionIdAndOrderId(20L, 30L)).thenReturn(Optional.of(result));
    }

    private UpdateOrderResultRequest request(String status, String reason) {
        return UpdateOrderResultRequest.builder().status(status).reasonCode(reason).exceptionText("driver note").build();
    }

    private void stubCompletionCounts(long total, long delivered, long failed, long partial) {
        execution.setStatus("IN_PROGRESS");
        when(results.countByTripExecutionIdAndStatus(20L, "PENDING")).thenReturn(0L);
        when(results.countByTripExecutionId(20L)).thenReturn(total);
        when(results.countByTripExecutionIdAndStatus(20L, "DELIVERED")).thenReturn(delivered);
        when(results.countByTripExecutionIdAndStatus(20L, "FAILED")).thenReturn(failed);
        when(results.countByTripExecutionIdAndStatus(20L, "PARTIALLY_DELIVERED")).thenReturn(partial);
    }

    private TripOutcome captureOutcome() {
        org.mockito.ArgumentCaptor<TripOutcome> captor = org.mockito.ArgumentCaptor.forClass(TripOutcome.class);
        verify(outcomes).save(captor.capture());
        return captor.getValue();
    }
}

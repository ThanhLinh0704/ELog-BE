package com.elog.service;

import com.elog.entity.Trip;
import com.elog.entity.TripStatus;
import com.elog.entity.Vehicle;
import com.elog.entity.VehicleStatus;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripStateMachineTest {

    private final TripStateMachine stateMachine = new TripStateMachine();

    @Test
    @DisplayName("[L1-SM-01] validated to dispatched sets lock and vehicle available")
    void l1Sm01_validatedToDispatched_setsLockAndVehicleAvailable() {
        Vehicle vehicle = Vehicle.builder().status(VehicleStatus.IN_USE).build();
        Trip trip = Trip.builder().tripId(1L).status(TripStatus.VALIDATED).vehicle(vehicle).build();

        stateMachine.transition(trip, TripStatus.DISPATCHED, 1L);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.DISPATCHED);
        assertThat(trip.getLockedAt()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    @DisplayName("[L1-SM-02] dispatched to in progress sets departure and vehicle in use")
    void l1Sm02_dispatchedToInProgress_setsDepartureAndVehicleInUse() {
        Vehicle vehicle = Vehicle.builder().status(VehicleStatus.AVAILABLE).build();
        Trip trip = Trip.builder().tripId(2L).status(TripStatus.DISPATCHED).vehicle(vehicle).build();

        stateMachine.transition(trip, TripStatus.IN_PROGRESS, 1L);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        assertThat(trip.getActualDepartureTime()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);
    }

    @Test
    @DisplayName("[L1-SM-03] in progress to completed sets completed timestamp")
    void l1Sm03_inProgressToCompleted_setsCompletedAt() {
        Trip trip = Trip.builder().tripId(3L).status(TripStatus.IN_PROGRESS).build();

        stateMachine.transition(trip, TripStatus.COMPLETED, 1L);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
        assertThat(trip.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("[L1-SM-04] validated to dispatched accepts a null vehicle")
    void l1Sm04_validatedToDispatched_acceptsNullVehicle() {
        Trip trip = Trip.builder().tripId(4L).status(TripStatus.VALIDATED).vehicle(null).build();

        stateMachine.transition(trip, TripStatus.DISPATCHED, 1L);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.DISPATCHED);
        assertThat(trip.getLockedAt()).isNotNull();
    }

    @Test
    @DisplayName("[L1-SM-05] completed trip rejects every subsequent target without mutation")
    void l1Sm05_completedRejectsEveryTargetWithoutMutation() {
        LocalDateTime completedAt = LocalDateTime.now().minusMinutes(1);
        for (TripStatus target : TripStatus.values()) {
            Trip trip = Trip.builder().tripId(5L).status(TripStatus.COMPLETED).completedAt(completedAt).build();

            assertThatThrownBy(() -> stateMachine.transition(trip, target, 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(error -> ((BusinessException) error).getErrorCode())
                    .isEqualTo(ErrorCode.TRIP_COMPLETED);
            assertThat(trip.getStatus()).isEqualTo(TripStatus.COMPLETED);
            assertThat(trip.getCompletedAt()).isEqualTo(completedAt);
        }
    }

    @Test
    @DisplayName("[L1-SM-06] dispatched to validated is blocked by the trip lock")
    void l1Sm06_dispatchedToValidated_reportsLocked() {
        LocalDateTime lockedAt = LocalDateTime.now().minusMinutes(1);
        Trip trip = Trip.builder().tripId(6L).status(TripStatus.DISPATCHED).lockedAt(lockedAt).build();

        assertThatThrownBy(() -> stateMachine.transition(trip, TripStatus.VALIDATED, 1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(error -> {
                    BusinessException businessError = (BusinessException) error;
                    assertThat(businessError.getErrorCode()).isEqualTo(ErrorCode.TRIP_LOCKED);
                    assertThat(businessError.getMessage()).contains("already dispatched (locked at");
                });
        assertThat(trip.getStatus()).isEqualTo(TripStatus.DISPATCHED);
        assertThat(trip.getLockedAt()).isEqualTo(lockedAt);
    }

    @Test
    @DisplayName("[L1-SM-07] unsupported nonterminal transitions are rejected")
    void l1Sm07_unsupportedNonTerminalTransitionsAreRejected() {
        TripStatus[][] invalid = {
                {TripStatus.VALIDATED, TripStatus.IN_PROGRESS},
                {TripStatus.VALIDATED, TripStatus.COMPLETED},
                {TripStatus.IN_PROGRESS, TripStatus.DISPATCHED},
                {TripStatus.IN_PROGRESS, TripStatus.VALIDATED},
                {TripStatus.DISPATCHED, TripStatus.COMPLETED},
        };

        for (TripStatus[] transition : invalid) {
            Trip trip = Trip.builder().tripId(7L).status(transition[0]).build();
            assertThatThrownBy(() -> stateMachine.transition(trip, transition[1], 1L))
                    .isInstanceOf(BusinessException.class)
                    .extracting(error -> ((BusinessException) error).getErrorCode())
                    .isEqualTo(ErrorCode.INVALID_TRIP_TRANSITION);
            assertThat(trip.getStatus()).isEqualTo(transition[0]);
        }
    }
}

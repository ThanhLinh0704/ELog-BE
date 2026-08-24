package com.elog.service;

import com.elog.entity.Trip;
import com.elog.entity.TripStatus;
import com.elog.entity.Vehicle;
import com.elog.entity.VehicleStatus;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TripStateMachineTest {

    private final TripStateMachine stateMachine = new TripStateMachine();

    private Trip tripWithStatus(TripStatus status) {
        Vehicle vehicle = Vehicle.builder().id(1L).plateNumber("29A-12345").status(VehicleStatus.IN_USE).build();
        return Trip.builder().tripId(100L).status(status).vehicle(vehicle).build();
    }

    @Test
    void dispatched_to_cancelled_releasesVehicleToAvailable() {
        Trip trip = tripWithStatus(TripStatus.DISPATCHED);

        stateMachine.transition(trip, TripStatus.CANCELLED, 1L);

        assertThat(trip.getStatus()).isEqualTo(TripStatus.CANCELLED);
        assertThat(trip.getCancelledAt()).isNotNull();
        assertThat(trip.getVehicle().getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
    }

    @Test
    void inProgress_to_cancelled_isRejected() {
        Trip trip = tripWithStatus(TripStatus.IN_PROGRESS);

        assertThatThrownBy(() -> stateMachine.transition(trip, TripStatus.CANCELLED, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRIP_TRANSITION);
    }

    @Test
    void completed_to_cancelled_isRejected() {
        Trip trip = tripWithStatus(TripStatus.COMPLETED);

        assertThatThrownBy(() -> stateMachine.transition(trip, TripStatus.CANCELLED, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_COMPLETED);
    }

    @Test
    void cancelled_to_anything_isTerminal() {
        Trip trip = tripWithStatus(TripStatus.CANCELLED);

        assertThatThrownBy(() -> stateMachine.transition(trip, TripStatus.DISPATCHED, 1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_TRIP_TRANSITION);
    }
}

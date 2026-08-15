package com.elog.integration;

import com.elog.dto.response.StopArriveResponse;
import com.elog.dto.response.TripStartResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.service.TripMonitoringService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class TripMonitoringServiceIntegrationTest {

    @Autowired TripMonitoringService tripMonitoringService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void l2Mon01StartPersistsDepartureAndReturnsFirstStop() {
        Fixture fixture = seedTrip(LocalDate.now(), "DISPATCHED", List.of("PENDING", "PENDING"));

        TripStartResponse response = tripMonitoringService.startTrip(fixture.tripId(), "driver01");
        Map<String, Object> trip = reloadTrip(fixture.tripId());

        assertThat(trip.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(trip.get("actual_departure_time")).isNotNull();
        assertThat(response.getFirstStopCode()).isEqualTo("KH0138");
        assertThat(response.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    void l2Mon02FutureDeliveryDateLeavesTripDispatched() {
        Fixture fixture = seedTrip(LocalDate.now().plusDays(1), "DISPATCHED", List.of("PENDING"));

        assertThatThrownBy(() -> tripMonitoringService.startTrip(fixture.tripId(), "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));

        Map<String, Object> trip = reloadTrip(fixture.tripId());
        assertThat(trip.get("status")).isEqualTo("DISPATCHED");
        assertThat(trip.get("actual_departure_time")).isNull();
    }

    @Test
    void l2Mon03OnTimeArrivalPersistsWithoutTimeException() {
        Fixture fixture = seedTrip(LocalDate.now(), "IN_PROGRESS", List.of("PENDING"));
        long stopId = fixture.stopIds().getFirst();
        jdbc.update("UPDATE trip_stops SET planned_eta = ? WHERE trip_stop_id = ?",
                LocalDateTime.now().minusMinutes(5), stopId);

        StopArriveResponse response = tripMonitoringService.arriveAtStop(stopId, "driver01");
        Map<String, Object> stop = reloadStop(stopId);

        assertThat(stop.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(stop.get("actual_arrival_time")).isNotNull();
        assertThat(response.getDelayMinutes()).isBetween(4L, 6L);
        assertThat(countExceptions(stopId)).isZero();
    }

    @Test
    void l2Mon04LateArrivalCreatesOneExceptionAndMarksStopException() {
        Fixture fixture = seedTrip(LocalDate.now(), "IN_PROGRESS", List.of("PENDING"));
        long stopId = fixture.stopIds().getFirst();
        jdbc.update("UPDATE trip_stops SET planned_eta = ? WHERE trip_stop_id = ?",
                LocalDateTime.now().minusMinutes(30), stopId);

        StopArriveResponse response = tripMonitoringService.arriveAtStop(stopId, "driver01");
        Map<String, Object> stop = reloadStop(stopId);

        assertThat(stop.get("status")).isEqualTo("EXCEPTION");
        assertThat(stop.get("actual_arrival_time")).isNotNull();
        assertThat(response.isTimeExceptionFlagged()).isTrue();
        assertThat(countExceptions(stopId)).isEqualTo(1);
    }

    @Test
    void l2Mon05SecondStopCannotBypassPendingFirstStop() {
        Fixture fixture = seedTrip(LocalDate.now(), "IN_PROGRESS", List.of("PENDING", "PENDING"));
        long secondStopId = fixture.stopIds().get(1);

        assertThatThrownBy(() -> tripMonitoringService.arriveAtStop(secondStopId, "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PREVIOUS_STOP_NOT_DONE));

        assertThat(reloadStop(secondStopId).get("status")).isEqualTo("PENDING");
    }

    @Test
    void l2Mon06CompletedStopCannotBeArrivedAgain() {
        Fixture fixture = seedTrip(LocalDate.now(), "IN_PROGRESS", List.of("COMPLETED"));
        long stopId = fixture.stopIds().getFirst();
        Map<String, Object> before = reloadStop(stopId);

        assertThatThrownBy(() -> tripMonitoringService.arriveAtStop(stopId, "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STOP_ALREADY_DONE));

        assertThat(reloadStop(stopId)).containsAllEntriesOf(before);
    }

    @Test
    void l2Mon07CompletingFirstOfThreeStopsKeepsTripInProgress() {
        Fixture fixture = seedTrip(LocalDate.now(), "IN_PROGRESS",
                List.of("IN_PROGRESS", "PENDING", "PENDING"));
        long firstStopId = fixture.stopIds().getFirst();

        tripMonitoringService.completeStop(firstStopId, "driver01");
        Map<String, Object> stop = reloadStop(firstStopId);

        assertThat(stop.get("status")).isEqualTo("COMPLETED");
        assertThat(stop.get("actual_departure_time")).isNotNull();
        assertThat(reloadTrip(fixture.tripId()).get("status")).isEqualTo("IN_PROGRESS");
    }

    private Fixture seedTrip(LocalDate deliveryDate, String tripStatus, List<String> stopStatuses) {
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, 1.000000, 100.000, ?, 0, 'VALIDATED')
                """, deliveryDate, stopStatuses.size());
        long draftId = lastInsertId();
        jdbc.update("""
                INSERT INTO trips
                    (trip_draft_id, route_id, vehicle_id, driver_id, delivery_date,
                     status, total_weight_kg, total_volume_m3, created_by)
                VALUES (?, 1, 1, 6, ?, ?, 100.000, 1.000000, 2)
                """, draftId, deliveryDate, tripStatus);
        long tripId = lastInsertId();

        List<Long> stopIds = new ArrayList<>();
        for (int index = 0; index < stopStatuses.size(); index++) {
            jdbc.update("""
                    INSERT INTO trip_stops
                        (trip_id, route_stop_id, sequence_order, planned_eta, status,
                         stop_weight_kg, stop_volume_m3)
                    VALUES (?, ?, ?, ?, ?, 10.000, 0.100000)
                    """, tripId, 2 + index, index + 1,
                    deliveryDate.atTime(9 + index, 0), stopStatuses.get(index));
            stopIds.add(lastInsertId());
        }
        return new Fixture(tripId, stopIds);
    }

    private long lastInsertId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Map<String, Object> reloadTrip(long tripId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForMap("""
                SELECT status, actual_departure_time, completed_at FROM trips WHERE trip_id = ?
                """, tripId);
    }

    private Map<String, Object> reloadStop(long stopId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForMap("""
                SELECT status, actual_arrival_time, actual_departure_time
                FROM trip_stops WHERE trip_stop_id = ?
                """, stopId);
    }

    private int countExceptions(long stopId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM delivery_exceptions
                WHERE trip_stop_id = ? AND exception_type = 'TIME_EXCEPTION'
                """, Integer.class, stopId);
    }

    private record Fixture(long tripId, List<Long> stopIds) {}
}


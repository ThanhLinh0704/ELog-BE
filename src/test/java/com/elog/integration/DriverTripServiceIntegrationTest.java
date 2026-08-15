package com.elog.integration;

import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.service.DriverTripService;
import com.elog.service.TripOutcomeHistoryService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class DriverTripServiceIntegrationTest {

    private static final AtomicInteger DAY_SEQUENCE = new AtomicInteger(1);

    @Autowired DriverTripService driverTripService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @MockBean TripOutcomeHistoryService tripOutcomeHistoryService;

    @Test
    void l2Drv01DispatchedExecutionStartsWithoutChangingTripOrVehicle() {
        Fixture fixture = seedExecution("DISPATCHED", null);
        String tripStatusBefore = scalar("SELECT status FROM trips WHERE trip_id = ?", fixture.tripId());
        String vehicleStatusBefore = scalar("SELECT status FROM vehicles WHERE id = ?", 1L);

        driverTripService.startTrip(fixture.executionId(), "driver01");
        Map<String, Object> execution = reloadExecution(fixture.executionId());

        assertThat(execution.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(execution.get("started_at")).isNotNull();
        assertThat(scalar("SELECT status FROM trips WHERE trip_id = ?", fixture.tripId())).isEqualTo(tripStatusBefore);
        assertThat(scalar("SELECT status FROM vehicles WHERE id = ?", 1L)).isEqualTo(vehicleStatusBefore);
    }

    @Test
    void l2Drv02DifferentDriverCannotStartExecution() {
        Fixture fixture = seedExecution("DISPATCHED", null);

        assertThatThrownBy(() -> driverTripService.startTrip(fixture.executionId(), "driver02"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED_ACCESS));

        Map<String, Object> execution = reloadExecution(fixture.executionId());
        assertThat(execution.get("status")).isEqualTo("DISPATCHED");
        assertThat(execution.get("started_at")).isNull();
    }

    @Test
    void l2Drv03AlreadyStartedExecutionIsUnchanged() {
        Fixture fixture = seedExecution("IN_PROGRESS", "2026-08-14 07:30:00");

        assertThatThrownBy(() -> driverTripService.startTrip(fixture.executionId(), "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));

        Map<String, Object> execution = reloadExecution(fixture.executionId());
        assertThat(execution.get("status")).isEqualTo("IN_PROGRESS");
        assertThat(execution.get("started_at").toString()).startsWith("2026-08-14T07:30");
    }

    @Test
    void l2Drv07TerminalResultsCreateOutcomeWithExactAggregates() {
        Fixture fixture = seedExecution("IN_PROGRESS", "2026-08-14 07:30:00");
        seedOrderResult(fixture, "DRV07-A", "DELIVERED");
        seedOrderResult(fixture, "DRV07-B", "FAILED");
        String vehicleStatusBefore = scalar("SELECT status FROM vehicles WHERE id = ?", 1L);

        driverTripService.completeTrip(fixture.executionId(), "driver01");
        Map<String, Object> execution = reloadExecution(fixture.executionId());
        Map<String, Object> outcome = jdbc.queryForMap("""
                SELECT status, total_orders, delivered_count, failed_count, partial_count
                FROM trip_outcomes WHERE trip_execution_id = ?
                """, fixture.executionId());

        assertThat(execution.get("status")).isEqualTo("COMPLETED_WITH_EXCEPTIONS");
        assertThat(execution.get("completed_at")).isNotNull();
        assertThat(outcome.get("status")).isEqualTo("SUBMITTED");
        assertThat(((Number) outcome.get("total_orders")).intValue()).isEqualTo(2);
        assertThat(((Number) outcome.get("delivered_count")).intValue()).isEqualTo(1);
        assertThat(((Number) outcome.get("failed_count")).intValue()).isEqualTo(1);
        assertThat(((Number) outcome.get("partial_count")).intValue()).isZero();
        assertThat(scalar("SELECT status FROM vehicles WHERE id = ?", 1L)).isEqualTo(vehicleStatusBefore);
    }

    @Test
    void l2Drv08PendingResultPreventsCompletionAndOutcomeInsert() {
        Fixture fixture = seedExecution("IN_PROGRESS", "2026-08-14 07:30:00");
        seedOrderResult(fixture, "DRV08-A", "PENDING");

        assertThatThrownBy(() -> driverTripService.completeTrip(fixture.executionId(), "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));

        assertThat(reloadExecution(fixture.executionId()).get("status")).isEqualTo("IN_PROGRESS");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM trip_outcomes WHERE trip_execution_id = ?",
                Integer.class, fixture.executionId())).isZero();
    }

    private Fixture seedExecution(String executionStatus, String startedAt) {
        LocalDate today = LocalDate.now().minusDays(DAY_SEQUENCE.getAndIncrement());
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, 1.000000, 100.000, 1, 0, 'VALIDATED')
                """, today);
        long draftId = lastInsertId();

        jdbc.update("""
                INSERT INTO trip_draft_stops
                    (trip_draft_id, route_stop_id, store_id, sequence_no, is_active, order_count)
                VALUES (?, 2, 136, 1, 1, 2)
                """, draftId);
        long stopId = lastInsertId();

        jdbc.update("""
                INSERT INTO trips
                    (trip_draft_id, route_id, vehicle_id, driver_id, delivery_date,
                     status, total_weight_kg, total_volume_m3, created_by)
                VALUES (?, 1, 1, 6, ?, 'DISPATCHED', 100.000, 1.000000, 2)
                """, draftId, today);
        long tripId = lastInsertId();

        jdbc.update("""
                INSERT INTO trip_executions
                    (trip_id, driver_id, status, assignment_version, started_at)
                VALUES (?, 6, ?, 1, ?)
                """, tripId, executionStatus, startedAt);
        return new Fixture(draftId, stopId, tripId, lastInsertId(), today);
    }

    private void seedOrderResult(Fixture fixture, String orderRef, String status) {
        jdbc.update("""
                INSERT INTO import_batches
                    (delivery_date, file_name, uploaded_by, total_rows, accepted_rows,
                     rejected_rows, status, is_active)
                VALUES (?, ?, 2, 1, 1, 0, 'COMPLETED', 0)
                """, LocalDate.now(), orderRef + ".xlsx");
        long batchId = lastInsertId();

        jdbc.update("""
                INSERT INTO orders
                    (import_batch_id, order_ref, store_id, delivery_date, status, trip_draft_id)
                VALUES (?, ?, 136, ?, 'ACCEPTED', ?)
                """, batchId, orderRef, fixture.deliveryDate(), fixture.draftId());
        long orderId = lastInsertId();

        jdbc.update("""
                INSERT INTO delivery_order_results
                    (trip_execution_id, order_id, stop_id, status, updated_at)
                VALUES (?, ?, ?, ?, NOW())
                """, fixture.executionId(), orderId, fixture.stopId(), status);
    }

    private long lastInsertId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Map<String, Object> reloadExecution(long executionId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForMap("""
                SELECT status, started_at, completed_at
                FROM trip_executions WHERE id = ?
                """, executionId);
    }

    private String scalar(String sql, Object value) {
        return jdbc.queryForObject(sql, String.class, value);
    }

    private record Fixture(long draftId, long stopId, long tripId, long executionId, LocalDate deliveryDate) {}
}


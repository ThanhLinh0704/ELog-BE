package com.elog.integration;

import com.elog.dto.request.exception.RejectStopRequest;
import com.elog.dto.response.exception.DeliveryExceptionResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.service.ExceptionService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class ExceptionServiceIntegrationTest {

    @Autowired ExceptionService exceptionService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    private static final AtomicInteger DAY_SEQUENCE = new AtomicInteger(0);

    @Test
    void l2Exc01PersistsDeliveryRejectionAndMarksStopException() {
        Fixture fixture = seedTrip(List.of("IN_PROGRESS"));
        long stopId = fixture.stopIds().getFirst();

        DeliveryExceptionResponse response = exceptionService.rejectStop(
                stopId, rejection("STORE_CLOSED", "Closed on arrival"), "driver01");

        assertThat(response.getExceptionId()).isNotNull();
        assertThat(response.getExceptionType()).isEqualTo("DELIVERY_REJECTION");
        assertThat(response.getRejectionType()).isEqualTo("STORE_CLOSED");
        assertThat(reloadStopStatus(stopId)).isEqualTo("EXCEPTION");
        assertThat(exceptionRows(stopId)).singleElement()
                .satisfies(row -> {
                    assertThat(row.get("exception_type")).isEqualTo("DELIVERY_REJECTION");
                    assertThat(row.get("description")).isEqualTo("[STORE_CLOSED] Closed on arrival");
                });
    }

    @Test
    void l2Exc02LastRejectedStopAutoCompletesTrip() {
        Fixture fixture = seedTrip(List.of("COMPLETED", "COMPLETED", "IN_PROGRESS"));
        long lastStopId = fixture.stopIds().get(2);

        exceptionService.rejectStop(lastStopId, rejection("CUSTOMER_REFUSED", "Refused"), "driver01");

        assertThat(reloadStopStatus(lastStopId)).isEqualTo("EXCEPTION");
        Map<String, Object> trip = reloadTrip(fixture.tripId());
        assertThat(trip.get("status")).isEqualTo("COMPLETED");
        assertThat(trip.get("completed_at")).isNotNull();
        assertThat(terminalStopCount(fixture.tripId())).isEqualTo(3);
    }

    @Test
    void l2Exc03WrongDriverCannotRejectStop() {
        Fixture fixture = seedTrip(List.of("IN_PROGRESS"));
        long stopId = fixture.stopIds().getFirst();

        assertThatThrownBy(() -> exceptionService.rejectStop(
                stopId, rejection("STORE_CLOSED", null), "driver02"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_YOUR_TRIP));

        assertThat(reloadStopStatus(stopId)).isEqualTo("IN_PROGRESS");
        assertThat(exceptionRows(stopId)).isEmpty();
    }

    @Test
    void l2Exc04PendingStopCannotBeRejected() {
        Fixture fixture = seedTrip(List.of("PENDING"));
        long stopId = fixture.stopIds().getFirst();

        assertThatThrownBy(() -> exceptionService.rejectStop(
                stopId, rejection("STORE_CLOSED", null), "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STOP_NOT_IN_PROGRESS));

        assertThat(reloadStopStatus(stopId)).isEqualTo("PENDING");
        assertThat(exceptionRows(stopId)).isEmpty();
    }

    @Test
    void l2Exc05DuplicateUnresolvedRejectionIsIdempotentlyRejected() {
        Fixture fixture = seedTrip(List.of("IN_PROGRESS"));
        long stopId = fixture.stopIds().getFirst();
        jdbc.update("""
                INSERT INTO delivery_exceptions
                    (trip_stop_id, exception_type, reported_by, description)
                VALUES (?, 'DELIVERY_REJECTION', 6, '[STORE_CLOSED] Existing')
                """, stopId);
        long existingId = lastInsertId();

        assertThatThrownBy(() -> exceptionService.rejectStop(
                stopId, rejection("STORE_CLOSED", "Duplicate"), "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REJECTION_ALREADY_RECORDED));

        assertThat(exceptionRows(stopId)).singleElement()
                .satisfies(row -> {
                    assertThat(((Number) row.get("exception_id")).longValue()).isEqualTo(existingId);
                    assertThat(row.get("description")).isEqualTo("[STORE_CLOSED] Existing");
                });
        assertThat(reloadStopStatus(stopId)).isEqualTo("IN_PROGRESS");
    }

    @Test
    void l2Exc06OtherRejectionRequiresDescription() {
        Fixture fixture = seedTrip(List.of("IN_PROGRESS"));
        long stopId = fixture.stopIds().getFirst();

        assertThatThrownBy(() -> exceptionService.rejectStop(
                stopId, rejection("OTHER", "  "), "driver01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));

        assertThat(reloadStopStatus(stopId)).isEqualTo("IN_PROGRESS");
        assertThat(exceptionRows(stopId)).isEmpty();
    }

    private Fixture seedTrip(List<String> stopStatuses) {
        LocalDate deliveryDate = LocalDate.now().plusYears(30).plusDays(DAY_SEQUENCE.getAndIncrement());
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
                VALUES (?, 1, 1, 6, ?, 'IN_PROGRESS', 100.000, 1.000000, 2)
                """, draftId, deliveryDate);
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

    private RejectStopRequest rejection(String type, String description) {
        RejectStopRequest request = new RejectStopRequest();
        request.setRejectionType(type);
        request.setDescription(description);
        return request;
    }

    private long lastInsertId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String reloadStopStatus(long stopId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForObject(
                "SELECT status FROM trip_stops WHERE trip_stop_id = ?", String.class, stopId);
    }

    private Map<String, Object> reloadTrip(long tripId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForMap(
                "SELECT status, completed_at FROM trips WHERE trip_id = ?", tripId);
    }

    private List<Map<String, Object>> exceptionRows(long stopId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForList("""
                SELECT exception_id, exception_type, description, resolved_at
                FROM delivery_exceptions WHERE trip_stop_id = ? ORDER BY exception_id
                """, stopId);
    }

    private int terminalStopCount(long tripId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM trip_stops
                WHERE trip_id = ? AND status IN ('COMPLETED', 'EXCEPTION')
                """, Integer.class, tripId);
    }

    private record Fixture(long tripId, List<Long> stopIds) {}
}


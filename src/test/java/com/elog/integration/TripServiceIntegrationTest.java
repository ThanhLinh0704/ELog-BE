package com.elog.integration;

import com.elog.dto.request.trip.TripAssignRequest;
import com.elog.dto.request.trip.TripSplitAssignRequest;
import com.elog.dto.response.trip.TripResponse;
import com.elog.dto.response.trip.TripSplitResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.service.TripService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class TripServiceIntegrationTest {

    @Autowired TripService tripService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void l2Dsp01AssignmentPersistsTripAndStops() {
        Draft draft = seedDraft(LocalDate.now().plusYears(20), 2);

        TripResponse response = tripService.assignVehicleAndDriver(
                draft.id(), new TripAssignRequest(1L, 6L), "admin");
        flushClear();

        assertThat(response.getTripId()).isNotNull();
        assertThat(jdbc.queryForObject("SELECT status FROM trips WHERE trip_id=?", String.class,
                response.getTripId())).isEqualTo("VALIDATED");
        assertThat(count("SELECT COUNT(*) FROM trip_stops WHERE trip_id=?", response.getTripId())).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT status FROM trip_drafts WHERE id=?", String.class,
                draft.id())).isEqualTo("VALIDATED");
    }

    @Test
    void l2Dsp02IncompatibleLicenseRollsBackAssignment() {
        Draft draft = seedDraft(LocalDate.now().plusYears(20).plusDays(1), 1);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(
                draft.id(), new TripAssignRequest(8L, 6L), "admin"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DRIVER_LICENSE_INCOMPATIBLE));

        assertThat(count("SELECT COUNT(*) FROM trips WHERE trip_draft_id=?", draft.id())).isZero();
        assertThat(count("SELECT COUNT(*) FROM trip_stops ts JOIN trips t ON t.trip_id=ts.trip_id WHERE t.trip_draft_id=?",
                draft.id())).isZero();
    }

    @Test
    void l2Dsp03BusyVehicleIsRejectedWithoutTargetTrip() {
        LocalDate date = LocalDate.now().plusYears(20).plusDays(2);
        Draft occupied = seedDraft(date.plusDays(100), 1);
        insertTrip(occupied.id(), date, 1, 6);
        Draft target = seedDraft(date, 1);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(
                target.id(), new TripAssignRequest(1L, 7L), "admin"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VEHICLE_CONFLICT));

        assertThat(count("SELECT COUNT(*) FROM trips WHERE trip_draft_id=?", target.id())).isZero();
    }

    @Test
    void l2Dsp04BusyDriverIsRejectedWithoutTargetTrip() {
        LocalDate date = LocalDate.now().plusYears(20).plusDays(3);
        Draft occupied = seedDraft(date.plusDays(100), 1);
        insertTrip(occupied.id(), date, 6, 6);
        Draft target = seedDraft(date, 1);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(
                target.id(), new TripAssignRequest(1L, 6L), "admin"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DRIVER_CONFLICT));

        assertThat(count("SELECT COUNT(*) FROM trips WHERE trip_draft_id=?", target.id())).isZero();
    }

    @Test
    void l2Dsp05SplitPersistsTwoOrderedStopGroups() {
        Draft draft = seedDraft(LocalDate.now().plusYears(20).plusDays(4), 4);
        TripSplitAssignRequest request = new TripSplitAssignRequest(List.of(
                new TripSplitAssignRequest.SplitAssignment(1L, 6L, draft.stopIds().subList(0, 2)),
                new TripSplitAssignRequest.SplitAssignment(6L, 7L, draft.stopIds().subList(2, 4))));

        TripSplitResponse response = tripService.assignSplit(draft.id(), request, "admin");
        flushClear();

        assertThat(response.getTripsCreated()).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM trips WHERE trip_draft_id=?", draft.id())).isEqualTo(2);
        assertThat(jdbc.queryForList("""
                SELECT COUNT(*) stop_count FROM trip_stops ts JOIN trips t ON t.trip_id=ts.trip_id
                WHERE t.trip_draft_id=? GROUP BY ts.trip_id ORDER BY ts.trip_id
                """, draft.id())).extracting(row -> ((Number) row.get("stop_count")).intValue())
                .containsExactly(2, 2);
        assertThat(jdbc.queryForObject("SELECT status FROM trip_drafts WHERE id=?", String.class,
                draft.id())).isEqualTo("VALIDATED");
    }

    private Draft seedDraft(LocalDate date, int stopCount) {
        jdbc.update("UPDATE stores SET max_allowed_vehicle_weight = NULL");
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, 1.000000, 100.000, ?, 0, 'VALIDATED')
                """, date, stopCount);
        long id = lastId();
        List<Long> stopIds = new ArrayList<>();
        for (int index = 0; index < stopCount; index++) {
            long routeStopId = 2L + index;
            Long storeId = jdbc.queryForObject(
                    "SELECT store_id FROM route_stops WHERE id=?", Long.class, routeStopId);
            jdbc.update("""
                    INSERT INTO trip_draft_stops
                        (trip_draft_id, route_stop_id, store_id, sequence_no, is_active, order_count)
                    VALUES (?, ?, ?, ?, TRUE, 0)
                    """, id, routeStopId, storeId, index + 1);
            stopIds.add(lastId());
        }
        return new Draft(id, stopIds);
    }

    private void insertTrip(long draftId, LocalDate date, long vehicleId, long driverId) {
        jdbc.update("""
                INSERT INTO trips
                    (trip_draft_id, route_id, vehicle_id, driver_id, delivery_date,
                     status, total_weight_kg, total_volume_m3, created_by)
                VALUES (?, 1, ?, ?, ?, 'VALIDATED', 100.000, 1.000000, 2)
                """, draftId, vehicleId, driverId, date);
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int count(String sql, Object argument) {
        flushClear();
        return jdbc.queryForObject(sql, Integer.class, argument);
    }

    private void flushClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private record Draft(long id, List<Long> stopIds) {}
}


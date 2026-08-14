package com.elog.integration;

import com.elog.service.impl.TimeExceptionDetectionJob;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class TimeExceptionDetectionJobReport5IntegrationTest {

    @Autowired TimeExceptionDetectionJob job;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void l2Ted01OneOverdueStopCreatesOneSystemException() {
        long stopId = seedStops(List.of(LocalDateTime.now().minusMinutes(26))).getFirst();

        job.detectTimeExceptions();
        Map<String, Object> exception = exceptionFor(stopId);
        long delay = describedDelayMinutes(exception);

        assertThat(status(stopId)).isEqualTo("EXCEPTION");
        assertThat(exception.get("exception_type")).isEqualTo("TIME_EXCEPTION");
        assertThat(((Number) exception.get("reported_by")).longValue()).isEqualTo(1L);
        assertThat(delay).isBetween(24L, 27L);
        assertThat(exception.get("description").toString())
                .contains(delay + " minutes past planned ETA");
    }

    @Test
    void l2Ted02ThreeOverdueStopsEachCreateOneException() {
        List<Long> stopIds = seedStops(List.of(
                LocalDateTime.now().minusMinutes(21),
                LocalDateTime.now().minusMinutes(31),
                LocalDateTime.now().minusMinutes(46)));

        job.detectTimeExceptions();

        assertThat(stopIds).allSatisfy(stopId -> {
            assertThat(status(stopId)).isEqualTo("EXCEPTION");
            assertThat(exceptionCount(stopId)).isEqualTo(1);
        });
        List<Long> delays = stopIds.stream().map(stopId -> {
            Map<String, Object> exception = exceptionFor(stopId);
            return describedDelayMinutes(exception);
        }).sorted().toList();
        assertThat(delays.get(0)).isBetween(19L, 22L);
        assertThat(delays.get(1)).isBetween(29L, 32L);
        assertThat(delays.get(2)).isBetween(44L, 47L);
    }

    @Test
    void l2Ted03SecondRunDoesNotDuplicateExistingException() {
        long stopId = seedStops(List.of(LocalDateTime.now().minusMinutes(26))).getFirst();
        job.detectTimeExceptions();
        long exceptionId = ((Number) exceptionFor(stopId).get("exception_id")).longValue();

        job.detectTimeExceptions();

        assertThat(exceptionCount(stopId)).isEqualTo(1);
        assertThat(((Number) exceptionFor(stopId).get("exception_id")).longValue()).isEqualTo(exceptionId);
        assertThat(status(stopId)).isEqualTo("EXCEPTION");
    }

    @Test
    void l2Ted04FutureStopsProduceNoSideEffects() {
        List<Long> stopIds = seedStops(List.of(
                LocalDateTime.now().plusMinutes(30),
                LocalDateTime.now().plusMinutes(60)));

        job.detectTimeExceptions();

        assertThat(stopIds).allSatisfy(stopId -> {
            assertThat(status(stopId)).isEqualTo("PENDING");
            assertThat(exceptionCount(stopId)).isZero();
        });
    }

    private List<Long> seedStops(List<LocalDateTime> plannedEtas) {
        LocalDate date = LocalDate.now();
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, 1.000000, 100.000, ?, 0, 'VALIDATED')
                """, date, plannedEtas.size());
        long draftId = lastInsertId();
        jdbc.update("""
                INSERT INTO trips
                    (trip_draft_id, route_id, vehicle_id, driver_id, delivery_date,
                     status, total_weight_kg, total_volume_m3, created_by)
                VALUES (?, 1, 1, 6, ?, 'IN_PROGRESS', 100.000, 1.000000, 2)
                """, draftId, date);
        long tripId = lastInsertId();

        List<Long> stopIds = new ArrayList<>();
        for (int index = 0; index < plannedEtas.size(); index++) {
            jdbc.update("""
                    INSERT INTO trip_stops
                        (trip_id, route_stop_id, sequence_order, planned_eta, status,
                         stop_weight_kg, stop_volume_m3)
                    VALUES (?, ?, ?, ?, 'PENDING', 10.000, 0.100000)
                    """, tripId, 2 + index, index + 1, plannedEtas.get(index));
            stopIds.add(lastInsertId());
        }
        return stopIds;
    }

    private long lastInsertId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private String status(long stopId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForObject("SELECT status FROM trip_stops WHERE trip_stop_id = ?",
                String.class, stopId);
    }

    private int exceptionCount(long stopId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM delivery_exceptions
                WHERE trip_stop_id = ? AND exception_type = 'TIME_EXCEPTION'
                """, Integer.class, stopId);
    }

    private Map<String, Object> exceptionFor(long stopId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForMap("""
                SELECT exception_id, exception_type, reported_by, description, created_at
                FROM delivery_exceptions WHERE trip_stop_id = ? AND exception_type = 'TIME_EXCEPTION'
                """, stopId);
    }

    private long describedDelayMinutes(Map<String, Object> exception) {
        Matcher matcher = Pattern.compile("is (\\d+) minutes past planned ETA")
                .matcher(exception.get("description").toString());
        assertThat(matcher.find()).isTrue();
        return Long.parseLong(matcher.group(1));
    }
}

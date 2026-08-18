package com.elog.integration;

import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.service.TripOutcomeHistoryService;
import com.elog.service.TripOutcomeService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class TripOutcomeServiceIntegrationTest {

    @Autowired TripOutcomeService tripOutcomeService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @MockBean TripOutcomeHistoryService tripOutcomeHistoryService;

    @Test
    void l2Toc04SubmittedOutcomeCommitsValidationAuditFields() {
        long outcomeId = seedOutcome("SUBMITTED", 1, LocalDate.of(2037, 1, 4));

        tripOutcomeService.validateOutcome(outcomeId, "dispatcher01");
        Map<String, Object> row = reload(outcomeId);

        assertThat(row.get("status")).isEqualTo("VALIDATED");
        assertThat(row.get("validated_by")).isEqualTo("dispatcher01");
        assertThat(row.get("validated_at")).isNotNull();
    }

    @Test
    void l2Toc05CorrectedOutcomeCanBeValidated() {
        long outcomeId = seedOutcome("NEEDS_CORRECTION", 2, LocalDate.of(2037, 1, 5));

        tripOutcomeService.validateOutcome(outcomeId, "dispatcher01");
        Map<String, Object> row = reload(outcomeId);

        assertThat(row.get("status")).isEqualTo("VALIDATED");
        assertThat(row.get("validated_by")).isEqualTo("dispatcher01");
        assertThat(row.get("validated_at")).isNotNull();
    }

    @Test
    void l2Toc06AmendmentPersistsCorrectionStateAndVersion() {
        long outcomeId = seedOutcome("VALIDATED", 2, LocalDate.of(2037, 1, 6));

        tripOutcomeService.amendOutcome(outcomeId, "Incorrect delivered total", "dispatcher01");
        Map<String, Object> row = reload(outcomeId);

        assertThat(row.get("status")).isEqualTo("NEEDS_CORRECTION");
        assertThat(row.get("amendment_reason")).isEqualTo("Incorrect delivered total");
        assertThat(((Number) row.get("version")).intValue()).isEqualTo(3);
    }

    @Test
    void l2Toc07BlankReasonLeavesPersistedOutcomeUnchanged() {
        long outcomeId = seedOutcome("VALIDATED", 2, LocalDate.of(2037, 1, 7));

        assertThatThrownBy(() -> tripOutcomeService.amendOutcome(outcomeId, "  ", "dispatcher01"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
        Map<String, Object> row = reload(outcomeId);

        assertThat(row.get("status")).isEqualTo("VALIDATED");
        assertThat(row.get("amendment_reason")).isNull();
        assertThat(((Number) row.get("version")).intValue()).isEqualTo(2);
    }

    private long seedOutcome(String status, int version, LocalDate deliveryDate) {
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, 1.000000, 100.000, 1, 0, 'VALIDATED')
                """, deliveryDate);
        long draftId = lastInsertId();

        jdbc.update("""
                INSERT INTO trips
                    (trip_draft_id, route_id, vehicle_id, driver_id, delivery_date,
                     status, total_weight_kg, total_volume_m3, created_by)
                VALUES (?, 1, 1, 6, ?, 'COMPLETED', 100.000, 1.000000, 2)
                """, draftId, deliveryDate);
        long tripId = lastInsertId();

        jdbc.update("""
                INSERT INTO trip_executions (trip_id, driver_id, status, assignment_version)
                VALUES (?, 6, 'COMPLETED', 1)
                """, tripId);
        long executionId = lastInsertId();

        jdbc.update("""
                INSERT INTO trip_outcomes
                    (trip_execution_id, status, total_orders, delivered_count,
                     failed_count, partial_count, submitted_at, version)
                VALUES (?, ?, 4, 4, 0, 0, NOW(), ?)
                """, executionId, status, version);
        return lastInsertId();
    }

    private long lastInsertId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Map<String, Object> reload(long outcomeId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForMap("""
                SELECT status, validated_at, validated_by, amendment_reason, version
                FROM trip_outcomes WHERE id = ?
                """, outcomeId);
    }
}


package com.elog.integration;

import com.elog.dto.response.trip.RecommendationResultResponse;
import com.elog.service.RecommendationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@ExtendWith(Report5L2EvidenceExtension.class)
class RecommendationServiceIntegrationTest {

    @Autowired RecommendationService recommendationService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired PlatformTransactionManager transactionManager;
    private final List<Long> createdDraftIds = new ArrayList<>();

    @AfterEach
    void removeCommittedPlanningEvents() {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        requiresNew.executeWithoutResult(status -> {
            createdDraftIds.forEach(id -> jdbc.update("DELETE FROM trip_planning_events WHERE trip_draft_id=?", id));
            createdDraftIds.forEach(id -> jdbc.update("DELETE FROM trips WHERE trip_draft_id=?", id));
            createdDraftIds.forEach(id -> jdbc.update("DELETE FROM trip_draft_stops WHERE trip_draft_id=?", id));
            for (int index = createdDraftIds.size() - 1; index >= 0; index--) {
                jdbc.update("DELETE FROM trip_drafts WHERE id=?", createdDraftIds.get(index));
            }
        });
    }

    @Test
    void l2Rcd01RanksAtMostThreeSingleVehiclePlansWithAuditEvents() {
        long draftId = seedDraft(LocalDate.now().plusYears(30), new BigDecimal("1"), new BigDecimal("100"));
        int eventsBefore = committedPlanningEvents(draftId);

        RecommendationResultResponse response = recommendationService.recommendTop3(draftId);
        flushClear();

        assertThat(response.getPlanType()).isEqualTo("SINGLE_VEHICLE");
        assertThat(response.getRecommendations()).hasSizeBetween(1, 3)
                .isSortedAccordingTo((left, right) -> right.getTotalScore().compareTo(left.getTotalScore()));
        assertThat(committedPlanningEvents(draftId)).isEqualTo(eventsBefore + 2);
    }

    @Test
    void l2Rcd02ExcludesBusyVehicleAndDriverWithAuditEvents() {
        LocalDate date = LocalDate.now().plusYears(30).plusDays(1);
        long draftId = seedDraft(date, new BigDecimal("1"), new BigDecimal("100"));
        long busyDraft = seedDraft(date.plusDays(100), new BigDecimal("1"), new BigDecimal("100"));
        jdbc.update("""
                INSERT INTO trips
                    (trip_draft_id, route_id, vehicle_id, driver_id, delivery_date,
                     status, total_weight_kg, total_volume_m3, created_by)
                VALUES (?, 1, 1, 6, ?, 'IN_PROGRESS', 100.000, 1.000000, 2)
                """, busyDraft, date);
        int eventsBefore = committedPlanningEvents(draftId);

        RecommendationResultResponse response = recommendationService.recommendTop3(draftId);
        flushClear();

        assertThat(response.getRecommendations()).isNotEmpty();
        assertThat(response.getRecommendations())
                .flatExtracting(plan -> plan.getVehicles())
                .noneMatch(vehicle -> vehicle.getVehicleId().equals(1L) || vehicle.getDriverId().equals(6L));
        assertThat(committedPlanningEvents(draftId)).isEqualTo(eventsBefore + 2);
    }

    @Test
    void l2Rcd03OversizedDraftReturnsNoPlanWithAuditEvents() {
        long draftId = seedDraft(LocalDate.now().plusYears(30).plusDays(2),
                new BigDecimal("1000"), new BigDecimal("1000000"));
        int eventsBefore = committedPlanningEvents(draftId);

        RecommendationResultResponse response = recommendationService.recommendTop3(draftId);
        flushClear();

        assertThat(response.getPlanType()).isEqualTo("NO_PLAN");
        assertThat(response.getRecommendations()).isEmpty();
        assertThat(response.getViolatedConstraints()).isNotEmpty();
        assertThat(committedPlanningEvents(draftId)).isEqualTo(eventsBefore + 2);
    }

    private long seedDraft(LocalDate date, BigDecimal volume, BigDecimal weight) {
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, ?, ?, 1, 0, 'VALIDATED')
                """, date, volume, weight);
        long draftId = lastId();
        createdDraftIds.add(draftId);
        Long storeId = jdbc.queryForObject("SELECT store_id FROM route_stops WHERE id=2", Long.class);
        jdbc.update("""
                INSERT INTO trip_draft_stops
                    (trip_draft_id, route_stop_id, store_id, sequence_no, is_active, order_count)
                VALUES (?, 2, ?, 1, TRUE, 0)
                """, draftId, storeId);
        return draftId;
    }

    private long lastId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int committedPlanningEvents(long draftId) {
        TransactionTemplate requiresNew = new TransactionTemplate(transactionManager);
        requiresNew.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return requiresNew.execute(status -> jdbc.queryForObject(
                "SELECT COUNT(*) FROM trip_planning_events WHERE trip_draft_id=?", Integer.class, draftId));
    }

    private void flushClear() {
        entityManager.clear();
    }
}


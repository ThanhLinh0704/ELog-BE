package com.elog.integration;

import com.elog.dto.response.vehicle.CapacityValidationResultResponse;
import com.elog.entity.ConstraintResult;
import com.elog.service.CapacityValidationService;
import com.elog.service.RecommendationService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class CapacityValidationServiceIntegrationTest {

    @Autowired CapacityValidationService capacityValidationService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @MockBean RecommendationService recommendationService;

    @Test
    void l2Cvd01WithinCapacityPersistsValidatedAuditFields() {
        long draftId = seedDraft(new BigDecimal("1.000000"), new BigDecimal("100.000"),
                LocalDate.of(2038, 1, 1));

        CapacityValidationResultResponse result = capacityValidationService.validate(draftId, "dispatcher01");
        Map<String, Object> row = reload(draftId);

        assertThat(result.isValidationPassed()).isTrue();
        assertThat(row.get("status")).isEqualTo("VALIDATED");
        assertThat(row.get("validated_at")).isNotNull();
        assertThat(((Number) row.get("validated_by")).longValue()).isEqualTo(2L);
    }

    @Test
    void l2Cvd02ExactNinetyPercentVolumeBoundaryIsEligible() {
        long draftId = seedDraft(new BigDecimal("9.000000"), new BigDecimal("100.000"),
                LocalDate.of(2038, 1, 2));

        CapacityValidationResultResponse result = capacityValidationService.validate(draftId, "dispatcher01");
        Map<String, Object> row = reload(draftId);

        assertThat(result.getEligibleVehicles()).anySatisfy(vehicle -> {
            assertThat(vehicle.getVehicleId()).isEqualTo(1L);
            assertThat(vehicle.getMaxVolumeM3()).isEqualByComparingTo("10.000");
        });
        assertThat(row.get("status")).isEqualTo("VALIDATED");
        assertThat(row.get("volume_check_result")).isEqualTo("PASS");
        assertThat(row.get("weight_check_result")).isEqualTo("PASS");
    }

    @Test
    void l2Cvd03LoadBeyondFleetKeepsDraftPlanned() {
        long draftId = seedDraft(new BigDecimal("1000.000000"), new BigDecimal("100000.000"),
                LocalDate.of(2038, 1, 3));
        when(recommendationService.isTwoVehicleFeasible(draftId)).thenReturn(false);

        CapacityValidationResultResponse result = capacityValidationService.validate(draftId, "dispatcher01");
        Map<String, Object> row = reload(draftId);

        assertThat(result.isValidationPassed()).isFalse();
        assertThat(row.get("status")).isEqualTo("PLANNED");
        assertThat(row.get("validated_at")).isNull();
        assertThat(row.get("validated_by")).isNull();
    }

    @Test
    void l2Cvd04StoreWeightLimitPersistsOverallConstraintResult() {
        long draftId = seedDraft(new BigDecimal("1.000000"), new BigDecimal("100.000"),
                LocalDate.of(2038, 1, 4));
        jdbc.update("UPDATE stores SET max_allowed_vehicle_weight = 100 WHERE id = 136");
        when(recommendationService.isTwoVehicleFeasible(draftId)).thenReturn(false);

        CapacityValidationResultResponse result = capacityValidationService.validate(draftId, "dispatcher01");
        Map<String, Object> row = reload(draftId);

        assertThat(result.isValidationPassed()).isFalse();
        assertThat(result.getIneligibleVehicles()).isNotEmpty();
        assertThat(result.getIneligibleVehicles()).allSatisfy(vehicle ->
                assertThat(vehicle.getFailureReason()).contains("exceeds store"));
        assertThat(row.get("status")).isEqualTo("PLANNED");
        assertThat(row.get("volume_check_result")).isEqualTo(ConstraintResult.PASS.name());
        assertThat(row.get("weight_check_result")).isEqualTo(ConstraintResult.PASS.name());
    }

    private long seedDraft(BigDecimal volume, BigDecimal weight, LocalDate deliveryDate) {
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, ?, ?, 1, 0, 'PLANNED')
                """, deliveryDate, volume, weight);
        long draftId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        jdbc.update("""
                INSERT INTO trip_draft_stops
                    (trip_draft_id, route_stop_id, store_id, sequence_no, is_active,
                     order_count, planned_eta)
                VALUES (?, 2, 136, 1, 1, 0, ?)
                """, draftId, deliveryDate.atTime(10, 0));
        return draftId;
    }

    private Map<String, Object> reload(long draftId) {
        entityManager.flush();
        entityManager.clear();
        return jdbc.queryForMap("""
                SELECT status, validated_at, validated_by, volume_check_result, weight_check_result
                FROM trip_drafts WHERE id = ?
                """, draftId);
    }
}


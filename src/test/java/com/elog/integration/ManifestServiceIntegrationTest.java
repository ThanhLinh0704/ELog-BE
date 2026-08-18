package com.elog.integration;

import com.elog.dto.response.trip.ManifestResponse;
import com.elog.service.ManifestService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class ManifestServiceIntegrationTest {

    @Autowired ManifestService manifestService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;

    @Test
    void l2Mnf01GenerationPersistsReversedStopsAndExactTotals() {
        long draftId = seedDraftWithTwoOrderItems(LocalDate.of(2039, 1, 1));

        ManifestResponse response = manifestService.generateManifest(draftId, "warehouse01");
        entityManager.flush();
        entityManager.clear();
        Map<String, Object> header = jdbc.queryForMap("""
                SELECT total_lines, total_weight_kg, total_volume_m3
                FROM manifests WHERE trip_draft_id = ?
                """, draftId);
        List<Map<String, Object>> lines = jdbc.queryForList("""
                SELECT lifo_sequence, stop_sequence_no, quantity, line_weight_kg, line_volume_m3
                FROM manifest_lines WHERE manifest_id = ? ORDER BY lifo_sequence
                """, response.getManifestId());

        assertThat(((Number) header.get("total_lines")).intValue()).isEqualTo(2);
        assertThat((BigDecimal) header.get("total_weight_kg")).isEqualByComparingTo("13.000");
        assertThat((BigDecimal) header.get("total_volume_m3")).isEqualByComparingTo("0.700000");
        assertThat(lines).extracting(row -> ((Number) row.get("stop_sequence_no")).intValue())
                .containsExactly(2, 1);
        assertThat(lines).extracting(row -> ((Number) row.get("lifo_sequence")).intValue())
                .containsExactly(1, 2);
    }

    @Test
    void l2Mnf02ReadReturnsPersistedLinesInLifoOrderWithoutMutation() {
        long draftId = seedDraftWithTwoOrderItems(LocalDate.of(2039, 1, 2));
        manifestService.generateManifest(draftId, "warehouse01");
        int manifestsBefore = count("SELECT COUNT(*) FROM manifests WHERE trip_draft_id = ?", draftId);
        int linesBefore = count("""
                SELECT COUNT(*) FROM manifest_lines ml
                JOIN manifests m ON m.manifest_id = ml.manifest_id
                WHERE m.trip_draft_id = ?
                """, draftId);

        ManifestResponse response = manifestService.getManifest(draftId);

        assertThat(response.getLines()).extracting(line -> line.getLifoSequence())
                .containsExactly(1, 2);
        assertThat(response.getLines()).extracting(line -> line.getStopSequenceNo())
                .containsExactly(2, 1);
        assertThat(count("SELECT COUNT(*) FROM manifests WHERE trip_draft_id = ?", draftId))
                .isEqualTo(manifestsBefore);
        assertThat(count("""
                SELECT COUNT(*) FROM manifest_lines ml
                JOIN manifests m ON m.manifest_id = ml.manifest_id
                WHERE m.trip_draft_id = ?
                """, draftId)).isEqualTo(linesBefore);
    }

    private long seedDraftWithTwoOrderItems(LocalDate deliveryDate) {
        jdbc.update("""
                INSERT INTO trip_drafts
                    (route_id, delivery_date, total_volume_m3, total_weight_kg,
                     active_stop_count, skipped_stop_count, status)
                VALUES (1, ?, 0.700000, 13.000, 2, 0, 'VALIDATED')
                """, deliveryDate);
        long draftId = lastInsertId();

        jdbc.update("""
                INSERT INTO trip_draft_stops
                    (trip_draft_id, route_stop_id, store_id, sequence_no, is_active, order_count)
                VALUES (?, 2, 136, 1, 1, 1)
                """, draftId);
        jdbc.update("""
                INSERT INTO trip_draft_stops
                    (trip_draft_id, route_stop_id, store_id, sequence_no, is_active, order_count)
                VALUES (?, 3, 174, 2, 1, 1)
                """, draftId);

        jdbc.update("""
                INSERT INTO import_batches
                    (delivery_date, file_name, uploaded_by, total_rows, accepted_rows,
                     rejected_rows, status, is_active)
                VALUES (?, ?, 3, 2, 2, 0, 'COMPLETED', 0)
                """, deliveryDate, "manifest-" + deliveryDate + ".xlsx");
        long batchId = lastInsertId();

        long orderOne = insertOrder(batchId, draftId, 136L, deliveryDate, "MNF-A");
        long orderTwo = insertOrder(batchId, draftId, 174L, deliveryDate, "MNF-B");
        insertItem(orderOne, 1L, "TOSTLR677WI", 2, "5.000", "0.250000");
        insertItem(orderTwo, 2L, "TOSTLR677WIMG", 1, "3.000", "0.200000");
        return draftId;
    }

    private long insertOrder(long batchId, long draftId, long storeId, LocalDate date, String ref) {
        jdbc.update("""
                INSERT INTO orders
                    (import_batch_id, order_ref, store_id, delivery_date, status, trip_draft_id)
                VALUES (?, ?, ?, ?, 'ACCEPTED', ?)
                """, batchId, ref + '-' + date, storeId, date, draftId);
        return lastInsertId();
    }

    private void insertItem(long orderId, long productId, String sku, int quantity,
                            String unitWeight, String unitVolume) {
        BigDecimal weight = new BigDecimal(unitWeight);
        BigDecimal volume = new BigDecimal(unitVolume);
        jdbc.update("""
                INSERT INTO order_items
                    (order_id, product_id, sku, quantity, unit_weight_kg, unit_volume_m3,
                     line_weight_kg, line_volume_m3)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, orderId, productId, sku, quantity, weight, volume,
                weight.multiply(BigDecimal.valueOf(quantity)),
                volume.multiply(BigDecimal.valueOf(quantity)));
    }

    private long lastInsertId() {
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private int count(String sql, long value) {
        return jdbc.queryForObject(sql, Integer.class, value);
    }
}


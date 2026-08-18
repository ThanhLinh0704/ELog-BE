package com.elog.integration;

import com.elog.dto.request.trip.RecalculateEtaRequest;
import com.elog.dto.response.trip.ConsolidateResponse;
import com.elog.dto.response.goong.RecalculateEtaResponse;
import com.elog.entity.TripDraft;
import com.elog.repository.TripDraftRepository;
import com.elog.service.TripDraftService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;

@SpringBootTest
@ActiveProfiles("dev")
@ExtendWith(Report5L2EvidenceExtension.class)
class TripDraftServiceIntegrationTest {

    @Autowired TripDraftService tripDraftService;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @SpyBean TripDraftRepository tripDraftRepository;
    private final List<LocalDate> dates = new ArrayList<>();

    @AfterEach
    void cleanup() {
        reset(tripDraftRepository);
        for (LocalDate date : dates) {
            jdbc.update("DELETE FROM trip_planning_events WHERE delivery_date=?", date);
            jdbc.update("UPDATE orders SET trip_draft_id=NULL WHERE delivery_date=?", date);
            jdbc.update("DELETE tds FROM trip_draft_stops tds JOIN trip_drafts td ON td.id=tds.trip_draft_id WHERE td.delivery_date=?", date);
            jdbc.update("DELETE FROM trips WHERE delivery_date=?", date);
            jdbc.update("DELETE FROM trip_drafts WHERE delivery_date=?", date);
            jdbc.update("DELETE oi FROM order_items oi JOIN orders o ON o.id=oi.order_id WHERE o.delivery_date=?", date);
            jdbc.update("DELETE FROM orders WHERE delivery_date=?", date);
            jdbc.update("DELETE FROM import_batches WHERE delivery_date=?", date);
        }
    }

    @Test
    void l2Tdc01ConsolidatesTenOrdersIntoTwoRouteDrafts() {
        LocalDate date = date(0);
        List<RouteStore> routeStores = twoRouteStores();
        seedAcceptedOrders(date, routeStores.get(0), 5, "A");
        seedAcceptedOrders(date, routeStores.get(1), 5, "B");

        ConsolidateResponse response = tripDraftService.consolidate(date);
        clear();

        assertThat(response.getTripDraftsCreatedOrUpdated()).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM trip_drafts WHERE delivery_date=?", date)).isEqualTo(2);
        assertThat(count("SELECT COUNT(*) FROM trip_draft_stops tds JOIN trip_drafts td ON td.id=tds.trip_draft_id WHERE td.delivery_date=?", date)).isGreaterThan(1);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE delivery_date=? AND trip_draft_id IS NOT NULL", date)).isEqualTo(10);
    }

    @Test
    void l2Tdc02RepositoryFailureRollsBackAllConsolidationWrites() {
        LocalDate date = date(1);
        seedAcceptedOrders(date, twoRouteStores().getFirst(), 5, "ROLLBACK");
        doThrow(new IllegalStateException("injected later draft write failure"))
                .when(tripDraftRepository).save(any(TripDraft.class));

        assertThatThrownBy(() -> tripDraftService.consolidate(date))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("injected later draft write failure");

        assertThat(count("SELECT COUNT(*) FROM trip_drafts WHERE delivery_date=?", date)).isZero();
        assertThat(count("SELECT COUNT(*) FROM trip_draft_stops tds JOIN trip_drafts td ON td.id=tds.trip_draft_id WHERE td.delivery_date=?", date)).isZero();
        assertThat(count("SELECT COUNT(*) FROM orders WHERE delivery_date=? AND trip_draft_id IS NOT NULL", date)).isZero();
        assertThat(count("SELECT COUNT(*) FROM orders WHERE delivery_date=? AND status='ACCEPTED'", date)).isEqualTo(5);
    }

    @Test
    void l2Tdc03RefreshesExistingDraftWithoutDuplicate() {
        LocalDate date = date(2);
        RouteStore route = twoRouteStores().getFirst();
        seedAcceptedOrders(date, route, 1, "FIRST");
        long firstDraftId = tripDraftService.consolidate(date).getTripDrafts().getFirst().getId();
        int originalStopRows = count("SELECT COUNT(*) FROM trip_draft_stops WHERE trip_draft_id=?", firstDraftId);
        seedAcceptedOrders(date, route, 2, "SECOND");

        ConsolidateResponse response = tripDraftService.consolidate(date);
        clear();

        assertThat(response.getTripDraftsCreatedOrUpdated()).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM trip_drafts WHERE delivery_date=?", date)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT id FROM trip_drafts WHERE delivery_date=?", Long.class, date)).isEqualTo(firstDraftId);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE delivery_date=? AND trip_draft_id=?", date, firstDraftId)).isEqualTo(3);
        assertThat(count("SELECT COUNT(*) FROM trip_draft_stops WHERE trip_draft_id=?", firstDraftId)).isEqualTo(originalStopRows);
    }

    @Test
    void l2Tdc04LeavesUnmappedOrdersWithoutDraftLink() {
        LocalDate date = date(3);
        RouteStore mapped = twoRouteStores().getFirst();
        Long unmappedStore = jdbc.queryForObject("""
                SELECT s.id FROM stores s WHERE NOT EXISTS
                (SELECT 1 FROM route_stops rs WHERE rs.store_id=s.id) LIMIT 1
                """, Long.class);
        seedAcceptedOrders(date, mapped, 8, "MAPPED");
        seedAcceptedOrders(date, new RouteStore(null, unmappedStore), 2, "UNMAPPED");

        ConsolidateResponse response = tripDraftService.consolidate(date);
        clear();

        assertThat(response.getTripDraftsCreatedOrUpdated()).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE delivery_date=? AND trip_draft_id IS NOT NULL", date)).isEqualTo(8);
        assertThat(count("SELECT COUNT(*) FROM orders WHERE delivery_date=? AND trip_draft_id IS NULL", date)).isEqualTo(2);
    }

    @Test
    void l2Tdc05RecalculatesEtaAndDepartureTogether() {
        LocalDate date = date(4);
        List<RouteStore> routes = twoRouteStores();
        jdbc.update("""
                INSERT INTO trip_drafts (route_id, delivery_date, total_volume_m3, total_weight_kg,
                    active_stop_count, skipped_stop_count, status)
                VALUES (?, ?, 1.000000, 100.000, 4, 0, 'PLANNED')
                """, routes.getFirst().routeId(), date);
        long draftId = lastId();
        List<Map<String,Object>> stops = jdbc.queryForList("SELECT id, store_id FROM route_stops WHERE route_id=? ORDER BY sequence_order LIMIT 4", routes.getFirst().routeId());
        assertThat(stops).hasSize(4);
        for (int index = 0; index < stops.size(); index++) {
            jdbc.update("""
                    INSERT INTO trip_draft_stops (trip_draft_id, route_stop_id, store_id, sequence_no, is_active, order_count)
                    VALUES (?, ?, ?, ?, TRUE, 0)
                    """, draftId, ((Number) stops.get(index).get("id")).longValue(),
                    ((Number) stops.get(index).get("store_id")).longValue(), index + 1);
        }

        RecalculateEtaResponse response = tripDraftService.recalculateEta(draftId,
                new RecalculateEtaRequest(LocalTime.of(8, 30)));
        clear();

        assertThat(response.getStops()).hasSize(4);
        assertThat(count("SELECT COUNT(*) FROM trip_draft_stops WHERE trip_draft_id=? AND planned_eta IS NOT NULL", draftId)).isEqualTo(4);
        assertThat(jdbc.queryForObject("SELECT planned_departure_time FROM trip_drafts WHERE id=?", LocalTime.class, draftId))
                .isEqualTo(LocalTime.of(8, 30));
    }

    private LocalDate date(int offset) {
        LocalDate result = LocalDate.now().plusYears(40).plusDays(offset);
        dates.add(result);
        return result;
    }

    private List<RouteStore> twoRouteStores() {
        List<Map<String,Object>> rows = jdbc.queryForList("""
                SELECT rs.route_id, rs.store_id FROM route_stops rs
                JOIN (SELECT route_id FROM route_stops GROUP BY route_id ORDER BY route_id LIMIT 2) chosen
                  ON chosen.route_id=rs.route_id
                GROUP BY rs.route_id, rs.store_id ORDER BY rs.route_id, MIN(rs.sequence_order)
                """);
        List<RouteStore> result = new ArrayList<>();
        long lastRoute = -1;
        for (Map<String,Object> row : rows) {
            long routeId = ((Number) row.get("route_id")).longValue();
            if (routeId != lastRoute) {
                result.add(new RouteStore(routeId, ((Number) row.get("store_id")).longValue()));
                lastRoute = routeId;
            }
        }
        assertThat(result).hasSize(2);
        return result;
    }

    private void seedAcceptedOrders(LocalDate date, RouteStore routeStore, int count, String prefix) {
        jdbc.update("""
                INSERT INTO import_batches (delivery_date, file_name, uploaded_by, total_rows, accepted_rows, rejected_rows, status, is_active)
                VALUES (?, ?, 2, ?, ?, 0, 'COMPLETED', TRUE)
                """, date, "report5-" + prefix + ".xlsx", count, count);
        long batchId = lastId();
        Long productId = jdbc.queryForObject("SELECT id FROM products ORDER BY id LIMIT 1", Long.class);
        for (int index = 0; index < count; index++) {
            jdbc.update("""
                    INSERT INTO orders (import_batch_id, order_ref, store_id, delivery_date, status)
                    VALUES (?, ?, ?, ?, 'ACCEPTED')
                    """, batchId, prefix + "-" + index, routeStore.storeId(), date);
            long orderId = lastId();
            jdbc.update("""
                    INSERT INTO order_items (order_id, product_id, sku, quantity, unit_weight_kg, unit_volume_m3, line_weight_kg, line_volume_m3)
                    VALUES (?, ?, 'R5-SKU', 1, 10.000, 1.000000, 10.000, 1.000000)
                    """, orderId, productId);
        }
    }

    private long lastId() { return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class); }
    private int count(String sql, Object... args) { return jdbc.queryForObject(sql, Integer.class, args); }
    private void clear() { entityManager.clear(); }
    private record RouteStore(Long routeId, Long storeId) {}
}


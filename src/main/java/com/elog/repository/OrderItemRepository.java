package com.elog.repository;

import com.elog.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN FETCH oi.product p " +
           "JOIN oi.order o " +
           "WHERE o.store.id = :storeId " +
           "AND o.tripDraft.id = :tripDraftId " +
           "ORDER BY p.sku ASC")
    List<OrderItem> findByStopForManifest(
            @Param("storeId") Long storeId,
            @Param("tripDraftId") Long tripDraftId);

    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN FETCH oi.product p " +
           "JOIN oi.order o " +
           "WHERE o.store.id IN :storeIds " +
           "AND o.tripDraft.id = :tripDraftId " +
           "ORDER BY p.sku ASC")
    List<OrderItem> findByStoreIdInAndTripDraftId(
            @Param("storeIds") java.util.Collection<Long> storeIds,
            @Param("tripDraftId") Long tripDraftId);

    void deleteByOrderId(Long orderId);

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);

    // ── Confirmed Dispatch Export ────────────────────────────────────────

    @Query("SELECT oi FROM OrderItem oi " +
           "JOIN FETCH oi.order o " +
           "JOIN FETCH o.store s " +
           "LEFT JOIN FETCH s.province " +
           "LEFT JOIN FETCH s.district " +
           "JOIN FETCH oi.product p " +
           "WHERE o.tripDraft.id IN :tripDraftIds")
    List<OrderItem> findByTripDraftIdInWithOrderAndProduct(
            @Param("tripDraftIds") java.util.Collection<Long> tripDraftIds);
}


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

    void deleteByOrderId(Long orderId);

    List<OrderItem> findByOrderId(Long orderId);

    List<OrderItem> findByOrderIdIn(List<Long> orderIds);
}


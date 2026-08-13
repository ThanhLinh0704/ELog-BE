package com.elog.repository;

import com.elog.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("SELECT o FROM Order o WHERE o.importBatch.id = :batchId AND o.orderRef = :orderRef AND o.store.id = :storeId")
    Optional<Order> findByBatchAndOrderRefAndStore(
            @Param("batchId") Long batchId,
            @Param("orderRef") String orderRef,
            @Param("storeId") Long storeId);

    @Query("SELECT o FROM Order o WHERE o.orderRef = :orderRef AND o.deliveryDate = :deliveryDate AND o.importBatch.isActive = true")
    Optional<Order> findActiveByOrderRefAndDeliveryDate(
            @Param("orderRef") String orderRef,
            @Param("deliveryDate") LocalDate deliveryDate);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.importBatch.id = :batchId")
    long countByBatchId(@Param("batchId") Long batchId);

    @Query("SELECT o FROM Order o " +
           "JOIN FETCH o.store s " +
           "JOIN FETCH o.items " +
           "WHERE o.deliveryDate = :deliveryDate AND o.status = :status " +
           "AND o.importBatch.isActive = true")
    List<Order> findByDeliveryDateAndStatus(
            @Param("deliveryDate") LocalDate deliveryDate,
            @Param("status") String status);

    @Modifying
    @Query("UPDATE Order o SET o.tripDraft.id = :tripDraftId WHERE o.id IN :orderIds")
    void updateTripDraftId(@Param("orderIds") List<Long> orderIds,
                           @Param("tripDraftId") Long tripDraftId);

    @Query("SELECT o FROM Order o WHERE o.tripDraft.id = :tripDraftId")
    List<Order> findByTripDraftId(@Param("tripDraftId") Long tripDraftId);

    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.store s " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product p " +
           "WHERE o.importBatch.id = :batchId " +
           "ORDER BY o.orderRef ASC, i.sku ASC")
    List<Order> findByImportBatchId(@Param("batchId") Long batchId);

    @Query("SELECT DISTINCT o FROM Order o " +
           "JOIN FETCH o.store s " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.product p " +
           "WHERE o.tripDraft IS NULL " +
           "AND o.status = 'UNASSIGNED' " +
           "AND o.deliveryDate = :deliveryDate " +
           "AND o.store.id IN :storeIds " +
           "AND o.importBatch.isActive = true " +
           "ORDER BY o.orderRef ASC")
    List<Order> findExcludedOrdersByDeliveryDateAndStores(
            @Param("deliveryDate") LocalDate deliveryDate,
            @Param("storeIds") List<Long> storeIds);
}


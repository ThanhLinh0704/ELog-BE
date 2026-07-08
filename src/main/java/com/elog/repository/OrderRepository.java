package com.elog.repository;

import com.elog.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT o FROM Order o WHERE o.importBatch.id = :batchId AND o.orderRef = :orderRef AND o.store.id = :storeId")
    Optional<Order> findByBatchAndOrderRefAndStore(
            @Param("batchId") Long batchId,
            @Param("orderRef") String orderRef,
            @Param("storeId") Long storeId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.importBatch.id = :batchId")
    long countByBatchId(@Param("batchId") Long batchId);
}

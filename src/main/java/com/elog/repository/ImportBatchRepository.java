package com.elog.repository;

import com.elog.entity.ImportBatch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> {

    @Query("SELECT b FROM ImportBatch b WHERE ((:date IS NULL AND b.deliveryDate IS NULL) OR (b.deliveryDate = :date)) AND b.isActive = true")
    java.util.List<ImportBatch> findAllActiveByDate(@Param("date") LocalDate date);

    @Query("SELECT b FROM ImportBatch b ORDER BY b.createdAt DESC")
    Page<ImportBatch> findAllBatches(Pageable pageable);

    @Query("SELECT b FROM ImportBatch b WHERE b.deliveryDate = :date ORDER BY b.createdAt DESC")
    Page<ImportBatch> findByDeliveryDate(@Param("date") LocalDate date, Pageable pageable);
}

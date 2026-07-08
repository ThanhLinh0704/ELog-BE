package com.elog.repository;

import com.elog.entity.ImportError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ImportErrorRepository extends JpaRepository<ImportError, Long> {

    List<ImportError> findByImportBatchIdOrderByRowNumberAsc(Long batchId);

    @Query("SELECT e FROM ImportError e WHERE e.importBatch.id = :batchId AND (:errorCode IS NULL OR e.errorCode = :errorCode) ORDER BY e.rowNumber ASC")
    Page<ImportError> findByImportBatchIdAndErrorCode(
            @Param("batchId") Long batchId,
            @Param("errorCode") String errorCode,
            Pageable pageable);
}

package com.elog.repository;

import com.elog.entity.ImportError;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportErrorRepository extends JpaRepository<ImportError, Long> {

    List<ImportError> findByImportBatchIdOrderByRowNumberAsc(Long batchId);
}

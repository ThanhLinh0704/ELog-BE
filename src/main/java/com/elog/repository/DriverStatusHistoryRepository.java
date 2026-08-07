package com.elog.repository;

import com.elog.entity.DriverStatusHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DriverStatusHistoryRepository extends JpaRepository<DriverStatusHistory, Long> {
    Page<DriverStatusHistory> findByDriverId(Long driverId, Pageable pageable);
}

package com.elog.repository;

import com.elog.entity.TripOutcomeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TripOutcomeEventRepository extends JpaRepository<TripOutcomeEvent, Long>, JpaSpecificationExecutor<TripOutcomeEvent> {
}

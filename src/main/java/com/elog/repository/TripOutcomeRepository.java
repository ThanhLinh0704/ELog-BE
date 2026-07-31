package com.elog.repository;

import com.elog.entity.TripOutcome;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TripOutcomeRepository extends JpaRepository<TripOutcome, Long> {

    Optional<TripOutcome> findByTripExecutionId(Long tripExecutionId);

    List<TripOutcome> findByStatus(String status);
}

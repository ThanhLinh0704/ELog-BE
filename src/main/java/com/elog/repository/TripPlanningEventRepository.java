package com.elog.repository;

import com.elog.entity.TripPlanningEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface TripPlanningEventRepository extends JpaRepository<TripPlanningEvent, Long>, JpaSpecificationExecutor<TripPlanningEvent> {
}

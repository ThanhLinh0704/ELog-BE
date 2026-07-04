package com.elog.repository;

import com.elog.entity.TripDraftStop;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripDraftStopRepository extends JpaRepository<TripDraftStop, Long> {

    void deleteByTripDraftId(Long tripDraftId);
}

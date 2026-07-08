package com.elog.repository;

import com.elog.entity.TripDraftStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripDraftStopRepository extends JpaRepository<TripDraftStop, Long> {

    void deleteByTripDraftId(Long tripDraftId);

    List<TripDraftStop> findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(Long tripDraftId);

    int countByTripDraftIdAndIsActiveTrue(Long tripDraftId);

    int countByTripDraftIdAndIsActiveTrueAndPlannedEtaIsNull(Long tripDraftId);
}

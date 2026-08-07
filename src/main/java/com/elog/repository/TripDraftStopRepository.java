package com.elog.repository;

import com.elog.entity.TripDraftStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripDraftStopRepository extends JpaRepository<TripDraftStop, Long> {

    void deleteByTripDraftId(Long tripDraftId);

    List<TripDraftStop> findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(Long tripDraftId);

    List<TripDraftStop> findByTripDraftIdOrderBySequenceNoAsc(Long tripDraftId);

    int countByTripDraftIdAndIsActiveTrue(Long tripDraftId);

    int countByTripDraftIdAndIsActiveTrueAndPlannedEtaIsNull(Long tripDraftId);

    boolean existsByRouteStopId(Long routeStopId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM TripDraftStop tds WHERE tds.routeStop.id = :routeStopId")
    void deleteByRouteStopId(@org.springframework.data.repository.query.Param("routeStopId") Long routeStopId);
}

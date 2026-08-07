package com.elog.repository;

import com.elog.entity.ManifestLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManifestLineRepository extends JpaRepository<ManifestLine, Long> {

    List<ManifestLine> findByManifestManifestIdOrderByLifoSequenceAsc(Long manifestId);

    List<ManifestLine> findByManifestManifestIdOrderByStopSequenceNoAscLifoSequenceAsc(Long manifestId);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM ManifestLine ml WHERE ml.tripDraftStop.id IN (SELECT tds.id FROM TripDraftStop tds WHERE tds.routeStop.id = :routeStopId)")
    void deleteByRouteStopId(@org.springframework.data.repository.query.Param("routeStopId") Long routeStopId);
}

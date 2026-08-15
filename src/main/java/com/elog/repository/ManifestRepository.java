package com.elog.repository;

import com.elog.entity.Manifest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ManifestRepository extends JpaRepository<Manifest, Long> {

    boolean existsByTripDraftId(Long tripDraftId);

    Optional<Manifest> findByTripDraftId(Long tripDraftId);

    java.util.List<Manifest> findByTripDraftIdIn(java.util.Collection<Long> tripDraftIds);

    @Query("SELECT m FROM Manifest m " +
           "JOIN FETCH m.tripDraft td " +
           "JOIN FETCH td.route " +
           "JOIN FETCH m.generatedBy " +
           "WHERE td.id = :tripDraftId")
    Optional<Manifest> findByTripDraftIdWithDetails(@Param("tripDraftId") Long tripDraftId);
}

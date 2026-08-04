package com.elog.repository;

import com.elog.entity.TripStop;
import com.elog.entity.TripStopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TripStopRepository extends JpaRepository<TripStop, Long> {

    List<TripStop> findByTripTripIdOrderBySequenceOrderAsc(Long tripId);

    java.util.Optional<TripStop> findByTripDraftStopId(Long tripDraftStopId);

    /** PENDING stops của IN_PROGRESS trips mà ETA đã qua cutoff — dùng bởi TimeExceptionDetectionJob */
    @Query("SELECT ts FROM TripStop ts " +
           "JOIN ts.trip t " +
           "WHERE t.status = 'IN_PROGRESS' " +
           "AND ts.status = 'PENDING' " +
           "AND ts.plannedEta IS NOT NULL " +
           "AND ts.plannedEta < :cutoff " +
           "AND NOT EXISTS (" +
           "  SELECT 1 FROM DeliveryException de " +
           "  WHERE de.tripStopId = ts.tripStopId " +
           "  AND de.exceptionType = 'TIME_EXCEPTION'" +
           ")")
    List<TripStop> findOverdueStops(@Param("cutoff") LocalDateTime cutoff);

    /** Các stops chưa hoàn thành (PENDING/IN_PROGRESS) theo thứ tự — dùng để kiểm tra sequential order */
    @Query("SELECT ts FROM TripStop ts " +
           "WHERE ts.trip.tripId = :tripId " +
           "AND ts.status IN ('PENDING', 'IN_PROGRESS') " +
           "ORDER BY ts.sequenceOrder ASC")
    List<TripStop> findRemainingStopsOrdered(@Param("tripId") Long tripId);

    @Query("SELECT COUNT(ts) FROM TripStop ts " +
           "WHERE ts.trip.tripId = :tripId " +
           "AND ts.status = :status")
    int countByTripIdAndStatus(@Param("tripId") Long tripId,
                               @Param("status") TripStopStatus status);

    // ── US-19 — KPI Dashboard ─────────────────────────────────────────

    /** K1: processed stops (COMPLETED or EXCEPTION) in date range with ETA data */
    @Query("SELECT ts FROM TripStop ts " +
           "JOIN FETCH ts.trip t " +
           "WHERE t.deliveryDate BETWEEN :startDate AND :endDate " +
           "AND ts.status IN ('COMPLETED', 'EXCEPTION') " +
           "AND ts.plannedEta IS NOT NULL " +
           "AND ts.actualArrivalTime IS NOT NULL")
    List<TripStop> findProcessedStopsInDateRange(
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}

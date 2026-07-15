package com.elog.repository;

import com.elog.entity.TripStop;
import com.elog.entity.TripStopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TripStopRepository extends JpaRepository<TripStop, Long> {

    List<TripStop> findByTripTripIdOrderBySequenceOrderAsc(Long tripId);

    // ── US-17 — Dashboard Monitoring ─────────────────────────────────────────

    /**
     * Tìm PENDING stops trên IN_PROGRESS trips mà planned_eta đã qua cutoff
     * VÀ chưa có TIME_EXCEPTION nào cho stop đó.
     *
     * Dùng bởi TimeExceptionDetectionJob (mỗi 5 phút).
     * cutoff = now() - ETA_THRESHOLD_MINUTES
     *
     * NOTE: delivery_exceptions table thuộc US-18 schema.
     * Query này sẽ compile nhưng chỉ test được sau khi V19 migration chạy.
     */
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

    /**
     * Tìm stop tiếp theo chưa COMPLETED/EXCEPTION trong 1 trip
     * Dùng để kiểm tra sequential order (Driver phải đến đúng thứ tự)
     */
    @Query("SELECT ts FROM TripStop ts " +
           "WHERE ts.trip.tripId = :tripId " +
           "AND ts.status IN ('PENDING', 'IN_PROGRESS') " +
           "ORDER BY ts.sequenceOrder ASC")
    List<TripStop> findRemainingStopsOrdered(@Param("tripId") Long tripId);

    /**
     * Đếm số stops theo status trong 1 trip — dùng cho dashboard progress count
     */
    @Query("SELECT COUNT(ts) FROM TripStop ts " +
           "WHERE ts.trip.tripId = :tripId " +
           "AND ts.status = :status")
    int countByTripIdAndStatus(@Param("tripId") Long tripId,
                               @Param("status") TripStopStatus status);
}


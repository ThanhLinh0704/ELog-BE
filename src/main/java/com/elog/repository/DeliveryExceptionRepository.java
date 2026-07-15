package com.elog.repository;

import com.elog.entity.DeliveryException;
import com.elog.entity.ExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface DeliveryExceptionRepository extends JpaRepository<DeliveryException, Long> {

    /** Kiểm tra đã có exception theo type cho stop — dùng để guard duplicate TIME_EXCEPTION */
    boolean existsByTripStopIdAndExceptionType(Long tripStopId, ExceptionType exceptionType);

    /** Kiểm tra đã có DELIVERY_REJECTION chưa resolve cho stop — guard NAC-18c */
    boolean existsByTripStopIdAndExceptionTypeAndResolvedAtIsNull(Long tripStopId, ExceptionType exceptionType);

    List<DeliveryException> findByTripStopIdOrderByCreatedAtDesc(Long tripStopId);

    /** Batch load exceptions cho nhiều stops — tránh N+1 trong dashboard/progress queries */
    List<DeliveryException> findByTripStopIdInOrderByCreatedAtDesc(List<Long> tripStopIds);

    @Query("SELECT de FROM DeliveryException de " +
                 "JOIN TripStop ts ON ts.tripStopId = de.tripStopId " +
                 "JOIN ts.trip t " +
                 "WHERE t.deliveryDate = CURRENT_DATE " +
                 "AND de.resolvedAt IS NULL " +
                 "ORDER BY de.createdAt DESC")
    List<DeliveryException> findUnresolvedExceptionsToday();

    /** Filter theo date/type/resolved — null = không filter theo field đó */
    @Query("SELECT de FROM DeliveryException de " +
           "JOIN TripStop ts ON ts.tripStopId = de.tripStopId " +
           "JOIN ts.trip t " +
           "WHERE (:date IS NULL OR t.deliveryDate = :date) " +
           "AND (:exceptionType IS NULL OR de.exceptionType = :exceptionType) " +
           "AND (:resolved IS NULL " +
           "     OR (:resolved = TRUE AND de.resolvedAt IS NOT NULL) " +
           "     OR (:resolved = FALSE AND de.resolvedAt IS NULL)) " +
           "ORDER BY de.createdAt DESC")
    List<DeliveryException> findByFilters(
            @Param("date") LocalDate date,
            @Param("exceptionType") ExceptionType exceptionType,
            @Param("resolved") Boolean resolved);
}

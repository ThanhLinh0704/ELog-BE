package com.elog.repository;

import com.elog.entity.DeliveryException;
import com.elog.entity.ExceptionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DeliveryExceptionRepository extends JpaRepository<DeliveryException, Long> {

       /**
        * Kiểm tra đã có TIME_EXCEPTION cho stop này chưa
        * Dùng trong TripMonitoringServiceImpl.createTimeException() để tránh duplicate
        */
       boolean existsByTripStopIdAndExceptionType(Long tripStopId, ExceptionType exceptionType);

       /**
        * Lấy tất cả exceptions của 1 stop
        * Dùng cho dashboard getTripProgress()
        */
       List<DeliveryException> findByTripStopIdOrderByCreatedAtDesc(Long tripStopId);

       /**
        * Lấy unresolved exceptions — dùng cho GET /api/dashboard/exceptions
        */
       @Query("SELECT de FROM DeliveryException de " +
                     "JOIN TripStop ts ON ts.tripStopId = de.tripStopId " +
                     "JOIN ts.trip t " +
                     "WHERE t.deliveryDate = CURRENT_DATE " +
                     "AND de.resolvedAt IS NULL " +
                     "ORDER BY de.createdAt DESC")
       List<DeliveryException> findUnresolvedExceptionsToday();
}

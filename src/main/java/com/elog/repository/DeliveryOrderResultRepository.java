package com.elog.repository;

import com.elog.entity.DeliveryOrderResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeliveryOrderResultRepository extends JpaRepository<DeliveryOrderResult, Long> {

    List<DeliveryOrderResult> findByTripExecutionId(Long tripExecutionId);

    Optional<DeliveryOrderResult> findByTripExecutionIdAndOrderId(Long tripExecutionId, Long orderId);

    long countByTripExecutionIdAndStatus(Long tripExecutionId, String status);

    long countByTripExecutionId(Long tripExecutionId);
}

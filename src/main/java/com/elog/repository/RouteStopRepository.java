package com.elog.repository;

import com.elog.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    Optional<RouteStop> findFirstByStoreId(Long storeId);
    boolean existsByStoreIdAndRouteIsActiveTrue(Long storeId);
}

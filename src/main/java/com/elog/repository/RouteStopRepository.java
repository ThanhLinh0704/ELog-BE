package com.elog.repository;

import com.elog.entity.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    Optional<RouteStop> findFirstByStoreId(Long storeId);
    boolean existsByStoreIdAndRouteIsActiveTrue(Long storeId);

    List<RouteStop> findByRouteIdOrderBySequenceNoAsc(Long routeId);
    int countByRouteId(Long routeId);
    boolean existsByRouteIdAndStoreId(Long routeId, Long storeId);

    @Query("SELECT COALESCE(MAX(rs.sequenceNo), 0) FROM RouteStop rs WHERE rs.route.id = :routeId")
    int findMaxSequenceNoByRouteId(Long routeId);

    @Query("SELECT COUNT(rs) FROM RouteStop rs WHERE rs.route.id = :routeId " +
           "AND rs.store.latitude IS NULL OR rs.store.longitude IS NULL")
    int countStopsWithoutCoordinatesByRouteId(Long routeId);
}


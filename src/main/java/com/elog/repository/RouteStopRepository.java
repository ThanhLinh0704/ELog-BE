package com.elog.repository;

import com.elog.entity.RouteStop;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    Optional<RouteStop> findFirstByStoreId(Long storeId);
    List<RouteStop> findAllByStoreId(Long storeId);
    boolean existsByStoreIdAndRouteIsActiveTrue(Long storeId);

    @EntityGraph(attributePaths = {"store", "store.ward", "store.district", "store.province", "route"})
    List<RouteStop> findByRouteIdOrderBySequenceOrderAsc(Long routeId);
    int countByRouteId(Long routeId);
    boolean existsByRouteIdAndStoreId(Long routeId, Long storeId);

    @Query("SELECT COALESCE(MAX(rs.sequenceOrder), 0) FROM RouteStop rs WHERE rs.route.id = :routeId")
    int findMaxSequenceOrderByRouteId(Long routeId);

    @Query("SELECT COUNT(rs) FROM RouteStop rs WHERE rs.route.id = :routeId " +
           "AND (rs.store.latitude IS NULL OR rs.store.longitude IS NULL)")
    int countStopsWithoutCoordinatesByRouteId(Long routeId);

    @Query("SELECT rs FROM RouteStop rs JOIN FETCH rs.route WHERE rs.store.id IN :storeIds")
    List<RouteStop> findByStoreIdIn(@org.springframework.data.repository.query.Param("storeIds") List<Long> storeIds);

    @Query("SELECT rs.route.id, COUNT(rs) FROM RouteStop rs WHERE rs.route.id IN :routeIds GROUP BY rs.route.id")
    List<Object[]> countStopsByRouteIdIn(@org.springframework.data.repository.query.Param("routeIds") List<Long> routeIds);
}


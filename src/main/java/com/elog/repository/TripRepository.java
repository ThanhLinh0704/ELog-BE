package com.elog.repository;

import com.elog.entity.Trip;
import com.elog.entity.TripStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

        List<Trip> findByTripDraftId(Long tripDraftId);

        boolean existsByTripDraftId(Long tripDraftId);

        boolean existsByVehicleIdAndDeliveryDateAndStatusIn(
                        Long vehicleId, LocalDate deliveryDate, List<TripStatus> statuses);

        boolean existsByVehicleIdAndDeliveryDateAndStatusInAndTripIdNot(
                        Long vehicleId, LocalDate deliveryDate, List<TripStatus> statuses, Long tripIdNot);

        boolean existsByDriverIdAndDeliveryDateAndStatusIn(
                        Long driverId, LocalDate deliveryDate, List<TripStatus> statuses);

        @Query("SELECT DISTINCT t.driver.id FROM Trip t WHERE t.deliveryDate = :date AND t.status IN :statuses AND t.driver.id IS NOT NULL")
        List<Long> findBusyDriverIdsOnDate(@Param("date") LocalDate date, @Param("statuses") List<TripStatus> statuses);

        List<Trip> findByDriverIdAndStatusIn(Long driverId, List<TripStatus> statuses);

        boolean existsByDriverIdAndDeliveryDateAndStatusInAndTripIdNot(
                        Long driverId, LocalDate deliveryDate, List<TripStatus> statuses, Long tripIdNot);

        @Query("SELECT t FROM Trip t " +
                        "JOIN FETCH t.vehicle " +
                        "JOIN FETCH t.driver " +
                        "JOIN FETCH t.route " +
                        "WHERE t.tripDraft.id = :tripDraftId")
        List<Trip> findByTripDraftIdWithDetails(@Param("tripDraftId") Long tripDraftId);

        List<Trip> findByDriverIdAndDeliveryDateAndStatus(
                        Long driverId, LocalDate deliveryDate, TripStatus status);

        List<Trip> findByDriverIdAndDeliveryDateBetween(
                        Long driverId, LocalDate startDate, LocalDate endDate);

        List<Trip> findByDeliveryDateAndStatus(LocalDate deliveryDate, TripStatus status);

        // ── US-17 — Dashboard Monitoring ─────────────────────────────────────────

        /**
         * Lấy tất cả active trips (DISPATCHED + IN_PROGRESS + COMPLETED) của 1 ngày
         * Dùng cho GET /api/dashboard/active-trips
         */
        @Query("SELECT t FROM Trip t " +
                        "JOIN FETCH t.vehicle " +
                        "JOIN FETCH t.driver " +
                        "JOIN FETCH t.route " +
                        "WHERE t.deliveryDate = :date " +
                        "AND t.status IN :statuses " +
                        "ORDER BY t.status ASC, t.plannedDepartureTime ASC")
        List<Trip> findActiveTripsByDate(
                        @Param("date") LocalDate date,
                        @Param("statuses") List<TripStatus> statuses);

        /**
         * Driver xem trips của mình theo ngày, filter theo nhiều status
         * Dùng cho GET /api/trips/my-trips với status=DISPATCHED,IN_PROGRESS
         */
        @Query("SELECT t FROM Trip t " +
                        "JOIN FETCH t.vehicle " +
                        "JOIN FETCH t.route " +
                        "WHERE t.driver.username = :username " +
                        "AND t.deliveryDate = :date " +
                        "AND t.status IN :statuses " +
                        "ORDER BY t.plannedDepartureTime ASC")
        List<Trip> findDriverTripsByDateAndStatuses(
                        @Param("username") String username,
                        @Param("date") LocalDate date,
                        @Param("statuses") List<TripStatus> statuses);

        // ── US-19 — KPI Dashboard ─────────────────────────────────────────

        /** K4: Trip completion — count trips by status in date range */
        @Query("SELECT t.status, COUNT(t) FROM Trip t " +
                        "WHERE t.deliveryDate BETWEEN :startDate AND :endDate " +
                        "AND t.status IN ('DISPATCHED','IN_PROGRESS','COMPLETED') " +
                        "GROUP BY t.status")
        List<Object[]> countTripsByStatusInDateRange(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /** K2, K3: Fleet utilization — join vehicle for capacity */
        @Query("SELECT t FROM Trip t " +
                        "JOIN FETCH t.vehicle " +
                        "WHERE t.deliveryDate BETWEEN :startDate AND :endDate " +
                        "AND t.status IN ('DISPATCHED','IN_PROGRESS','COMPLETED')")
        List<Trip> findTripsWithVehicleInDateRange(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);

        /** By-route: trips with vehicle + route for route-level KPI */
        @Query("SELECT t FROM Trip t " +
                        "JOIN FETCH t.vehicle " +
                        "JOIN FETCH t.route " +
                        "WHERE t.deliveryDate BETWEEN :startDate AND :endDate " +
                        "AND t.status IN ('DISPATCHED','IN_PROGRESS','COMPLETED')")
        List<Trip> findTripsWithVehicleAndRouteInDateRange(
                        @Param("startDate") LocalDate startDate,
                        @Param("endDate") LocalDate endDate);
}

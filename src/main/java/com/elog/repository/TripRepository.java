package com.elog.repository;

import com.elog.entity.Trip;
import com.elog.entity.TripStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TripRepository extends JpaRepository<Trip, Long> {

        @EntityGraph(attributePaths = {"vehicle", "driver", "route", "tripDraft", "lockedBy"})
        List<Trip> findByTripDraftId(Long tripDraftId);

        /**
         * "Đang có Trip nào ràng buộc TripDraft này không" — dùng cho mọi cổng chặn thao tác
         * (gán xe, gán tách, revert về nháp...). CANCELLED không tính là ràng buộc: 1 TripDraft có
         * thể có nhiều Trip lịch sử (vd Trip A huỷ, Trip B đang chạy) nhưng chỉ được có tối đa 1
         * Trip không-CANCELLED tại 1 thời điểm. Các màn hình audit/lịch sử vẫn dùng
         * findByTripDraftId / findByTripDraftIdWithDetails (không lọc status) để thấy đủ cả CANCELLED.
         */
        boolean existsByTripDraftIdAndStatusNot(Long tripDraftId, TripStatus status);

        boolean existsByVehicleIdAndDeliveryDateAndStatusIn(
                        Long vehicleId, LocalDate deliveryDate, List<TripStatus> statuses);

        boolean existsByVehicleIdAndDeliveryDateAndStatusInAndTripIdNot(
                        Long vehicleId, LocalDate deliveryDate, List<TripStatus> statuses, Long tripIdNot);

        boolean existsByDriverIdAndDeliveryDateAndStatusIn(
                        Long driverId, LocalDate deliveryDate, List<TripStatus> statuses);

        @Query("SELECT DISTINCT t.driver.id FROM Trip t WHERE t.deliveryDate = :date AND t.status IN :statuses AND t.driver.id IS NOT NULL")
        List<Long> findBusyDriverIdsOnDate(@Param("date") LocalDate date, @Param("statuses") List<TripStatus> statuses);

        List<Trip> findByDriverIdAndStatusIn(Long driverId, List<TripStatus> statuses);

        List<Trip> findByVehicleIdAndStatusIn(Long vehicleId, List<TripStatus> statuses);

        List<Trip> findByVehicleIdAndDeliveryDate(Long vehicleId, LocalDate deliveryDate);

        /** US-XX stale-trip detection: chuyến ở status cho trước, deliveryDate trước cutoff (chưa bắt đầu quá lâu). */
        List<Trip> findByStatusAndDeliveryDateBefore(TripStatus status, LocalDate cutoff);

        /**
         * Trip-start-deadline sweep: chuyến ở status cho trước mà lockedAt (thời điểm gán xe) đã
         * trước cutoff — tức quá hạn N phút cho phép mà tài xế vẫn chưa bấm "Bắt đầu chuyến".
         * lockedAt IS NULL bị loại tự nhiên vì so sánh NULL < cutoff luôn false.
         */
        List<Trip> findByStatusAndLockedAtBefore(TripStatus status, LocalDateTime cutoff);

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

        @EntityGraph(attributePaths = {"vehicle", "driver", "route", "tripDraft", "lockedBy"})
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

        // ── Confirmed Dispatch Export ────────────────────────────────────────

        @Query("SELECT t FROM Trip t " +
                        "JOIN FETCH t.vehicle " +
                        "JOIN FETCH t.driver " +
                        "LEFT JOIN FETCH t.route " +
                        "JOIN FETCH t.tripDraft " +
                        "WHERE t.tripDraft.id = :tripDraftId " +
                        "AND t.status IN :statuses")
        List<Trip> findByTripDraftIdAndStatusInWithDetails(
                        @Param("tripDraftId") Long tripDraftId,
                        @Param("statuses") java.util.Collection<TripStatus> statuses);

        @Query("SELECT t FROM Trip t " +
                        "JOIN FETCH t.vehicle " +
                        "JOIN FETCH t.driver " +
                        "LEFT JOIN FETCH t.route " +
                        "JOIN FETCH t.tripDraft " +
                        "WHERE t.deliveryDate BETWEEN :fromDate AND :toDate " +
                        "AND t.status IN :statuses " +
                        "ORDER BY t.deliveryDate ASC, t.tripId ASC")
        List<Trip> findByDeliveryDateBetweenAndStatusInWithDetails(
                        @Param("fromDate") LocalDate fromDate,
                        @Param("toDate") LocalDate toDate,
                        @Param("statuses") java.util.Collection<TripStatus> statuses);
}

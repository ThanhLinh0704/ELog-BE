package com.elog.repository;

import com.elog.entity.TripExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripExecutionRepository extends JpaRepository<TripExecution, Long> {

    List<TripExecution> findByDriverUsernameAndStatusIn(String username, List<String> statuses);

    @Query("SELECT te FROM TripExecution te WHERE te.trip.tripId = :tripId")
    Optional<TripExecution> findByTripId(@Param("tripId") Long tripId);

    List<TripExecution> findByTripTripIdIn(java.util.Collection<Long> tripIds);

    List<TripExecution> findByDriverId(Long driverId);

    @Query("SELECT te FROM TripExecution te WHERE te.trip.vehicle.id = :vehicleId")
    List<TripExecution> findByVehicleId(@Param("vehicleId") Long vehicleId);

    @Query("SELECT te FROM TripExecution te WHERE te.trip.vehicle.id = :vehicleId AND te.returnedToWarehouseAt IS NULL AND te.status != 'CANCELLED'")
    List<TripExecution> findUnreturnedByVehicleId(@Param("vehicleId") Long vehicleId);

    @Query("SELECT te FROM TripExecution te WHERE te.driver.id = :driverId AND te.returnedToWarehouseAt IS NULL AND te.status != 'CANCELLED'")
    List<TripExecution> findUnreturnedByDriverId(@Param("driverId") Long driverId);

    @Query("SELECT te FROM TripExecution te LEFT JOIN FETCH te.trip WHERE te.returnedToWarehouseAt IS NULL AND te.status != 'CANCELLED' AND te.driver.id IS NOT NULL")
    List<TripExecution> findAllUnreturnedExecutions();
}

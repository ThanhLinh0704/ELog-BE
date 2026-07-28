package com.elog.repository;

import com.elog.entity.TripExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TripExecutionRepository extends JpaRepository<TripExecution, Long> {

    @Query("SELECT te FROM TripExecution te WHERE te.driver.username = :username AND te.status IN ('ASSIGNED', 'IN_PROGRESS')")
    Optional<TripExecution> findActiveByDriverUsername(@Param("username") String username);

    @Query("SELECT te FROM TripExecution te WHERE te.trip.tripId = :tripId")
    Optional<TripExecution> findByTripId(@Param("tripId") Long tripId);

    List<TripExecution> findByDriverId(Long driverId);
}

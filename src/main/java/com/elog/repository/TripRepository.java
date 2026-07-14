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

    boolean existsByVehicleIdAndDeliveryDateAndStatusIn(
            Long vehicleId, LocalDate deliveryDate, List<TripStatus> statuses);

    boolean existsByDriverIdAndDeliveryDateAndStatusIn(
            Long driverId, LocalDate deliveryDate, List<TripStatus> statuses);

    @Query("SELECT t FROM Trip t " +
           "JOIN FETCH t.vehicle " +
           "JOIN FETCH t.driver " +
           "JOIN FETCH t.route " +
           "WHERE t.tripDraft.id = :tripDraftId")
    List<Trip> findByTripDraftIdWithDetails(@Param("tripDraftId") Long tripDraftId);

    List<Trip> findByDriverIdAndDeliveryDateAndStatus(
            Long driverId, LocalDate deliveryDate, TripStatus status);

    List<Trip> findByDeliveryDateAndStatus(LocalDate deliveryDate, TripStatus status);
}

package com.elog.repository;

import com.elog.entity.TripStop;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TripStopRepository extends JpaRepository<TripStop, Long> {

    List<TripStop> findByTripTripIdOrderBySequenceOrderAsc(Long tripId);
}

package com.elog.repository;

import com.elog.entity.TripDraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface TripDraftRepository extends JpaRepository<TripDraft, Long> {

    Optional<TripDraft> findByRouteIdAndDeliveryDate(Long routeId, LocalDate deliveryDate);

    Page<TripDraft> findByDeliveryDate(LocalDate deliveryDate, Pageable pageable);

    boolean existsByDeliveryDateAndStatusNot(LocalDate deliveryDate, String status);
}

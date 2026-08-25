package com.elog.repository;

import com.elog.entity.Vehicle;
import com.elog.entity.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    @Override
    @EntityGraph(attributePaths = {"assignedDriver"})
    Page<Vehicle> findAll(Specification<Vehicle> spec, Pageable pageable);

    boolean existsByPlateNumber(String plateNumber);

    boolean existsByVehicleCode(String vehicleCode);

    long countByIsActiveTrue();

    List<Vehicle> findByIsActiveTrue();

    /** AVAILABLE only — excludes IN_USE/MAINTENANCE/OUT_OF_SERVICE. See CapacityValidationServiceImpl. */
    List<Vehicle> findByIsActiveTrueAndStatus(VehicleStatus status);

    List<Vehicle> findByAssignedDriverId(Long driverId);

    @Query("select sum(v.payloadKg) from Vehicle v where v.isActive = true")
    BigDecimal sumActiveMaxWeightKg();

    @Query("select sum(v.maxVolumeM3) from Vehicle v where v.isActive = true")
    BigDecimal sumActiveMaxVolumeM3();
}

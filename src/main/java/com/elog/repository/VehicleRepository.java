package com.elog.repository;

import com.elog.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByPlateNumber(String plateNumber);

    long countByIsActiveTrue();

    @Query("select sum(v.maxWeightKg) from Vehicle v where v.isActive = true")
    BigDecimal sumActiveMaxWeightKg();

    @Query("select sum(v.maxVolumeM3) from Vehicle v where v.isActive = true")
    BigDecimal sumActiveMaxVolumeM3();
}

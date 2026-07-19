package com.elog.repository;

import com.elog.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle> {

    boolean existsByPlateNumber(String plateNumber);

    boolean existsByVehicleCode(String vehicleCode);

    long countByIsActiveTrue();

    List<Vehicle> findByIsActiveTrue();

    @Query("select sum(v.payloadKg) from Vehicle v where v.isActive = true")
    BigDecimal sumActiveMaxWeightKg();

    @Query("select sum(v.maxVolumeM3) from Vehicle v where v.isActive = true")
    BigDecimal sumActiveMaxVolumeM3();
}

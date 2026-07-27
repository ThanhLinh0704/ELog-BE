package com.elog.service.impl;

import com.elog.entity.Store;
import com.elog.entity.Vehicle;
import com.elog.service.ConstraintValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ConstraintValidationServiceImpl implements ConstraintValidationService {

    @Override
    public List<String> validateStoreVehicle(Store store, Vehicle vehicle) {
        List<String> violations = new ArrayList<>();
        if (store == null || vehicle == null) {
            return violations;
        }

        // 1. Weight Constraint
        if (store.getMaxAllowedVehicleWeight() != null) {
            BigDecimal maxAllowedTons = store.getMaxAllowedVehicleWeight();
            BigDecimal vehicleWeightKg = vehicle.getGrossVehicleWeightKg() != null 
                    ? vehicle.getGrossVehicleWeightKg() 
                    : vehicle.getPayloadKg();

            if (vehicleWeightKg != null) {
                BigDecimal vehicleTons = vehicleWeightKg.divide(new BigDecimal("1000"), 3, java.math.RoundingMode.HALF_UP);
                if (vehicleTons.compareTo(maxAllowedTons) > 0) {
                    violations.add("ERR_WEIGHT_EXCEEDED: Cửa hàng " + store.getCode() + 
                            " giới hạn xe <= " + maxAllowedTons + " tấn, nhưng xe " + 
                            vehicle.getPlateNumber() + " nặng " + vehicleTons + " tấn");
                }
            }
        }

        // 2. Restricted Vehicle Type
        if (store.getRestrictedVehicleTypes() != null && !store.getRestrictedVehicleTypes().isBlank()) {
            String vehicleType = vehicle.getVehicleType();
            if (vehicleType != null) {
                String[] restrictedTypes = store.getRestrictedVehicleTypes().split(",");
                for (String restricted : restrictedTypes) {
                    if (vehicleType.trim().equalsIgnoreCase(restricted.trim())) {
                        violations.add("ERR_RESTRICTED_VEHICLE: Cửa hàng " + store.getCode() + 
                                " cấm xe loại " + vehicleType);
                        break;
                    }
                }
            }
        }

        return violations;
    }

    @Override
    public List<String> validateTripVehicleStops(List<Store> stores, Vehicle vehicle) {
        List<String> allViolations = new ArrayList<>();
        if (stores == null || vehicle == null) return allViolations;

        for (Store store : stores) {
            List<String> violations = validateStoreVehicle(store, vehicle);
            allViolations.addAll(violations);
        }
        return allViolations;
    }
}

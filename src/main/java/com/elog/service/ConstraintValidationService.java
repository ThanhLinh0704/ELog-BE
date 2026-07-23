package com.elog.service;

import com.elog.entity.Store;
import com.elog.entity.Vehicle;

import java.util.List;

public interface ConstraintValidationService {
    
    /**
     * Validate store constraints against a vehicle.
     * @return List of error/warning codes or empty list if valid.
     */
    List<String> validateStoreVehicle(Store store, Vehicle vehicle);

    /**
     * Validate all stops in a trip/trip draft against a vehicle.
     * @return List of error/warning messages or empty list if valid.
     */
    List<String> validateTripVehicleStops(List<Store> stores, Vehicle vehicle);
}

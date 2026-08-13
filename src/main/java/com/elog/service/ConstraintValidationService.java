package com.elog.service;

import com.elog.entity.Order;
import com.elog.entity.Store;
import com.elog.entity.TripDraftStop;
import com.elog.entity.Vehicle;

import java.time.LocalTime;
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

    /**
     * Check if a time is within allowed delivery hours string (e.g. "08:00-17:00").
     */
    boolean isTimeWithinAllowedHours(LocalTime time, String allowedHours);

    /**
     * Check if a time is within order delivery time window string (e.g. "13:00 - 17:00", "trước 12h", etc.).
     */
    boolean isTimeWithinOrderWindow(LocalTime time, String window);

    /**
     * Validate a stop's ETA against store allowed delivery hours, store closing time, and order delivery time windows.
     * @return Violation reason string if invalid, or null if valid.
     */
    String validateStopEta(TripDraftStop stop, List<Order> draftOrders);
}


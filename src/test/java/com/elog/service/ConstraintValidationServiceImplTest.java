package com.elog.service;

import com.elog.entity.Store;
import com.elog.entity.Vehicle;
import com.elog.service.impl.ConstraintValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintValidationServiceImplTest {

    private ConstraintValidationServiceImpl validationService;

    @BeforeEach
    void setUp() {
        validationService = new ConstraintValidationServiceImpl();
    }

    @Test
    @DisplayName("Should pass when vehicle weight and type satisfy store constraints")
    void testValidateStoreVehicle_Pass() {
        Store store = Store.builder()
                .code("STORE01")
                .maxAllowedVehicleWeight(new BigDecimal("5.0"))
                .restrictedVehicleTypes("Xe 10 tấn, Container")
                .build();

        Vehicle vehicle = Vehicle.builder()
                .plateNumber("29A-12345")
                .vehicleType("Xe 3.5 tấn")
                .payloadKg(new BigDecimal("3500"))
                .grossVehicleWeightKg(new BigDecimal("4500"))
                .build();

        List<String> violations = validationService.validateStoreVehicle(store, vehicle);
        assertTrue(violations.isEmpty());
    }

    @Test
    @DisplayName("Should return error when vehicle weight exceeds store max weight limit")
    void testValidateStoreVehicle_WeightExceeded() {
        Store store = Store.builder()
                .code("STORE02")
                .maxAllowedVehicleWeight(new BigDecimal("3.5"))
                .build();

        Vehicle vehicle = Vehicle.builder()
                .plateNumber("29A-99999")
                .vehicleType("Xe 5 tấn")
                .payloadKg(new BigDecimal("5000"))
                .grossVehicleWeightKg(new BigDecimal("6000"))
                .build();

        List<String> violations = validationService.validateStoreVehicle(store, vehicle);
        assertFalse(violations.isEmpty());
        assertTrue(violations.get(0).contains("ERR_WEIGHT_EXCEEDED"));
    }

    @Test
    @DisplayName("Should return error when vehicle type is restricted by store")
    void testValidateStoreVehicle_RestrictedType() {
        Store store = Store.builder()
                .code("STORE03")
                .restrictedVehicleTypes("Xe 8 tấn, Xe 10 tấn")
                .build();

        Vehicle vehicle = Vehicle.builder()
                .plateNumber("29A-88888")
                .vehicleType("Xe 8 tấn")
                .payloadKg(new BigDecimal("8000"))
                .build();

        List<String> violations = validationService.validateStoreVehicle(store, vehicle);
        assertFalse(violations.isEmpty());
        assertTrue(violations.get(0).contains("ERR_RESTRICTED_VEHICLE"));
    }
}

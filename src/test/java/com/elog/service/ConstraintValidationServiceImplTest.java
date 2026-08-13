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

    @Test
    @DisplayName("Should validate order delivery time window correctly")
    void testValidateStopEta_OrderDeliveryWindowViolation() {
        Store store = Store.builder().id(1L).code("ST01").build();
        com.elog.entity.Order order = com.elog.entity.Order.builder()
                .id(10L)
                .orderRef("DH-20260808-146A")
                .store(store)
                .deliveryTimeWindow("13:00 - 17:00")
                .isDeliveryTimeOverridden(false)
                .build();

        com.elog.entity.TripDraftStop stop = com.elog.entity.TripDraftStop.builder()
                .id(100L)
                .store(store)
                .plannedEta(java.time.LocalDateTime.of(2026, 8, 10, 8, 39))
                .isActive(true)
                .build();

        String violation = validationService.validateStopEta(stop, List.of(order));
        assertNotNull(violation);
        assertTrue(violation.contains("violates delivery window (13:00 - 17:00) for order DH-20260808-146A"));
    }
}


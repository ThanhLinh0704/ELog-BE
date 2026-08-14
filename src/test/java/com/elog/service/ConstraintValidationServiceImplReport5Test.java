package com.elog.service;

import com.elog.entity.Store;
import com.elog.entity.Vehicle;
import com.elog.service.impl.ConstraintValidationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintValidationServiceImplReport5Test {
    private final ConstraintValidationServiceImpl service=new ConstraintValidationServiceImpl();
    private Vehicle vehicle(){return Vehicle.builder().plateNumber("29A-1").vehicleType("VAN").grossVehicleWeightKg(new BigDecimal("8000")).build();}
    @Test @DisplayName("[L1-CS-10] compatible store and vehicle produce no violation") void compatibleProducesNoViolation(){Store store=Store.builder().code("S1").maxAllowedVehicleWeight(new BigDecimal("10")).restrictedVehicleTypes("TRUCK").build();assertTrue(service.validateStoreVehicle(store,vehicle()).isEmpty());}
    @Test @DisplayName("[L1-CS-11] vehicle one kilogram above store tonnage limit is rejected") void weightAboveBoundaryIsRejected(){Store store=Store.builder().code("S1").maxAllowedVehicleWeight(new BigDecimal("10")).build();Vehicle v=vehicle();v.setGrossVehicleWeightKg(new BigDecimal("10001"));List<String> result=service.validateStoreVehicle(store,v);assertAll(()->assertEquals(1,result.size()),()->assertTrue(result.getFirst().startsWith("ERR_WEIGHT_EXCEEDED")));}
    @Test @DisplayName("[L1-CS-12] violations accumulate across stores in iteration order") void violationsAccumulateInStoreOrder(){Store weight=Store.builder().code("WEIGHT").maxAllowedVehicleWeight(new BigDecimal("7")).build();Store type=Store.builder().code("TYPE").restrictedVehicleTypes("VAN").build();List<String> result=service.validateTripVehicleStops(List.of(weight,type),vehicle());assertAll(()->assertEquals(2,result.size()),()->assertTrue(result.get(0).startsWith("ERR_WEIGHT_EXCEEDED")),()->assertTrue(result.get(1).startsWith("ERR_RESTRICTED_VEHICLE")));}
}

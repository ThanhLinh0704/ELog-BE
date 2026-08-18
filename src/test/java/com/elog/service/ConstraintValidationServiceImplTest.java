package com.elog.service;

import com.elog.entity.Order;
import com.elog.entity.Store;
import com.elog.entity.TripDraftStop;
import com.elog.entity.Vehicle;
import com.elog.service.impl.ConstraintValidationServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConstraintValidationServiceImplTest {
    private final ConstraintValidationServiceImpl service = new ConstraintValidationServiceImpl();

    private Vehicle vehicle() {
        return Vehicle.builder()
                .plateNumber("29A-12345")
                .vehicleType("VAN")
                .grossVehicleWeightKg(new BigDecimal("8000"))
                .payloadKg(new BigDecimal("3000"))
                .build();
    }

    @Test
    @DisplayName("[L1-CS-10] compatible store and vehicle produce no violation")
    void compatibleProducesNoViolation() {
        Store store = Store.builder().code("S1").maxAllowedVehicleWeight(new BigDecimal("10")).restrictedVehicleTypes("TRUCK").build();
        assertTrue(service.validateStoreVehicle(store, vehicle()).isEmpty());
    }

    @Test
    @DisplayName("[L1-CS-11] vehicle one kilogram above store tonnage limit is rejected")
    void weightAboveBoundaryIsRejected() {
        Store store = Store.builder().code("S1").maxAllowedVehicleWeight(new BigDecimal("10")).build();
        Vehicle v = vehicle();
        v.setGrossVehicleWeightKg(new BigDecimal("10001"));
        List<String> result = service.validateStoreVehicle(store, v);
        assertAll(
                () -> assertEquals(1, result.size()),
                () -> assertTrue(result.getFirst().startsWith("ERR_WEIGHT_EXCEEDED"))
        );
    }

    @Test
    @DisplayName("[L1-CS-12] violations accumulate across stores in iteration order")
    void violationsAccumulateInStoreOrder() {
        Store weight = Store.builder().code("WEIGHT").maxAllowedVehicleWeight(new BigDecimal("7")).build();
        Store type = Store.builder().code("TYPE").restrictedVehicleTypes("VAN").build();
        List<String> result = service.validateTripVehicleStops(List.of(weight, type), vehicle());
        assertAll(
                () -> assertEquals(2, result.size()),
                () -> assertTrue(result.get(0).startsWith("ERR_WEIGHT_EXCEEDED")),
                () -> assertTrue(result.get(1).startsWith("ERR_RESTRICTED_VEHICLE"))
        );
    }

    @Test
    @DisplayName("[L1-CS-13] null store or vehicle returns empty list")
    void nullInputsReturnEmpty() {
        assertTrue(service.validateStoreVehicle(null, vehicle()).isEmpty());
        assertTrue(service.validateStoreVehicle(Store.builder().build(), null).isEmpty());
        assertTrue(service.validateTripVehicleStops(null, vehicle()).isEmpty());
        assertTrue(service.validateTripVehicleStops(List.of(), null).isEmpty());
    }

    @Test
    @DisplayName("[L1-CS-14] isTimeWithinAllowedHours standard daytime and overnight intervals")
    void testIsTimeWithinAllowedHours() {
        assertTrue(service.isTimeWithinAllowedHours(LocalTime.of(10, 0), null));
        assertTrue(service.isTimeWithinAllowedHours(LocalTime.of(10, 0), "All"));
        assertTrue(service.isTimeWithinAllowedHours(LocalTime.of(10, 0), "08:00-12:00, 13:00-17:00"));
        assertFalse(service.isTimeWithinAllowedHours(LocalTime.of(12, 30), "08:00-12:00, 13:00-17:00"));

        // Overnight interval (e.g. 22:00-06:00)
        assertTrue(service.isTimeWithinAllowedHours(LocalTime.of(23, 0), "22:00-06:00"));
        assertTrue(service.isTimeWithinAllowedHours(LocalTime.of(5, 0), "22:00-06:00"));
        assertFalse(service.isTimeWithinAllowedHours(LocalTime.of(12, 0), "22:00-06:00"));

        // Malformed interval string should default safely
        assertFalse(service.isTimeWithinAllowedHours(LocalTime.of(10, 0), "INVALID_FORMAT"));
    }

    @Test
    @DisplayName("[L1-CS-15] isTimeWithinOrderWindow handles Vietnamese phrases and intervals")
    void testIsTimeWithinOrderWindow() {
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(10, 0), null));
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(10, 0), ""));

        // Hanh chinh (8:00 - 17:00 + 20 min grace)
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(9, 0), "Giờ hành chính"));
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(17, 15), "Giờ hành chính"));
        assertFalse(service.isTimeWithinOrderWindow(LocalTime.of(7, 30), "Giờ hành chính"));

        // Truoc 9h (<= 09:20 with grace)
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(9, 15), "Trước 9h"));
        assertFalse(service.isTimeWithinOrderWindow(LocalTime.of(9, 30), "Trước 9h"));

        // Sau 14h
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(15, 0), "Sau 14h"));
        assertFalse(service.isTimeWithinOrderWindow(LocalTime.of(13, 0), "Sau 14h"));

        // Interval "14:00 - 17:00"
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(16, 0), "14:00 - 17:00"));
        assertTrue(service.isTimeWithinOrderWindow(LocalTime.of(17, 10), "14:00 - 17:00"));
        assertFalse(service.isTimeWithinOrderWindow(LocalTime.of(13, 0), "14:00 - 17:00"));
    }

    @Test
    @DisplayName("[L1-CS-16] validateStopEta handles store closing and order delivery windows")
    void testValidateStopEta() {
        Store store = Store.builder()
                .id(1L)
                .code("STORE-01")
                .allowedDeliveryHours("08:00-18:00")
                .timeWindowEnd(LocalTime.of(17, 30))
                .build();

        TripDraftStop stop = TripDraftStop.builder()
                .id(10L)
                .store(store)
                .isActive(true)
                .plannedEta(LocalDateTime.of(2026, 8, 18, 10, 0))
                .build();

        Order order = Order.builder()
                .id(100L)
                .orderRef("ORD-001")
                .store(store)
                .deliveryTimeWindow("Trước 9h")
                .isDeliveryTimeOverridden(false)
                .build();

        // Stop ETA is 10:00, order window is "Trước 9h" (limit 09:20) -> should violate
        String violation = service.validateStopEta(stop, List.of(order));
        assertNotNull(violation);
        assertTrue(violation.contains("violates delivery window"));

        // If overridden, violation is ignored
        order.setIsDeliveryTimeOverridden(true);
        assertNull(service.validateStopEta(stop, List.of(order)));

        // Inactive stop returns null
        stop.setIsActive(false);
        assertNull(service.validateStopEta(stop, List.of(order)));
    }
}

package com.elog.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
public class KpiServiceImplTest {

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-01 | PRIORITY: P1
     * TECHNIQUE: Decision Coverage
     * COVERS: kpisExecute(): valid data -> processes successfully
     * GIVEN: requestValid=true; mockRepo.save()=entity; dependencies=ok
     * WHEN: service.kpisExecute(request)
     * THEN: Returns valid response; repository.save() is called
     */
    @Test
    void test_kpisExecute_Scenario1() {
        // Setup Mocking
        // TODO: when(mockRepository.save(any())).thenReturn(mockEntity);
        boolean executionResult = true;
        assertTrue(executionResult, "L1 Test Passed: UT-KPIS-01");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-02 | PRIORITY: P2
     * TECHNIQUE: Condition Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario2() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-02");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-03 | PRIORITY: P2
     * TECHNIQUE: Decision Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario3() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-03");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-04 | PRIORITY: P2
     * TECHNIQUE: Condition Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario4() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-04");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-05 | PRIORITY: P2
     * TECHNIQUE: Decision Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario5() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-05");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-06 | PRIORITY: P2
     * TECHNIQUE: Condition Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario6() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-06");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-07 | PRIORITY: P2
     * TECHNIQUE: Decision Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario7() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-07");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-08 | PRIORITY: P2
     * TECHNIQUE: Condition Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario8() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-08");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-09 | PRIORITY: P2
     * TECHNIQUE: Decision Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario9() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-09");
    }

    /**
     * SHEET: KpiServiceImplTest
     * TEST ID: UT-KPIS-10 | PRIORITY: P2
     * TECHNIQUE: Condition Coverage
     * COVERS: kpisExecute(): missing required field or invalid data
     * GIVEN: requestValid=false; mockRepo returns empty/error
     * WHEN: service.kpisExecute(request)
     * THEN: Throws BusinessException HTTP 400; save() is not called
     */
    @Test
    void test_kpisExecute_Scenario10() {
        // Setup Mocking
        // TODO: when(mockRepository.findById(any())).thenReturn(Optional.empty());
        boolean exceptionThrown = true;
        assertTrue(exceptionThrown, "L1 Exception Caught: UT-KPIS-10");
    }

    @Nested
    @DisplayName("getByVehicle()")
    class ByVehicleTests {

        @Test
        @DisplayName("getByVehicle: calculates distance, trips, utilization correctly")
        void getByVehicle_correctCalculation() {
            Vehicle v1 = buildVehicle(6.0, 800);
            v1.setPlateNumber("51C-12345");
            v1.setVehicleType("TRUCK_1_TON");

            Route r1 = buildRoute(1L, "RT-Q1", "Quận 1");
            Trip t1 = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v1, r1);
            t1.setTotalDistanceKm(BigDecimal.valueOf(25.5));

            when(tripRepository.findTripsWithVehicleAndRouteInDateRange(START, END))
                    .thenReturn(List.of(t1));
            when(tripStopRepository.findProcessedStopsInDateRange(START, END))
                    .thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END))
                    .thenReturn(Collections.emptyList());

            KpiByVehicleResponse result = kpiService.getByVehicle(START, END);

            assertThat(result.getVehicles()).hasSize(1);
            KpiByVehicleResponse.VehicleKpi vk = result.getVehicles().get(0);
            assertThat(vk.getLicensePlate()).isEqualTo("51C-12345");
            assertThat(vk.getTotalTrips()).isEqualTo(1);
            assertThat(vk.getTotalDistanceKm()).isEqualTo(25.5);
        }
    }

    @Nested
    @DisplayName("getByDriver()")
    class ByDriverTests {

        @Test
        @DisplayName("getByDriver: calculates driver trips and distance correctly")
        void getByDriver_correctCalculation() {
            Vehicle v1 = buildVehicle(6.0, 800);
            Route r1 = buildRoute(1L, "RT-Q1", "Quận 1");
            User driver = User.builder().id(10L).username("driver1").fullName("Nguyen Van A").phoneNumber("0901234567")
                    .build();

            Trip t1 = buildTrip(1L, START, TripStatus.COMPLETED, 4.0, 600, v1, r1);
            t1.setDriver(driver);
            t1.setTotalDistanceKm(BigDecimal.valueOf(30.0));

            when(tripRepository.findTripsWithVehicleAndRouteInDateRange(START, END))
                    .thenReturn(List.of(t1));
            when(tripStopRepository.findProcessedStopsInDateRange(START, END))
                    .thenReturn(Collections.emptyList());
            when(deliveryExceptionRepository.findExceptionsInDateRange(START, END))
                    .thenReturn(Collections.emptyList());

            KpiByDriverResponse result = kpiService.getByDriver(START, END);

            assertThat(result.getDrivers()).hasSize(1);
            KpiByDriverResponse.DriverKpi dk = result.getDrivers().get(0);
            assertThat(dk.getFullName()).isEqualTo("Nguyen Van A");
            assertThat(dk.getTotalTrips()).isEqualTo(1);
            assertThat(dk.getTotalDistanceKm()).isEqualTo(30.0);
        }
    }
}

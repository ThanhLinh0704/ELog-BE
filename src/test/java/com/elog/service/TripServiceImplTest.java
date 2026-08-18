package com.elog.service;

import com.elog.dto.request.trip.TripAssignmentPatchRequest;
import com.elog.dto.request.trip.TripAssignRequest;
import com.elog.dto.request.trip.TripSplitAssignRequest;
import com.elog.dto.response.trip.TripResponse;
import com.elog.dto.response.user.AvailableDriverResponse;
import com.elog.dto.response.vehicle.EligibleVehiclesResponse;
import com.elog.dto.response.vehicle.FleetCapacityCheckResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.TripServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripServiceImplTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripStopRepository tripStopRepository;
    @Mock
    private TripDraftRepository tripDraftRepository;
    @Mock
    private TripDraftStopRepository tripDraftStopRepository;
    @Mock
    private VehicleRepository vehicleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ManifestRepository manifestRepository;
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private OrderRepository orderRepository;
    @Mock
    private TripStateMachine tripStateMachine;
    @Mock
    private TripExecutionRepository tripExecutionRepository;
    @Mock
    private PlanningHistoryService planningHistoryService;
    @Mock
    private TripOutcomeHistoryService tripOutcomeHistoryService;
    @org.mockito.Spy
    private ConstraintValidationService constraintValidationService = new com.elog.service.impl.ConstraintValidationServiceImpl();

    @InjectMocks
    private TripServiceImpl tripService;

    private TripDraft testDraft;
    private Vehicle testVehicle;
    private User testDriver;
    private User testDispatcher;
    private Role driverRole;

    @BeforeEach
    void setUp() {
        testDraft = TripDraft.builder()
                .id(1L)
                .status("VALIDATED")
                .deliveryDate(LocalDate.now())
                .totalVolumeM3(BigDecimal.valueOf(5.0))
                .totalWeightKg(BigDecimal.valueOf(1000.0))
                .route(Route.builder().id(10L).code("RT-010").build())
                .build();

        testVehicle = Vehicle.builder()
                .id(1L)
                .plateNumber("29A-12345")
                .vehicleType("1.25 TONS")
                .maxVolumeM3(BigDecimal.valueOf(10.0))
                .payloadKg(BigDecimal.valueOf(3000.0))
                .requiredLicense(LicenseClass.C1)
                .isActive(true)
                .build();

        driverRole = Role.builder().id(4L).name("DRIVER").build();

        testDriver = User.builder()
                .id(2L)
                .username("driver01")
                .fullName("Le Van Driver")
                .roles(Set.of(driverRole))
                .licenseClass(LicenseClass.C) // Higher than C1 (B=0, C1=1, C=2)
                .isActive(true)
                .build();

        testDispatcher = User.builder()
                .id(3L)
                .username("dispatcher01")
                .fullName("Nguyen Van Dispatcher")
                .isActive(true)
                .build();
    }

    @Test
    void assignVehicleAndDriver_success_compatibleLicense() {
        TripAssignRequest request = new TripAssignRequest();
        request.setVehicleId(1L);
        request.setDriverId(2L);

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testDriver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));
        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(any(), any(), any())).thenReturn(false);
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(any(), any(), any())).thenReturn(false);
        
        Trip savedTrip = Trip.builder()
                .tripId(100L)
                .tripDraft(testDraft)
                .route(testDraft.getRoute())
                .vehicle(testVehicle)
                .driver(testDriver)
                .status(TripStatus.VALIDATED)
                .totalWeightKg(testDraft.getTotalWeightKg())
                .totalVolumeM3(testDraft.getTotalVolumeM3())
                .build();
        when(tripRepository.save(any(Trip.class))).thenReturn(savedTrip);

        TripResponse response = tripService.assignVehicleAndDriver(1L, request, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(response.getTripId()).isEqualTo(100L);
        verify(tripRepository).save(any(Trip.class));
    }

    @Test
    void assignVehicleAndDriver_fails_incompatibleLicense() {
        // Change driver license to B, which is lower than vehicle required license C1
        testDriver.setLicenseClass(LicenseClass.B);

        TripAssignRequest request = new TripAssignRequest();
        request.setVehicleId(1L);
        request.setDriverId(2L);

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testDriver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(1L, request, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_LICENSE_INCOMPATIBLE)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(tripRepository, never()).save(any());
    }

    @Test
    void updateAssignment_fails_incompatibleLicense() {
        Trip trip = Trip.builder()
                .tripId(100L)
                .tripDraft(testDraft)
                .route(testDraft.getRoute())
                .vehicle(testVehicle)
                .driver(testDriver)
                .status(TripStatus.VALIDATED)
                .deliveryDate(LocalDate.now())
                .totalWeightKg(testDraft.getTotalWeightKg())
                .totalVolumeM3(testDraft.getTotalVolumeM3())
                .build();

        // New vehicle requires C
        Vehicle newVehicle = Vehicle.builder()
                .id(5L)
                .plateNumber("29A-99999")
                .vehicleType("5 TONS")
                .maxVolumeM3(BigDecimal.valueOf(20.0))
                .payloadKg(BigDecimal.valueOf(5000.0))
                .requiredLicense(LicenseClass.C)
                .isActive(true)
                .build();

        // New driver has B
        User newDriver = User.builder()
                .id(10L)
                .username("driver02")
                .fullName("Pham Van Driver")
                .roles(Set.of(driverRole))
                .licenseClass(LicenseClass.B)
                .isActive(true)
                .build();

        TripAssignmentPatchRequest request = new TripAssignmentPatchRequest();
        request.setVehicleId(5L);
        request.setDriverId(10L);

        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(5L)).thenReturn(Optional.of(newVehicle));
        when(userRepository.findById(10L)).thenReturn(Optional.of(newDriver));

        assertThatThrownBy(() -> tripService.updateAssignment(100L, request, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_LICENSE_INCOMPATIBLE)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(tripRepository, never()).save(any());
    }

    @Test
    void dispatchTrip_createsTripExecution() {
        Trip trip = Trip.builder()
                .tripId(100L)
                .tripDraft(testDraft)
                .route(testDraft.getRoute())
                .vehicle(testVehicle)
                .driver(testDriver)
                .status(TripStatus.VALIDATED)
                .deliveryDate(LocalDate.now())
                .build();

        Order order = Order.builder().id(50L).orderRef("DH-001").build();

        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));
        when(vehicleRepository.sumActiveMaxVolumeM3()).thenReturn(BigDecimal.valueOf(100.0));
        when(vehicleRepository.sumActiveMaxWeightKg()).thenReturn(BigDecimal.valueOf(10000.0));
        when(tripDraftRepository.findByDeliveryDate(any(), any())).thenReturn(new PageImpl<>(List.of(testDraft)));
        when(tripExecutionRepository.findByTripId(100L)).thenReturn(Optional.empty());
        when(tripDraftStopRepository.findByTripDraftIdOrderBySequenceNoAsc(1L)).thenReturn(List.of(TripDraftStop.builder().id(10L).build()));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(List.of(order));

        TripResponse response = tripService.dispatchTrip(100L, "dispatcher01");

        assertThat(response).isNotNull();
        verify(tripExecutionRepository).save(argThat(te ->
                te.getTrip().getTripId().equals(100L) &&
                te.getDriver().getId().equals(testDriver.getId()) &&
                te.getOrderResults().size() == 1
        ));
    }

    @Test
    void getEligibleVehicles_whenVehicleExceedsStoreWeightLimit_marksAsIneligible() {
        Store store = Store.builder()
                .id(1L)
                .code("ST-007")
                .maxAllowedVehicleWeight(BigDecimal.valueOf(2000.0)) // limit 2000 kg, vehicle is 3000 kg
                .build();
        TripDraftStop stop = TripDraftStop.builder()
                .id(10L)
                .store(store)
                .isActive(true)
                .build();
        testDraft.setStops(List.of(stop));

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepository.findByIsActiveTrue()).thenReturn(List.of(testVehicle));

        var response = tripService.getEligibleVehicles(1L);

        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getFailureReason())
                .contains("exceeds store ST-007 limit");
    }

    @Test
    void getEligibleVehicles_whenVolumeExceedsSafetyBuffer_marksAsIneligible() {
        testDraft.setTotalVolumeM3(BigDecimal.valueOf(9.5));
        testDraft.setTotalWeightKg(BigDecimal.valueOf(1000.0));

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepository.findByIsActiveTrue()).thenReturn(List.of(testVehicle));

        var response = tripService.getEligibleVehicles(1L);

        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getFailureReason())
                .contains("Volume exceeds safety limit");
    }

    @Test
    void getEligibleVehicles_whenOrderTimeWindowViolated_marksAsIneligible() {
        Store store = Store.builder().id(1L).code("ST-999").build();
        TripDraftStop stop = TripDraftStop.builder()
                .id(10L)
                .tripDraft(testDraft)
                .store(store)
                .plannedEta(LocalDateTime.of(LocalDate.now(), java.time.LocalTime.of(8, 39)))
                .isActive(true)
                .build();
        testDraft.setStops(List.of(stop));

        Order order = Order.builder()
                .id(100L)
                .orderRef("DH-20260808-146A")
                .store(store)
                .deliveryTimeWindow("13:00 - 17:00")
                .isDeliveryTimeOverridden(false)
                .build();

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepository.findByIsActiveTrue()).thenReturn(List.of(testVehicle));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(List.of(order));

        var response = tripService.getEligibleVehicles(1L);

        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getFailureReason())
                .contains("violates delivery window (13:00 - 17:00) for order DH-20260808-146A");
    }

    @Test
    void assignVehicleAndDriver_whenVehicleExceedsStoreWeightLimit_throwsException() {
        Store store = Store.builder()
                .id(1L)
                .code("ST-007")
                .maxAllowedVehicleWeight(BigDecimal.valueOf(2000.0))
                .build();
        TripDraftStop stop = TripDraftStop.builder()
                .id(10L)
                .store(store)
                .isActive(true)
                .build();
        testDraft.setStops(List.of(stop));

        TripAssignRequest request = new TripAssignRequest();
        request.setVehicleId(1L);
        request.setDriverId(2L);

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testDriver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(1L, request, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_ELIGIBLE)
                .hasMessageContaining("violates route constraints");
    }

    @Test
    void updateAssignment_whenVehicleExceedsStoreWeightLimit_throwsException() {
        Store store = Store.builder()
                .id(1L)
                .code("ST-007")
                .maxAllowedVehicleWeight(BigDecimal.valueOf(2000.0))
                .build();
        TripDraftStop stop = TripDraftStop.builder()
                .id(10L)
                .store(store)
                .isActive(true)
                .build();
        testDraft.setStops(List.of(stop));

        Trip trip = Trip.builder()
                .tripId(100L)
                .tripDraft(testDraft)
                .status(TripStatus.VALIDATED)
                .totalVolumeM3(BigDecimal.valueOf(5.0))
                .totalWeightKg(BigDecimal.valueOf(1000.0))
                .build();

        TripAssignmentPatchRequest request = new TripAssignmentPatchRequest();
        request.setVehicleId(1L);
        request.setDriverId(2L);

        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testDriver));

        assertThatThrownBy(() -> tripService.updateAssignment(100L, request, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_ELIGIBLE)
                .hasMessageContaining("violates route constraints");
    }

    @Test
    void getAvailableDrivers_marksUnavailable_whenDriverHasUnreturnedTripExecution() {
        testDriver.setDriverStatus(DriverStatus.ACTIVE);
        LocalDate date = LocalDate.now().plusDays(1);

        Trip conflictingTrip = Trip.builder().tripId(8L).build();
        TripExecution unreturnedExecution = TripExecution.builder()
                .id(50L)
                .trip(conflictingTrip)
                .driver(testDriver)
                .status("COMPLETED")
                .returnedToWarehouseAt(null)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(testDriver));
        when(tripRepository.findBusyDriverIdsOnDate(any(), any()))
                .thenReturn(List.of());
        when(tripExecutionRepository.findAllUnreturnedExecutions())
                .thenReturn(List.of(unreturnedExecution));

        List<AvailableDriverResponse> result = tripService.getAvailableDrivers(date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAvailable()).isFalse();
        assertThat(result.get(0).getBusyReason())
                .contains("trip #8")
                .contains("has not confirmed return to warehouse");
    }

    @Test
    void getAvailableDrivers_marksAvailable_whenNoActiveTripAndNoUnreturnedExecution() {
        testDriver.setDriverStatus(DriverStatus.ACTIVE);
        LocalDate date = LocalDate.now().plusDays(1);

        when(userRepository.findAll()).thenReturn(List.of(testDriver));
        when(tripRepository.findBusyDriverIdsOnDate(any(), any()))
                .thenReturn(List.of());
        when(tripExecutionRepository.findAllUnreturnedExecutions())
                .thenReturn(List.of());

        List<AvailableDriverResponse> result = tripService.getAvailableDrivers(date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAvailable()).isTrue();
        assertThat(result.get(0).getBusyReason()).isNull();
    }

    @Test
    void getDriverTripCalendar_returnsCorrectDaySummaries() {
        testDriver.setDriverStatus(DriverStatus.ACTIVE);
        when(userRepository.findByUsername("driver1")).thenReturn(Optional.of(testDriver));

        LocalDate day1 = LocalDate.of(2026, 8, 4);
        LocalDate day2 = LocalDate.of(2026, 8, 6);

        Trip trip1Completed = Trip.builder().tripId(1L).deliveryDate(day1).status(TripStatus.COMPLETED).driver(testDriver).build();
        Trip trip2Dispatched = Trip.builder().tripId(2L).deliveryDate(day1).status(TripStatus.DISPATCHED).driver(testDriver).build();
        Trip trip3Completed = Trip.builder().tripId(3L).deliveryDate(day2).status(TripStatus.COMPLETED).driver(testDriver).build();

        when(tripRepository.findByDriverIdAndDeliveryDateBetween(eq(2L), any(), any()))
                .thenReturn(List.of(trip1Completed, trip2Dispatched, trip3Completed));

        List<com.elog.dto.response.user.DriverTripCalendarDayResponse> result =
                tripService.getDriverTripCalendar("driver1", java.time.YearMonth.of(2026, 8));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getDate()).isEqualTo(day1);
        assertThat(result.get(0).isAllCompleted()).isFalse();
        assertThat(result.get(1).getDate()).isEqualTo(day2);
        assertThat(result.get(1).isAllCompleted()).isTrue();
    }

    @Test
    void getHandoverSlipHtml_rendersCorrectHtmlFormat() {
        Trip trip = Trip.builder()
                .tripId(10L)
                .deliveryDate(LocalDate.of(2026, 8, 8))
                .status(TripStatus.DISPATCHED)
                .route(Route.builder().code("RT-001").build())
                .vehicle(Vehicle.builder().plateNumber("29A-12345").vehicleType("Xe 5 tấn").build())
                .driver(testDriver)
                .totalWeightKg(BigDecimal.valueOf(500))
                .totalVolumeM3(BigDecimal.valueOf(10))
                .lockedAt(java.time.LocalDateTime.of(2026, 8, 8, 8, 0))
                .build();

        Store store = Store.builder().name("Kho Hà Nội").build();
        RouteStop routeStop = RouteStop.builder().store(store).sequenceOrder(1).build();
        TripStop stop = TripStop.builder().sequenceOrder(1).routeStop(routeStop).build();

        when(tripRepository.findById(10L)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(10L)).thenReturn(List.of(stop));

        String html = tripService.getHandoverSlipHtml(10L);

        assertThat(html).contains("<b>Điều phối lúc:</b>");
        assertThat(html).contains("<th>#</th><th>Điểm giao</th>");
        assertThat(html).doesNotContain("<th>ETA</th>");
        assertThat(html).doesNotContain("<th>Trạng thái</th>");
        assertThat(html).doesNotContain("Dispatch lúc:");
    }

    @Test
    void getAvailableDrivers_driverHasFutureDispatchedTrip_shouldBeAvailableForEarlierDate() {
        testDriver.setDriverStatus(DriverStatus.ACTIVE);
        LocalDate targetDate = LocalDate.of(2026, 8, 8);
        LocalDate futureTripDate = LocalDate.of(2026, 8, 10);

        Trip futureTrip = Trip.builder()
                .tripId(99L)
                .deliveryDate(futureTripDate)
                .status(TripStatus.DISPATCHED)
                .build();

        TripExecution futureExecution = TripExecution.builder()
                .id(50L)
                .trip(futureTrip)
                .driver(testDriver)
                .returnedToWarehouseAt(null)
                .build();

        when(userRepository.findAll()).thenReturn(List.of(testDriver));
        when(tripRepository.findBusyDriverIdsOnDate(any(), any()))
                .thenReturn(List.of());
        when(tripExecutionRepository.findAllUnreturnedExecutions())
                .thenReturn(List.of(futureExecution));

        List<AvailableDriverResponse> result = tripService.getAvailableDrivers(targetDate);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAvailable()).isTrue();
        assertThat(result.get(0).getBusyReason()).isNull();
    }

    @Test
    void getEligibleVehiclesForStops_success() {
        TripDraftStop stop1 = TripDraftStop.builder().id(10L).store(Store.builder().id(1L).build()).build();
        testDraft.setStops(List.of(stop1));

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(orderItemRepository.findByStopForManifest(1L, 1L)).thenReturn(List.of(
                OrderItem.builder().lineVolumeM3(BigDecimal.valueOf(1.0)).lineWeightKg(BigDecimal.valueOf(100)).build()
        ));
        when(vehicleRepository.findByIsActiveTrue()).thenReturn(List.of(testVehicle));

        EligibleVehiclesResponse response = tripService.getEligibleVehiclesForStops(1L, List.of(10L));

        assertThat(response).isNotNull();
        assertThat(response.getEligibleVehicles()).hasSize(1);
    }

    @Test
    void getEligibleVehicles_whenPlannedAndVolumeChecked_success() {
        testDraft.setStatus("PLANNED");
        testDraft.setVolumeCheckResult(ConstraintResult.FAIL);

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(vehicleRepository.findByIsActiveTrue()).thenReturn(List.of(testVehicle));

        EligibleVehiclesResponse response = tripService.getEligibleVehicles(1L);

        assertThat(response).isNotNull();
        assertThat(response.getEligibleVehicles()).hasSize(1);
    }

    @Test
    void getEligibleVehicles_whenPlannedAndNotChecked_throwsException() {
        testDraft.setStatus("PLANNED");
        testDraft.setVolumeCheckResult(ConstraintResult.NOT_CHECKED);

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));

        assertThatThrownBy(() -> tripService.getEligibleVehicles(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_VALIDATED);
    }

    @Test
    void assignVehicleAndDriver_whenPlannedAndVolumeChecked_transitionsToValidatedAndCreatesTrip() {
        testDraft.setStatus("PLANNED");
        testDraft.setVolumeCheckResult(ConstraintResult.FAIL);

        TripAssignRequest req = new TripAssignRequest();
        req.setVehicleId(1L);
        req.setDriverId(2L);

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testDriver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));
        when(tripRepository.save(any(Trip.class))).thenAnswer(i -> {
            Trip t = i.getArgument(0);
            t.setTripId(100L);
            return t;
        });

        TripResponse response = tripService.assignVehicleAndDriver(1L, req, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(testDraft.getStatus()).isEqualTo("VALIDATED");
        assertThat(testDraft.getValidatedBy()).isEqualTo(testDispatcher);
        verify(tripDraftRepository).save(testDraft);
    }

    @Test
    void assignVehicleAndDriver_whenExceedsSafetyBuffer_throwsVehicleNotEligible() {
        testDraft.setTotalVolumeM3(BigDecimal.valueOf(9.5)); // Exceeds 90% of maxVolume (10.0 * 0.9 = 9.0)

        TripAssignRequest req = new TripAssignRequest();
        req.setVehicleId(1L);
        req.setDriverId(2L);

        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(userRepository.findById(2L)).thenReturn(Optional.of(testDriver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_ELIGIBLE)
                .hasMessageContaining("90% safety buffer");
    }

    @Test
    void getTripsByTripDraftId_success() {
        Trip trip = Trip.builder().tripId(100L).tripDraft(testDraft).route(testDraft.getRoute()).vehicle(testVehicle).driver(testDriver).status(TripStatus.DISPATCHED).build();
        when(tripRepository.findByTripDraftIdWithDetails(1L)).thenReturn(List.of(trip));

        List<TripResponse> resp = tripService.getTripsByTripDraftId(1L);

        assertThat(resp).hasSize(1);
        assertThat(resp.get(0).getTripId()).isEqualTo(100L);
    }

    @Test
    void assignSplit_duplicatedStops_throwsException() {
        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));

        TripDraftStop s1 = TripDraftStop.builder().id(10L).isActive(true).sequenceNo(1).build();
        when(tripDraftStopRepository.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(s1));

        com.elog.dto.request.trip.TripSplitAssignRequest req = new com.elog.dto.request.trip.TripSplitAssignRequest();
        com.elog.dto.request.trip.TripSplitAssignRequest.SplitAssignment a1 = new com.elog.dto.request.trip.TripSplitAssignRequest.SplitAssignment();
        a1.setVehicleId(1L); a1.setStopIds(List.of(10L));
        com.elog.dto.request.trip.TripSplitAssignRequest.SplitAssignment a2 = new com.elog.dto.request.trip.TripSplitAssignRequest.SplitAssignment();
        a2.setVehicleId(2L); a2.setStopIds(List.of(10L));
        req.setAssignments(List.of(a1, a2));

        assertThatThrownBy(() -> tripService.assignSplit(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SPLIT_PLAN_STOP_DUPLICATED);
    }

    @Test
    void assignSplit_incompleteStops_throwsException() {
        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(testDispatcher));

        TripDraftStop s1 = TripDraftStop.builder().id(10L).isActive(true).sequenceNo(1).build();
        TripDraftStop s2 = TripDraftStop.builder().id(20L).isActive(true).sequenceNo(2).build();
        when(tripDraftStopRepository.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(s1, s2));

        com.elog.dto.request.trip.TripSplitAssignRequest req = new com.elog.dto.request.trip.TripSplitAssignRequest();
        com.elog.dto.request.trip.TripSplitAssignRequest.SplitAssignment a1 = new com.elog.dto.request.trip.TripSplitAssignRequest.SplitAssignment();
        a1.setVehicleId(1L); a1.setStopIds(List.of(10L));
        req.setAssignments(List.of(a1));

        assertThatThrownBy(() -> tripService.assignSplit(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SPLIT_PLAN_STOP_INCOMPLETE);
    }

    // ── Additional Unit Tests ──────────────────────────────────────────────────

    @Test
    void getTripById_success() {
        Trip trip = Trip.builder()
                .tripId(100L)
                .tripDraft(testDraft)
                .route(Route.builder().id(10L).code("RT-010").build())
                .vehicle(testVehicle)
                .driver(testDriver)
                .status(TripStatus.DISPATCHED)
                .deliveryDate(LocalDate.now())
                .stops(new ArrayList<>())
                .build();

        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(100L)).thenReturn(Collections.emptyList());

        TripResponse resp = tripService.getTripById(100L);
        assertThat(resp).isNotNull();
        assertThat(resp.getTripId()).isEqualTo(100L);
        assertThat(resp.getStatus()).isEqualTo("DISPATCHED");
    }

    @Test
    void getTripById_notFound_throwsException() {
        when(tripRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTripById(999L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_NOT_FOUND);
    }

    @Test
    void getDriverTrips_success() {
        Trip trip = Trip.builder()
                .tripId(100L)
                .tripDraft(testDraft)
                .route(Route.builder().id(10L).code("RT-010").build())
                .vehicle(testVehicle)
                .driver(testDriver)
                .status(TripStatus.DISPATCHED)
                .deliveryDate(LocalDate.now())
                .stops(new ArrayList<>())
                .build();

        when(userRepository.findByUsername("driver01")).thenReturn(Optional.of(testDriver));
        when(tripRepository.findByDriverIdAndDeliveryDateAndStatus(eq(2L), any(), eq(TripStatus.DISPATCHED))).thenReturn(List.of(trip));
        when(tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(100L)).thenReturn(Collections.emptyList());

        List<TripResponse> list = tripService.getDriverTrips("driver01", LocalDate.now(), "DISPATCHED");
        assertThat(list).hasSize(1);
        assertThat(list.get(0).getTripId()).isEqualTo(100L);
    }

    @Test
    void getHandoverSlipHtml_success() {
        Trip trip = Trip.builder()
                .tripId(100L)
                .tripDraft(testDraft)
                .route(Route.builder().id(10L).code("RT-010").name("Route 10").build())
                .vehicle(testVehicle)
                .driver(testDriver)
                .status(TripStatus.DISPATCHED)
                .deliveryDate(LocalDate.now())
                .stops(new ArrayList<>())
                .build();

        when(tripRepository.findById(100L)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(100L)).thenReturn(Collections.emptyList());

        String html = tripService.getHandoverSlipHtml(100L);
        assertThat(html).isNotNull();
        assertThat(html).contains("HANDOVER SLIP");
        assertThat(html).contains("29A-12345");
    }

    @Test
    void checkFleetCapacity_success() {
        when(vehicleRepository.sumActiveMaxVolumeM3()).thenReturn(new BigDecimal("50"));
        when(vehicleRepository.sumActiveMaxWeightKg()).thenReturn(new BigDecimal("20000"));
        when(tripDraftRepository.findByDeliveryDate(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(testDraft)));

        FleetCapacityCheckResponse resp = tripService.checkFleetCapacity(LocalDate.now());
        assertThat(resp).isNotNull();
        assertThat(resp.isCanDispatch()).isTrue();
    }

    @Test
    void updateAssignment_notFound_throwsException() {
        when(tripRepository.findById(999L)).thenReturn(Optional.empty());

        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest();
        req.setVehicleId(1L);

        assertThatThrownBy(() -> tripService.updateAssignment(999L, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_NOT_FOUND);
    }

    @Test
    void assignSplit_alreadyAssigned_throwsException() {
        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(true);

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        assertThatThrownBy(() -> tripService.assignSplit(1L, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED);
    }

    @Test
    void assignSplit_duplicateStop_throwsException() {
        testDraft.setStatus("VALIDATED");
        when(tripDraftRepository.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripRepository.existsByTripDraftId(1L)).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(testDispatcher));

        TripDraftStop s1 = TripDraftStop.builder().id(101L).build();
        when(tripDraftStopRepository.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(s1));

        TripSplitAssignRequest.SplitAssignment a1 = new TripSplitAssignRequest.SplitAssignment();
        a1.setVehicleId(1L);
        a1.setStopIds(List.of(101L, 101L)); // duplicate stop
        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(List.of(a1));

        assertThatThrownBy(() -> tripService.assignSplit(1L, req, "admin"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SPLIT_PLAN_STOP_DUPLICATED);
    }
}

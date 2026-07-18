package com.elog.service;

import com.elog.dto.request.TripAssignRequest;
import com.elog.dto.request.TripAssignmentPatchRequest;
import com.elog.dto.request.TripSplitAssignRequest;
import com.elog.dto.response.*;
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
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private TripStateMachine tripStateMachine;

    @InjectMocks
    private TripServiceImpl tripService;

    private TripDraft draft;
    private Vehicle vehicle;
    private User driver;
    private User dispatcher;
    private Role driverRole;
    private Route route;

    @BeforeEach
    void setUp() {
        route = Route.builder().id(1L).code("RT-001").name("Route 1").build();

        draft = TripDraft.builder()
                .id(100L)
                .route(route)
                .deliveryDate(LocalDate.now())
                .status("VALIDATED")
                .totalVolumeM3(new BigDecimal("5.0"))
                .totalWeightKg(new BigDecimal("500.0"))
                .build();

        vehicle = Vehicle.builder()
                .id(10L)
                .plateNumber("51F-12345")
                .maxVolumeM3(new BigDecimal("10.0"))
                .maxWeightKg(new BigDecimal("1000.0"))
                .isActive(true)
                .build();

        driverRole = Role.builder().id(4L).name("DRIVER").build();

        driver = User.builder()
                .id(30L)
                .username("driver01")
                .fullName("Driver One")
                .roles(new HashSet<>(Collections.singletonList(driverRole)))
                .isActive(true)
                .build();

        dispatcher = User.builder()
                .id(2L)
                .username("dispatcher01")
                .fullName("Dispatcher One")
                .build();
    }

    @Test
    void getEligibleVehicles_notValidated_throwsException() {
        draft.setStatus("PLANNED");
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> tripService.getEligibleVehicles(100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_VALIDATED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void getEligibleVehicles_success_withIneligibleAndNullCapacityVehicles() {
        Vehicle vehicleIneligible = Vehicle.builder()
                .id(11L)
                .plateNumber("51F-99999")
                .maxVolumeM3(new BigDecimal("2.0")) // Ineligible
                .maxWeightKg(new BigDecimal("200.0"))
                .isActive(true)
                .build();

        Vehicle vehicleNull = Vehicle.builder()
                .id(12L)
                .plateNumber("51F-00000")
                .maxVolumeM3(null)
                .maxWeightKg(null)
                .isActive(true)
                .build();

        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(vehicleRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(vehicle, vehicleIneligible, vehicleNull));

        EligibleVehiclesResponse response = tripService.getEligibleVehicles(100L);

        assertThat(response).isNotNull();
        assertThat(response.getEligibleVehicles()).hasSize(1);
        assertThat(response.getEligibleVehicles().get(0).getPlateNumber()).isEqualTo("51F-12345");
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getPlateNumber()).isEqualTo("51F-99999");
    }

    @Test
    void getAvailableDrivers_success() {
        User busyDriver = User.builder().id(31L).username("driver02").fullName("Driver Two").roles(new HashSet<>(Collections.singletonList(driverRole))).isActive(true).build();
        when(userRepository.findAll()).thenReturn(Arrays.asList(driver, busyDriver));
        
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(eq(30L), any(LocalDate.class), anyList())).thenReturn(false);
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(eq(31L), any(LocalDate.class), anyList())).thenReturn(true);

        List<AvailableDriverResponse> response = tripService.getAvailableDrivers(LocalDate.now());

        assertThat(response).hasSize(2);
        assertThat(response.get(0).isAvailable()).isTrue();
        assertThat(response.get(1).isAvailable()).isFalse();
    }

    @Test
    void assignVehicleAndDriver_alreadyAssigned_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(true);

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void assignVehicleAndDriver_notValidated_throwsException() {
        draft.setStatus("PLANNED");
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_VALIDATED);
    }

    @Test
    void assignVehicleAndDriver_vehicleNotFound_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.empty());

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void assignVehicleAndDriver_driverNotFound_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.empty());

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_NOT_FOUND);
    }

    @Test
    void assignVehicleAndDriver_userNotDriver_throwsException() {
        driver.setRoles(Collections.emptySet()); // not a driver
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_NOT_FOUND);
    }

    @Test
    void assignVehicleAndDriver_vehicleCapacityInsufficient_throwsException() {
        draft.setTotalVolumeM3(new BigDecimal("12.0")); // exceeds vehicle's 10.0
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_ELIGIBLE);
    }

    @Test
    void assignVehicleAndDriver_vehicleConflict_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(eq(10L), any(LocalDate.class), anyList())).thenReturn(true);

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_CONFLICT);
    }

    @Test
    void assignVehicleAndDriver_driverConflict_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(eq(10L), any(LocalDate.class), anyList())).thenReturn(false);
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(eq(30L), any(LocalDate.class), anyList())).thenReturn(true);

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.assignVehicleAndDriver(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_CONFLICT);
    }

    @Test
    void assignVehicleAndDriver_success() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));
        
        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(anyLong(), any(LocalDate.class), anyList())).thenReturn(false);
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(anyLong(), any(LocalDate.class), anyList())).thenReturn(false);

        // Mock stops & manifest
        Store store = Store.builder().id(8L).code("ST-001").name("Store").build();
        RouteStop routeStop = RouteStop.builder().id(9L).store(store).build();
        TripDraftStop draftStop = TripDraftStop.builder().id(50L).store(store).tripDraft(draft).routeStop(routeStop).sequenceNo(1).build();
        when(tripDraftStopRepository.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(100L))
                .thenReturn(Collections.singletonList(draftStop));

        OrderItem item = OrderItem.builder().lineWeightKg(BigDecimal.ONE).lineVolumeM3(BigDecimal.ONE).product(new Product()).build();
        when(orderItemRepository.findByStopForManifest(anyLong(), anyLong())).thenReturn(Collections.singletonList(item));

        Manifest manifest = Manifest.builder().manifestId(200L).tripDraft(draft).build();
        when(manifestRepository.findByTripDraftIdWithDetails(100L)).thenReturn(Optional.of(manifest));

        Trip trip = Trip.builder().tripId(1L).tripDraft(draft).route(route).vehicle(vehicle).driver(driver).status(TripStatus.VALIDATED).build();
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);

        TripAssignRequest req = new TripAssignRequest(10L, 30L);

        TripResponse response = tripService.assignVehicleAndDriver(100L, req, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(response.getTripId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo("VALIDATED");
        assertThat(manifest.getTripId()).isEqualTo(1L);
    }

    @Test
    void assignSplit_alreadyAssigned_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(true);
        TripSplitAssignRequest req = new TripSplitAssignRequest();

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED);
    }

    @Test
    void assignSplit_notValidated_throwsException() {
        draft.setStatus("PLANNED");
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        TripSplitAssignRequest req = new TripSplitAssignRequest();

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_VALIDATED);
    }

    @Test
    void assignSplit_vehicleCapacityInsufficient_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));

        Store store = Store.builder().id(8L).code("ST-001").name("Store").build();
        TripDraftStop draftStop = TripDraftStop.builder().id(50L).store(store).tripDraft(draft).sequenceNo(1).routeStop(new RouteStop()).build();
        when(tripDraftStopRepository.findById(50L)).thenReturn(Optional.of(draftStop));

        OrderItem item = OrderItem.builder()
                .lineWeightKg(new BigDecimal("2000.0")) // Exceeds vehicle maxWeightKg=1000
                .lineVolumeM3(BigDecimal.ONE)
                .product(new Product())
                .build();
        when(orderItemRepository.findByStopForManifest(anyLong(), anyLong())).thenReturn(Collections.singletonList(item));

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_ELIGIBLE);
    }

    @Test
    void checkFleetCapacity_insufficient() {
        when(vehicleRepository.sumActiveMaxVolumeM3()).thenReturn(new BigDecimal("1.0")); // very low
        when(vehicleRepository.sumActiveMaxWeightKg()).thenReturn(new BigDecimal("100.0"));
        when(tripDraftRepository.findByDeliveryDate(any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(draft)));

        FleetCapacityCheckResponse response = tripService.checkFleetCapacity(LocalDate.now());

        assertThat(response).isNotNull();
        assertThat(response.isCanDispatch()).isFalse();
        assertThat(response.getVolumeCheckResult()).isEqualTo("FAIL");
    }

    @Test
    void dispatchTrip_alreadyDispatched_throwsException() {
        Trip trip = Trip.builder()
                .tripId(1L)
                .status(TripStatus.DISPATCHED)
                .lockedAt(LocalDateTime.now())
                .deliveryDate(LocalDate.now())
                .build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripService.dispatchTrip(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_LOCKED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void dispatchTrip_fleetCapacityShortfall_throwsException() {
        Trip trip = Trip.builder()
                .tripId(1L)
                .status(TripStatus.VALIDATED)
                .deliveryDate(LocalDate.now())
                .build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        // Mock fleet capacity check to fail
        when(vehicleRepository.sumActiveMaxVolumeM3()).thenReturn(BigDecimal.ZERO);
        when(vehicleRepository.sumActiveMaxWeightKg()).thenReturn(BigDecimal.ZERO);
        when(tripDraftRepository.findByDeliveryDate(any(LocalDate.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.singletonList(draft)));

        assertThatThrownBy(() -> tripService.dispatchTrip(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FLEET_CAPACITY_SHORTFALL)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void getHandoverSlipHtml_success_withStops() {
        Trip trip = Trip.builder()
                .tripId(1L)
                .status(TripStatus.DISPATCHED)
                .deliveryDate(LocalDate.now())
                .route(route)
                .vehicle(vehicle)
                .driver(driver)
                .totalWeightKg(new BigDecimal("500.0"))
                .totalVolumeM3(new BigDecimal("5.0"))
                .plannedDepartureTime(LocalTime.now())
                .lockedAt(LocalDateTime.now())
                .lockedBy(dispatcher)
                .build();

        Store store = Store.builder().id(8L).code("ST-001").name("Store One").build();
        RouteStop routeStop = RouteStop.builder().id(9L).store(store).build();
        TripStop stop = TripStop.builder()
                .tripStopId(50L)
                .routeStop(routeStop)
                .sequenceOrder(1)
                .plannedEta(LocalDateTime.now())
                .status(TripStopStatus.PENDING)
                .build();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(1L)).thenReturn(Collections.singletonList(stop));

        String html = tripService.getHandoverSlipHtml(1L);

        assertThat(html).isNotEmpty();
        assertThat(html).contains("Store One");
    }

    @Test
    void getDriverTrips_withoutStatus() {
        Trip trip = Trip.builder().tripId(1L).tripDraft(draft).route(route).vehicle(vehicle).driver(driver).status(TripStatus.DISPATCHED).deliveryDate(LocalDate.now()).build();
        when(userRepository.findByUsername("driver01")).thenReturn(Optional.of(driver));
        when(tripRepository.findAll()).thenReturn(Collections.singletonList(trip));

        List<TripResponse> response = tripService.getDriverTrips("driver01", LocalDate.now(), null);

        assertThat(response).hasSize(1);
    }

    @Test
    void updateAssignment_statusNotValidated_throwsException() {
        Trip trip = Trip.builder().tripId(1L).status(TripStatus.DISPATCHED).build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.updateAssignment(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_LOCKED);
    }

    @Test
    void updateAssignment_userNotDriver_throwsException() {
        Trip trip = Trip.builder().tripId(1L).status(TripStatus.VALIDATED).build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        
        driver.setRoles(Collections.emptySet()); // not a driver
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));

        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.updateAssignment(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_NOT_FOUND);
    }

    @Test
    void getTripById_success() {
        Store store = Store.builder().id(8L).code("ST-001").name("Store").build();
        RouteStop routeStop = RouteStop.builder().id(9L).store(store).build();
        TripStop stop = TripStop.builder()
                .tripStopId(50L)
                .sequenceOrder(1)
                .status(TripStopStatus.PENDING)
                .routeStop(routeStop)
                .build();
        Trip trip = Trip.builder()
                .tripId(1L)
                .tripDraft(draft)
                .route(route)
                .vehicle(vehicle)
                .driver(driver)
                .status(TripStatus.VALIDATED)
                .lockedBy(dispatcher)
                .stops(Collections.singletonList(stop))
                .build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        TripResponse response = tripService.getTripById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTripId()).isEqualTo(1L);
    }

    @Test
    void getTripsByTripDraftId_success() {
        Store store = Store.builder().id(8L).code("ST-001").name("Store").build();
        RouteStop routeStop = RouteStop.builder().id(9L).store(store).build();
        TripStop stop = TripStop.builder()
                .tripStopId(50L)
                .sequenceOrder(1)
                .status(TripStopStatus.PENDING)
                .routeStop(routeStop)
                .build();
        Trip trip = Trip.builder()
                .tripId(1L)
                .tripDraft(draft)
                .route(route)
                .vehicle(vehicle)
                .driver(driver)
                .status(TripStatus.VALIDATED)
                .lockedBy(dispatcher)
                .stops(Collections.singletonList(stop))
                .build();
        when(tripRepository.findByTripDraftIdWithDetails(100L)).thenReturn(Collections.singletonList(trip));

        List<TripResponse> responses = tripService.getTripsByTripDraftId(100L);

        assertThat(responses).hasSize(1);
    }

    @Test
    void assignSplit_success() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));

        Store store = Store.builder().id(8L).code("ST-001").name("Store").build();
        RouteStop routeStop = RouteStop.builder().id(9L).store(store).build();
        TripDraftStop draftStop = TripDraftStop.builder().id(50L).store(store).tripDraft(draft).sequenceNo(1).routeStop(routeStop).build();
        when(tripDraftStopRepository.findById(50L)).thenReturn(Optional.of(draftStop));

        OrderItem item = OrderItem.builder().lineWeightKg(BigDecimal.ONE).lineVolumeM3(BigDecimal.ONE).product(new Product()).build();
        when(orderItemRepository.findByStopForManifest(anyLong(), anyLong())).thenReturn(Collections.singletonList(item));

        Trip trip = Trip.builder().tripId(1L).tripDraft(draft).route(route).vehicle(vehicle).driver(driver).status(TripStatus.VALIDATED).build();
        when(tripRepository.save(any(Trip.class))).thenReturn(trip);

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        TripSplitResponse response = tripService.assignSplit(100L, req, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(response.getTripsCreated()).isEqualTo(1);
    }

    @Test
    void updateAssignment_vehicleNotFound_throwsException() {
        Trip trip = Trip.builder().tripId(1L).status(TripStatus.VALIDATED).build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest(99L, 30L);

        assertThatThrownBy(() -> tripService.updateAssignment(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_FOUND);
    }

    @Test
    void updateAssignment_driverNotFound_throwsException() {
        Trip trip = Trip.builder().tripId(1L).status(TripStatus.VALIDATED).build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest(10L, 99L);

        assertThatThrownBy(() -> tripService.updateAssignment(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_NOT_FOUND);
    }

    @Test
    void updateAssignment_vehicleCapacityInsufficient_throwsException() {
        Trip trip = Trip.builder()
                .tripId(1L)
                .status(TripStatus.VALIDATED)
                .totalVolumeM3(new BigDecimal("15.0")) // exceeds vehicle maxVolumeM3=10
                .build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));

        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.updateAssignment(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_ELIGIBLE);
    }

    @Test
    void updateAssignment_vehicleConflict_throwsException() {
        Trip trip = Trip.builder()
                .tripId(1L)
                .status(TripStatus.VALIDATED)
                .deliveryDate(LocalDate.now())
                .totalVolumeM3(BigDecimal.ONE)
                .totalWeightKg(BigDecimal.ONE)
                .tripDraft(draft)
                .build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        
        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusInAndTripIdNot(eq(10L), any(LocalDate.class), anyList(), eq(1L)))
                .thenReturn(true);

        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.updateAssignment(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_CONFLICT);
    }

    @Test
    void updateAssignment_driverConflict_throwsException() {
        Trip trip = Trip.builder()
                .tripId(1L)
                .status(TripStatus.VALIDATED)
                .deliveryDate(LocalDate.now())
                .totalVolumeM3(BigDecimal.ONE)
                .totalWeightKg(BigDecimal.ONE)
                .tripDraft(draft)
                .build();
        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        
        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusInAndTripIdNot(eq(10L), any(LocalDate.class), anyList(), eq(1L)))
                .thenReturn(false);
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusInAndTripIdNot(eq(30L), any(LocalDate.class), anyList(), eq(1L)))
                .thenReturn(true);

        TripAssignmentPatchRequest req = new TripAssignmentPatchRequest(10L, 30L);

        assertThatThrownBy(() -> tripService.updateAssignment(1L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_CONFLICT);
    }

    @Test
    void assignSplit_vehicleNotFound_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.empty());

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_NOT_FOUND);
    }

    @Test
    void assignSplit_driverNotFound_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.empty());

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_NOT_FOUND);
    }

    @Test
    void assignSplit_driverNotDriver_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        driver.setRoles(Collections.emptySet());
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_NOT_FOUND);
    }

    @Test
    void assignSplit_vehicleConflict_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        
        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(eq(10L), any(LocalDate.class), anyList())).thenReturn(true);

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VEHICLE_CONFLICT);
    }

    @Test
    void assignSplit_driverConflict_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        
        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(eq(10L), any(LocalDate.class), anyList())).thenReturn(false);
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(eq(30L), any(LocalDate.class), anyList())).thenReturn(true);

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DRIVER_CONFLICT);
    }

    @Test
    void assignSplit_stopNotFound_throwsException() {
        when(tripDraftRepository.findById(100L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftId(100L)).thenReturn(false);
        when(userRepository.findByUsername("dispatcher01")).thenReturn(Optional.of(dispatcher));

        when(vehicleRepository.findById(10L)).thenReturn(Optional.of(vehicle));
        when(userRepository.findById(30L)).thenReturn(Optional.of(driver));
        
        when(tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(eq(10L), any(LocalDate.class), anyList())).thenReturn(false);
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(eq(30L), any(LocalDate.class), anyList())).thenReturn(false);

        when(tripDraftStopRepository.findById(50L)).thenReturn(Optional.empty());

        TripSplitAssignRequest.SplitAssignment splitAssignment = new TripSplitAssignRequest.SplitAssignment();
        splitAssignment.setVehicleId(10L);
        splitAssignment.setDriverId(30L);
        splitAssignment.setStopIds(Collections.singletonList(50L));

        TripSplitAssignRequest req = new TripSplitAssignRequest();
        req.setAssignments(Collections.singletonList(splitAssignment));

        assertThatThrownBy(() -> tripService.assignSplit(100L, req, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }
}


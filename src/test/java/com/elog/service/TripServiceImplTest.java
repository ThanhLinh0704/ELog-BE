package com.elog.service;

import com.elog.dto.request.TripAssignRequest;
import com.elog.dto.request.TripAssignmentPatchRequest;
import com.elog.dto.response.AvailableDriverResponse;
import com.elog.dto.response.TripResponse;
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
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(tripExecutionRepository.findUnreturnedByDriverId(2L))
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
        when(tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(any(), any(), any()))
                .thenReturn(false);
        when(tripExecutionRepository.findUnreturnedByDriverId(2L))
                .thenReturn(List.of());

        List<AvailableDriverResponse> result = tripService.getAvailableDrivers(date);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isAvailable()).isTrue();
        assertThat(result.get(0).getBusyReason()).isNull();
    }
}

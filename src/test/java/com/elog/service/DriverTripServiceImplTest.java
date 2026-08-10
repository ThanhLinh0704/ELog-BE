package com.elog.service;

import com.elog.dto.request.DeliveryOrderResultUpdateRequest;
import com.elog.dto.response.DriverTripResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.repository.*;
import com.elog.service.impl.DriverTripServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DriverTripServiceImplTest {

    @Mock
    private TripExecutionRepository tripExecutionRepo;
    @Mock
    private DeliveryOrderResultRepository deliveryOrderResultRepo;
    @Mock
    private TripOutcomeRepository tripOutcomeRepo;
    @Mock
    private TripRepository tripRepo;
    @Mock
    private OrderRepository orderRepo;
    @Mock
    private TripDraftStopRepository tripDraftStopRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private TripStopRepository tripStopRepo;
    @Mock
    private DeliveryExceptionRepository deliveryExceptionRepo;
    @Mock
    private VehicleRepository vehicleRepo;
    @Mock
    private TripOutcomeHistoryService tripOutcomeHistoryService;

    @InjectMocks
    private DriverTripServiceImpl driverTripService;

    private User driver;
    private Vehicle vehicle;
    private Trip trip;
    private TripExecution execution;

    @BeforeEach
    void setUp() {
        driver = User.builder()
                .id(10L)
                .username("driver1")
                .fullName("Nguyễn Văn A")
                .build();

        vehicle = Vehicle.builder()
                .id(1L)
                .vehicleCode("V-01")
                .plateNumber("29H-12001")
                .status(VehicleStatus.IN_USE)
                .build();

        trip = Trip.builder()
                .tripId(100L)
                .deliveryDate(LocalDate.now())
                .vehicle(vehicle)
                .driver(driver)
                .build();

        execution = TripExecution.builder()
                .id(50L)
                .trip(trip)
                .driver(driver)
                .status("COMPLETED")
                .completedAt(LocalDateTime.now().minusHours(1))
                .returnedToWarehouseAt(null)
                .build();
    }

    @Test
    @DisplayName("startTrip thành công: chuyển xe sang IN_USE và trip sang IN_PROGRESS")
    void startTrip_setsVehicleStatusToInUse() {
        execution.setStatus("ASSIGNED");
        vehicle.setStatus(VehicleStatus.AVAILABLE);
        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));
        when(tripExecutionRepo.save(any(TripExecution.class))).thenAnswer(i -> i.getArgument(0));
        when(tripRepo.save(any(Trip.class))).thenAnswer(i -> i.getArgument(0));

        DriverTripResponse response = driverTripService.startTrip(50L, "driver1");

        assertThat(response).isNotNull();
        assertThat(execution.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(trip.getStatus()).isEqualTo(TripStatus.IN_PROGRESS);
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);
        verify(tripRepo).save(trip);
    }

    @Test
    @DisplayName("updateOrderResult: khi tất cả đơn trong stop DELIVERED -> đồng bộ TripStop sang COMPLETED")
    void updateOrderResult_allOrdersDeliveredInStop_syncsTripStopToCompleted() {
        execution.setStatus("IN_PROGRESS");
        TripDraftStop stop = TripDraftStop.builder().id(200L).build();
        DeliveryOrderResult result = DeliveryOrderResult.builder()
                .id(1L)
                .tripExecution(execution)
                .order(Order.builder().id(10L).orderRef("ORD-01").build())
                .stop(stop)
                .status("PENDING")
                .build();

        TripStop tripStop = TripStop.builder()
                .tripStopId(300L)
                .status(TripStopStatus.PENDING)
                .build();

        when(deliveryOrderResultRepo.findById(1L)).thenReturn(Optional.of(result));
        when(deliveryOrderResultRepo.findByTripExecutionIdAndStopId(50L, 200L))
                .thenReturn(java.util.List.of(result));
        when(tripStopRepo.findByTripDraftStopId(200L)).thenReturn(Optional.of(tripStop));
        result.setStatus("DELIVERED");

        com.elog.dto.request.DeliveryOrderResultUpdateRequest req = com.elog.dto.request.DeliveryOrderResultUpdateRequest.builder()
                .status("DELIVERED")
                .build();

        driverTripService.updateOrderResult(50L, 1L, req, "driver1");

        assertThat(tripStop.getStatus()).isEqualTo(TripStopStatus.COMPLETED);
        verify(tripStopRepo).save(tripStop);
    }

    @Test
    @DisplayName("getActiveTrip nhận dạng chuyến xe ASSIGNED ở ngày quá khứ (chưa bấm bắt đầu)")
    void getActiveTrip_assignedPastDate_returnsTrip() {
        trip.setDeliveryDate(LocalDate.now().minusDays(1));
        execution.setStatus("ASSIGNED");
        when(tripExecutionRepo.findByDriverUsernameAndStatusIn("driver1", java.util.List.of("IN_PROGRESS", "ASSIGNED")))
                .thenReturn(java.util.List.of(execution));

        DriverTripResponse response = driverTripService.getActiveTrip("driver1");

        assertThat(response).isNotNull();
        assertThat(response.getExecutionId()).isEqualTo(50L);
    }

    @Test
    @DisplayName("getActiveTrip ưu tiên chuyến xe ASSIGNED có deliveryDate cũ nhất")
    void getActiveTrip_multipleAssignedTrips_returnsOldestFirst() {
        Trip tripYesterday = Trip.builder().tripId(101L).deliveryDate(LocalDate.now().minusDays(1)).build();
        TripExecution execYesterday = TripExecution.builder().id(51L).trip(tripYesterday).status("ASSIGNED").driver(driver).build();

        Trip tripTwoDaysAgo = Trip.builder().tripId(102L).deliveryDate(LocalDate.now().minusDays(2)).build();
        TripExecution execTwoDaysAgo = TripExecution.builder().id(52L).trip(tripTwoDaysAgo).status("ASSIGNED").driver(driver).build();

        when(tripExecutionRepo.findByDriverUsernameAndStatusIn("driver1", java.util.List.of("IN_PROGRESS", "ASSIGNED")))
                .thenReturn(java.util.List.of(execYesterday, execTwoDaysAgo));

        DriverTripResponse response = driverTripService.getActiveTrip("driver1");

        assertThat(response).isNotNull();
        assertThat(response.getExecutionId()).isEqualTo(52L);
    }

    @Test
    @DisplayName("arriveAtStop cập nhật status IN_PROGRESS và actualArrivalTime thành công")
    void arriveAtStop_success() {
        execution.setStatus("IN_PROGRESS");
        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));

        TripStop stop1 = TripStop.builder()
                .tripStopId(101L)
                .trip(trip)
                .sequenceOrder(1)
                .status(TripStopStatus.PENDING)
                .build();

        when(tripStopRepo.findRemainingStopsOrdered(100L)).thenReturn(java.util.List.of(stop1));
        when(tripStopRepo.findByTripDraftStopId(1L)).thenReturn(Optional.of(stop1));

        DriverTripResponse response = driverTripService.arriveAtStop(50L, 1L, "driver1");

        assertThat(response).isNotNull();
        assertThat(stop1.getStatus()).isEqualTo(TripStopStatus.IN_PROGRESS);
        assertThat(stop1.getActualArrivalTime()).isNotNull();
        verify(tripStopRepo).save(stop1);
    }

    @Test
    @DisplayName("adminOverrideTripExecution FORCE_RETURN giải phóng xe về AVAILABLE thành công")
    void adminOverride_forceReturn_success() {
        execution.setStatus("COMPLETED");
        trip.setVehicle(vehicle);
        vehicle.setStatus(VehicleStatus.IN_USE);

        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));

        com.elog.dto.request.AdminTripOverrideRequest request = com.elog.dto.request.AdminTripOverrideRequest.builder()
                .action("FORCE_RETURN")
                .reason("Driver lost phone, confirmed vehicle returned")
                .build();

        DriverTripResponse response = driverTripService.adminOverrideTripExecution(50L, request, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(execution.getReturnedToWarehouseAt()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepo).save(vehicle);
    }

    @Test
    @DisplayName("adminOverrideTripExecution FORCE_COMPLETE_AND_RETURN hoàn tất chuyến và giải phóng xe")
    void adminOverride_forceCompleteAndReturn_success() {
        execution.setStatus("IN_PROGRESS");
        trip.setVehicle(vehicle);
        vehicle.setStatus(VehicleStatus.IN_USE);

        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));

        com.elog.dto.request.AdminTripOverrideRequest request = com.elog.dto.request.AdminTripOverrideRequest.builder()
                .action("FORCE_COMPLETE_AND_RETURN")
                .reason("Driver sick, admin force complete")
                .defaultPendingOrderStatus("DELIVERED")
                .build();

        DriverTripResponse response = driverTripService.adminOverrideTripExecution(50L, request, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(execution.getStatus()).isEqualTo("COMPLETED");
        assertThat(execution.getReturnedToWarehouseAt()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(vehicleRepo).save(vehicle);
    }
}

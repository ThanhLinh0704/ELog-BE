package com.elog.service;

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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

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
    @DisplayName("returnToWarehouse thành công: cập nhật returnedToWarehouseAt và giải phóng xe sang AVAILABLE")
    void returnToWarehouse_success() {
        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));
        when(tripExecutionRepo.save(any(TripExecution.class))).thenAnswer(i -> i.getArgument(0));

        DriverTripResponse response = driverTripService.returnToWarehouse(50L, "driver1");

        assertThat(response).isNotNull();
        assertThat(execution.getReturnedToWarehouseAt()).isNotNull();
        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
        verify(tripExecutionRepo).save(execution);
    }

    @Test
    @DisplayName("returnToWarehouse thất bại khi chuyến xe chưa COMPLETED")
    void returnToWarehouse_tripNotCompleted_throwsException() {
        execution.setStatus("IN_PROGRESS");
        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> driverTripService.returnToWarehouse(50L, "driver1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chuyến xe chưa hoàn thành");

        assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.IN_USE);
    }

    @Test
    @DisplayName("returnToWarehouse thất bại khi chuyến xe đã được xác nhận về kho trước đó")
    void returnToWarehouse_alreadyReturned_throwsException() {
        execution.setReturnedToWarehouseAt(LocalDateTime.now().minusMinutes(20));
        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> driverTripService.returnToWarehouse(50L, "driver1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chuyến xe đã được xác nhận về kho trước đó");
    }

    @Test
    @DisplayName("startTrip thất bại khi chuyến xe thuộc về ngày trong tương lai")
    void startTrip_futureDeliveryDate_throwsException() {
        trip.setDeliveryDate(LocalDate.now().plusDays(2));
        execution.setStatus("ASSIGNED");
        when(tripExecutionRepo.findById(50L)).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> driverTripService.startTrip(50L, "driver1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Chưa đến ngày giao hàng");
    }

    @Test
    @DisplayName("getActiveTrip lọc bỏ chuyến xe ASSIGNED ở ngày tương lai")
    void getActiveTrip_assignedFutureDate_ignored() {
        trip.setDeliveryDate(LocalDate.now().plusDays(1));
        execution.setStatus("ASSIGNED");
        when(tripExecutionRepo.findByDriverUsernameAndStatusIn("driver1", java.util.List.of("IN_PROGRESS", "ASSIGNED")))
                .thenReturn(java.util.List.of(execution));

        assertThatThrownBy(() -> driverTripService.getActiveTrip("driver1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Không tìm thấy chuyến xe đang phân công");
    }
}

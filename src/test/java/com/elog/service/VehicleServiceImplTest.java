package com.elog.service;

import com.elog.dto.request.vehicle.VehicleCreateRequest;
import com.elog.dto.response.vehicle.VehicleListItemResponse;
import com.elog.dto.response.vehicle.VehicleResponse;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.entity.TripStatus;
import com.elog.entity.Vehicle;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.VehicleMapper;
import com.elog.repository.TripExecutionRepository;
import com.elog.repository.TripRepository;
import com.elog.repository.VehicleRepository;
import com.elog.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VehicleServiceImplTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleMapper vehicleMapper;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TripExecutionRepository tripExecutionRepository;

    @InjectMocks
    private VehicleServiceImpl vehicleService;

    private VehicleCreateRequest request;
    private Vehicle vehicle;

    @BeforeEach
    void setUp() {
        request = new VehicleCreateRequest();
        request.setPlateNumber("29A-12345");
        request.setVehicleType("1.25 TONS");
        request.setPayloadKg(BigDecimal.valueOf(1250.0));
        request.setMaxVolumeM3(BigDecimal.valueOf(6.0)); // ratio = 1250 / 6 = 208 kg/m3

        vehicle = Vehicle.builder()
                .id(1L)
                .plateNumber("29A-12345")
                .vehicleType("1.25 TONS")
                .payloadKg(BigDecimal.valueOf(1250.0))
                .maxVolumeM3(BigDecimal.valueOf(6.0))
                .isActive(true)
                .build();
    }

    @Test
    void createVehicle_success_validRatio() {
        when(vehicleMapper.normalizePlate("29A-12345")).thenReturn("29A-12345");
        when(vehicleRepository.existsByPlateNumber("29A-12345")).thenReturn(false);
        when(vehicleMapper.toEntity(any(VehicleCreateRequest.class))).thenReturn(vehicle);
        when(vehicleRepository.save(any(Vehicle.class))).thenReturn(vehicle);
        when(vehicleMapper.toResponse(any(Vehicle.class))).thenReturn(VehicleResponse.builder().id(1L).plateNumber("29A-12345").build());

        VehicleResponse response = vehicleService.createVehicle(request);

        assertThat(response.getId()).isEqualTo(1L);
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_fails_anomalyRatioTooHigh() {
        // ratio = 10000 / 2.0 = 5000 kg/m3 (extremely heavy and small)
        request.setPayloadKg(BigDecimal.valueOf(10000.0));
        request.setMaxVolumeM3(BigDecimal.valueOf(2.0));

        when(vehicleMapper.normalizePlate("29A-12345")).thenReturn("29A-12345");
        when(vehicleRepository.existsByPlateNumber("29A-12345")).thenReturn(false);

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CAPACITY_RATIO)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void createVehicle_fails_anomalyRatioTooLow() {
        // ratio = 500 / 15.0 = 33.3 kg/m3 (extremely light and huge)
        request.setPayloadKg(BigDecimal.valueOf(500.0));
        request.setMaxVolumeM3(BigDecimal.valueOf(15.0));

        when(vehicleMapper.normalizePlate("29A-12345")).thenReturn("29A-12345");
        when(vehicleRepository.existsByPlateNumber("29A-12345")).thenReturn(false);

        assertThatThrownBy(() -> vehicleService.createVehicle(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CAPACITY_RATIO)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(vehicleRepository, never()).save(any(Vehicle.class));
    }

    @Test
    void getAllVehicles_withDate_ignoresCancelledTrip_picksMostRecentActiveTrip() {
        LocalDate date = LocalDate.of(2026, 8, 23);

        // Same vehicle has 2 trips today: an earlier CANCELLED one (tripId 7) and the real,
        // still-active one (tripId 9, COMPLETED but not yet returned) — mirrors the real bug found
        // in production data after the "Huỷ chuyến" feature let a cancelled vehicle be reassigned.
        Trip cancelledTrip = Trip.builder().tripId(7L).vehicle(vehicle).status(TripStatus.CANCELLED).build();
        Trip activeTrip = Trip.builder().tripId(9L).vehicle(vehicle).status(TripStatus.COMPLETED).build();

        TripExecution execution = TripExecution.builder().id(90L).trip(activeTrip).status("COMPLETED").returnedToWarehouseAt(null).build();

        when(vehicleRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleMapper.toListItem(vehicle)).thenReturn(VehicleListItemResponse.builder().id(1L).build());
        when(tripRepository.findByVehicleIdAndDeliveryDate(1L, date)).thenReturn(List.of(cancelledTrip, activeTrip));
        when(tripExecutionRepository.findByTripId(9L)).thenReturn(java.util.Optional.of(execution));

        var response = vehicleService.getAllVehicles(null, null, null, null, PageRequest.of(0, 10), date);

        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getCurrentTrip()).isNotNull();
        assertThat(response.getData().get(0).getCurrentTrip().getTripId()).isEqualTo(9L);
        assertThat(response.getData().get(0).getCurrentTrip().getPhase()).isEqualTo("RETURNING");
    }

    @Test
    void getAllVehicles_withTodayDate_fallsBackToUnreturnedTrip_whenNoTripScheduledToday() {
        // Xe giao xong hôm qua, tài xế chưa xác nhận về kho, và hôm nay không có chuyến mới nào —
        // dashboard xem "hôm nay" vẫn phải hiện RETURNING chứ không phải mặc định "Sẵn sàng".
        LocalDate today = LocalDate.now();
        Trip yesterdayTrip = Trip.builder().tripId(9L).vehicle(vehicle).status(TripStatus.COMPLETED)
                .deliveryDate(today.minusDays(1)).build();
        TripExecution unreturnedExecution = TripExecution.builder().id(90L).trip(yesterdayTrip)
                .status("COMPLETED").returnedToWarehouseAt(null).build();

        when(vehicleRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleMapper.toListItem(vehicle)).thenReturn(VehicleListItemResponse.builder().id(1L).build());
        when(tripRepository.findByVehicleIdAndDeliveryDate(1L, today)).thenReturn(List.of());
        when(tripRepository.findByVehicleIdAndStatusIn(eq(1L), any())).thenReturn(List.of());
        when(tripExecutionRepository.findUnreturnedByVehicleId(1L)).thenReturn(List.of(unreturnedExecution));

        var response = vehicleService.getAllVehicles(null, null, null, null, PageRequest.of(0, 10), today);

        assertThat(response.getData().get(0).getCurrentTrip()).isNotNull();
        assertThat(response.getData().get(0).getCurrentTrip().getTripId()).isEqualTo(9L);
        assertThat(response.getData().get(0).getCurrentTrip().getPhase()).isEqualTo("RETURNING");
    }

    @Test
    void getAllVehicles_withTodayDate_showsNotStartedYesterdayTrip_asDispatched_notReturning() {
        // Regression cho đúng bug thật vừa gặp: chuyến hôm qua vẫn DISPATCHED, tài xế CHƯA TỪNG bấm
        // "Bắt đầu chuyến" (execution vẫn ASSIGNED). Không có chuyến nào cho hôm nay. Vì
        // returnedToWarehouseAt cũng NULL (chưa chạy nên chưa thể về), fallback không được nhầm nó
        // thành RETURNING — phải ưu tiên nhánh "chuyến chưa hoàn thành" (findByVehicleIdAndStatusIn)
        // và trả về đúng phase DISPATCHED.
        LocalDate today = LocalDate.now();
        Trip notStartedTrip = Trip.builder().tripId(8L).vehicle(vehicle).status(TripStatus.DISPATCHED)
                .deliveryDate(today.minusDays(1)).build();

        when(vehicleRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleMapper.toListItem(vehicle)).thenReturn(VehicleListItemResponse.builder().id(1L).build());
        when(tripRepository.findByVehicleIdAndDeliveryDate(1L, today)).thenReturn(List.of());
        when(tripRepository.findByVehicleIdAndStatusIn(eq(1L), any())).thenReturn(List.of(notStartedTrip));

        var response = vehicleService.getAllVehicles(null, null, null, null, PageRequest.of(0, 10), today);

        assertThat(response.getData().get(0).getCurrentTrip()).isNotNull();
        assertThat(response.getData().get(0).getCurrentTrip().getTripId()).isEqualTo(8L);
        assertThat(response.getData().get(0).getCurrentTrip().getPhase()).isEqualTo("DISPATCHED");
        verify(tripExecutionRepository, never()).findUnreturnedByVehicleId(any());
    }

    @Test
    void getAllVehicles_withPastDate_doesNotLeakTodaysUnreturnedTrip() {
        // Xem 1 ngày trong quá khứ (audit lịch sử) — không được để trạng thái "đang nợ về kho" của
        // HÔM NAY rò rỉ vào kết quả của 1 ngày cũ không liên quan.
        LocalDate pastDate = LocalDate.now().minusDays(5);

        when(vehicleRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(vehicle)));
        when(vehicleMapper.toListItem(vehicle)).thenReturn(VehicleListItemResponse.builder().id(1L).build());
        when(tripRepository.findByVehicleIdAndDeliveryDate(1L, pastDate)).thenReturn(List.of());

        var response = vehicleService.getAllVehicles(null, null, null, null, PageRequest.of(0, 10), pastDate);

        assertThat(response.getData().get(0).getCurrentTrip()).isNull();
        verify(tripExecutionRepository, never()).findUnreturnedByVehicleId(any());
    }
}

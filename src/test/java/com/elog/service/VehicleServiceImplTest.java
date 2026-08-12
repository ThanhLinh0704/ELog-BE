package com.elog.service;

import com.elog.dto.request.vehicle.VehicleCreateRequest;
import com.elog.dto.response.vehicle.VehicleResponse;
import com.elog.entity.Vehicle;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.VehicleMapper;
import com.elog.repository.VehicleRepository;
import com.elog.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

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
}

package com.elog.service;

import com.elog.dto.response.VehicleFleetCapacityResponse;
import com.elog.mapper.VehicleMapper;
import com.elog.repository.UserRepository;
import com.elog.repository.VehicleRepository;
import com.elog.service.impl.VehicleServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VehicleFleetCapacityReport5Test {
    @Test @DisplayName("[L1-VH-11] active fleet returns count and aggregate payload and volume")
    void activeFleetReturnsAggregates(){VehicleRepository vehicles=mock(VehicleRepository.class);when(vehicles.countByIsActiveTrue()).thenReturn(5L);when(vehicles.sumActiveMaxWeightKg()).thenReturn(new BigDecimal("15000"));when(vehicles.sumActiveMaxVolumeM3()).thenReturn(new BigDecimal("50"));VehicleFleetCapacityResponse r=new VehicleServiceImpl(vehicles,mock(UserRepository.class),mock(VehicleMapper.class)).getFleetCapacity();assertAll(()->assertEquals(5,r.getActiveVehicleCount()),()->assertEquals(new BigDecimal("15000"),r.getTotalMaxWeightKg()),()->assertEquals(new BigDecimal("50"),r.getTotalMaxVolumeM3()));}
    @Test @DisplayName("[L1-VH-12] empty active fleet returns zeros without division")
    void emptyFleetReturnsZeros(){VehicleRepository vehicles=mock(VehicleRepository.class);when(vehicles.countByIsActiveTrue()).thenReturn(0L);VehicleFleetCapacityResponse r=new VehicleServiceImpl(vehicles,mock(UserRepository.class),mock(VehicleMapper.class)).getFleetCapacity();assertAll(()->assertEquals(0,r.getActiveVehicleCount()),()->assertEquals(BigDecimal.ZERO,r.getTotalMaxWeightKg()),()->assertEquals(BigDecimal.ZERO,r.getTotalMaxVolumeM3()));}
}

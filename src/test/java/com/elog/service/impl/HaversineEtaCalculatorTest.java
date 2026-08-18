package com.elog.service.impl;

import com.elog.dto.response.trip.StopEtaResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.TripDraftStopRepository;
import com.elog.service.impl.HaversineEtaCalculator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HaversineEtaCalculatorTest {

    @Mock
    private SystemConfigRepository configRepo;

    @Mock
    private TripDraftRepository tripDraftRepo;

    @Mock
    private TripDraftStopRepository stopRepo;

    @InjectMocks
    private HaversineEtaCalculator etaCalculator;

    private SystemConfig createConfig(String key, String val) {
        SystemConfig config = new SystemConfig();
        config.setConfigKey(key);
        config.setConfigValue(val);
        return config;
    }

    @Test
    void calculateAndPersist_missingConfig_throwsException() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etaCalculator.calculateAndPersist(1L, LocalTime.NOON))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_ERROR);
    }

    @Test
    void calculateAndPersist_tripDraftNotFound_throwsException() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> etaCalculator.calculateAndPersist(1L, LocalTime.NOON))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_FOUND);
    }

    @Test
    void calculateAndPersist_tripDraftLocked_throwsException() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));
        
        TripDraft lockedDraft = TripDraft.builder().id(1L).status("CONFIRMED").build();
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(lockedDraft));

        assertThatThrownBy(() -> etaCalculator.calculateAndPersist(1L, LocalTime.NOON))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_LOCKED);
    }

    @Test
    void calculateAndPersist_noActiveStops_throwsException() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));
        
        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").build();
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> etaCalculator.calculateAndPersist(1L, LocalTime.NOON))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_ACTIVE_STOP);
    }

    @Test
    void calculateAndPersist_missingCoordinates_throwsException() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));
        
        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").deliveryDate(LocalDate.now()).build();
        Store store = Store.builder().code("ST01").latitude(null).longitude(106.0).build();
        TripDraftStop stop = TripDraftStop.builder().id(10L).store(store).build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop));

        assertThatThrownBy(() -> etaCalculator.calculateAndPersist(1L, LocalTime.NOON))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ETA_MISSING_COORDINATES);
    }

    @Test
    void calculateAndPersist_success() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));
        
        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").deliveryDate(LocalDate.of(2026, 7, 22)).build();
        
        Store store1 = Store.builder().code("ST01").latitude(10.800).longitude(106.720).build();
        RouteStop rs1 = RouteStop.builder().avgServiceTimeMin(15).build();
        TripDraftStop stop1 = TripDraftStop.builder().id(10L).store(store1).routeStop(rs1).sequenceNo(1).build();

        Store store2 = Store.builder().code("ST02").latitude(10.820).longitude(106.740).build();
        RouteStop rs2 = RouteStop.builder().avgServiceTimeMin(20).build();
        TripDraftStop stop2 = TripDraftStop.builder().id(11L).store(store2).routeStop(rs2).sequenceNo(2).build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop1, stop2));

        List<StopEtaResponse> result = etaCalculator.calculateAndPersist(1L, LocalTime.of(8, 0));

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getStoreCode()).isEqualTo("ST01");
        assertThat(result.get(1).getStoreCode()).isEqualTo("ST02");
        verify(stopRepo).saveAll(any());
        verify(tripDraftRepo).save(any());
    }

    @Test
    void calculateAndPersist_tripDraftCancelled_throwsException() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));

        TripDraft cancelledDraft = TripDraft.builder().id(1L).status("CANCELLED").build();
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(cancelledDraft));

        assertThatThrownBy(() -> etaCalculator.calculateAndPersist(1L, LocalTime.NOON))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_LOCKED);
    }

    @Test
    void calculateAndPersist_timeWindowEarly_waitUnder30Min() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));

        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").deliveryDate(LocalDate.of(2026, 7, 22)).build();
        Store store = Store.builder().code("ST01").latitude(10.780).longitude(106.705)
                .timeWindowStart(LocalTime.of(8, 20)).timeWindowEnd(LocalTime.of(12, 0)).build();
        TripDraftStop stop = TripDraftStop.builder().id(10L).store(store).sequenceNo(1).build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop));

        List<StopEtaResponse> result = etaCalculator.calculateAndPersist(1L, LocalTime.of(8, 0));
        assertThat(result).hasSize(1);
        assertThat(stop.getViolationCode()).isNull();
        assertThat(stop.getPlannedWaitingTimeMin()).isGreaterThan(0);
    }

    @Test
    void calculateAndPersist_timeWindowEarly_waitOver30Min() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));

        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").deliveryDate(LocalDate.of(2026, 7, 22)).build();
        Store store = Store.builder().code("ST01").latitude(10.780).longitude(106.705)
                .timeWindowStart(LocalTime.of(11, 0)).timeWindowEnd(LocalTime.of(15, 0)).build();
        TripDraftStop stop = TripDraftStop.builder().id(10L).store(store).sequenceNo(1).build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop));

        List<StopEtaResponse> result = etaCalculator.calculateAndPersist(1L, LocalTime.of(8, 0));
        assertThat(result).hasSize(1);
        assertThat(stop.getViolationCode()).isEqualTo("TIME_WINDOW_EARLY");
    }

    @Test
    void calculateAndPersist_timeWindowLate() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));

        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").deliveryDate(LocalDate.of(2026, 7, 22)).build();
        Store store = Store.builder().code("ST01").latitude(10.780).longitude(106.705)
                .timeWindowStart(LocalTime.of(6, 0)).timeWindowEnd(LocalTime.of(7, 0)).build();
        TripDraftStop stop = TripDraftStop.builder().id(10L).store(store).sequenceNo(1).build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop));

        List<StopEtaResponse> result = etaCalculator.calculateAndPersist(1L, LocalTime.of(8, 0));
        assertThat(result).hasSize(1);
        assertThat(stop.getViolationCode()).isEqualTo("TIME_WINDOW_LATE");
    }

    @Test
    void calculateAndPersist_fallbackAllowedDeliveryHours() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));

        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").deliveryDate(LocalDate.of(2026, 7, 22)).build();
        Store store = Store.builder().code("ST01").latitude(10.780).longitude(106.705)
                .allowedDeliveryHours("10:00-12:00").build();
        TripDraftStop stop = TripDraftStop.builder().id(10L).store(store).sequenceNo(1).build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop));

        List<StopEtaResponse> result = etaCalculator.calculateAndPersist(1L, LocalTime.of(8, 0));
        assertThat(result).hasSize(1);
        assertThat(stop.getViolationCode()).isEqualTo("TIME_WINDOW_EARLY");
    }

    @Test
    void calculateAndPersist_fallbackAllowedDeliveryHoursInvalid() {
        when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(createConfig("AVG_SPEED_KMH", "40.0")));
        when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(createConfig("WAREHOUSE_LAT", "10.776")));
        when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(createConfig("WAREHOUSE_LNG", "106.701")));

        TripDraft draft = TripDraft.builder().id(1L).status("DRAFT").deliveryDate(LocalDate.of(2026, 7, 22)).build();
        Store store = Store.builder().code("ST01").latitude(10.780).longitude(106.705)
                .allowedDeliveryHours("INVALID_FORMAT").build();
        TripDraftStop stop1 = TripDraftStop.builder().id(10L).store(store).sequenceNo(1).build();
        TripDraftStop stop2 = TripDraftStop.builder().id(11L).store(store).sequenceNo(2).build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop1, stop2));

        List<StopEtaResponse> result = etaCalculator.calculateAndPersist(1L, LocalTime.of(8, 0));
        assertThat(result).hasSize(2);
        assertThat(stop1.getViolationCode()).isNull();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("[L1-HV-01] same coordinates have zero distance")
    void l1Hv01_sameCoordinatesReturnZero() {
        assertThat(etaCalculator.haversine(21.0, 105.8, 21.0, 105.8)).isZero();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("[L1-HV-02] Hanoi to Ho Chi Minh City has known distance")
    void l1Hv02_hanoiToHoChiMinhIsApproximatelyKnownDistance() {
        assertThat(etaCalculator.haversine(21.0285, 105.8542, 10.8231, 106.6297))
                .isBetween(1_133.0, 1_143.0);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("[L1-HV-03] antipodal poles have half-earth circumference")
    void l1Hv03_antipodalPolesReturnHalfEarthCircumference() {
        assertThat(etaCalculator.haversine(-90.0, 0.0, 90.0, 0.0))
                .isBetween(20_010.0, 20_020.0);
    }

    @Test
    @org.junit.jupiter.api.DisplayName("[L1-HV-04] short urban points have local positive distance")
    void l1Hv04_shortUrbanPairReturnsPositiveLocalDistance() {
        assertThat(etaCalculator.haversine(21.02, 105.83, 21.05, 105.86))
                .isBetween(4.4, 4.8);
    }
}


package com.elog.service;

import com.elog.dto.response.trip.StopEtaResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.TripDraftStopRepository;
import com.elog.service.impl.GoongEtaCalculator;
import com.elog.service.impl.HaversineEtaCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoongEtaCalculatorTest {
    @Mock GoongMapService goongMapService;
    @Mock HaversineEtaCalculator haversineEtaCalculator;
    @Mock SystemConfigRepository configRepo;
    @Mock TripDraftRepository tripDraftRepo;
    @Mock TripDraftStopRepository stopRepo;

    GoongEtaCalculator calculator;
    TripDraft draft;

    @BeforeEach
    void setUp() {
        calculator = new GoongEtaCalculator(goongMapService, haversineEtaCalculator, configRepo, tripDraftRepo, stopRepo);
        draft = TripDraft.builder().id(10L).status("PLANNED").deliveryDate(LocalDate.now()).build();
    }

    @Test
    @DisplayName("[L1-GE-01] calculateAndPersist falls back to Haversine when Goong is unconfigured")
    void calculateAndPersistFallsBackToHaversineWhenUnconfigured() {
        when(goongMapService.isConfigured()).thenReturn(false);
        when(haversineEtaCalculator.calculateAndPersist(10L, LocalTime.of(8, 0))).thenReturn(List.of(new StopEtaResponse()));

        List<StopEtaResponse> resp = calculator.calculateAndPersist(10L, LocalTime.of(8, 0));

        assertAll(
                () -> assertNotNull(resp),
                () -> verify(haversineEtaCalculator).calculateAndPersist(10L, LocalTime.of(8, 0))
        );
    }

    @Test
    @DisplayName("[L1-GE-02] calculateAndPersist rejects confirmed draft")
    void calculateAndPersistRejectsConfirmedDraft() {
        draft.setStatus("CONFIRMED");
        when(goongMapService.isConfigured()).thenReturn(true);
        when(tripDraftRepo.findById(10L)).thenReturn(Optional.of(draft));

        BusinessException e = assertThrows(BusinessException.class, () -> calculator.calculateAndPersist(10L, LocalTime.of(8, 0)));
        assertEquals(ErrorCode.TRIP_DRAFT_LOCKED, e.getErrorCode());
    }

    @Test
    @DisplayName("[L1-GE-03] calculateAndPersist rejects draft without active stops")
    void calculateAndPersistRejectsEmptyStops() {
        when(goongMapService.isConfigured()).thenReturn(true);
        when(tripDraftRepo.findById(10L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(10L)).thenReturn(List.of());

        BusinessException e = assertThrows(BusinessException.class, () -> calculator.calculateAndPersist(10L, LocalTime.of(8, 0)));
        assertEquals(ErrorCode.NO_ACTIVE_STOP, e.getErrorCode());
    }

    @Test
    @DisplayName("[L1-GE-04] calculateAndPersist rejects missing GPS coordinates")
    void calculateAndPersistRejectsMissingCoordinates() {
        Store storeNoGps = Store.builder().code("S1").latitude(null).longitude(105.0).build();
        TripDraftStop stop = TripDraftStop.builder().store(storeNoGps).build();

        lenient().when(goongMapService.isConfigured()).thenReturn(true);
        lenient().when(tripDraftRepo.findById(10L)).thenReturn(Optional.of(draft));
        lenient().when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(10L)).thenReturn(List.of(stop));
        lenient().when(configRepo.findByConfigKey("AVG_SPEED_KMH")).thenReturn(Optional.of(SystemConfig.builder().configValue("40.0").build()));
        lenient().when(configRepo.findByConfigKey("WAREHOUSE_LAT")).thenReturn(Optional.of(SystemConfig.builder().configValue("21.0").build()));
        lenient().when(configRepo.findByConfigKey("WAREHOUSE_LNG")).thenReturn(Optional.of(SystemConfig.builder().configValue("105.0").build()));

        BusinessException e = assertThrows(BusinessException.class, () -> calculator.calculateAndPersist(10L, LocalTime.of(8, 0)));
        assertEquals(ErrorCode.ETA_MISSING_COORDINATES, e.getErrorCode());
    }
}

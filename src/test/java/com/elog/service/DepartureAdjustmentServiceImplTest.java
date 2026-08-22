package com.elog.service;

import com.elog.entity.TripDraft;
import com.elog.entity.TripDraftStop;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.TripDraftStopRepository;
import com.elog.service.impl.DepartureAdjustmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartureAdjustmentServiceImplTest {
    @Mock TripDraftRepository tripDraftRepo;
    @Mock TripDraftStopRepository stopRepo;

    DepartureAdjustmentServiceImpl service;
    TripDraft draft;
    TripDraftStop stop;

    @BeforeEach
    void setUp() {
        service = new DepartureAdjustmentServiceImpl(tripDraftRepo, stopRepo);
        draft = TripDraft.builder().id(10L).plannedDepartureTime(LocalTime.of(8, 0)).build();
        stop = TripDraftStop.builder().id(100L).tripDraft(draft).isActive(true).build();
    }

    @Test
    @DisplayName("[L1-DA-01] calculateOptimalDepartureTime suggests delayed departure for early violation")
    void calculateOptimalDepartureTimeEarlyViolation() {
        stop.setViolationCode("TIME_WINDOW_EARLY");
        stop.setPlannedWaitingTimeMin(45);
        when(tripDraftRepo.findById(10L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(10L)).thenReturn(List.of(stop));

        Map<String, Object> resp = service.calculateOptimalDepartureTime(10L);

        assertAll(
                () -> assertEquals(10L, resp.get("tripDraftId")),
                () -> assertEquals("08:45", resp.get("suggestedDepartureTime")),
                () -> assertEquals(true, resp.get("hasViolations"))
        );
    }

    @Test
    @DisplayName("[L1-DA-02] calculateOptimalDepartureTime suggests earlier departure for late violation")
    void calculateOptimalDepartureTimeLateViolation() {
        stop.setViolationCode("TIME_WINDOW_LATE");
        when(tripDraftRepo.findById(10L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(10L)).thenReturn(List.of(stop));

        Map<String, Object> resp = service.calculateOptimalDepartureTime(10L);

        assertAll(
                () -> assertEquals("07:30", resp.get("suggestedDepartureTime")),
                () -> assertEquals(true, resp.get("hasViolations"))
        );
    }

    @Test
    @DisplayName("[L1-DA-03] calculateOptimalDepartureTime returns no change when no violations")
    void calculateOptimalDepartureTimeNoViolations() {
        when(tripDraftRepo.findById(10L)).thenReturn(Optional.of(draft));
        when(stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(10L)).thenReturn(List.of(stop));

        Map<String, Object> resp = service.calculateOptimalDepartureTime(10L);

        assertAll(
                () -> assertEquals("08:00", resp.get("suggestedDepartureTime")),
                () -> assertEquals(false, resp.get("hasViolations"))
        );
    }
}

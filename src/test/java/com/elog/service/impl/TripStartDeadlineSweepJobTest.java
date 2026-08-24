package com.elog.service.impl;

import com.elog.entity.Trip;
import com.elog.entity.TripStatus;
import com.elog.repository.TripRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripStartDeadlineSweepJobTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private TripStartDeadlineService deadlineService;

    @InjectMocks
    private TripStartDeadlineSweepJob job;

    @Test
    void flagsEachOverdueTrip_notAlreadyFlagged() {
        Trip trip1 = Trip.builder().tripId(1L).lockedAt(LocalDateTime.now().minusMinutes(30)).build();
        Trip trip2 = Trip.builder().tripId(2L).lockedAt(LocalDateTime.now().minusMinutes(45)).build();

        when(deadlineService.getDeadlineMinutes()).thenReturn(15);
        when(tripRepository.findByStatusAndLockedAtBefore(eq(TripStatus.DISPATCHED), any()))
                .thenReturn(List.of(trip1, trip2));
        when(deadlineService.flagIfNeeded(trip1)).thenReturn(true);
        when(deadlineService.flagIfNeeded(trip2)).thenReturn(false);

        job.sweep();

        verify(deadlineService).flagIfNeeded(trip1);
        verify(deadlineService).flagIfNeeded(trip2);
    }

    @Test
    void noOverdueTrips_noop() {
        when(deadlineService.getDeadlineMinutes()).thenReturn(15);
        when(tripRepository.findByStatusAndLockedAtBefore(eq(TripStatus.DISPATCHED), any()))
                .thenReturn(List.of());

        job.sweep();

        verify(deadlineService, never()).flagIfNeeded(any());
    }
}

package com.elog.service.impl;

import com.elog.entity.ExceptionType;
import com.elog.entity.SystemConfig;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.repository.DeliveryExceptionRepository;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripExecutionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripStartDeadlineServiceTest {

    @Mock
    private SystemConfigRepository systemConfigRepo;
    @Mock
    private DeliveryExceptionRepository deliveryExceptionRepo;
    @Mock
    private TripExecutionRepository tripExecutionRepo;

    @InjectMocks
    private TripStartDeadlineService service;

    @Test
    void getDeadlineMinutes_readsFromConfig() {
        when(systemConfigRepo.findByConfigKey("TRIP_START_DEADLINE_MINUTES"))
                .thenReturn(Optional.of(SystemConfig.builder().configValue("20").build()));

        assertThat(service.getDeadlineMinutes()).isEqualTo(20);
    }

    @Test
    void getDeadlineMinutes_defaultsTo15_whenConfigMissing() {
        when(systemConfigRepo.findByConfigKey("TRIP_START_DEADLINE_MINUTES")).thenReturn(Optional.empty());

        assertThat(service.getDeadlineMinutes()).isEqualTo(15);
    }

    @Test
    void isDeadlineExceeded_false_whenLockedAtNull() {
        Trip trip = Trip.builder().tripId(1L).lockedAt(null).build();

        assertThat(service.isDeadlineExceeded(trip)).isFalse();
        verifyNoInteractions(systemConfigRepo);
    }

    @Test
    void isDeadlineExceeded_true_whenPastDeadline() {
        when(systemConfigRepo.findByConfigKey("TRIP_START_DEADLINE_MINUTES"))
                .thenReturn(Optional.of(SystemConfig.builder().configValue("15").build()));
        Trip trip = Trip.builder().tripId(1L).lockedAt(LocalDateTime.now().minusMinutes(30)).build();

        assertThat(service.isDeadlineExceeded(trip)).isTrue();
    }

    @Test
    void isDeadlineExceeded_false_whenWithinDeadline() {
        when(systemConfigRepo.findByConfigKey("TRIP_START_DEADLINE_MINUTES"))
                .thenReturn(Optional.of(SystemConfig.builder().configValue("15").build()));
        Trip trip = Trip.builder().tripId(1L).lockedAt(LocalDateTime.now().minusMinutes(5)).build();

        assertThat(service.isDeadlineExceeded(trip)).isFalse();
    }

    @Test
    void flagIfNeeded_createsException_whenNotAlreadyFlagged() {
        Trip trip = Trip.builder().tripId(1L).lockedAt(LocalDateTime.now().minusMinutes(30)).build();
        TripExecution execution = TripExecution.builder().id(10L).trip(trip).status("ASSIGNED").build();

        when(tripExecutionRepo.findByTripId(1L)).thenReturn(Optional.of(execution));
        when(deliveryExceptionRepo.existsByTripExecutionIdAndExceptionTypeAndResolvedAtIsNull(
                10L, ExceptionType.TRIP_START_DEADLINE_EXCEEDED)).thenReturn(false);
        when(systemConfigRepo.findByConfigKey("TRIP_START_DEADLINE_MINUTES")).thenReturn(Optional.empty());

        boolean created = service.flagIfNeeded(trip);

        assertThat(created).isTrue();
        verify(deliveryExceptionRepo).save(argThat(ex ->
                ex.getTripExecutionId().equals(10L)
                        && ex.getExceptionType() == ExceptionType.TRIP_START_DEADLINE_EXCEEDED));
    }

    @Test
    void flagIfNeeded_doesNotDuplicate_whenAlreadyFlagged() {
        Trip trip = Trip.builder().tripId(1L).lockedAt(LocalDateTime.now().minusMinutes(30)).build();
        TripExecution execution = TripExecution.builder().id(10L).trip(trip).status("ASSIGNED").build();

        when(tripExecutionRepo.findByTripId(1L)).thenReturn(Optional.of(execution));
        when(deliveryExceptionRepo.existsByTripExecutionIdAndExceptionTypeAndResolvedAtIsNull(
                10L, ExceptionType.TRIP_START_DEADLINE_EXCEEDED)).thenReturn(true);

        boolean created = service.flagIfNeeded(trip);

        assertThat(created).isFalse();
        verify(deliveryExceptionRepo, never()).save(any());
    }

    @Test
    void flagIfNeeded_noop_whenNoTripExecution() {
        Trip trip = Trip.builder().tripId(1L).build();
        when(tripExecutionRepo.findByTripId(1L)).thenReturn(Optional.empty());

        boolean created = service.flagIfNeeded(trip);

        assertThat(created).isFalse();
        verifyNoInteractions(deliveryExceptionRepo);
    }
}

package com.elog.service.impl;

import com.elog.entity.ExceptionType;
import com.elog.entity.SystemConfig;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.entity.TripStatus;
import com.elog.repository.DeliveryExceptionRepository;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripExecutionRepository;
import com.elog.repository.TripRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripStaleDetectionJobTest {

    @Mock
    private TripRepository tripRepository;
    @Mock
    private SystemConfigRepository systemConfigRepo;
    @Mock
    private DeliveryExceptionRepository deliveryExceptionRepo;
    @Mock
    private TripExecutionRepository tripExecutionRepo;

    @InjectMocks
    private TripStaleDetectionJob job;

    @BeforeEach
    void setUp() {
        when(systemConfigRepo.findByConfigKey("TRIP_STALE_THRESHOLD_DAYS"))
                .thenReturn(Optional.of(SystemConfig.builder().configKey("TRIP_STALE_THRESHOLD_DAYS").configValue("3").build()));
    }

    @Test
    void flagsStaleTrip_whenNotAlreadyFlagged() {
        Trip trip = Trip.builder().tripId(1L).deliveryDate(LocalDate.now().minusDays(5)).status(TripStatus.DISPATCHED).build();
        TripExecution execution = TripExecution.builder().id(10L).trip(trip).status("ASSIGNED").build();

        when(tripRepository.findByStatusAndDeliveryDateBefore(eq(TripStatus.DISPATCHED), any()))
                .thenReturn(List.of(trip));
        when(tripExecutionRepo.findByTripId(1L)).thenReturn(Optional.of(execution));
        when(deliveryExceptionRepo.existsByTripExecutionIdAndExceptionTypeAndResolvedAtIsNull(
                10L, ExceptionType.TRIP_STALE_UNSTARTED)).thenReturn(false);

        job.detectStaleTrips();

        verify(deliveryExceptionRepo).save(argThat(ex ->
                ex.getTripExecutionId().equals(10L)
                        && ex.getExceptionType() == ExceptionType.TRIP_STALE_UNSTARTED
                        && ex.getReportedBy().equals(1L)
        ));
    }

    @Test
    void doesNotDuplicate_whenAlreadyFlagged() {
        Trip trip = Trip.builder().tripId(1L).deliveryDate(LocalDate.now().minusDays(5)).status(TripStatus.DISPATCHED).build();
        TripExecution execution = TripExecution.builder().id(10L).trip(trip).status("ASSIGNED").build();

        when(tripRepository.findByStatusAndDeliveryDateBefore(eq(TripStatus.DISPATCHED), any()))
                .thenReturn(List.of(trip));
        when(tripExecutionRepo.findByTripId(1L)).thenReturn(Optional.of(execution));
        when(deliveryExceptionRepo.existsByTripExecutionIdAndExceptionTypeAndResolvedAtIsNull(
                10L, ExceptionType.TRIP_STALE_UNSTARTED)).thenReturn(true);

        job.detectStaleTrips();

        verify(deliveryExceptionRepo, never()).save(any());
    }

    @Test
    void noStaleTrips_noop() {
        when(tripRepository.findByStatusAndDeliveryDateBefore(eq(TripStatus.DISPATCHED), any()))
                .thenReturn(List.of());

        job.detectStaleTrips();

        verifyNoInteractions(deliveryExceptionRepo);
    }
}

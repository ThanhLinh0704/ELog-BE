package com.elog.service;

import com.elog.entity.*;
import com.elog.repository.DeliveryExceptionRepository;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripStopRepository;
import com.elog.service.impl.TimeExceptionDetectionJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeExceptionDetectionJobTest {

    @Mock
    private TripStopRepository tripStopRepo;
    @Mock
    private SystemConfigRepository systemConfigRepo;
    @Mock
    private DeliveryExceptionRepository deliveryExceptionRepo;

    private TimeExceptionDetectionJob job;

    @BeforeEach
    void setUp() {
        job = new TimeExceptionDetectionJob(tripStopRepo, systemConfigRepo, deliveryExceptionRepo);
    }

    @Test
    @DisplayName("[L1-TED-01] detectTimeExceptions with no overdue stops does nothing")
    void detectTimeExceptions_noOverdueStops() {
        when(systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES"))
                .thenReturn(Optional.of(SystemConfig.builder().configKey("ETA_THRESHOLD_MINUTES").configValue("15").build()));
        when(tripStopRepo.findOverdueStops(any())).thenReturn(Collections.emptyList());

        job.detectTimeExceptions();

        verify(deliveryExceptionRepo, never()).save(any());
    }

    @Test
    @DisplayName("[L1-TED-02] detectTimeExceptions creates exception and updates stop status")
    void detectTimeExceptions_withOverdueStops() {
        Store store = Store.builder().id(1L).code("ST-01").build();
        RouteStop routeStop = RouteStop.builder().id(10L).store(store).build();
        TripStop stop = TripStop.builder()
                .tripStopId(100L)
                .routeStop(routeStop)
                .plannedEta(LocalDateTime.now().minusMinutes(30))
                .status(TripStopStatus.PENDING)
                .build();

        when(systemConfigRepo.findByConfigKey("ETA_THRESHOLD_MINUTES")).thenReturn(Optional.empty()); // default 15 min
        when(tripStopRepo.findOverdueStops(any())).thenReturn(List.of(stop));

        job.detectTimeExceptions();

        ArgumentCaptor<DeliveryException> exCaptor = ArgumentCaptor.forClass(DeliveryException.class);
        verify(deliveryExceptionRepo).save(exCaptor.capture());
        assertEquals(100L, exCaptor.getValue().getTripStopId());
        assertEquals(ExceptionType.TIME_EXCEPTION, exCaptor.getValue().getExceptionType());

        verify(tripStopRepo).save(stop);
        assertEquals(TripStopStatus.EXCEPTION, stop.getStatus());
    }
}

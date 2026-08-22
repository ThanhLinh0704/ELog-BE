package com.elog.controller;

import com.elog.dto.response.trip.ActiveTripsResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.TripMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TripMonitoringService tripMonitoringService;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("L3-MONITORING-041: GET /api/v1/dashboard/active-trips - Returns active trips with 200 OK")
    void getActiveTrips_Success() throws Exception {
        ActiveTripsResponse response = ActiveTripsResponse.builder()
                .date("2026-08-16")
                .totalActiveTrips(0)
                .trips(List.of())
                .build();

        when(tripMonitoringService.getActiveTripsDashboard(any(LocalDate.class))).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/active-trips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalActiveTrips").value(0));
    }

    @Test
    @DisplayName("L3-MONITORING-042: GET /api/v1/dashboard/active-trips?date=2026-08-16 - Filter by date returns 200 OK")
    void getActiveTripsWithDate_Success() throws Exception {
        ActiveTripsResponse response = ActiveTripsResponse.builder()
                .date("2026-08-16")
                .totalActiveTrips(2)
                .trips(List.of())
                .build();

        when(tripMonitoringService.getActiveTripsDashboard(LocalDate.of(2026, 8, 16))).thenReturn(response);

        mockMvc.perform(get("/api/v1/dashboard/active-trips?date=2026-08-16"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalActiveTrips").value(2));
    }
}

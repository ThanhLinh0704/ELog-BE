package com.elog.controller;

import com.elog.dto.response.trip.StopArriveResponse;
import com.elog.dto.response.trip.StopCompleteResponse;
import com.elog.dto.response.trip.TripStartResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.TripMonitoringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TripMonitoringControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TripMonitoringService tripMonitoringService;

    @InjectMocks
    private TripMonitoringController tripMonitoringController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tripMonitoringController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("driver01", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-MONITORING-039: POST /api/v1/trips/{id}/start - Start trip returns 200 OK")
    void startTrip_Success() throws Exception {
        TripStartResponse response = TripStartResponse.builder().tripId(100L).status("IN_PROGRESS").build();
        when(tripMonitoringService.startTrip(eq(100L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trips/100/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-040: POST /api/v1/trip-stops/{id}/arrive - Arrive at stop returns 200 OK")
    void arriveAtStop_Success() throws Exception {
        StopArriveResponse response = StopArriveResponse.builder().tripStopId(10L).status("IN_PROGRESS").build();
        when(tripMonitoringService.arriveAtStop(eq(10L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-stops/10/arrive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-041: POST /api/v1/trip-stops/{id}/complete - Complete stop returns 200 OK")
    void completeStop_Success() throws Exception {
        StopCompleteResponse response = StopCompleteResponse.builder().tripStopId(10L).status("COMPLETED").build();
        when(tripMonitoringService.completeStop(eq(10L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-stops/10/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

package com.elog.controller;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.TripOutcomeResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.TripOutcomeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TripOutcomeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TripOutcomeService tripOutcomeService;

    @InjectMocks
    private TripOutcomeController tripOutcomeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tripOutcomeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dispatcher01", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-MONITORING-041_B: GET /api/v1/trip-outcomes - Get submitted outcomes returns 200 OK")
    void getSubmittedOutcomes_Success() throws Exception {
        TripOutcomeResponse response = TripOutcomeResponse.builder().id(1L).tripId(100L).status("SUBMITTED").build();
        when(tripOutcomeService.getSubmittedOutcomes()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/trip-outcomes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-042_B: POST /api/v1/trip-outcomes/{id}/validate - Validate outcome returns 200 OK")
    void validateOutcome_Success() throws Exception {
        TripOutcomeResponse response = TripOutcomeResponse.builder().id(1L).tripId(100L).status("VALIDATED").build();
        when(tripOutcomeService.validateOutcome(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-outcomes/1/validate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-043_B: POST /api/v1/trip-outcomes/{id}/amend - Amend outcome returns 200 OK")
    void amendOutcome_Success() throws Exception {
        TripOutcomeResponse response = TripOutcomeResponse.builder().id(1L).tripId(100L).status("AMENDED").build();
        when(tripOutcomeService.amendOutcome(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-outcomes/1/amend?amendmentReason=Fix+count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

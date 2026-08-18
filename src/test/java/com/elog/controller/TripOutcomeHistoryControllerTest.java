package com.elog.controller;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.TripOutcomeEventResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.TripOutcomeHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TripOutcomeHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TripOutcomeHistoryService tripOutcomeHistoryService;

    @InjectMocks
    private TripOutcomeHistoryController tripOutcomeHistoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tripOutcomeHistoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dispatcher01", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-MONITORING-044_B: GET /api/v1/trip-outcome-events - Search history returns 200 OK")
    void searchHistory_Success() throws Exception {
        TripOutcomeEventResponse item = TripOutcomeEventResponse.builder().id(1L).tripId(100L).build();
        ApiResponse<List<TripOutcomeEventResponse>> apiResponse = ApiResponse.<List<TripOutcomeEventResponse>>builder().success(true).data(List.of(item)).build();

        when(tripOutcomeHistoryService.search(any(), any(Pageable.class), any(), anyBoolean())).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/trip-outcome-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-045_B: GET /api/v1/trips/{tripId}/outcome-history - Get history by trip ID returns 200 OK")
    void getHistoryByTripId_Success() throws Exception {
        TripOutcomeEventResponse item = TripOutcomeEventResponse.builder().id(1L).tripId(100L).build();
        ApiResponse<List<TripOutcomeEventResponse>> apiResponse = ApiResponse.<List<TripOutcomeEventResponse>>builder().success(true).data(List.of(item)).build();
        when(tripOutcomeHistoryService.search(any(), any(Pageable.class), any(), anyBoolean())).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/trips/100/outcome-history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

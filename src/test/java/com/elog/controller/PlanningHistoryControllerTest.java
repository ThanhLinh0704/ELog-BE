package com.elog.controller;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.PlanningEventResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.PlanningHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlanningHistoryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlanningHistoryService planningHistoryService;

    @InjectMocks
    private PlanningHistoryController planningHistoryController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(planningHistoryController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("L3-PLANNING-038: GET /api/v1/planning-events - Returns planning history audit events with 200 OK")
    void searchPlanningEvents_Success() throws Exception {
        PlanningEventResponse item = PlanningEventResponse.builder()
                .id(1L)
                .actorUsername("dispatcher01")
                .build();

        ApiResponse<List<PlanningEventResponse>> apiResponse = ApiResponse.<List<PlanningEventResponse>>builder()
                .success(true)
                .data(List.of(item))
                .build();

        when(planningHistoryService.search(any(PlanningHistoryService.PlanningHistoryFilter.class), any(Pageable.class)))
                .thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/planning-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1));
    }
}

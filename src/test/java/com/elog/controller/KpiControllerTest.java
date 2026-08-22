package com.elog.controller;

import com.elog.dto.response.kpi.KpiByDriverResponse;
import com.elog.dto.response.kpi.KpiByRouteResponse;
import com.elog.dto.response.kpi.KpiByVehicleResponse;
import com.elog.dto.response.kpi.KpiDailyTrendResponse;
import com.elog.dto.response.kpi.KpiSummaryResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.KpiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KpiControllerTest {

    private MockMvc mockMvc;

    @Mock
    private KpiService kpiService;

    @InjectMocks
    private KpiController kpiController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(kpiController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("L3-MONITORING-047: GET /api/v1/kpi/summary - Get KPI summary returns 200 OK")
    void getSummary_Success() throws Exception {
        KpiSummaryResponse response = KpiSummaryResponse.builder().period(new KpiSummaryResponse.PeriodInfo()).build();
        when(kpiService.calculate(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/kpi/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-048: GET /api/v1/kpi/daily-trend - Daily trend returns 200 OK")
    void getDailyTrend_Success() throws Exception {
        KpiDailyTrendResponse response = KpiDailyTrendResponse.builder().period(new KpiSummaryResponse.PeriodInfo()).build();
        when(kpiService.getDailyTrend(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/kpi/daily-trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-049: GET /api/v1/kpi/by-route - KPI by route returns 200 OK")
    void getByRoute_Success() throws Exception {
        KpiByRouteResponse response = KpiByRouteResponse.builder().period(new KpiSummaryResponse.PeriodInfo()).build();
        when(kpiService.getByRoute(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/kpi/by-route"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-132: GET /api/v1/kpi/by-vehicle - KPI by vehicle returns 200 OK")
    void getByVehicle_Success() throws Exception {
        KpiByVehicleResponse response = KpiByVehicleResponse.builder().period(new KpiSummaryResponse.PeriodInfo()).build();
        when(kpiService.getByVehicle(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/kpi/by-vehicle"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-133: GET /api/v1/kpi/by-driver - KPI by driver returns 200 OK")
    void getByDriver_Success() throws Exception {
        KpiByDriverResponse response = KpiByDriverResponse.builder().period(new KpiSummaryResponse.PeriodInfo()).build();
        when(kpiService.getByDriver(any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/kpi/by-driver"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

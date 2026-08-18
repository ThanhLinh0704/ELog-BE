package com.elog.controller;

import com.elog.dto.request.route.RouteCreateRequest;
import com.elog.dto.request.route.RouteStatusUpdateRequest;
import com.elog.dto.request.route.RouteStopAddRequest;
import com.elog.dto.request.route.RouteStopReorderRequest;
import com.elog.dto.request.route.RouteUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.route.RouteDetailResponse;
import com.elog.dto.response.route.RouteDirectionsResponse;
import com.elog.dto.response.route.RouteResponse;
import com.elog.dto.response.route.RouteStopResponse;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.RouteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RouteControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RouteService routeService;

    @InjectMocks
    private RouteController routeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(routeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("L3-MASTERDATA-009: GET /api/v1/routes - List routes returns 200 OK")
    void getAllRoutes_Success() throws Exception {
        RouteResponse response = RouteResponse.builder().id(1L).code("TUYEN-01").build();
        ApiResponse<List<RouteResponse>> apiResponse = ApiResponse.<List<RouteResponse>>builder().success(true).data(List.of(response)).build();

        when(routeService.getAllRoutes(any(), any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/routes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-010: POST /api/v1/routes - Create route returns 201 Created")
    void createRoute_Success() throws Exception {
        RouteCreateRequest request = new RouteCreateRequest();
        request.setCode("TUYEN-01");
        request.setName("Tuyen 1");

        RouteResponse response = RouteResponse.builder().id(1L).code("TUYEN-01").build();
        when(routeService.createRoute(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-011: GET /api/v1/routes/{id} - Get route detail returns 200 OK")
    void getRouteById_Success() throws Exception {
        RouteDetailResponse response = RouteDetailResponse.builder().id(1L).code("TUYEN-01").build();
        when(routeService.getRouteById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/routes/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-012: PUT /api/v1/routes/{id} - Update route returns 200 OK")
    void updateRoute_Success() throws Exception {
        RouteUpdateRequest request = new RouteUpdateRequest();
        request.setName("Tuyen Updated");

        RouteResponse response = RouteResponse.builder().id(1L).code("TUYEN-01").build();
        when(routeService.updateRoute(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/routes/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-013: POST /api/v1/routes/{id}/stops - Add stop returns 201 Created")
    void addStop_Success() throws Exception {
        RouteStopAddRequest request = new RouteStopAddRequest();
        request.setStoreId(10L);

        RouteStopResponse response = RouteStopResponse.builder().id(1L).build();
        when(routeService.addStop(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/routes/1/stops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-014: DELETE /api/v1/routes/{id}/stops/{stopId} - Remove stop returns 200 OK")
    void removeStop_Success() throws Exception {
        doNothing().when(routeService).removeStop(1L, 10L);

        mockMvc.perform(delete("/api/v1/routes/1/stops/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-114: PUT /api/v1/routes/{id}/stops/reorder - Reorder stops returns 200 OK")
    void reorderStops_Success() throws Exception {
        RouteStopReorderRequest request = new RouteStopReorderRequest();
        request.setOrderedStopIds(List.of(1L, 2L));

        RouteStopResponse response = RouteStopResponse.builder().id(1L).build();
        when(routeService.reorderStops(eq(1L), any())).thenReturn(List.of(response));

        mockMvc.perform(put("/api/v1/routes/1/stops/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-115: PATCH /api/v1/routes/{id}/status - Update route status returns 200 OK")
    void updateRouteStatus_Success() throws Exception {
        RouteStatusUpdateRequest request = new RouteStatusUpdateRequest();
        request.setIsActive(false);

        RouteResponse response = RouteResponse.builder().id(1L).isActive(false).build();
        when(routeService.updateRouteStatus(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/routes/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-116: GET /api/v1/routes/{id}/directions - Get directions returns 200 OK")
    void getRouteDirections_Success() throws Exception {
        RouteDirectionsResponse response = RouteDirectionsResponse.builder().totalDistanceKm(5.0).build();
        when(routeService.getRouteDirections(eq(1L), anyBoolean())).thenReturn(response);

        mockMvc.perform(get("/api/v1/routes/1/directions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

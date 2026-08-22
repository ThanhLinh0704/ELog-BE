package com.elog.controller;

import com.elog.dto.request.trip.UpdateOrderResultRequest;
import com.elog.dto.response.trip.TripOutcomeResponse;
import com.elog.dto.response.user.DriverTripResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.DriverTripService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverTripControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DriverTripService driverTripService;

    @InjectMocks
    private DriverTripController driverTripController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(driverTripController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("driver01", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-EXECUTION-050: GET /api/v1/driver/trips/active - Get active trip returns 200 OK")
    void getActiveTrip_Success() throws Exception {
        DriverTripResponse response = DriverTripResponse.builder().executionId(1L).tripId(100L).status("IN_PROGRESS").build();
        when(driverTripService.getActiveTrip("driver01")).thenReturn(response);

        mockMvc.perform(get("/api/v1/driver/trips/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-051: GET /api/v1/driver/trips/pending-return - Get pending return trips returns 200 OK")
    void getPendingReturnTrips_Success() throws Exception {
        DriverTripResponse response = DriverTripResponse.builder().executionId(1L).tripId(100L).build();
        when(driverTripService.getPendingReturnTrips("driver01")).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/driver/trips/pending-return"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-052: POST /api/v1/driver/trips/{executionId}/start - Start trip returns 200 OK")
    void startTrip_Success() throws Exception {
        DriverTripResponse response = DriverTripResponse.builder().executionId(1L).status("IN_PROGRESS").build();
        when(driverTripService.startTrip(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/driver/trips/1/start"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-053: POST /api/v1/driver/trips/{executionId}/stops/{stopId}/arrive - Arrive at stop returns 200 OK")
    void arriveAtStop_Success() throws Exception {
        DriverTripResponse response = DriverTripResponse.builder().executionId(1L).build();
        when(driverTripService.arriveAtStop(eq(1L), eq(10L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/driver/trips/1/stops/10/arrive"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-054: PUT /api/v1/driver/trips/{executionId}/orders/{orderId}/result - Update order result returns 200 OK")
    void updateOrderResult_Success() throws Exception {
        UpdateOrderResultRequest request = UpdateOrderResultRequest.builder().status("DELIVERED").build();

        DriverTripResponse response = DriverTripResponse.builder().executionId(1L).build();
        when(driverTripService.updateOrderResult(eq(1L), eq(100L), any(), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/driver/trips/1/orders/100/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-055: POST /api/v1/driver/trips/{executionId}/complete - Complete trip returns 200 OK")
    void completeTrip_Success() throws Exception {
        TripOutcomeResponse response = TripOutcomeResponse.builder().id(1L).status("SUBMITTED").build();
        when(driverTripService.completeTrip(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/driver/trips/1/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-056: POST /api/v1/driver/trips/{executionId}/return-to-warehouse - Return to warehouse returns 200 OK")
    void returnToWarehouse_Success() throws Exception {
        DriverTripResponse response = DriverTripResponse.builder().executionId(1L).build();
        when(driverTripService.returnToWarehouse(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/driver/trips/1/return-to-warehouse"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-135: PUT /api/v1/driver/trips/{executionId}/orders/{orderId}/result - Returns 400 on invalid result")
    void updateOrderResult_Invalid_Returns400() throws Exception {
        UpdateOrderResultRequest request = UpdateOrderResultRequest.builder().status("INVALID").build();

        when(driverTripService.updateOrderResult(eq(1L), eq(100L), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_FAILED, "Result invalid", HttpStatus.BAD_REQUEST));

        mockMvc.perform(put("/api/v1/driver/trips/1/orders/100/result")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("L3-EXECUTION-136: GET /api/v1/driver/trips/active - Returns 404 when no active trip")
    void getActiveTrip_NoActiveTrip_Returns404() throws Exception {
        when(driverTripService.getActiveTrip("driver01"))
                .thenThrow(new BusinessException(ErrorCode.TRIP_NOT_FOUND, "No active trip found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/driver/trips/active"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("TRIP_NOT_FOUND"));
    }
}

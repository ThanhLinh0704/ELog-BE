package com.elog.controller;

import com.elog.dto.request.trip.AdminTripOverrideRequest;
import com.elog.dto.response.user.DriverTripResponse;
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
import org.springframework.http.MediaType;
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
class TripExecutionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DriverTripService driverTripService;

    @InjectMocks
    private TripExecutionController tripExecutionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tripExecutionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-EXECUTION-056_B: POST /api/v1/trip-executions/{id}/admin-override - Admin override returns 200 OK")
    void adminOverrideTripExecution_Success() throws Exception {
        AdminTripOverrideRequest request = new AdminTripOverrideRequest();
        request.setAction("FORCE_RETURN");
        request.setReason("Emergency forced return");

        DriverTripResponse response = DriverTripResponse.builder().executionId(1L).build();
        when(driverTripService.adminOverrideTripExecution(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-executions/1/admin-override")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

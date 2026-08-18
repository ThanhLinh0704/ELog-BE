package com.elog.controller;

import com.elog.dto.request.user.DriverStatusUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.user.DriverResponse;
import com.elog.entity.DriverStatus;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.DriverStatusService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DriverStatusControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private DriverStatusService driverStatusService;

    @InjectMocks
    private DriverStatusController driverStatusController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(driverStatusController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-IDENTITY-104: GET /api/v1/drivers/{id} - Get driver details returns 200 OK")
    void getDriverById_Success() throws Exception {
        DriverResponse response = DriverResponse.builder().id(10L).fullName("Nguyen Van Driver").driverStatus("AVAILABLE").build();
        when(driverStatusService.getDriverById(10L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/drivers/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-105: PATCH /api/v1/drivers/{id}/status - Updates driver status with 200 OK")
    void updateStatus_Success() throws Exception {
        DriverStatusUpdateRequest request = new DriverStatusUpdateRequest();
        request.setStatus(DriverStatus.INACTIVE);

        DriverResponse response = DriverResponse.builder().id(10L).fullName("Nguyen Van Driver").driverStatus("INACTIVE").build();
        when(driverStatusService.updateStatus(eq(10L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/drivers/10/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-129: GET /api/v1/drivers - List drivers returns 200 OK")
    void listDrivers_Success() throws Exception {
        DriverResponse response = DriverResponse.builder().id(10L).fullName("Nguyen Van Driver").driverStatus("AVAILABLE").build();
        ApiResponse<List<DriverResponse>> apiResponse = ApiResponse.<List<DriverResponse>>builder().success(true).data(List.of(response)).build();
        when(driverStatusService.getDrivers(any(), any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/drivers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IDENTITY-130: PATCH /api/v1/drivers/{id}/status - Returns 404 when driver not found")
    void updateStatus_NotFound_Returns404() throws Exception {
        DriverStatusUpdateRequest request = new DriverStatusUpdateRequest();
        request.setStatus(DriverStatus.INACTIVE);

        when(driverStatusService.updateStatus(eq(999L), any(), any()))
                .thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND, "Driver not found: 999", HttpStatus.NOT_FOUND));

        mockMvc.perform(patch("/api/v1/drivers/999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"));
    }
}

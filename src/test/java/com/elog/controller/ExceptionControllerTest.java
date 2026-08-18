package com.elog.controller;

import com.elog.dto.request.exception.RejectStopRequest;
import com.elog.dto.request.exception.ResolveExceptionRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.exception.DeliveryExceptionResponse;
import com.elog.dto.response.exception.ExceptionListResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.ExceptionService;
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

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExceptionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ExceptionService exceptionService;

    @InjectMocks
    private ExceptionController exceptionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(exceptionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dispatcher01", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-MONITORING-043: GET /api/v1/exceptions - List exceptions returns 200 OK")
    void listExceptions_Success() throws Exception {
        ExceptionListResponse response = ExceptionListResponse.builder().date("2026-08-16").totalCount(1).unresolvedCount(1).exceptions(List.of()).build();
        when(exceptionService.listExceptions(any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/exceptions?date=" + LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-044: GET /api/v1/exceptions/{id} - Get exception detail returns 200 OK")
    void getException_Success() throws Exception {
        DeliveryExceptionResponse response = DeliveryExceptionResponse.builder().exceptionId(1L).exceptionType("DELIVERY_REJECTION").build();
        when(exceptionService.getException(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/exceptions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-045: POST /api/v1/trip-stops/{id}/reject - Reject stop returns 201 Created")
    void rejectStop_Success() throws Exception {
        RejectStopRequest request = new RejectStopRequest();
        request.setRejectionType("STORE_CLOSED");
        request.setDescription("Store closed on Sunday");

        DeliveryExceptionResponse response = DeliveryExceptionResponse.builder().exceptionId(1L).build();
        when(exceptionService.rejectStop(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-stops/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-046: PATCH /api/v1/exceptions/{id}/resolve - Resolve exception returns 200 OK")
    void resolveException_Success() throws Exception {
        ResolveExceptionRequest request = new ResolveExceptionRequest();
        request.setResolutionNotes("Resolved and rescheduled");

        DeliveryExceptionResponse response = DeliveryExceptionResponse.builder().exceptionId(1L).build();
        when(exceptionService.resolveException(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/exceptions/1/resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MONITORING-131: GET /api/v1/exceptions/violations - List violations returns 200 OK")
    void listViolations_Success() throws Exception {
        ExceptionListResponse response = ExceptionListResponse.builder().date("2026-08-16").totalCount(1).build();
        when(exceptionService.listExceptions(any(), any(), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/exceptions/violations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

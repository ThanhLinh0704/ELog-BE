package com.elog.controller;

import com.elog.dto.request.vehicle.VehicleCreateRequest;
import com.elog.dto.request.vehicle.VehicleStatusUpdateRequest;
import com.elog.dto.request.vehicle.VehicleUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.vehicle.VehicleFleetCapacityResponse;
import com.elog.dto.response.vehicle.VehicleListItemResponse;
import com.elog.dto.response.vehicle.VehicleResponse;
import com.elog.entity.LicenseClass;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.VehicleService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VehicleControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private VehicleService vehicleService;

    @InjectMocks
    private VehicleController vehicleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vehicleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("L3-MASTERDATA-019: POST /api/v1/vehicles - Create vehicle returns 201 Created")
    void createVehicle_Success() throws Exception {
        VehicleCreateRequest request = new VehicleCreateRequest();
        request.setVehicleCode("XE-01");
        request.setPlateNumber("29C-12345");
        request.setVehicleType("TRUCK");
        request.setPayloadKg(new BigDecimal("500.0"));
        request.setMaxVolumeM3(new BigDecimal("3.5"));
        request.setRequiredLicense(LicenseClass.B);

        VehicleResponse response = VehicleResponse.builder().id(1L).vehicleCode("XE-01").build();
        when(vehicleService.createVehicle(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-020: GET /api/v1/vehicles/fleet-capacity - Get fleet capacity returns 200 OK")
    void getFleetCapacity_Success() throws Exception {
        VehicleFleetCapacityResponse response = VehicleFleetCapacityResponse.builder().activeVehicleCount(10).build();
        when(vehicleService.getFleetCapacity()).thenReturn(response);

        mockMvc.perform(get("/api/v1/vehicles/fleet-capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-021: GET /api/v1/vehicles/available - Get available vehicles returns 200 OK")
    void getAvailableVehicles_Success() throws Exception {
        VehicleResponse response = VehicleResponse.builder().id(1L).vehicleCode("XE-01").build();
        when(vehicleService.findAvailableVehiclesForTrip(any())).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/vehicles/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-022: GET /api/v1/vehicles/{id} - Get vehicle by ID returns 200 OK")
    void getVehicleById_Success() throws Exception {
        VehicleResponse response = VehicleResponse.builder().id(1L).vehicleCode("XE-01").build();
        when(vehicleService.getVehicleById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-023: GET /api/v1/vehicles - List vehicles returns 200 OK")
    void getAllVehicles_Success() throws Exception {
        VehicleListItemResponse item = VehicleListItemResponse.builder().id(1L).vehicleCode("XE-01").build();
        ApiResponse<List<VehicleListItemResponse>> apiResponse = ApiResponse.<List<VehicleListItemResponse>>builder().success(true).data(List.of(item)).build();
        when(vehicleService.getAllVehicles(any(), any(), any(), any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-119: PUT /api/v1/vehicles/{id} - Update vehicle returns 200 OK")
    void updateVehicle_Success() throws Exception {
        VehicleUpdateRequest request = new VehicleUpdateRequest();
        request.setVehicleType("TRUCK");
        request.setPayloadKg(new BigDecimal("600.0"));
        request.setMaxVolumeM3(new BigDecimal("4.0"));
        request.setRequiredLicense(LicenseClass.B);

        VehicleResponse response = VehicleResponse.builder().id(1L).vehicleCode("XE-01").build();
        when(vehicleService.updateVehicle(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/vehicles/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-120: PATCH /api/v1/vehicles/{id}/status - Update status returns 200 OK")
    void updateVehicleStatus_Success() throws Exception {
        VehicleStatusUpdateRequest request = new VehicleStatusUpdateRequest();
        request.setIsActive(false);

        VehicleResponse response = VehicleResponse.builder().id(1L).isActive(false).build();
        when(vehicleService.updateVehicleStatus(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/vehicles/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-121: POST /api/v1/vehicles - Returns 400 on validation failure")
    void createVehicle_ValidationError_Returns400() throws Exception {
        VehicleCreateRequest request = new VehicleCreateRequest();
        request.setVehicleCode("XE-01");

        mockMvc.perform(post("/api/v1/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("L3-MASTERDATA-122: GET /api/v1/vehicles/{id} - Returns 404 when vehicle not found")
    void getVehicleById_NotFound_Returns404() throws Exception {
        when(vehicleService.getVehicleById(999L))
                .thenThrow(new BusinessException(ErrorCode.VEHICLE_NOT_FOUND, "Vehicle not found: 999", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/vehicles/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("VEHICLE_NOT_FOUND"));
    }
}

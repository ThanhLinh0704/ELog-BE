package com.elog.controller;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.importbatch.ImportBatchResponse;
import com.elog.dto.response.importbatch.ImportErrorResponse;
import com.elog.dto.response.importbatch.ImportedOrderDetailResponse;
import com.elog.entity.User;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.repository.UserRepository;
import com.elog.service.ImportService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ImportControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ImportService importService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ImportController importController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(importController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("L3-IMPORT-001: POST /api/v1/imports - Batch import orders returns 201 Created")
    void importOrders_Success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "orders.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());

        User user = User.builder().id(1L).username("user").build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        ImportBatchResponse response = ImportBatchResponse.builder()
                .batchId(1L)
                .status("SUCCESS")
                .acceptedRows(10)
                .rejectedRows(0)
                .build();

        when(importService.importExcel(any(), any(), anyBoolean(), eq(1L))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", LocalDate.now().toString())
                        .principal(new UsernamePasswordAuthenticationToken("user", "pass", List.of())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IMPORT-002: GET /api/v1/imports/{id} - Get batch summary returns 200 OK")
    void getImportBatch_Success() throws Exception {
        ImportBatchResponse response = ImportBatchResponse.builder().batchId(1L).status("SUCCESS").build();
        when(importService.getBatchById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/imports/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IMPORT-003: GET /api/v1/imports/{id}/errors - Get import error rows returns 200 OK")
    void getImportErrors_Success() throws Exception {
        ImportErrorResponse error = ImportErrorResponse.builder().rowNumber(2).errorCode("INVALID_STORE").build();
        ApiResponse<List<ImportErrorResponse>> apiResponse = ApiResponse.<List<ImportErrorResponse>>builder().success(true).data(List.of(error)).build();
        when(importService.getBatchErrors(eq(1L), any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/imports/1/errors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IMPORT-004: GET /api/v1/imports/{id}/orders - Get imported orders list returns 200 OK")
    void getImportOrders_Success() throws Exception {
        ImportedOrderDetailResponse order = ImportedOrderDetailResponse.builder().orderRef("ORD-001").build();
        when(importService.getImportedOrders(eq(1L), any())).thenReturn(List.of(order));

        mockMvc.perform(get("/api/v1/imports/1/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IMPORT-005: GET /api/v1/imports - List all import batches returns 200 OK")
    void getAllBatches_Success() throws Exception {
        ImportBatchResponse item = ImportBatchResponse.builder().batchId(1L).status("SUCCESS").build();
        ApiResponse<List<ImportBatchResponse>> apiResponse = ApiResponse.<List<ImportBatchResponse>>builder().success(true).data(List.of(item)).build();
        when(importService.getBatches(any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-IMPORT-126: POST /api/v1/imports - Duplicate orders exist returns 409 Conflict")
    void importOrders_DuplicateConflict_Returns409() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "orders.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "content".getBytes());

        User user = User.builder().id(1L).username("user").build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        when(importService.importExcel(any(), any(), anyBoolean(), eq(1L)))
                .thenThrow(new BusinessException(ErrorCode.DUPLICATE_ORDERS_EXIST, "Orders already exist for delivery date", HttpStatus.CONFLICT));

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", LocalDate.now().toString())
                        .principal(new UsernamePasswordAuthenticationToken("user", "pass", List.of())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_ORDERS_EXIST"));
    }

    @Test
    @DisplayName("L3-IMPORT-127: POST /api/v1/imports - Empty or invalid file returns 400 Bad Request")
    void importOrders_EmptyFile_Returns400() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        User user = User.builder().id(1L).username("user").build();
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));

        when(importService.importExcel(any(), any(), anyBoolean(), eq(1L)))
                .thenThrow(new BusinessException(ErrorCode.VALIDATION_FAILED, "File is empty", HttpStatus.BAD_REQUEST));

        mockMvc.perform(multipart("/api/v1/imports")
                        .file(file)
                        .param("deliveryDate", LocalDate.now().toString())
                        .principal(new UsernamePasswordAuthenticationToken("user", "pass", List.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
    }
}

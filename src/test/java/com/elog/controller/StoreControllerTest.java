package com.elog.controller;

import com.elog.dto.request.store.StoreCreateRequest;
import com.elog.dto.request.store.StoreStatusUpdateRequest;
import com.elog.dto.request.store.StoreUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.store.StoreListItemResponse;
import com.elog.dto.response.store.StoreResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.StoreService;
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
class StoreControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StoreService storeService;

    @InjectMocks
    private StoreController storeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("L3-MASTERDATA-015: GET /api/v1/stores - List stores returns 200 OK")
    void getAllStores_Success() throws Exception {
        StoreListItemResponse item = StoreListItemResponse.builder().id(1L).storeCode("ST-01").build();
        ApiResponse<List<StoreListItemResponse>> apiResponse = ApiResponse.<List<StoreListItemResponse>>builder().success(true).data(List.of(item)).build();

        when(storeService.getAllStores(any(), any(), any(), any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-016: POST /api/v1/stores - Create store returns 201 Created")
    void createStore_Success() throws Exception {
        StoreCreateRequest request = validRequest("ST-01");
        StoreResponse response = StoreResponse.builder().id(1L).storeCode("ST-01").build();
        when(storeService.createStore(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-017: GET /api/v1/stores/{id} - Get store by ID returns 200 OK")
    void getStoreById_Success() throws Exception {
        StoreResponse response = StoreResponse.builder().id(1L).storeCode("ST-01").build();
        when(storeService.getStoreById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/stores/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-018: PUT /api/v1/stores/{id} - Update store returns 200 OK")
    void updateStore_Success() throws Exception {
        StoreUpdateRequest request = new StoreUpdateRequest();
        request.setStoreName("Updated Store");
        request.setProvinceCode("P01");
        request.setDistrictCode("D01");
        request.setWardCode("W01");
        request.setAddressDetail("456 Cau Giay");

        StoreResponse response = StoreResponse.builder().id(1L).storeCode("ST-01").build();
        when(storeService.updateStore(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/stores/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-117: PATCH /api/v1/stores/{id}/status - Update store status returns 200 OK")
    void updateStoreStatus_Success() throws Exception {
        StoreStatusUpdateRequest request = new StoreStatusUpdateRequest();
        request.setIsActive(false);

        StoreResponse response = StoreResponse.builder().id(1L).isActive(false).build();
        when(storeService.updateStoreStatus(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/stores/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-118: GET /api/v1/stores/{id} - Returns 404 when store not found")
    void getStoreById_NotFound_Returns404() throws Exception {
        when(storeService.getStoreById(999L))
                .thenThrow(new BusinessException(ErrorCode.STORE_NOT_FOUND, "Store not found: 999", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/stores/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
    }

    private StoreCreateRequest validRequest(String code) {
        StoreCreateRequest request = new StoreCreateRequest();
        request.setStoreCode(code);
        request.setStoreName("VinMart Ba Dinh");
        request.setProvinceCode("P01");
        request.setDistrictCode("D01");
        request.setWardCode("W01");
        request.setAddressDetail("123 Doi Can");
        request.setLatitude(21.0333);
        request.setLongitude(105.8167);
        return request;
    }
}

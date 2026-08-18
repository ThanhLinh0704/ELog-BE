package com.elog.controller;

import com.elog.dto.request.product.ProductCreateRequest;
import com.elog.dto.request.product.ProductStatusUpdateRequest;
import com.elog.dto.request.product.ProductUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.product.ProductListItemResponse;
import com.elog.dto.response.product.ProductResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.ProductService;
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
class ProductControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    @DisplayName("L3-MASTERDATA-005: GET /api/v1/products - List products returns 200 OK")
    void getAllProducts_Success() throws Exception {
        ProductListItemResponse response = ProductListItemResponse.builder().id(1L).sku("SKU-COCA").build();
        ApiResponse<List<ProductListItemResponse>> apiResponse = ApiResponse.<List<ProductListItemResponse>>builder().success(true).data(List.of(response)).build();

        when(productService.getAllProducts(any(), any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-006: POST /api/v1/products - Create product returns 201 Created")
    void createProduct_Success() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku("SKU-01");
        request.setProductName("Test Product");
        request.setWeightKg(new BigDecimal("10.0"));
        request.setLengthM(new BigDecimal("0.5"));
        request.setWidthM(new BigDecimal("0.5"));
        request.setHeightM(new BigDecimal("0.5"));

        ProductResponse response = ProductResponse.builder().id(1L).sku("SKU-01").build();
        when(productService.createProduct(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-007: GET /api/v1/products/{id} - Get product by ID returns 200 OK")
    void getProductById_Success() throws Exception {
        ProductResponse response = ProductResponse.builder().id(1L).sku("SKU-01").build();
        when(productService.getProductById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-008: PUT /api/v1/products/{id} - Update product returns 200 OK")
    void updateProduct_Success() throws Exception {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setProductName("Updated Product");
        request.setWeightKg(new BigDecimal("15.0"));
        request.setLengthM(new BigDecimal("0.6"));
        request.setWidthM(new BigDecimal("0.6"));
        request.setHeightM(new BigDecimal("0.6"));

        ProductResponse response = ProductResponse.builder().id(1L).sku("SKU-01").build();
        when(productService.updateProduct(eq(1L), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-112: PATCH /api/v1/products/{id}/status - Update status returns 200 OK")
    void updateProductStatus_Success() throws Exception {
        ProductStatusUpdateRequest request = new ProductStatusUpdateRequest();
        request.setIsActive(false);

        ProductResponse response = ProductResponse.builder().id(1L).isActive(false).build();
        when(productService.updateProductStatus(eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/products/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-113: GET /api/v1/products/by-sku/{sku} - Get product by SKU returns 200 OK")
    void getProductBySku_Success() throws Exception {
        ProductResponse response = ProductResponse.builder().id(1L).sku("SKU-01").build();
        when(productService.getProductBySku("SKU-01")).thenReturn(response);

        mockMvc.perform(get("/api/v1/products/by-sku/SKU-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

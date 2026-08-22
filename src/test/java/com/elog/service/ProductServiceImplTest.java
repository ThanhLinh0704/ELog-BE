package com.elog.service;

import com.elog.dto.request.product.ProductCreateRequest;
import com.elog.dto.request.product.ProductStatusUpdateRequest;
import com.elog.dto.request.product.ProductUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.product.ProductListItemResponse;
import com.elog.dto.response.product.ProductResponse;
import com.elog.entity.Product;
import com.elog.mapper.ProductMapper;
import com.elog.repository.ProductRepository;
import com.elog.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock ProductRepository productRepository;
    @Mock ProductMapper productMapper;

    ProductServiceImpl service;
    Product product;

    @BeforeEach
    void setUp() {
        service = new ProductServiceImpl(productRepository, productMapper);
        product = Product.builder().id(10L).sku("SKU-01").productName("Milk Box").weightKg(new BigDecimal("10")).volumeM3(new BigDecimal("0.02")).isActive(true).build();
    }

    @Test
    @DisplayName("[L1-PR-01] createProduct saves product for valid capacity ratio")
    void createProductSuccess() {
        ProductCreateRequest req = new ProductCreateRequest();
        req.setSku("SKU-01");
        req.setWeightKg(new BigDecimal("10"));
        req.setLengthM(new BigDecimal("0.2"));
        req.setWidthM(new BigDecimal("0.2"));
        req.setHeightM(new BigDecimal("0.5"));

        when(productRepository.existsBySku("SKU-01")).thenReturn(false);
        when(productMapper.toEntity(req)).thenReturn(product);
        when(productRepository.save(any())).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(ProductResponse.builder().id(10L).sku("SKU-01").build());

        ProductResponse resp = service.createProduct(req);

        assertAll(
                () -> assertEquals(10L, resp.getId()),
                () -> verify(productRepository).save(product)
        );
    }

    @Test
    @DisplayName("[L1-PR-02] getProductById returns product response")
    void getProductByIdSuccess() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(ProductResponse.builder().id(10L).sku("SKU-01").build());

        ProductResponse resp = service.getProductById(10L);

        assertEquals(10L, resp.getId());
    }

    @Test
    @DisplayName("[L1-PR-03] getProductBySku returns product response")
    void getProductBySkuSuccess() {
        when(productRepository.findBySku("SKU-01")).thenReturn(Optional.of(product));
        when(productMapper.toResponse(product)).thenReturn(ProductResponse.builder().id(10L).sku("SKU-01").build());

        ProductResponse resp = service.getProductBySku("SKU-01");

        assertEquals(10L, resp.getId());
    }

    @Test
    @DisplayName("[L1-PR-04] getAllProducts returns paginated product list")
    void getAllProductsSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toListItem(product)).thenReturn(ProductListItemResponse.builder().id(10L).sku("SKU-01").build());

        ApiResponse<List<ProductListItemResponse>> resp = service.getAllProducts(null, null, pageable);

        assertAll(
                () -> assertTrue(resp.isSuccess()),
                () -> assertEquals(1, resp.getData().size())
        );
    }

    @Test
    @DisplayName("[L1-PR-05] updateProductStatus updates active status")
    void updateProductStatusSuccess() {
        ProductStatusUpdateRequest req = new ProductStatusUpdateRequest();
        req.setIsActive(false);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toResponse(product)).thenReturn(ProductResponse.builder().id(10L).sku("SKU-01").isActive(false).build());

        ProductResponse resp = service.updateProductStatus(10L, req);

        assertAll(
                () -> assertEquals(10L, resp.getId()),
                () -> assertFalse(resp.getIsActive())
        );
    }

    @Test
    void updateProduct_success_withClassificationFields() {
        ProductUpdateRequest updateReq = new ProductUpdateRequest();
        updateReq.setProductName("Updated Name");
        updateReq.setBrand("Brand A");
        updateReq.setProductGroup("Tủ lạnh");
        updateReq.setProductType("1.Tủ lạnh");
        updateReq.setCapacityValue(BigDecimal.valueOf(500.0));
        updateReq.setWeightKg(BigDecimal.valueOf(50.0));
        updateReq.setLengthM(BigDecimal.valueOf(0.8));
        updateReq.setWidthM(BigDecimal.valueOf(0.8));
        updateReq.setHeightM(BigDecimal.valueOf(1.8));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().id(1L).brand("Brand A").build());

        ProductResponse res = service.updateProduct(1L, updateReq);

        assertThat(res).isNotNull();
        assertThat(product.getBrand()).isEqualTo("Brand A");
        assertThat(product.getProductGroup()).isEqualTo("Tủ lạnh");
        assertThat(product.getProductType()).isEqualTo("1.Tủ lạnh");
        assertThat(product.getCapacityValue()).isEqualByComparingTo(BigDecimal.valueOf(500.0));
        verify(productRepository).save(product);
    }
}

package com.elog.service;

import com.elog.dto.request.product.ProductCreateRequest;
import com.elog.dto.response.product.ProductResponse;
import com.elog.entity.Product;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.ProductMapper;
import com.elog.repository.ProductRepository;
import com.elog.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductMapper productMapper;

    @InjectMocks
    private ProductServiceImpl productService;

    private ProductCreateRequest request;
    private Product product;

    @BeforeEach
    void setUp() {
        request = new ProductCreateRequest();
        request.setSku("SKU-VALID");
        request.setProductName("Valid Product");
        request.setWeightKg(BigDecimal.valueOf(10.0));
        request.setLengthM(BigDecimal.valueOf(0.5));
        request.setWidthM(BigDecimal.valueOf(0.5));
        request.setHeightM(BigDecimal.valueOf(0.5)); // volume = 0.125, ratio = 10 / 0.125 = 80 kg/m3

        product = Product.builder()
                .id(1L)
                .sku("SKU-VALID")
                .productName("Valid Product")
                .weightKg(BigDecimal.valueOf(10.0))
                .lengthM(BigDecimal.valueOf(0.5))
                .widthM(BigDecimal.valueOf(0.5))
                .heightM(BigDecimal.valueOf(0.5))
                .volumeM3(BigDecimal.valueOf(0.125))
                .isActive(true)
                .build();
    }

    @Test
    void createProduct_success_validRatio() {
        when(productRepository.existsBySku("SKU-VALID")).thenReturn(false);
        when(productMapper.toEntity(any(ProductCreateRequest.class))).thenReturn(product);
        when(productRepository.save(any(Product.class))).thenReturn(product);
        when(productMapper.toResponse(any(Product.class))).thenReturn(ProductResponse.builder().id(1L).sku("SKU-VALID").build());

        ProductResponse response = productService.createProduct(request);

        assertThat(response.getId()).isEqualTo(1L);
        verify(productRepository).save(any(Product.class));
    }

    @Test
    void createProduct_fails_anomalyRatioTooHigh() {
        // volume = 0.5 * 0.5 * 0.5 = 0.125. Let's make weight 2000.0 kg -> ratio = 2000 / 0.125 = 16000 kg/m3 (extremely dense)
        request.setWeightKg(BigDecimal.valueOf(2000.0));

        when(productRepository.existsBySku("SKU-VALID")).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CAPACITY_RATIO)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void createProduct_fails_anomalyRatioTooLow() {
        // volume = 1.0 * 1.0 * 1.0 = 1.0. Let's make weight 0.001 kg -> ratio = 0.001 / 1.0 = 0.001 kg/m3 (extremely light)
        request.setLengthM(BigDecimal.valueOf(1.0));
        request.setWidthM(BigDecimal.valueOf(1.0));
        request.setHeightM(BigDecimal.valueOf(1.0));
        request.setWeightKg(BigDecimal.valueOf(0.001));

        when(productRepository.existsBySku("SKU-VALID")).thenReturn(false);

        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CAPACITY_RATIO)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);

        verify(productRepository, never()).save(any(Product.class));
    }
}

package com.elog.integration;

import com.elog.dto.request.ProductCreateRequest;
import com.elog.dto.request.ProductUpdateRequest;
import com.elog.dto.response.ProductResponse;
import com.elog.entity.Product;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.ProductRepository;
import com.elog.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class ProductServiceReport5IntegrationTest {

    @Autowired ProductService productService;
    @Autowired ProductRepository productRepository;
    @Autowired EntityManager entityManager;

    @Test
    void l2Prd01CreatesProductAndPersistsCalculatedVolume() {
        String sku = "R5-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProductCreateRequest request = createRequest(sku);

        ProductResponse response = productService.createProduct(request);
        entityManager.flush();
        entityManager.clear();

        Product persisted = productRepository.findBySku(sku).orElseThrow();
        assertThat(response.getSku()).isEqualTo(sku);
        assertThat(persisted.getVolumeM3()).isEqualByComparingTo("0.030000");
        assertThat(persisted.getWeightKg()).isEqualByComparingTo("5.000");
    }

    @Test
    void l2Prd02RejectsDuplicateSkuWithoutInsertingASecondRow() {
        String sku = "R5-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        productService.createProduct(createRequest(sku));
        entityManager.flush();
        long before = productRepository.count();

        assertThatThrownBy(() -> productService.createProduct(createRequest(sku)))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_SKU_DUPLICATE));
        assertThat(productRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Prd03RejectsSkuChangeAndLeavesPersistedProductUnchanged() {
        String sku = "R5-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        ProductResponse created = productService.createProduct(createRequest(sku));
        entityManager.flush();
        ProductUpdateRequest update = updateRequest("CHANGED-SKU");

        assertThatThrownBy(() -> productService.updateProduct(created.getId(), update))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.PRODUCT_SKU_IMMUTABLE));
        entityManager.clear();
        assertThat(productRepository.findById(created.getId()).orElseThrow().getSku()).isEqualTo(sku);
    }

    private ProductCreateRequest createRequest(String sku) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku(sku);
        request.setProductName("Report 5 product");
        request.setWeightKg(new BigDecimal("5.000"));
        request.setLengthM(new BigDecimal("0.5000"));
        request.setWidthM(new BigDecimal("0.3000"));
        request.setHeightM(new BigDecimal("0.2000"));
        return request;
    }

    private ProductUpdateRequest updateRequest(String sku) {
        ProductUpdateRequest request = new ProductUpdateRequest();
        request.setSku(sku);
        request.setProductName("Changed name");
        request.setWeightKg(new BigDecimal("5.000"));
        request.setLengthM(new BigDecimal("0.5000"));
        request.setWidthM(new BigDecimal("0.3000"));
        request.setHeightM(new BigDecimal("0.2000"));
        return request;
    }
}

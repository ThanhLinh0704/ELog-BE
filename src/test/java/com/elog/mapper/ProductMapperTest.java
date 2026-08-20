package com.elog.mapper;

import com.elog.dto.request.product.ProductCreateRequest;
import com.elog.dto.response.product.ProductListItemResponse;
import com.elog.dto.response.product.ProductResponse;
import com.elog.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private ProductMapper productMapper;

    @BeforeEach
    void setUp() {
        productMapper = new ProductMapper();
    }

    @Test
    void toEntity_mapsAllFieldsIncludingClassification() {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setSku("SKU-100");
        request.setProductName("Test TV");
        request.setBrand("Sony");
        request.setProductGroup("Tivi");
        request.setProductType("1.Tivi");
        request.setCapacityValue(BigDecimal.valueOf(65.0));
        request.setWeightKg(BigDecimal.valueOf(20.0));
        request.setLengthM(BigDecimal.valueOf(1.5));
        request.setWidthM(BigDecimal.valueOf(0.9));
        request.setHeightM(BigDecimal.valueOf(0.1));

        Product entity = productMapper.toEntity(request);

        assertThat(entity.getSku()).isEqualTo("SKU-100");
        assertThat(entity.getProductName()).isEqualTo("Test TV");
        assertThat(entity.getBrand()).isEqualTo("Sony");
        assertThat(entity.getProductGroup()).isEqualTo("Tivi");
        assertThat(entity.getProductType()).isEqualTo("1.Tivi");
        assertThat(entity.getCapacityValue()).isEqualByComparingTo(BigDecimal.valueOf(65.0));
        assertThat(entity.getVolumeM3()).isNotNull();
    }

    @Test
    void toResponse_mapsAllFieldsIncludingClassification() {
        Product entity = Product.builder()
                .id(10L)
                .sku("SKU-200")
                .productName("Washing Machine")
                .brand("Toshiba")
                .productGroup("Máy giặt")
                .productType("2.Máy giặt")
                .capacityValue(BigDecimal.valueOf(10.5))
                .weightKg(BigDecimal.valueOf(65.0))
                .lengthM(BigDecimal.valueOf(0.7))
                .widthM(BigDecimal.valueOf(0.7))
                .heightM(BigDecimal.valueOf(1.0))
                .volumeM3(BigDecimal.valueOf(0.49))
                .isActive(true)
                .build();

        ProductResponse response = productMapper.toResponse(entity);

        assertThat(response.getId()).isEqualTo(10L);
        assertThat(response.getSku()).isEqualTo("SKU-200");
        assertThat(response.getBrand()).isEqualTo("Toshiba");
        assertThat(response.getProductGroup()).isEqualTo("Máy giặt");
        assertThat(response.getProductType()).isEqualTo("2.Máy giặt");
        assertThat(response.getCapacityValue()).isEqualByComparingTo(BigDecimal.valueOf(10.5));
    }

    @Test
    void toListItem_mapsAllFieldsIncludingClassification() {
        Product entity = Product.builder()
                .id(10L)
                .sku("SKU-300")
                .productName("Fridge")
                .brand("Hitachi")
                .productGroup("Tủ lạnh")
                .productType("1.Tủ lạnh")
                .capacityValue(BigDecimal.valueOf(500.0))
                .weightKg(BigDecimal.valueOf(100.0))
                .volumeM3(BigDecimal.valueOf(1.2))
                .isActive(true)
                .build();

        ProductListItemResponse item = productMapper.toListItem(entity);

        assertThat(item.getId()).isEqualTo(10L);
        assertThat(item.getSku()).isEqualTo("SKU-300");
        assertThat(item.getBrand()).isEqualTo("Hitachi");
        assertThat(item.getProductGroup()).isEqualTo("Tủ lạnh");
        assertThat(item.getProductType()).isEqualTo("1.Tủ lạnh");
        assertThat(item.getCapacityValue()).isEqualByComparingTo(BigDecimal.valueOf(500.0));
    }
}

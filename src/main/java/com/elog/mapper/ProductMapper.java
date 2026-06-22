package com.elog.mapper;

import com.elog.dto.request.ProductCreateRequest;
import com.elog.dto.response.ProductListItemResponse;
import com.elog.dto.response.ProductResponse;
import com.elog.entity.Product;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class ProductMapper {

    public Product toEntity(ProductCreateRequest request) {
        return Product.builder()
                .sku(request.getSku())
                .productName(request.getProductName())
                .weightKg(request.getWeightKg())
                .lengthCm(request.getLengthCm())
                .widthCm(request.getWidthCm())
                .heightCm(request.getHeightCm())
                .volumeM3(calculateVolume(request.getLengthCm(), request.getWidthCm(), request.getHeightCm()))
                .isActive(true)
                .build();
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .productName(product.getProductName())
                .weightKg(product.getWeightKg())
                .lengthCm(product.getLengthCm())
                .widthCm(product.getWidthCm())
                .heightCm(product.getHeightCm())
                .volumeM3(product.getVolumeM3())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductListItemResponse toListItem(Product product) {
        return ProductListItemResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .productName(product.getProductName())
                .weightKg(product.getWeightKg())
                .volumeM3(product.getVolumeM3())
                .isActive(product.getIsActive())
                .build();
    }

    public BigDecimal calculateVolume(BigDecimal lengthCm, BigDecimal widthCm, BigDecimal heightCm) {
        return lengthCm
                .multiply(widthCm)
                .multiply(heightCm)
                .divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP);
    }
}

package com.elog.mapper;

import com.elog.dto.request.product.ProductCreateRequest;
import com.elog.dto.response.product.ProductListItemResponse;
import com.elog.dto.response.product.ProductResponse;
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
                .brand(request.getBrand())
                .productGroup(request.getProductGroup())
                .productType(request.getProductType())
                .capacityValue(request.getCapacityValue())
                .weightKg(request.getWeightKg())
                .lengthM(request.getLengthM())
                .widthM(request.getWidthM())
                .heightM(request.getHeightM())
                .volumeM3(calculateVolume(request.getLengthM(), request.getWidthM(), request.getHeightM()))
                .isActive(true)
                .shape(request.getShape())
                .isFragile(request.getIsFragile() != null ? request.getIsFragile() : false)
                .packageImageUrl(request.getPackageImageUrl())
                .description(request.getDescription())
                .build();
    }

    public ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .productName(product.getProductName())
                .brand(product.getBrand())
                .productGroup(product.getProductGroup())
                .productType(product.getProductType())
                .capacityValue(product.getCapacityValue())
                .weightKg(product.getWeightKg())
                .lengthM(product.getLengthM())
                .widthM(product.getWidthM())
                .heightM(product.getHeightM())
                .volumeM3(product.getVolumeM3())
                .isActive(product.getIsActive())
                .shape(product.getShape())
                .isFragile(product.getIsFragile())
                .packageImageUrl(product.getPackageImageUrl())
                .description(product.getDescription())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    public ProductListItemResponse toListItem(Product product) {
        return ProductListItemResponse.builder()
                .id(product.getId())
                .sku(product.getSku())
                .productName(product.getProductName())
                .brand(product.getBrand())
                .productGroup(product.getProductGroup())
                .productType(product.getProductType())
                .capacityValue(product.getCapacityValue())
                .weightKg(product.getWeightKg())
                .volumeM3(product.getVolumeM3())
                .isActive(product.getIsActive())
                .shape(product.getShape())
                .isFragile(product.getIsFragile())
                .packageImageUrl(product.getPackageImageUrl())
                .description(product.getDescription())
                .build();
    }

    public BigDecimal calculateVolume(BigDecimal lengthM, BigDecimal widthM, BigDecimal heightM) {
        return lengthM
                .multiply(widthM)
                .multiply(heightM)
                .setScale(6, RoundingMode.HALF_UP);
    }
}

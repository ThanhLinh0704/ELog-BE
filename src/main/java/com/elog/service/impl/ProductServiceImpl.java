package com.elog.service.impl;

import com.elog.dto.request.ProductCreateRequest;
import com.elog.dto.request.ProductStatusUpdateRequest;
import com.elog.dto.request.ProductUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ProductListItemResponse;
import com.elog.dto.response.ProductResponse;
import com.elog.entity.Product;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.ProductMapper;
import com.elog.repository.ProductRepository;
import com.elog.repository.specification.ProductSpecification;
import com.elog.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    // ── API 1: POST /api/products ─────────────────────────────────────────────

    @Override
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        if (productRepository.existsBySku(request.getSku())) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_DUPLICATE,
                    "SKU already exists: " + request.getSku(), HttpStatus.CONFLICT);
        }

        Product product = productMapper.toEntity(request);
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    // ── API 2: GET /api/products/{id} ─────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = findOrThrow(id);
        return productMapper.toResponse(product);
    }

    // ── API 3: GET /api/products/by-sku/{sku} ────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with SKU: " + sku, HttpStatus.NOT_FOUND));
        return productMapper.toResponse(product);
    }

    // ── API 4: GET /api/products ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<ProductListItemResponse>> getAllProducts(
            String keyword, Boolean isActive, Pageable pageable) {

        Specification<Product> spec = Specification
                .where(ProductSpecification.hasKeyword(keyword))
                .and(ProductSpecification.hasActiveStatus(isActive));

        Page<Product> page = productRepository.findAll(spec, pageable);

        List<ProductListItemResponse> items = page.getContent().stream()
                .map(productMapper::toListItem)
                .toList();

        return ApiResponse.<List<ProductListItemResponse>>builder()
                .success(true)
                .data(items)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    // ── API 5: PUT /api/products/{id} ─────────────────────────────────────────

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = findOrThrow(id);

        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_IMMUTABLE,
                    "SKU cannot be changed", HttpStatus.BAD_REQUEST);
        }

        product.setProductName(request.getProductName());
        product.setWeightKg(request.getWeightKg());
        product.setLengthCm(request.getLengthCm());
        product.setWidthCm(request.getWidthCm());
        product.setHeightCm(request.getHeightCm());
        product.setVolumeM3(productMapper.calculateVolume(
                request.getLengthCm(), request.getWidthCm(), request.getHeightCm()));

        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    // ── API 6: PATCH /api/products/{id}/status ────────────────────────────────

    @Override
    @Transactional
    public ProductResponse updateProductStatus(Long id, ProductStatusUpdateRequest request) {
        Product product = findOrThrow(id);

        // Order-in-use check deferred to Sprint 3 (US-08) when Order entity is available.

        product.setIsActive(request.getIsActive());
        Product saved = productRepository.save(product);
        return productMapper.toResponse(saved);
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Product findOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND,
                        "Product not found with id: " + id, HttpStatus.NOT_FOUND));
    }
}

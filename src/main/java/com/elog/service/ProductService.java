package com.elog.service;

import com.elog.dto.request.product.ProductCreateRequest;
import com.elog.dto.request.product.ProductStatusUpdateRequest;
import com.elog.dto.request.product.ProductUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.product.ProductListItemResponse;
import com.elog.dto.response.product.ProductResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductCreateRequest request);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    ApiResponse<List<ProductListItemResponse>> getAllProducts(String keyword, Boolean isActive, Pageable pageable);

    ProductResponse updateProduct(Long id, ProductUpdateRequest request);

    ProductResponse updateProductStatus(Long id, ProductStatusUpdateRequest request);
}

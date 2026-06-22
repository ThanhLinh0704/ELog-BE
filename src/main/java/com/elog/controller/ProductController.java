package com.elog.controller;

import com.elog.dto.request.ProductCreateRequest;
import com.elog.dto.request.ProductStatusUpdateRequest;
import com.elog.dto.request.ProductUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ProductListItemResponse;
import com.elog.dto.response.ProductResponse;
import com.elog.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Tag(name = "Products", description = "Product catalog management APIs")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Create a new product")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        ProductResponse response = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Product created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product detail by ID")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DISPATCHER', 'LOGISTICS_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/by-sku/{sku}")
    @Operation(summary = "Look up product by SKU — returns 200 even if inactive (caller checks isActive)")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DISPATCHER', 'LOGISTICS_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductBySku(@PathVariable String sku) {
        ProductResponse response = productService.getProductBySku(sku);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get paginated and filtered product list")
    @PreAuthorize("hasAnyRole('SYSTEM_ADMIN', 'DISPATCHER', 'LOGISTICS_MANAGER', 'WAREHOUSE_STAFF')")
    public ResponseEntity<ApiResponse<List<ProductListItemResponse>>> getAllProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        ApiResponse<List<ProductListItemResponse>> response =
                productService.getAllProducts(keyword, isActive, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update product information (SKU is immutable)")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Product updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a product")
    @PreAuthorize("hasRole('SYSTEM_ADMIN')")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProductStatus(
            @PathVariable Long id,
            @Valid @RequestBody ProductStatusUpdateRequest request) {
        ProductResponse response = productService.updateProductStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Product status updated"));
    }
}

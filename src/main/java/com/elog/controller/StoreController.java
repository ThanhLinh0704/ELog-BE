package com.elog.controller;

import com.elog.dto.request.*;
import com.elog.dto.response.*;
import com.elog.service.StoreService;
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
@RequestMapping("/api/v1/stores")
@RequiredArgsConstructor
@Tag(name = "Stores", description = "Store management APIs")
public class StoreController {

    private final StoreService storeService;

    @PostMapping
    @Operation(summary = "Create a new store")
    @PreAuthorize("hasAuthority('store:write')")
    public ResponseEntity<ApiResponse<StoreResponse>> createStore(
            @Valid @RequestBody StoreCreateRequest request) {
        StoreResponse response = storeService.createStore(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Store created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get store detail by ID")
    @PreAuthorize("hasAnyAuthority('store:read', 'store:write')")
    public ResponseEntity<ApiResponse<StoreResponse>> getStoreById(@PathVariable Long id) {
        StoreResponse response = storeService.getStoreById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get paginated and filtered list of stores")
    @PreAuthorize("hasAnyAuthority('store:read', 'store:write')")
    public ResponseEntity<ApiResponse<List<StoreListItemResponse>>> getAllStores(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Boolean hasRoute,
            @RequestParam(required = false) String routeCode,
            @PageableDefault(size = 20) Pageable pageable) {
        ApiResponse<List<StoreListItemResponse>> response =
                storeService.getAllStores(keyword, isActive, hasRoute, routeCode, pageable);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update store information")
    @PreAuthorize("hasAuthority('store:write')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStore(
            @PathVariable Long id,
            @Valid @RequestBody StoreUpdateRequest request) {
        StoreResponse response = storeService.updateStore(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Store updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a store")
    @PreAuthorize("hasAuthority('store:write')")
    public ResponseEntity<ApiResponse<StoreResponse>> updateStoreStatus(
            @PathVariable Long id,
            @Valid @RequestBody StoreStatusUpdateRequest request) {
        StoreResponse response = storeService.updateStoreStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Store status updated"));
    }
}

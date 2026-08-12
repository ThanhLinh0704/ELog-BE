package com.elog.service;

import com.elog.dto.request.store.StoreCreateRequest;
import com.elog.dto.request.store.StoreStatusUpdateRequest;
import com.elog.dto.request.store.StoreUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.store.StoreListItemResponse;
import com.elog.dto.response.store.StoreResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StoreService {
    StoreResponse createStore(StoreCreateRequest request);
    StoreResponse getStoreById(Long id);
    ApiResponse<List<StoreListItemResponse>> getAllStores(String keyword, Boolean isActive, Boolean hasRoute, String routeCode, Pageable pageable);
    StoreResponse updateStore(Long id, StoreUpdateRequest request);
    StoreResponse updateStoreStatus(Long id, StoreStatusUpdateRequest request);
}

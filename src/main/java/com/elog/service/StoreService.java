package com.elog.service;

import com.elog.dto.request.*;
import com.elog.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StoreService {
    StoreResponse createStore(StoreCreateRequest request);
    StoreResponse getStoreById(Long id);
    ApiResponse<List<StoreListItemResponse>> getAllStores(String keyword, Boolean isActive, Boolean hasRoute, Pageable pageable);
    StoreResponse updateStore(Long id, StoreUpdateRequest request);
    StoreResponse updateStoreStatus(Long id, StoreStatusUpdateRequest request);
}

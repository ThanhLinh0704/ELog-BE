package com.elog.mapper;

import com.elog.dto.AssignedRouteDto;
import com.elog.dto.StoreCreateRequest;
import com.elog.dto.StoreListItemResponse;
import com.elog.dto.StoreResponse;
import com.elog.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public Store toEntity(StoreCreateRequest request) {
        return Store.builder()
                .code(request.getStoreCode())
                .name(request.getStoreName())
                .address(request.getAddress())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(true)
                .build();
    }

    public StoreResponse toResponse(Store store, AssignedRouteDto assignedRoute) {
        return StoreResponse.builder()
                .id(store.getId())
                .storeCode(store.getCode())
                .storeName(store.getName())
                .address(store.getAddress())
                .contactName(store.getContactName())
                .contactPhone(store.getContactPhone())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .isActive(store.getIsActive())
                .assignedRoute(assignedRoute)
                .createdAt(store.getCreatedAt())
                .build();
    }

    public StoreListItemResponse toListItem(Store store, AssignedRouteDto assignedRoute) {
        return StoreListItemResponse.builder()
                .id(store.getId())
                .storeCode(store.getCode())
                .storeName(store.getName())
                .address(store.getAddress())
                .isActive(store.getIsActive())
                .hasCoordinates(store.getLatitude() != null && store.getLongitude() != null)
                .assignedRoute(assignedRoute)
                .build();
    }
}

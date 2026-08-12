package com.elog.mapper;

import com.elog.dto.request.store.StoreCreateRequest;
import com.elog.dto.response.route.AssignedRouteDto;
import com.elog.dto.response.store.StoreListItemResponse;
import com.elog.dto.response.store.StoreResponse;
import com.elog.entity.Store;
import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public Store toEntity(StoreCreateRequest request) {
        return Store.builder()
                .code(request.getStoreCode())
                .name(request.getStoreName())
                .addressDetail(request.getAddressDetail())
                .contactName(request.getContactName())
                .contactPhone(request.getContactPhone())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .isActive(true)
                .allowedDeliveryHours(request.getAllowedDeliveryHours() != null ? request.getAllowedDeliveryHours() : "All")
                .maxAllowedVehicleWeight(request.getMaxAllowedVehicleWeight())
                .imageUrl(request.getImageUrl())
                .build();
    }

    public StoreResponse toResponse(Store store, AssignedRouteDto assignedRoute, java.util.List<AssignedRouteDto> assignedRoutes) {
        return StoreResponse.builder()
                .id(store.getId())
                .storeCode(store.getCode())
                .storeName(store.getName())
                .address(formatFullAddress(store))
                .provinceCode(store.getProvince() != null ? store.getProvince().getCode() : null)
                .provinceName(store.getProvince() != null ? store.getProvince().getFullName() : null)
                .districtCode(store.getDistrict() != null ? store.getDistrict().getCode() : null)
                .districtName(store.getDistrict() != null ? store.getDistrict().getFullName() : null)
                .wardCode(store.getWard() != null ? store.getWard().getCode() : null)
                .wardName(store.getWard() != null ? store.getWard().getFullName() : null)
                .addressDetail(store.getAddressDetail())
                .contactName(store.getContactName())
                .contactPhone(store.getContactPhone())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .isActive(store.getIsActive())
                .assignedRoute(assignedRoute)
                .assignedRoutes(assignedRoutes)
                .allowedDeliveryHours(store.getAllowedDeliveryHours())
                .maxAllowedVehicleWeight(store.getMaxAllowedVehicleWeight())
                .imageUrl(store.getImageUrl())
                .createdAt(store.getCreatedAt())
                .build();
    }

    public StoreResponse toResponse(Store store, AssignedRouteDto assignedRoute) {
        return toResponse(store, assignedRoute, java.util.Collections.emptyList());
    }

    public StoreListItemResponse toListItem(Store store, AssignedRouteDto assignedRoute, java.util.List<AssignedRouteDto> assignedRoutes) {
        return StoreListItemResponse.builder()
                .id(store.getId())
                .storeCode(store.getCode())
                .storeName(store.getName())
                .address(formatFullAddress(store))
                .isActive(store.getIsActive())
                .hasCoordinates(store.getLatitude() != null && store.getLongitude() != null)
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .assignedRoute(assignedRoute)
                .assignedRoutes(assignedRoutes)
                .allowedDeliveryHours(store.getAllowedDeliveryHours())
                .maxAllowedVehicleWeight(store.getMaxAllowedVehicleWeight())
                .imageUrl(store.getImageUrl())
                .build();
    }

    public StoreListItemResponse toListItem(Store store, AssignedRouteDto assignedRoute) {
        return toListItem(store, assignedRoute, java.util.Collections.emptyList());
    }

    private String formatFullAddress(Store store) {
        if (store == null) return null;
        StringBuilder sb = new StringBuilder();
        if (store.getAddressDetail() != null && !store.getAddressDetail().isEmpty()) {
            sb.append(store.getAddressDetail());
        }
        if (store.getWard() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(store.getWard().getFullName());
        }
        if (store.getDistrict() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(store.getDistrict().getFullName());
        }
        if (store.getProvince() != null) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(store.getProvince().getFullName());
        }
        return sb.toString();
    }
}

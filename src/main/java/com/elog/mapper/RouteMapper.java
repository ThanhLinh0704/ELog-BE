package com.elog.mapper;

import com.elog.dto.response.*;
import com.elog.entity.Route;
import com.elog.entity.RouteStop;
import com.elog.entity.Store;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RouteMapper {

    public RouteResponse toResponse(Route route, int stopCount) {
        return RouteResponse.builder()
                .id(route.getId())
                .code(route.getCode())
                .name(route.getName())
                .description(route.getDescription())
                .isActive(route.getIsActive())
                .stopCount(stopCount)
                .createdAt(route.getCreatedAt())
                .build();
    }

    public RouteDetailResponse toDetailResponse(Route route, List<RouteStop> stops) {
        int warningCount = 0;
        List<RouteStopResponse> stopResponses = new java.util.ArrayList<>();

        for (RouteStop rs : stops) {
            boolean hasCoords = rs.getStore().getLatitude() != null
                    && rs.getStore().getLongitude() != null;
            if (!hasCoords) warningCount++;

            stopResponses.add(toStopResponse(rs, !hasCoords));
        }

        return RouteDetailResponse.builder()
                .id(route.getId())
                .code(route.getCode())
                .name(route.getName())
                .description(route.getDescription())
                .isActive(route.getIsActive())
                .stopCount(stops.size())
                .coordinatesWarningCount(warningCount)
                .stops(stopResponses)
                .createdAt(route.getCreatedAt())
                .build();
    }

    public RouteStopResponse toStopResponse(RouteStop rs, boolean coordinatesWarning) {
        Store store = rs.getStore();
        boolean hasCoords = store.getLatitude() != null && store.getLongitude() != null;

        return RouteStopResponse.builder()
                .id(rs.getId())
                .routeId(rs.getRoute().getId())
                .sequenceOrder(rs.getSequenceOrder())
                .store(RouteStopStoreDto.builder()
                        .id(store.getId())
                        .storeCode(store.getCode())
                        .storeName(store.getName())
                        .address(formatFullAddress(store))
                        .contactName(store.getContactName())
                        .contactPhone(store.getContactPhone())
                        .hasCoordinates(hasCoords)
                        .build())
                .coordinatesWarning(coordinatesWarning)
                .build();
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

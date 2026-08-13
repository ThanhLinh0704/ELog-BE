package com.elog.service;

import com.elog.dto.request.*;
import com.elog.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RouteService {
    RouteResponse createRoute(RouteCreateRequest request);
    RouteDetailResponse getRouteById(Long id);
    ApiResponse<List<RouteResponse>> getAllRoutes(String keyword, Boolean isActive, Pageable pageable);
    RouteResponse updateRoute(Long id, RouteUpdateRequest request);
    RouteResponse updateRouteStatus(Long id, RouteStatusUpdateRequest request);

    // Stop management
    RouteStopResponse addStop(Long routeId, RouteStopAddRequest request);
    List<RouteStopResponse> reorderStops(Long routeId, RouteStopReorderRequest request);
    void removeStop(Long routeId, Long stopId);

    // Road directions
    RouteDirectionsResponse getRouteDirections(Long routeId);
}

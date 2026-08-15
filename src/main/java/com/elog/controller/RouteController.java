package com.elog.controller;

import com.elog.dto.request.route.RouteCreateRequest;
import com.elog.dto.request.route.RouteStatusUpdateRequest;
import com.elog.dto.request.route.RouteStopAddRequest;
import com.elog.dto.request.route.RouteStopReorderRequest;
import com.elog.dto.request.route.RouteUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.route.RouteDetailResponse;
import com.elog.dto.response.route.RouteDirectionsResponse;
import com.elog.dto.response.route.RouteResponse;
import com.elog.dto.response.route.RouteStopResponse;
import com.elog.service.RouteService;
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
@RequestMapping("/api/v1/routes")
@RequiredArgsConstructor
@Tag(name = "Routes", description = "Route management APIs")
public class RouteController {

    private final RouteService routeService;

    // ── Route Header ────────────────────────────────────────────

    @PostMapping
    @Operation(summary = "Create a new route")
    @PreAuthorize("hasAuthority('route:write')")
    public ResponseEntity<ApiResponse<RouteResponse>> createRoute(
            @Valid @RequestBody RouteCreateRequest request) {
        RouteResponse response = routeService.createRoute(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Route created successfully"));
    }

    @GetMapping
    @Operation(summary = "Get paginated and filtered list of routes")
    @PreAuthorize("hasAnyAuthority('route:read', 'route:write')")
    public ResponseEntity<ApiResponse<List<RouteResponse>>> getAllRoutes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {
        ApiResponse<List<RouteResponse>> response =
                routeService.getAllRoutes(keyword, isActive, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get route detail with stops")
    @PreAuthorize("hasAnyAuthority('route:read', 'route:write')")
    public ResponseEntity<ApiResponse<RouteDetailResponse>> getRouteById(@PathVariable Long id) {
        RouteDetailResponse response = routeService.getRouteById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update route name and description")
    @PreAuthorize("hasAuthority('route:write')")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRoute(
            @PathVariable Long id,
            @Valid @RequestBody RouteUpdateRequest request) {
        RouteResponse response = routeService.updateRoute(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Route updated successfully"));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate or deactivate a route")
    @PreAuthorize("hasAuthority('route:write')")
    public ResponseEntity<ApiResponse<RouteResponse>> updateRouteStatus(
            @PathVariable Long id,
            @Valid @RequestBody RouteStatusUpdateRequest request) {
        RouteResponse response = routeService.updateRouteStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Route status updated"));
    }

    // ── Route Stops ─────────────────────────────────────────────

    @PostMapping("/{id}/stops")
    @Operation(summary = "Add a stop to the route")
    @PreAuthorize("hasAuthority('route:write')")
    public ResponseEntity<ApiResponse<RouteStopResponse>> addStop(
            @PathVariable Long id,
            @RequestBody RouteStopAddRequest request) {
        RouteStopResponse response = routeService.addStop(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Stop added successfully"));
    }

    @PutMapping("/{id}/stops/reorder")
    @Operation(summary = "Reorder all stops in the route")
    @PreAuthorize("hasAuthority('route:write')")
    public ResponseEntity<ApiResponse<List<RouteStopResponse>>> reorderStops(
            @PathVariable Long id,
            @Valid @RequestBody RouteStopReorderRequest request) {
        List<RouteStopResponse> response = routeService.reorderStops(id, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Stops reordered successfully"));
    }

    @DeleteMapping("/{id}/stops/{stopId}")
    @Operation(summary = "Remove a stop from the route")
    @PreAuthorize("hasAuthority('route:write')")
    public ResponseEntity<ApiResponse<Void>> removeStop(
            @PathVariable Long id,
            @PathVariable Long stopId) {
        routeService.removeStop(id, stopId);
        return ResponseEntity.ok(ApiResponse.success(null, "Stop removed successfully"));
    }

    @GetMapping("/{id}/directions")
    @Operation(summary = "Get route road directions polyline and warehouse location")
    @PreAuthorize("hasAuthority('route:read')")
    public ResponseEntity<ApiResponse<RouteDirectionsResponse>> getRouteDirections(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") Boolean forceRefresh) {
        RouteDirectionsResponse response = routeService.getRouteDirections(id, Boolean.TRUE.equals(forceRefresh));
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

package com.elog.controller;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.order.OrderResponse;
import com.elog.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Orders", description = "Order management & search APIs")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Search, filter, and paginate system-wide orders")
    @PreAuthorize("hasAnyAuthority('order:import', 'trip:read')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> searchOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long routeId,
            @RequestParam(required = false) String routeCode,
            @RequestParam(required = false) Long batchId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        ApiResponse<List<OrderResponse>> response = orderService.searchOrders(
                deliveryDate, status, routeId, routeCode, batchId, search, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get order detail by ID")
    @PreAuthorize("hasAnyAuthority('order:import', 'trip:read')")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable Long id) {
        OrderResponse response = orderService.getOrderById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}

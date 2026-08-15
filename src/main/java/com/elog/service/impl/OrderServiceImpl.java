package com.elog.service.impl;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.order.OrderResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.OrderRepository;
import com.elog.repository.RouteStopRepository;
import com.elog.repository.specification.OrderSpecification;
import com.elog.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final RouteStopRepository routeStopRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<OrderResponse>> searchOrders(
            LocalDate deliveryDate,
            String status,
            Long routeId,
            String routeCode,
            Long batchId,
            String search,
            Pageable pageable) {

        Specification<Order> spec = OrderSpecification.filterOrders(
                deliveryDate, status, routeId, routeCode, batchId, search);

        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        // Batch resolve route stops for stores to avoid N+1 query problem
        List<Order> orders = orderPage.getContent();
        List<Long> storeIds = orders.stream()
                .map(o -> o.getStore() != null ? o.getStore().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<Long, Route> storeRouteMap = new HashMap<>();
        if (!storeIds.isEmpty()) {
            List<RouteStop> routeStops = routeStopRepository.findByStoreIdIn(storeIds);
            for (RouteStop rs : routeStops) {
                if (rs.getStore() != null && rs.getRoute() != null) {
                    storeRouteMap.putIfAbsent(rs.getStore().getId(), rs.getRoute());
                }
            }
        }

        List<OrderResponse> responseList = orders.stream()
                .map(o -> toOrderResponse(o, storeRouteMap))
                .toList();

        return ApiResponse.<List<OrderResponse>>builder()
                .success(true)
                .data(responseList)
                .pagination(ApiResponse.PaginationInfo.builder()
                        .page(orderPage.getNumber())
                        .size(orderPage.getSize())
                        .totalElements(orderPage.getTotalElements())
                        .totalPages(orderPage.getTotalPages())
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "Order not found with id: " + id, HttpStatus.NOT_FOUND));

        Map<Long, Route> storeRouteMap = new HashMap<>();
        if (order.getStore() != null) {
            routeStopRepository.findFirstByStoreId(order.getStore().getId())
                    .ifPresent(rs -> storeRouteMap.put(order.getStore().getId(), rs.getRoute()));
        }

        return toOrderResponse(order, storeRouteMap);
    }

    private OrderResponse toOrderResponse(Order order, Map<Long, Route> storeRouteMap) {
        // Resolve Route: 1. TripDraft Route, 2. Store Route fallback
        Route route = null;
        if (order.getTripDraft() != null && order.getTripDraft().getRoute() != null) {
            route = order.getTripDraft().getRoute();
        } else if (order.getStore() != null) {
            route = storeRouteMap.get(order.getStore().getId());
        }

        OrderResponse.RouteSummaryDto routeDto = null;
        if (route != null) {
            routeDto = OrderResponse.RouteSummaryDto.builder()
                    .id(route.getId())
                    .code(route.getCode())
                    .name(route.getName())
                    .build();
        }

        // Map Store
        OrderResponse.StoreSummaryDto storeDto = null;
        if (order.getStore() != null) {
            Store s = order.getStore();
            storeDto = OrderResponse.StoreSummaryDto.builder()
                    .id(s.getId())
                    .code(s.getCode())
                    .name(s.getName())
                    .address(s.getAddressDetail())
                    .provinceCode(s.getProvince() != null ? s.getProvince().getCode() : null)
                    .districtCode(s.getDistrict() != null ? s.getDistrict().getCode() : null)
                    .wardCode(s.getWard() != null ? s.getWard().getCode() : null)
                    .build();
        }

        // Map Order Items & calculate totals
        int totalItems = 0;
        int totalQuantity = 0;
        BigDecimal totalWeightKg = BigDecimal.ZERO;
        BigDecimal totalVolumeM3 = BigDecimal.ZERO;

        List<OrderResponse.OrderItemDetailDto> itemDtos = new ArrayList<>();
        if (order.getItems() != null) {
            totalItems = order.getItems().size();
            for (OrderItem item : order.getItems()) {
                int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                totalQuantity += qty;

                BigDecimal lineW = item.getLineWeightKg() != null ? item.getLineWeightKg() : BigDecimal.ZERO;
                BigDecimal lineV = item.getLineVolumeM3() != null ? item.getLineVolumeM3() : BigDecimal.ZERO;
                totalWeightKg = totalWeightKg.add(lineW);
                totalVolumeM3 = totalVolumeM3.add(lineV);

                itemDtos.add(OrderResponse.OrderItemDetailDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct() != null ? item.getProduct().getId() : null)
                        .sku(item.getSku())
                        .productName(item.getProduct() != null ? item.getProduct().getProductName() : null)
                        .quantity(qty)
                        .unitWeightKg(item.getUnitWeightKg())
                        .unitVolumeM3(item.getUnitVolumeM3())
                        .lineWeightKg(lineW)
                        .lineVolumeM3(lineV)
                        .build());
            }
        }

        return OrderResponse.builder()
                .id(order.getId())
                .batchId(order.getImportBatch() != null ? order.getImportBatch().getId() : null)
                .orderRef(order.getOrderRef())
                .deliveryDate(order.getDeliveryDate())
                .status(order.getStatus())
                .route(routeDto)
                .store(storeDto)
                .recipientName(order.getRecipientName())
                .recipientPhone(order.getRecipientPhone())
                .deliveryTimeWindow(order.getDeliveryTimeWindow())
                .notes(order.getNotes())
                .totalItems(totalItems)
                .totalQuantity(totalQuantity)
                .totalWeightKg(totalWeightKg)
                .totalVolumeM3(totalVolumeM3)
                .items(itemDtos)
                .tripDraftId(order.getTripDraft() != null ? order.getTripDraft().getId() : null)
                .createdAt(order.getCreatedAt())
                .build();
    }
}

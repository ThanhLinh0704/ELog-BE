package com.elog.service;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.order.OrderResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {

    ApiResponse<List<OrderResponse>> searchOrders(
            LocalDate deliveryDate,
            String status,
            Long routeId,
            String routeCode,
            Long batchId,
            String search,
            Pageable pageable);

    OrderResponse getOrderById(Long id);
}

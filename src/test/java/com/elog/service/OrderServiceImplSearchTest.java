package com.elog.service;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.order.OrderResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.repository.OrderRepository;
import com.elog.repository.RouteStopRepository;
import com.elog.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplSearchTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RouteStopRepository routeStopRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private Order order1;
    private Store store1;
    private Route route1;

    @BeforeEach
    void setUp() {
        store1 = Store.builder()
                .id(1L)
                .code("ST-BT-001")
                .name("Store Binh Thanh")
                .addressDetail("123 XVNT")
                .build();

        route1 = Route.builder()
                .id(10L)
                .code("ROUTE-01")
                .name("Route Binh Thanh")
                .build();

        order1 = Order.builder()
                .id(100L)
                .orderRef("DH-001")
                .deliveryDate(LocalDate.now().plusDays(1))
                .status("ACCEPTED")
                .store(store1)
                .importBatch(ImportBatch.builder().id(1L).isActive(true).build())
                .recipientName("Nguyen A")
                .recipientPhone("0901234567")
                .items(new ArrayList<>())
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void searchOrders_success_returnsPaginatedResponse() {
        Page<Order> page = new PageImpl<>(List.of(order1), PageRequest.of(0, 10), 1);
        when(orderRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        RouteStop rs = RouteStop.builder().store(store1).route(route1).build();
        when(routeStopRepository.findByStoreIdIn(anyList())).thenReturn(List.of(rs));

        ApiResponse<List<OrderResponse>> response = orderService.searchOrders(
                LocalDate.now().plusDays(1), "ACCEPTED", null, null, null, "DH-001", PageRequest.of(0, 10));

        assertThat(response).isNotNull();
        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getOrderRef()).isEqualTo("DH-001");
        assertThat(response.getData().get(0).getRoute()).isNotNull();
        assertThat(response.getData().get(0).getRoute().getCode()).isEqualTo("ROUTE-01");
        assertThat(response.getPagination().getTotalElements()).isEqualTo(1);
    }

    @Test
    void getOrderById_found_returnsOrderResponse() {
        when(orderRepository.findById(100L)).thenReturn(Optional.of(order1));
        RouteStop rs = RouteStop.builder().store(store1).route(route1).build();
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(rs));

        OrderResponse response = orderService.getOrderById(100L);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getRoute().getCode()).isEqualTo("ROUTE-01");
    }

    @Test
    void getOrderById_notFound_throwsException() {
        when(orderRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Order not found with id: 999");
    }
}

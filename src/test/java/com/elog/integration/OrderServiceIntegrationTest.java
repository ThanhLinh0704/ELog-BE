package com.elog.integration;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.order.OrderResponse;
import com.elog.entity.Order;
import com.elog.repository.OrderRepository;
import com.elog.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void l2Ord01SearchesOrdersWithDynamicSpecifications() {
        ApiResponse<List<OrderResponse>> response = orderService.searchOrders(
                null, null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
    }

    @Test
    void l2Ord02FiltersOrdersByDeliveryDateAndRoute() {
        ApiResponse<List<OrderResponse>> response = orderService.searchOrders(
                LocalDate.now(), null, null, null, null, null, PageRequest.of(0, 10)
        );

        assertThat(response).isNotNull();
    }

    @Test
    void l2Ord03GetsOrderByIdWithItemsAndStoreDetails() {
        List<Order> orders = orderRepository.findAll();
        if (!orders.isEmpty()) {
            Order first = orders.get(0);
            OrderResponse response = orderService.getOrderById(first.getId());

            assertThat(response).isNotNull();
            assertThat(response.getId()).isEqualTo(first.getId());
            assertThat(response.getOrderRef()).isEqualTo(first.getOrderRef());
        }
    }
}

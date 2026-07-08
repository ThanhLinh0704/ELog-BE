package com.elog.service;

import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.repository.*;
import com.elog.service.impl.TripDraftServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripDraftServiceImplTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private RouteStopRepository routeStopRepository;
    @Mock
    private TripDraftRepository tripDraftRepository;
    @Mock
    private TripDraftStopRepository tripDraftStopRepository;
    @Mock
    private RouteRepository routeRepository;

    @InjectMocks
    private TripDraftServiceImpl tripDraftService;

    private LocalDate deliveryDate;

    @BeforeEach
    void setUp() {
        deliveryDate = LocalDate.of(2026, 3, 16);
    }

    @Test
    void consolidate_emptyOrders_returnsEmptyResponse() {
        when(orderRepository.findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED"))
                .thenReturn(Collections.emptyList());

        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        assertThat(response.getDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(response.getTripDraftsCreatedOrUpdated()).isZero();
        assertThat(response.getTripDrafts()).isEmpty();
        assertThat(response.getSkippedRoutes()).isEmpty();

        verify(orderRepository).findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED");
        verifyNoInteractions(routeStopRepository, tripDraftRepository, tripDraftStopRepository, routeRepository);
    }

    @Test
    void consolidate_unmappedOrders_skipped() {
        Store store = Store.builder().id(1L).code("ST-001").build();
        Order order = Order.builder().id(100L).store(store).deliveryDate(deliveryDate).build();
        List<Order> orders = List.of(order);

        when(orderRepository.findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED"))
                .thenReturn(orders);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.empty());

        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        assertThat(response.getDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(response.getTripDraftsCreatedOrUpdated()).isZero();
        assertThat(response.getTripDrafts()).isEmpty();
        assertThat(response.getSkippedRoutes()).isEmpty();

        verify(routeStopRepository).findFirstByStoreId(1L);
        verifyNoInteractions(routeRepository, tripDraftRepository, tripDraftStopRepository);
    }

    @Test
    void consolidate_routeWithoutRouteStops_skippedRoute() {
        Store store = Store.builder().id(1L).code("ST-001").build();
        Order order = Order.builder().id(100L).store(store).deliveryDate(deliveryDate).build();
        List<Order> orders = List.of(order);

        Route route = Route.builder().id(10L).code("RT-001").build();
        RouteStop routeStop = RouteStop.builder().id(1L).route(route).store(store).sequenceOrder(1).build();

        when(orderRepository.findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED"))
                .thenReturn(orders);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(routeStop));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(10L))
                .thenReturn(Collections.emptyList());

        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        assertThat(response.getDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(response.getTripDraftsCreatedOrUpdated()).isZero();
        assertThat(response.getSkippedRoutes()).hasSize(1);
        assertThat(response.getSkippedRoutes().get(0).getRouteCode()).isEqualTo("RT-001");
        assertThat(response.getSkippedRoutes().get(0).getReason()).contains("no RouteStop defined");

        verify(routeStopRepository).findByRouteIdOrderBySequenceOrderAsc(10L);
        verifyNoInteractions(tripDraftRepository, tripDraftStopRepository);
    }

    @Test
    void consolidate_lockedTripDraft_throwsConflict() {
        Store store = Store.builder().id(1L).code("ST-001").build();
        Order order = Order.builder().id(100L).store(store).deliveryDate(deliveryDate).build();
        List<Order> orders = List.of(order);

        Route route = Route.builder().id(10L).code("RT-001").build();
        RouteStop routeStop = RouteStop.builder().id(1L).route(route).store(store).sequenceOrder(1).build();

        TripDraft existingDraft = TripDraft.builder().id(50L).route(route).deliveryDate(deliveryDate).status("PLANNED").build();

        when(orderRepository.findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED"))
                .thenReturn(orders);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(routeStop));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(10L))
                .thenReturn(List.of(routeStop));
        when(tripDraftRepository.findByRouteIdAndDeliveryDate(10L, deliveryDate))
                .thenReturn(Optional.of(existingDraft));

        assertThatThrownBy(() -> tripDraftService.consolidate(deliveryDate))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT)
                .hasMessageContaining("đã được xác nhận");

        verify(tripDraftRepository, never()).save(any());
        verifyNoInteractions(tripDraftStopRepository);
    }

    @Test
    void consolidate_success() {
        Store store1 = Store.builder().id(1L).code("ST-001").name("Store 1").build();
        Store store2 = Store.builder().id(2L).code("ST-002").name("Store 2").build();

        OrderItem item1 = OrderItem.builder()
                .lineVolumeM3(BigDecimal.valueOf(1.5))
                .lineWeightKg(BigDecimal.valueOf(100))
                .build();
        OrderItem item2 = OrderItem.builder()
                .lineVolumeM3(BigDecimal.valueOf(0.5))
                .lineWeightKg(BigDecimal.valueOf(50))
                .build();

        Order order = Order.builder()
                .id(100L)
                .store(store1)
                .deliveryDate(deliveryDate)
                .items(List.of(item1, item2))
                .build();

        Route route = Route.builder().id(10L).code("RT-001").build();
        RouteStop rs1 = RouteStop.builder().id(1L).route(route).store(store1).sequenceOrder(1).build();
        RouteStop rs2 = RouteStop.builder().id(2L).route(route).store(store2).sequenceOrder(2).build();

        when(orderRepository.findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED"))
                .thenReturn(List.of(order));
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(rs1));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(10L))
                .thenReturn(List.of(rs1, rs2));
        when(tripDraftRepository.findByRouteIdAndDeliveryDate(10L, deliveryDate))
                .thenReturn(Optional.empty());

        TripDraft savedDraft = TripDraft.builder()
                .id(50L)
                .route(route)
                .deliveryDate(deliveryDate)
                .totalVolumeM3(BigDecimal.valueOf(2.0))
                .totalWeightKg(BigDecimal.valueOf(150))
                .activeStopCount(1)
                .skippedStopCount(1)
                .status("DRAFT")
                .build();

        when(tripDraftRepository.save(any(TripDraft.class))).thenAnswer(invocation -> {
            TripDraft td = invocation.getArgument(0);
            td.setId(50L);
            return td;
        });

        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        assertThat(response.getDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(response.getTripDraftsCreatedOrUpdated()).isEqualTo(1);
        assertThat(response.getTripDrafts()).hasSize(1);
        
        TripDraftResponse draftResponse = response.getTripDrafts().get(0);
        assertThat(draftResponse.getId()).isEqualTo(50L);
        assertThat(draftResponse.getRouteCode()).isEqualTo("RT-001");
        assertThat(draftResponse.getTotalVolumeM3()).isEqualByComparingTo(BigDecimal.valueOf(2.0));
        assertThat(draftResponse.getTotalWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(150));
        assertThat(draftResponse.getActiveStopCount()).isEqualTo(1);
        assertThat(draftResponse.getSkippedStopCount()).isEqualTo(1);

        verify(tripDraftRepository).save(any(TripDraft.class));
        verify(tripDraftStopRepository).deleteByTripDraftId(50L);
        verify(tripDraftStopRepository, times(2)).save(any(TripDraftStop.class));
        verify(orderRepository).updateTripDraftId(List.of(100L), 50L);
    }

    @Test
    void consolidate_routeNotFound_skipsRoute() {
        Store store = Store.builder().id(1L).code("ST-001").build();
        Order order = Order.builder().id(100L).store(store).deliveryDate(deliveryDate).build();
        List<Order> orders = List.of(order);

        Route route = Route.builder().id(10L).code("RT-001").build();
        RouteStop routeStop = RouteStop.builder().id(1L).route(route).store(store).sequenceOrder(1).build();

        when(orderRepository.findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED"))
                .thenReturn(orders);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(routeStop));
        when(routeRepository.findById(10L)).thenReturn(Optional.empty()); // Route not found

        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        assertThat(response.getDeliveryDate()).isEqualTo(deliveryDate);
        assertThat(response.getTripDraftsCreatedOrUpdated()).isZero();
        assertThat(response.getTripDrafts()).isEmpty();

        verify(routeRepository).findById(10L);
        verifyNoInteractions(tripDraftRepository, tripDraftStopRepository);
    }

    @Test
    void consolidate_existingDraftInDraftStatus_updatesDraft() {
        Store store1 = Store.builder().id(1L).code("ST-001").name("Store 1").build();

        OrderItem item1 = OrderItem.builder()
                .lineVolumeM3(BigDecimal.valueOf(1.5))
                .lineWeightKg(BigDecimal.valueOf(100))
                .build();

        Order order = Order.builder()
                .id(100L)
                .store(store1)
                .deliveryDate(deliveryDate)
                .items(List.of(item1))
                .build();

        Route route = Route.builder().id(10L).code("RT-001").build();
        RouteStop rs1 = RouteStop.builder().id(1L).route(route).store(store1).sequenceOrder(1).build();

        TripDraft existingDraft = TripDraft.builder()
                .id(50L)
                .route(route)
                .deliveryDate(deliveryDate)
                .status("DRAFT")
                .build();

        when(orderRepository.findByDeliveryDateAndStatus(deliveryDate, "ACCEPTED"))
                .thenReturn(List.of(order));
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(rs1));
        when(routeRepository.findById(10L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(10L))
                .thenReturn(List.of(rs1));
        when(tripDraftRepository.findByRouteIdAndDeliveryDate(10L, deliveryDate))
                .thenReturn(Optional.of(existingDraft));

        when(tripDraftRepository.save(any(TripDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        assertThat(response.getTripDraftsCreatedOrUpdated()).isEqualTo(1);
        TripDraftResponse draftResponse = response.getTripDrafts().get(0);
        assertThat(draftResponse.getId()).isEqualTo(50L);

        verify(tripDraftRepository).save(existingDraft);
    }

    @Test
    void getTripDrafts_success() {
        org.springframework.data.domain.Pageable pageable = mock(org.springframework.data.domain.Pageable.class);
        org.springframework.data.domain.Page<TripDraft> page = mock(org.springframework.data.domain.Page.class);
        
        Route route = Route.builder().id(10L).code("RT-001").build();
        TripDraft draft = TripDraft.builder()
                .id(50L)
                .route(route)
                .deliveryDate(deliveryDate)
                .totalVolumeM3(BigDecimal.valueOf(2.0))
                .totalWeightKg(BigDecimal.valueOf(150))
                .activeStopCount(1)
                .skippedStopCount(1)
                .status("DRAFT")
                .build();

        when(page.getContent()).thenReturn(List.of(draft));
        when(page.getNumber()).thenReturn(0);
        when(page.getSize()).thenReturn(10);
        when(page.getTotalElements()).thenReturn(1L);
        when(page.getTotalPages()).thenReturn(1);
        
        when(tripDraftRepository.findByDeliveryDate(deliveryDate, pageable)).thenReturn(page);

        ApiResponse<List<TripDraftResponse>> response = tripDraftService.getTripDrafts(deliveryDate, pageable);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).hasSize(1);
        assertThat(response.getData().get(0).getId()).isEqualTo(50L);
        assertThat(response.getPagination().getTotalElements()).isEqualTo(1L);
    }

    @Test
    void getTripDraftById_success() {
        Route route = Route.builder().id(10L).code("RT-001").build();
        Store store = Store.builder().id(1L).code("ST-001").name("Store 1").build();
        TripDraftStop stop = TripDraftStop.builder()
                .sequenceNo(1)
                .store(store)
                .isActive(true)
                .orderCount(1)
                .build();
        
        TripDraft draft = TripDraft.builder()
                .id(50L)
                .route(route)
                .deliveryDate(deliveryDate)
                .totalVolumeM3(BigDecimal.valueOf(2.0))
                .totalWeightKg(BigDecimal.valueOf(150))
                .activeStopCount(1)
                .skippedStopCount(1)
                .status("DRAFT")
                .stops(List.of(stop))
                .build();

        when(tripDraftRepository.findById(50L)).thenReturn(Optional.of(draft));

        TripDraftResponse response = tripDraftService.getTripDraftById(50L);

        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getStops()).hasSize(1);
        assertThat(response.getStops().get(0).getStoreCode()).isEqualTo("ST-001");
    }

    @Test
    void getTripDraftById_notFound_throwsNotFoundException() {
        when(tripDraftRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripDraftService.getTripDraftById(99L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND)
                .hasMessageContaining("Trip Draft not found");
    }
}

package com.elog.service;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.ConsolidateResponse;
import com.elog.dto.response.trip.StopOrderItemResponse;
import com.elog.dto.response.trip.TripDraftResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
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
    @Mock
    private OrderItemRepository orderItemRepository;
    @Mock
    private PlanningHistoryService planningHistoryService;
    @Mock
    private TripRepository tripRepository;
    @Mock
    private ManifestRepository manifestRepository;

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

        when(tripDraftRepository.saveAndFlush(any(TripDraft.class))).thenAnswer(invocation -> {
            TripDraft td = invocation.getArgument(0);
            td.setId(50L);
            return td;
        });
        when(tripDraftRepository.save(any(TripDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

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

        verify(tripDraftRepository).saveAndFlush(any(TripDraft.class));
        verify(tripDraftRepository).save(any(TripDraft.class));
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

        when(tripDraftRepository.saveAndFlush(any(TripDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tripDraftRepository.save(any(TripDraft.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsolidateResponse response = tripDraftService.consolidate(deliveryDate);

        assertThat(response.getTripDraftsCreatedOrUpdated()).isEqualTo(1);
        TripDraftResponse draftResponse = response.getTripDrafts().get(0);
        assertThat(draftResponse.getId()).isEqualTo(50L);

        verify(tripDraftRepository).save(existingDraft);
    }

    @Test
    @SuppressWarnings("unchecked")
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

    @Test
    void getStopOrderItems_success() {
        TripDraft draft = TripDraft.builder().id(50L).build();
        Store store = Store.builder().id(1L).code("ST-001").name("Store 1").build();
        TripDraftStop stop = TripDraftStop.builder()
                .id(100L)
                .tripDraft(draft)
                .store(store)
                .build();

        Product product = Product.builder().id(10L).sku("SKU-001").productName("TV LG").build();
        Order order = Order.builder().id(200L).orderRef("DH-001").build();
        OrderItem item = OrderItem.builder()
                .id(300L)
                .order(order)
                .product(product)
                .sku("SKU-001")
                .quantity(5)
                .lineWeightKg(BigDecimal.valueOf(150.0))
                .lineVolumeM3(BigDecimal.valueOf(1.2))
                .build();

        when(tripDraftStopRepository.findById(100L)).thenReturn(Optional.of(stop));
        when(orderItemRepository.findByStopForManifest(1L, 50L)).thenReturn(List.of(item));

        List<StopOrderItemResponse> response = tripDraftService.getStopOrderItems(50L, 100L);

        assertThat(response).hasSize(1);
        StopOrderItemResponse resItem = response.get(0);
        assertThat(resItem.getOrderId()).isEqualTo(200L);
        assertThat(resItem.getOrderRef()).isEqualTo("DH-001");
        assertThat(resItem.getSku()).isEqualTo("SKU-001");
        assertThat(resItem.getProductName()).isEqualTo("TV LG");
        assertThat(resItem.getQuantity()).isEqualTo(5);
        assertThat(resItem.getWeightKg()).isEqualByComparingTo(BigDecimal.valueOf(150.0));
        assertThat(resItem.getVolumeM3()).isEqualByComparingTo(BigDecimal.valueOf(1.2));
    }

    @Test
    void getStopOrderItems_stopNotFound_throwsNotFound() {
        when(tripDraftStopRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripDraftService.getStopOrderItems(50L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND)
                .hasMessageContaining("TripDraftStop not found");
    }

    @Test
    void getStopOrderItems_mismatchDraftId_throwsNotFound() {
        TripDraft draft = TripDraft.builder().id(99L).build(); // different draft ID
        TripDraftStop stop = TripDraftStop.builder()
                .id(100L)
                .tripDraft(draft)
                .build();

        when(tripDraftStopRepository.findById(100L)).thenReturn(Optional.of(stop));

        assertThatThrownBy(() -> tripDraftService.getStopOrderItems(50L, 100L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND)
                .hasMessageContaining("does not belong to Trip Draft");
    }

    @Test
    void getExcludedOrders_success() {
        TripDraft draft = TripDraft.builder().id(50L).deliveryDate(LocalDate.now()).build();
        Store store = Store.builder().id(10L).code("ST-001").build();
        TripDraftStop stop = TripDraftStop.builder().id(100L).store(store).tripDraft(draft).build();
        draft.setStops(List.of(stop));

        Product product = Product.builder().id(1L).sku("SKU-999").productName("Excluded Prod").build();
        Order excludedOrder = Order.builder().id(300L).orderRef("DH-EXCLUDED").deliveryDate(draft.getDeliveryDate()).store(store).status("UNASSIGNED").build();
        OrderItem item = OrderItem.builder().id(500L).order(excludedOrder).product(product).sku("SKU-999").quantity(2).lineWeightKg(BigDecimal.valueOf(20.0)).lineVolumeM3(BigDecimal.valueOf(0.5)).build();
        excludedOrder.setItems(List.of(item));

        when(tripDraftRepository.findById(50L)).thenReturn(Optional.of(draft));
        when(orderRepository.findExcludedOrdersByDeliveryDateAndStores(eq(draft.getDeliveryDate()), eq(List.of(10L))))
                .thenReturn(List.of(excludedOrder));

        List<StopOrderItemResponse> result = tripDraftService.getExcludedOrders(50L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(300L);
        assertThat(result.get(0).getOrderRef()).isEqualTo("DH-EXCLUDED");
        assertThat(result.get(0).getSku()).isEqualTo("SKU-999");
    }

    // ── revertToDraft — CANCELLED Trip must not block revert, other statuses must ────────────

    @Test
    void revertToDraft_success_whenNoTripEverExisted() {
        TripDraft draft = TripDraft.builder().id(60L).status("VALIDATED").build();

        when(tripDraftRepository.findById(60L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftIdAndStatusNot(60L, TripStatus.CANCELLED)).thenReturn(false);
        when(manifestRepository.findByTripDraftId(60L)).thenReturn(Optional.empty());

        tripDraftService.revertToDraft(60L, "dispatcher01");

        assertThat(draft.getStatus()).isEqualTo("DRAFT");
        verify(tripDraftRepository).save(draft);
    }

    @Test
    void revertToDraft_success_whenOnlyCancelledTripExists() {
        // Trip A đã bị huỷ — không được tính là "đang tồn tại" để chặn revert.
        TripDraft draft = TripDraft.builder().id(61L).status("VALIDATED").build();

        when(tripDraftRepository.findById(61L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftIdAndStatusNot(61L, TripStatus.CANCELLED)).thenReturn(false);
        when(manifestRepository.findByTripDraftId(61L)).thenReturn(Optional.empty());

        tripDraftService.revertToDraft(61L, "dispatcher01");

        assertThat(draft.getStatus()).isEqualTo("DRAFT");
        verify(tripDraftRepository).save(draft);
    }

    @Test
    void revertToDraft_throws_whenActiveTripExists() {
        // Trip B đang DISPATCHED — phải chặn revert.
        TripDraft draft = TripDraft.builder().id(62L).status("VALIDATED").build();

        when(tripDraftRepository.findById(62L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftIdAndStatusNot(62L, TripStatus.CANCELLED)).thenReturn(true);

        assertThatThrownBy(() -> tripDraftService.revertToDraft(62L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED);

        verify(tripDraftRepository, never()).save(any());
        verifyNoInteractions(manifestRepository);
    }

    @Test
    void revertToDraft_throws_whenTripAlreadyCompleted() {
        // Chuyến đã giao xong — không được phép revert kế hoạch, dù chuyến không còn "active" theo
        // nghĩa đang chạy. Guard phải dựa trên "khác CANCELLED", không phải liệt kê 1 danh sách
        // trạng thái "active" cụ thể — nếu không COMPLETED sẽ vô tình lọt qua.
        TripDraft draft = TripDraft.builder().id(63L).status("VALIDATED").build();

        when(tripDraftRepository.findById(63L)).thenReturn(Optional.of(draft));
        when(tripRepository.existsByTripDraftIdAndStatusNot(63L, TripStatus.CANCELLED)).thenReturn(true);

        assertThatThrownBy(() -> tripDraftService.revertToDraft(63L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED);

        verify(tripDraftRepository, never()).save(any());
    }
}

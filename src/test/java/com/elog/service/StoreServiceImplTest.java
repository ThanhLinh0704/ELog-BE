package com.elog.service;

import com.elog.dto.request.*;
import com.elog.dto.response.*;
import com.elog.entity.Route;
import com.elog.entity.RouteStop;
import com.elog.entity.Store;
import com.elog.exception.BusinessException;
import com.elog.mapper.StoreMapper;
import com.elog.repository.RouteStopRepository;
import com.elog.repository.StoreRepository;
import com.elog.service.impl.StoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {

    @Mock
    StoreRepository storeRepository;
    @Mock
    RouteStopRepository routeStopRepository;
    @Mock
    StoreMapper storeMapper;
    @InjectMocks
    StoreServiceImpl storeService;

    private StoreCreateRequest validCreateRequest;
    private Store savedStore;

    @BeforeEach
    void setUp() {
        validCreateRequest = new StoreCreateRequest();
        validCreateRequest.setStoreCode("ST-Q1-001");
        validCreateRequest.setStoreName("Dien May Test");
        validCreateRequest.setAddress("10 Le Lai, Q.1, TP.HCM");
        validCreateRequest.setContactPhone("0901234567");
        validCreateRequest.setLatitude(10.7756587);
        validCreateRequest.setLongitude(106.7004238);

        savedStore = Store.builder()
                .id(1L).code("ST-Q1-001").name("Dien May Test")
                .address("10 Le Lai, Q.1, TP.HCM").isActive(true)
                .latitude(10.7756587).longitude(106.7004238)
                .build();
    }

    // ── createStore ──────────────────────────────────────────

    @Test
    void createStore_success() {
        when(storeRepository.existsByCode("ST-Q1-001")).thenReturn(false);
        when(storeMapper.toEntity(validCreateRequest)).thenReturn(savedStore);
        when(storeRepository.save(any(Store.class))).thenReturn(savedStore);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.empty());
        StoreResponse expectedResponse = StoreResponse.builder().id(1L).storeCode("ST-Q1-001").build();
        when(storeMapper.toResponse(savedStore, null)).thenReturn(expectedResponse);

        StoreResponse result = storeService.createStore(validCreateRequest);

        assertThat(result.getId()).isEqualTo(1L);
        verify(storeRepository).save(any(Store.class));
    }

    @Test
    void createStore_duplicateCode_throws409() {
        when(storeRepository.existsByCode("ST-Q1-001")).thenReturn(true);

        BusinessException ex = catchThrowableOfType(
                () -> storeService.createStore(validCreateRequest), BusinessException.class);

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStore_latitudeWithoutLongitude_throws400() {
        validCreateRequest.setLatitude(10.77);
        validCreateRequest.setLongitude(null);
        when(storeRepository.existsByCode(any())).thenReturn(false);

        BusinessException ex = catchThrowableOfType(
                () -> storeService.createStore(validCreateRequest), BusinessException.class);

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(storeRepository, never()).save(any());
    }

    @Test
    void createStore_longitudeWithoutLatitude_throws400() {
        validCreateRequest.setLatitude(null);
        validCreateRequest.setLongitude(106.70);
        when(storeRepository.existsByCode(any())).thenReturn(false);

        BusinessException ex = catchThrowableOfType(
                () -> storeService.createStore(validCreateRequest), BusinessException.class);

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(storeRepository, never()).save(any());
    }

    // ── getStoreById ─────────────────────────────────────────

    @Test
    void getStoreById_success() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(savedStore));
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.empty());
        StoreResponse expected = StoreResponse.builder().id(1L).build();
        when(storeMapper.toResponse(savedStore, null)).thenReturn(expected);

        StoreResponse result = storeService.getStoreById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getStoreById_notFound_throws404() {
        when(storeRepository.findById(99L)).thenReturn(Optional.empty());

        BusinessException ex = catchThrowableOfType(
                () -> storeService.getStoreById(99L), BusinessException.class);

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // â”€â”€ getAllStores â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void getAllStores_returnsMappedContentAndPagination() {
        Pageable pageable = PageRequest.of(1, 2);
        Route route = Route.builder().id(7L).code("RT-Q1").name("Tuyen Q1").build();
        RouteStop routeStop = RouteStop.builder()
                .route(route).store(savedStore).sequenceOrder(1).build();
        StoreListItemResponse item = StoreListItemResponse.builder()
                .id(1L).storeCode("ST-Q1-001").assignedRoute(
                        AssignedRouteDto.builder().id(7L).code("RT-Q1").name("Tuyen Q1").build())
                .build();

        when(storeRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(savedStore), pageable, 3));
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(routeStop));
        when(storeMapper.toListItem(eq(savedStore), any(AssignedRouteDto.class))).thenReturn(item);

        ApiResponse<List<StoreListItemResponse>> result =
                storeService.getAllStores("test", true, true, pageable);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).containsExactly(item);
        assertThat(result.getPagination().getPage()).isEqualTo(1);
        assertThat(result.getPagination().getSize()).isEqualTo(2);
        assertThat(result.getPagination().getTotalElements()).isEqualTo(3);
        assertThat(result.getPagination().getTotalPages()).isEqualTo(2);
    }

    // â”€â”€ updateStore â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    @Test
    void updateStore_success() {
        StoreUpdateRequest request = new StoreUpdateRequest();
        request.setStoreName("Dien May Updated");
        request.setAddress("20 Nguyen Hue, Q.1, TP.HCM");
        request.setContactName("Anh Test");
        request.setContactPhone("0912345678");
        request.setLatitude(10.776);
        request.setLongitude(106.701);
        StoreResponse expected = StoreResponse.builder()
                .id(1L).storeName("Dien May Updated").build();

        when(storeRepository.findById(1L)).thenReturn(Optional.of(savedStore));
        when(storeRepository.save(savedStore)).thenReturn(savedStore);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.empty());
        when(storeMapper.toResponse(savedStore, null)).thenReturn(expected);

        StoreResponse result = storeService.updateStore(1L, request);

        assertThat(result).isSameAs(expected);
        assertThat(savedStore.getName()).isEqualTo("Dien May Updated");
        assertThat(savedStore.getAddress()).isEqualTo("20 Nguyen Hue, Q.1, TP.HCM");
        assertThat(savedStore.getContactName()).isEqualTo("Anh Test");
        assertThat(savedStore.getContactPhone()).isEqualTo("0912345678");
        assertThat(savedStore.getLatitude()).isEqualTo(10.776);
        assertThat(savedStore.getLongitude()).isEqualTo(106.701);
    }

    // ── updateStoreStatus ─────────────────────────────────────

    @Test
    void updateStoreStatus_deactivate_storeInActiveRoute_throws409() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(savedStore));
        when(routeStopRepository.existsByStoreIdAndRouteIsActiveTrue(1L)).thenReturn(true);
        Route activeRoute = Route.builder().id(1L).code("RT-Q1").name("Tuyen Q1").isActive(true).build();
        RouteStop rs = RouteStop.builder().route(activeRoute).store(savedStore).sequenceOrder(1).build();
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.of(rs));

        StoreStatusUpdateRequest req = new StoreStatusUpdateRequest();
        req.setIsActive(false);

        BusinessException ex = catchThrowableOfType(
                () -> storeService.updateStoreStatus(1L, req), BusinessException.class);

        assertThat(ex.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
        verify(storeRepository, never()).save(any());
    }

    @Test
    void updateStoreStatus_deactivate_noActiveRoute_success() {
        when(storeRepository.findById(1L)).thenReturn(Optional.of(savedStore));
        when(routeStopRepository.existsByStoreIdAndRouteIsActiveTrue(1L)).thenReturn(false);
        when(storeRepository.save(any())).thenReturn(savedStore);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.empty());
        StoreResponse expected = StoreResponse.builder().id(1L).isActive(false).build();
        when(storeMapper.toResponse(any(), any())).thenReturn(expected);

        StoreStatusUpdateRequest req = new StoreStatusUpdateRequest();
        req.setIsActive(false);

        StoreResponse result = storeService.updateStoreStatus(1L, req);

        assertThat(result.getIsActive()).isFalse();
        verify(storeRepository).save(argThat(s -> !s.getIsActive()));
    }

    @Test
    void updateStoreStatus_activate_successWithoutActiveRouteCheck() {
        StoreStatusUpdateRequest req = new StoreStatusUpdateRequest();
        req.setIsActive(true);
        StoreResponse expected = StoreResponse.builder().id(1L).isActive(true).build();

        when(storeRepository.findById(1L)).thenReturn(Optional.of(savedStore));
        when(storeRepository.save(savedStore)).thenReturn(savedStore);
        when(routeStopRepository.findFirstByStoreId(1L)).thenReturn(Optional.empty());
        when(storeMapper.toResponse(savedStore, null)).thenReturn(expected);

        StoreResponse result = storeService.updateStoreStatus(1L, req);

        assertThat(result.getIsActive()).isTrue();
        verify(routeStopRepository, never()).existsByStoreIdAndRouteIsActiveTrue(anyLong());
    }
}

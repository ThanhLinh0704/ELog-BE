package com.elog.service;

import com.elog.dto.request.StoreCreateRequest;
import com.elog.dto.request.StoreStatusUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.StoreListItemResponse;
import com.elog.dto.response.StoreResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.StoreMapper;
import com.elog.repository.*;
import com.elog.service.impl.StoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceImplTest {
    @Mock StoreRepository storeRepository;
    @Mock RouteStopRepository routeStopRepository;
    @Mock ProvinceRepository provinceRepository;
    @Mock DistrictRepository districtRepository;
    @Mock WardRepository wardRepository;
    @Mock StoreMapper storeMapper;

    StoreServiceImpl service;
    Store store;
    Province province;
    District district;
    Ward ward;

    @BeforeEach
    void setUp() {
        service = new StoreServiceImpl(storeRepository, routeStopRepository, provinceRepository, districtRepository, wardRepository, storeMapper);
        province = Province.builder().code("01").name("Hà Nội").build();
        district = District.builder().code("001").name("Ba Đình").province(province).build();
        ward = Ward.builder().code("00001").name("Phúc Xá").district(district).build();
        store = Store.builder().id(10L).code("S10").name("Store 10").province(province).district(district).ward(ward).isActive(true).build();
    }

    @Test
    @DisplayName("[L1-ST-01] createStore validates address hierarchy and saves new store")
    void createStoreSuccess() {
        StoreCreateRequest req = new StoreCreateRequest();
        req.setStoreCode("S10");
        req.setProvinceCode("01");
        req.setDistrictCode("001");
        req.setWardCode("00001");

        when(storeRepository.existsByCode("S10")).thenReturn(false);
        when(provinceRepository.findById("01")).thenReturn(Optional.of(province));
        when(districtRepository.findById("001")).thenReturn(Optional.of(district));
        when(wardRepository.findById("00001")).thenReturn(Optional.of(ward));
        when(storeMapper.toEntity(req)).thenReturn(store);
        when(storeRepository.save(any())).thenReturn(store);
        when(storeMapper.toResponse(eq(store), any(), any())).thenReturn(StoreResponse.builder().id(10L).storeCode("S10").build());

        StoreResponse resp = service.createStore(req);

        assertAll(
                () -> assertEquals(10L, resp.getId()),
                () -> verify(storeRepository).save(store)
        );
    }

    @Test
    @DisplayName("[L1-ST-02] getStoreById returns detail response")
    void getStoreByIdSuccess() {
        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(storeMapper.toResponse(eq(store), any(), any())).thenReturn(StoreResponse.builder().id(10L).storeCode("S10").build());

        StoreResponse resp = service.getStoreById(10L);

        assertEquals(10L, resp.getId());
    }

    @Test
    @DisplayName("[L1-ST-03] getAllStores returns paginated store list")
    void getAllStoresSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        when(storeRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(store)));
        when(routeStopRepository.findByStoreIdIn(List.of(10L))).thenReturn(List.of());
        when(storeMapper.toListItem(eq(store), any(), any())).thenReturn(StoreListItemResponse.builder().id(10L).storeCode("S10").build());

        ApiResponse<List<StoreListItemResponse>> resp = service.getAllStores(null, null, null, null, pageable);

        assertAll(
                () -> assertTrue(resp.isSuccess()),
                () -> assertEquals(1, resp.getData().size())
        );
    }

    @Test
    @DisplayName("[L1-ST-04] updateStoreStatus rejects deactivating store assigned to active route")
    void updateStoreStatusRejectsActiveRoute() {
        StoreStatusUpdateRequest req = new StoreStatusUpdateRequest();
        req.setIsActive(false);

        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(routeStopRepository.existsByStoreIdAndRouteIsActiveTrue(10L)).thenReturn(true);
        when(routeStopRepository.findFirstByStoreId(10L)).thenReturn(Optional.of(RouteStop.builder().route(Route.builder().code("R1").build()).build()));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.updateStoreStatus(10L, req));
        assertEquals(ErrorCode.STORE_ACTIVE_ROUTE, ex.getErrorCode());
    }

    @Test
    @DisplayName("[L1-ST-05] updateStore updates store details when address hierarchy is valid")
    void updateStoreSuccess() {
        com.elog.dto.request.StoreUpdateRequest req = new com.elog.dto.request.StoreUpdateRequest();
        req.setStoreName("Updated Store Name");
        req.setProvinceCode("01");
        req.setDistrictCode("001");
        req.setWardCode("00001");

        when(storeRepository.findById(10L)).thenReturn(Optional.of(store));
        when(provinceRepository.findById("01")).thenReturn(Optional.of(province));
        when(districtRepository.findById("001")).thenReturn(Optional.of(district));
        when(wardRepository.findById("00001")).thenReturn(Optional.of(ward));
        when(storeRepository.save(store)).thenReturn(store);
        when(storeMapper.toResponse(eq(store), any(), any())).thenReturn(StoreResponse.builder().id(10L).storeName("Updated Store Name").build());

        StoreResponse resp = service.updateStore(10L, req);

        assertEquals("Updated Store Name", resp.getStoreName());
    }

    @Test
    @DisplayName("[L1-ST-06] createStore rejects single coordinate input")
    void createStoreRejectsSingleCoordinate() {
        StoreCreateRequest req = new StoreCreateRequest();
        req.setStoreCode("S10");
        req.setLatitude(21.0);
        req.setLongitude(null);

        when(storeRepository.existsByCode("S10")).thenReturn(false);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createStore(req));
        assertEquals(ErrorCode.INVALID_COORDINATES, ex.getErrorCode());
    }
}

package com.elog.integration;

import com.elog.dto.request.RouteCreateRequest;
import com.elog.dto.request.RouteStopAddRequest;
import com.elog.dto.request.RouteStopReorderRequest;
import com.elog.dto.response.RouteResponse;
import com.elog.entity.District;
import com.elog.entity.Province;
import com.elog.entity.Route;
import com.elog.entity.RouteStop;
import com.elog.entity.Store;
import com.elog.entity.Ward;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.DistrictRepository;
import com.elog.repository.ProvinceRepository;
import com.elog.repository.RouteRepository;
import com.elog.repository.RouteStopRepository;
import com.elog.repository.StoreRepository;
import com.elog.repository.WardRepository;
import com.elog.service.RouteService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class RouteServiceIntegrationTest {

    @Autowired RouteService routeService;
    @Autowired RouteRepository routeRepository;
    @Autowired RouteStopRepository routeStopRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired ProvinceRepository provinceRepository;
    @Autowired DistrictRepository districtRepository;
    @Autowired WardRepository wardRepository;
    @Autowired EntityManager entityManager;

    @Test
    void l2Rsm01CreatesInactiveNormalizedRouteWithoutStops() {
        RouteCreateRequest request = new RouteCreateRequest();
        request.setCode(unique("rt").toLowerCase());
        request.setName("Report 5 route");

        RouteResponse response = routeService.createRoute(request);
        entityManager.flush();
        entityManager.clear();

        Route route = routeRepository.findById(response.getId()).orElseThrow();
        assertThat(route.getCode()).isEqualTo(request.getCode().toUpperCase());
        assertThat(route.getIsActive()).isFalse();
        assertThat(routeStopRepository.countByRouteId(route.getId())).isZero();
    }

    @Test
    void l2Rsm02RejectsDuplicateStopWithoutInsert() {
        Route route = route();
        Store store = store(true);
        routeStopRepository.saveAndFlush(RouteStop.builder().route(route).store(store).sequenceOrder(1).build());
        RouteStopAddRequest request = add(store.getId());
        long before = routeStopRepository.count();

        assertThatThrownBy(() -> routeService.addStop(route.getId(), request))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ROUTE_STOP_DUPLICATE));
        assertThat(routeStopRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Rsm03RejectsInactiveStoreWithoutInsert() {
        Route route = route();
        Store store = store(false);
        long before = routeStopRepository.count();

        assertThatThrownBy(() -> routeService.addStop(route.getId(), add(store.getId())))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.STORE_INACTIVE));
        assertThat(routeStopRepository.count()).isEqualTo(before);
    }

    @Test
    void l2Rsm04ReordersAllStopsWithoutUniqueSequenceCollision() {
        Route route = route();
        List<RouteStop> stops = new ArrayList<>();
        for (int sequence = 1; sequence <= 4; sequence++) {
            stops.add(routeStopRepository.save(RouteStop.builder()
                    .route(route).store(store(true)).sequenceOrder(sequence).build()));
        }
        routeStopRepository.flush();
        RouteStopReorderRequest request = reorder(List.of(
                stops.get(3).getId(), stops.get(1).getId(), stops.get(0).getId(), stops.get(2).getId()));

        routeService.reorderStops(route.getId(), request);
        entityManager.flush();
        entityManager.clear();

        assertThat(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(route.getId()))
                .extracting(RouteStop::getId).containsExactlyElementsOf(request.getOrderedStopIds());
    }

    @Test
    void l2Rsm05RejectsIncompleteReorderAndLeavesSequenceUnchanged() {
        Route route = route();
        RouteStop first = routeStopRepository.save(RouteStop.builder()
                .route(route).store(store(true)).sequenceOrder(1).build());
        RouteStop second = routeStopRepository.saveAndFlush(RouteStop.builder()
                .route(route).store(store(true)).sequenceOrder(2).build());

        assertThatThrownBy(() -> routeService.reorderStops(route.getId(), reorder(List.of(second.getId()))))
                .isInstanceOfSatisfying(BusinessException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.ROUTE_STOP_REORDER_INVALID));
        entityManager.clear();
        assertThat(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(route.getId()))
                .extracting(RouteStop::getId).containsExactly(first.getId(), second.getId());
    }

    private Route route() {
        return routeRepository.saveAndFlush(Route.builder()
                .code(unique("RT")).name("Route").isActive(false).build());
    }

    private Store store(boolean active) {
        String suffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        Province province = provinceRepository.save(Province.builder()
                .code("P" + suffix).name("Province").fullName("Province " + suffix).build());
        District district = districtRepository.save(District.builder()
                .code("D" + suffix).name("District").fullName("District " + suffix).province(province).build());
        Ward ward = wardRepository.save(Ward.builder()
                .code("W" + suffix).name("Ward").fullName("Ward " + suffix).district(district).build());
        return storeRepository.saveAndFlush(Store.builder().code(unique("ST")).name("Store")
                .province(province).district(district).ward(ward).addressDetail("123 Street")
                .isActive(active).latitude(21.0).longitude(105.0).build());
    }

    private RouteStopAddRequest add(Long storeId) {
        RouteStopAddRequest request = new RouteStopAddRequest();
        request.setStoreId(storeId);
        return request;
    }

    private RouteStopReorderRequest reorder(List<Long> ids) {
        RouteStopReorderRequest request = new RouteStopReorderRequest();
        request.setOrderedStopIds(ids);
        return request;
    }

    private String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}


package com.elog.service;

import com.elog.dto.request.route.RouteCreateRequest;
import com.elog.dto.request.route.RouteStopAddRequest;
import com.elog.dto.request.route.RouteStopReorderRequest;
import com.elog.dto.response.route.RouteResponse;
import com.elog.dto.response.route.RouteStopResponse;
import com.elog.entity.Route;
import com.elog.entity.RouteStop;
import com.elog.entity.Store;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.mapper.RouteMapper;
import com.elog.repository.*;
import com.elog.service.impl.RouteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {
    @Mock RouteRepository routeRepository; @Mock RouteStopRepository routeStopRepository; @Mock StoreRepository storeRepository;
    @Mock RouteMapper routeMapper; @Mock TripStopRepository tripStopRepository; @Mock TripDraftStopRepository tripDraftStopRepository;
    @Mock ManifestLineRepository manifestLineRepository; @Mock DeliveryOrderResultRepository deliveryOrderResultRepository; @Mock GoongMapService goongMapService;
    RouteServiceImpl service; Route route;

    @BeforeEach void setUp(){service=new RouteServiceImpl(routeRepository,routeStopRepository,storeRepository,routeMapper,tripStopRepository,tripDraftStopRepository,manifestLineRepository,deliveryOrderResultRepository,goongMapService); route=Route.builder().id(1L).code("RT-01").routePolyline("cached").build();}
    private RouteStopAddRequest addRequest(long storeId){RouteStopAddRequest r=new RouteStopAddRequest();r.setStoreId(storeId);return r;}
    private RouteStopReorderRequest reorder(Long... ids){RouteStopReorderRequest r=new RouteStopReorderRequest();r.setOrderedStopIds(List.of(ids));return r;}
    private RouteStop stop(long id,int sequence){Store store=Store.builder().id(100L+id).code("S"+id).name("Store "+id).latitude(21.0).longitude(105.0).build();return RouteStop.builder().id(id).route(route).store(store).sequenceOrder(sequence).build();}

    @Test @DisplayName("[L1-RT-03] add stop appends max sequence and reports missing coordinates")
    void addStopAppendsSequenceAndMapsCoordinateWarning(){Store store=Store.builder().id(20L).code("S20").name("No GPS").isActive(true).build(); when(routeRepository.findById(1L)).thenReturn(Optional.of(route)); when(storeRepository.findById(20L)).thenReturn(Optional.of(store)); when(routeStopRepository.findMaxSequenceOrderByRouteId(1L)).thenReturn(2); when(routeStopRepository.save(any())).thenAnswer(i->i.getArgument(0)); when(routeMapper.toStopResponse(any(),eq(true))).thenReturn(RouteStopResponse.builder().sequenceOrder(3).coordinatesWarning(true).build());
        RouteStopResponse response=service.addStop(1L,addRequest(20L)); ArgumentCaptor<RouteStop> saved=ArgumentCaptor.forClass(RouteStop.class); verify(routeStopRepository).save(saved.capture());
        assertAll(()->assertEquals(3,saved.getValue().getSequenceOrder()),()->assertSame(store,saved.getValue().getStore()),()->assertTrue(response.getCoordinatesWarning()),()->assertNull(route.getRoutePolyline()));}

    @Test @DisplayName("[L1-RT-04] duplicate store stop is rejected before save")
    void duplicateStopIsRejected(){Store store=Store.builder().id(20L).code("S20").isActive(true).build();when(routeRepository.findById(1L)).thenReturn(Optional.of(route));when(storeRepository.findById(20L)).thenReturn(Optional.of(store));when(routeStopRepository.existsByRouteIdAndStoreId(1L,20L)).thenReturn(true);
        BusinessException e=assertThrows(BusinessException.class,()->service.addStop(1L,addRequest(20L)));assertEquals(ErrorCode.ROUTE_STOP_DUPLICATE,e.getErrorCode());verify(routeStopRepository,never()).save(any());}

    @Test @DisplayName("[L1-RT-05] missing route rejects add before loading store")
    void missingRouteRejectsAdd(){when(routeRepository.findById(999L)).thenReturn(Optional.empty());BusinessException e=assertThrows(BusinessException.class,()->service.addStop(999L,addRequest(20L)));assertEquals(ErrorCode.ROUTE_NOT_FOUND,e.getErrorCode());verifyNoInteractions(storeRepository);}

    @Test @DisplayName("[L1-RT-06] reorder assigns requested sequence C A B")
    void reorderAssignsRequestedSequence(){RouteStop a=stop(1,1),b=stop(2,2),c=stop(3,3);when(routeRepository.findById(1L)).thenReturn(Optional.of(route));when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(a,b,c));when(routeStopRepository.save(any())).thenAnswer(i->i.getArgument(0));when(routeMapper.toStopResponse(any(),anyBoolean())).thenReturn(new RouteStopResponse());
        service.reorderStops(1L,reorder(3L,1L,2L));assertAll(()->assertEquals(1,c.getSequenceOrder()),()->assertEquals(2,a.getSequenceOrder()),()->assertEquals(3,b.getSequenceOrder()));verify(routeStopRepository).flush();}

    @Test @DisplayName("[L1-RT-07] reorder with incomplete stop set is rejected before sequence save")
    void incompleteReorderIsRejected(){RouteStop a=stop(1,1),b=stop(2,2),c=stop(3,3);when(routeRepository.findById(1L)).thenReturn(Optional.of(route));when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(a,b,c));BusinessException e=assertThrows(BusinessException.class,()->service.reorderStops(1L,reorder(1L,2L)));assertEquals(ErrorCode.ROUTE_STOP_REORDER_INVALID,e.getErrorCode());verify(routeStopRepository,never()).save(any());}

    @Test @DisplayName("[L1-RT-08] remove stop deletes it and normalizes remaining sequences")
    void removeDeletesAndNormalizes(){RouteStop removed=stop(2,2),a=stop(1,1),c=stop(3,3);when(routeRepository.findById(1L)).thenReturn(Optional.of(route));when(routeStopRepository.findById(2L)).thenReturn(Optional.of(removed));when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(a,c));
        service.removeStop(1L,2L);verify(routeStopRepository).delete(removed);assertAll(()->assertEquals(1,a.getSequenceOrder()),()->assertEquals(2,c.getSequenceOrder()));}

    @Test @DisplayName("[L1-RT-09] missing stop is rejected without delete")
    void missingStopIsRejected(){when(routeRepository.findById(1L)).thenReturn(Optional.of(route));when(routeStopRepository.findById(999L)).thenReturn(Optional.empty());BusinessException e=assertThrows(BusinessException.class,()->service.removeStop(1L,999L));assertEquals(ErrorCode.ROUTE_STOP_NOT_FOUND,e.getErrorCode());verify(routeStopRepository,never()).delete(any());}

    @Test @DisplayName("[L1-RT-10] createRoute saves new route when code is unique")
    void createRouteSuccess(){RouteCreateRequest req=new RouteCreateRequest();req.setCode("RT-02");req.setName("Route 2");when(routeRepository.existsByCode("RT-02")).thenReturn(false);when(routeRepository.save(any())).thenReturn(route);when(routeMapper.toResponse(eq(route),eq(0))).thenReturn(RouteResponse.builder().id(1L).code("RT-02").build());RouteResponse resp=service.createRoute(req);assertEquals(1L,resp.getId());}

    @Test @DisplayName("[L1-RT-11] createRoute rejects duplicate route code")
    void createRouteDuplicateRejects(){RouteCreateRequest req=new RouteCreateRequest();req.setCode("RT-01");when(routeRepository.existsByCode("RT-01")).thenReturn(true);BusinessException e=assertThrows(BusinessException.class,()->service.createRoute(req));assertEquals(ErrorCode.ROUTE_CODE_DUPLICATE,e.getErrorCode());}

    // ── Additional Unit Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("[L1-RT-12] getRouteById returns route detail with stops")
    void getRouteByIdSuccess() {
        RouteStop s1 = stop(1L, 1);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(s1));
        when(routeMapper.toDetailResponse(eq(route), anyList())).thenReturn(com.elog.dto.response.route.RouteDetailResponse.builder().id(1L).code("RT-01").build());

        var resp = service.getRouteById(1L);
        assertNotNull(resp);
        assertEquals(1L, resp.getId());
    }

    @Test
    @DisplayName("[L1-RT-13] getAllRoutes returns paginated response with stop count")
    void getAllRoutesSuccess() {
        org.springframework.data.domain.Page<Route> page = new org.springframework.data.domain.PageImpl<>(List.of(route));
        when(routeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(org.springframework.data.domain.Pageable.class))).thenReturn(page);
        when(routeStopRepository.countStopsByRouteIdIn(List.of(1L))).thenReturn(List.<Object[]>of(new Object[]{1L, 3L}));
        when(routeMapper.toResponse(eq(route), eq(3))).thenReturn(RouteResponse.builder().id(1L).code("RT-01").build());

        var resp = service.getAllRoutes("RT", true, org.springframework.data.domain.PageRequest.of(0, 10));
        assertNotNull(resp);
        assertTrue(resp.isSuccess());
        assertEquals(1, resp.getData().size());
    }

    @Test
    @DisplayName("[L1-RT-14] updateRoute updates name and description")
    void updateRouteSuccess() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeRepository.save(any())).thenReturn(route);
        when(routeStopRepository.countByRouteId(1L)).thenReturn(2);
        when(routeMapper.toResponse(eq(route), eq(2))).thenReturn(RouteResponse.builder().id(1L).code("RT-01").build());

        com.elog.dto.request.route.RouteUpdateRequest req = new com.elog.dto.request.route.RouteUpdateRequest();
        req.setName("Updated Name");
        req.setDescription("Updated Desc");

        var resp = service.updateRoute(1L, req);
        assertNotNull(resp);
        assertEquals("Updated Name", route.getName());
    }

    @Test
    @DisplayName("[L1-RT-15] updateRouteStatus to active with < 2 stops throws ROUTE_INSUFFICIENT_STOPS")
    void updateRouteStatusInsufficientStops() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStopRepository.countByRouteId(1L)).thenReturn(1);

        var req = new com.elog.dto.request.route.RouteStatusUpdateRequest();
        req.setIsActive(true);

        BusinessException e = assertThrows(BusinessException.class, () -> service.updateRouteStatus(1L, req));
        assertEquals(ErrorCode.ROUTE_INSUFFICIENT_STOPS, e.getErrorCode());
    }

    @Test
    @DisplayName("[L1-RT-16] updateRouteStatus to active with >= 2 stops succeeds")
    void updateRouteStatusSuccess() {
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStopRepository.countByRouteId(1L)).thenReturn(3);
        when(routeRepository.save(any())).thenReturn(route);
        when(routeMapper.toResponse(eq(route), eq(3))).thenReturn(RouteResponse.builder().id(1L).isActive(true).build());

        var req = new com.elog.dto.request.route.RouteStatusUpdateRequest();
        req.setIsActive(true);

        var resp = service.updateRouteStatus(1L, req);
        assertNotNull(resp);
        assertTrue(route.getIsActive());
    }

    @Test
    @DisplayName("[L1-RT-17] getRouteDirections returns cached polyline")
    void getRouteDirectionsCached() {
        route.setRoutePolyline("cached_points");
        route.setTotalDistanceKm(15.5);
        route.setTotalDurationMin(25);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(stop(1L, 1)));

        var resp = service.getRouteDirections(1L, false);
        assertNotNull(resp);
        assertEquals("cached_points", resp.getRoutePolyline());
        assertEquals(15.5, resp.getTotalDistanceKm());
    }

    @Test
    @DisplayName("[L1-RT-18] getRouteDirections with Goong Map directions")
    void getRouteDirectionsWithGoong() {
        RouteStop s1 = stop(1L, 1);
        RouteStop s2 = stop(2L, 2);
        route.setRoutePolyline(null);

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(s1, s2));
        when(goongMapService.isConfigured()).thenReturn(true);

        com.elog.dto.response.goong.GoongDirectionsResponse goongResp = new com.elog.dto.response.goong.GoongDirectionsResponse();
        var r = new com.elog.dto.response.goong.GoongDirectionsResponse.Route();
        var poly = new com.elog.dto.response.goong.GoongDirectionsResponse.Polyline();
        poly.setPoints("goong_poly");
        r.setOverviewPolyline(poly);

        var leg = new com.elog.dto.response.goong.GoongDirectionsResponse.Leg();
        var dist = new com.elog.dto.response.goong.GoongDirectionsResponse.ValueText();
        dist.setValue(20000L);
        leg.setDistance(dist);
        r.setLegs(List.of(leg));
        goongResp.setRoutes(List.of(r));

        when(goongMapService.getDirections(anyString(), anyString(), any())).thenReturn(goongResp);

        var resp = service.getRouteDirections(1L, true);
        assertNotNull(resp);
        assertEquals("goong_poly", resp.getRoutePolyline());
        assertEquals(20.0, resp.getTotalDistanceKm());
    }

    @Test
    @DisplayName("[L1-RT-19] getRouteDirections(Long) default overload")
    void getRouteDirectionsOverload() {
        route.setRoutePolyline("cached_points");
        route.setTotalDistanceKm(15.5);
        route.setTotalDurationMin(25);
        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findByRouteIdOrderBySequenceOrderAsc(1L)).thenReturn(List.of(stop(1L, 1)));

        var resp = service.getRouteDirections(1L);
        assertNotNull(resp);
        assertEquals("cached_points", resp.getRoutePolyline());
    }
}


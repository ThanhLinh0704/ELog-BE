package com.elog.service;

import com.elog.dto.request.RouteStopAddRequest;
import com.elog.dto.request.RouteStopReorderRequest;
import com.elog.dto.response.RouteStopResponse;
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
class RouteServiceImplReport5Test {
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
}

package com.elog.service;

import com.elog.dto.request.RecalculateEtaRequest;
import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.repository.*;
import com.elog.service.impl.TripDraftServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripDraftServiceImplReport5Test {
 @Mock OrderRepository orders;@Mock RouteStopRepository routeStops;@Mock TripDraftRepository drafts;@Mock TripDraftStopRepository draftStops;@Mock RouteRepository routes;@Mock UserRepository users;@Mock EtaCalculationService eta;@Mock OrderItemRepository items;@Mock TripRepository trips;@Mock ManifestRepository manifests;@Mock PlanningHistoryService history;
 TripDraftServiceImpl service;LocalDate date=LocalDate.of(2026,8,14);long nextId=50;
 @BeforeEach void setup(){service=new TripDraftServiceImpl(orders,routeStops,drafts,draftStops,routes,users,eta,items,trips,manifests,history);}
 Store store(long id){return Store.builder().id(id).code("S"+id).name("Store "+id).build();}
 Route route(long id){return Route.builder().id(id).code("R"+id).build();}
 RouteStop rs(long id,Route r,Store s,int seq){return RouteStop.builder().id(id).route(r).store(s).sequenceOrder(seq).build();}
 com.elog.entity.Order order(long id,Store s,String volume,String weight){return com.elog.entity.Order.builder().id(id).store(s).deliveryDate(date).items(List.of(OrderItem.builder().lineVolumeM3(new BigDecimal(volume)).lineWeightKg(new BigDecimal(weight)).build())).build();}
 void saveAnswers(){when(drafts.saveAndFlush(any())).thenAnswer(i->{TripDraft d=i.getArgument(0);if(d.getId()==null)d.setId(nextId++);return d;});when(drafts.save(any())).thenAnswer(i->i.getArgument(0));}
 void map(Store s,Route r,RouteStop mapping,List<RouteStop> all){when(routeStops.findFirstByStoreId(s.getId())).thenReturn(Optional.of(mapping));when(routes.findById(r.getId())).thenReturn(Optional.of(r));when(routeStops.findByRouteIdOrderBySequenceOrderAsc(r.getId())).thenReturn(all);when(drafts.findByRouteIdAndDeliveryDate(r.getId(),date)).thenReturn(Optional.empty());}

 @Test @DisplayName("[L1-TD-01] orders on two routes create two trip drafts") void twoRoutesCreateTwoDrafts(){Store a=store(1),b=store(2);Route ra=route(10),rb=route(20);RouteStop rsa=rs(1,ra,a,1),rsb=rs(2,rb,b,1);when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of(order(1,a,"1","10"),order(2,b,"1","10")));map(a,ra,rsa,List.of(rsa));map(b,rb,rsb,List.of(rsb));saveAnswers();ConsolidateResponse r=service.consolidate(date);assertAll(()->assertEquals(2,r.getTripDraftsCreatedOrUpdated()),()->assertEquals(Set.of("R10","R20"),r.getTripDrafts().stream().map(TripDraftResponse::getRouteCode).collect(java.util.stream.Collectors.toSet())));}
 @Test @DisplayName("[L1-TD-02] draft totals sum all order-item weight and volume") void totalsAreSummed(){Store s=store(1);Route route=route(10);RouteStop mapping=rs(1,route,s,1);com.elog.entity.Order one=order(1,s,"1.5","100"),two=order(2,s,"0.5","50");when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of(one,two));map(s,route,mapping,List.of(mapping));saveAnswers();TripDraftResponse r=service.consolidate(date).getTripDrafts().getFirst();assertAll(()->assertEquals(new BigDecimal("2.0"),r.getTotalVolumeM3()),()->assertEquals(new BigDecimal("150"),r.getTotalWeightKg()));}
 @Test @DisplayName("[L1-TD-03] consolidation recreates every route stop with active flags") void everyRouteStopIsRecreated(){Store a=store(1),b=store(2);Route route=route(10);RouteStop rsa=rs(1,route,a,1),rsb=rs(2,route,b,2);when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of(order(1,a,"1","10")));map(a,route,rsa,List.of(rsa,rsb));saveAnswers();service.consolidate(date);ArgumentCaptor<TripDraft> saved=ArgumentCaptor.forClass(TripDraft.class);verify(drafts).save(saved.capture());assertAll(()->assertEquals(2,saved.getValue().getStops().size()),()->assertTrue(saved.getValue().getStops().get(0).getIsActive()),()->assertFalse(saved.getValue().getStops().get(1).getIsActive()));}
 @Test @DisplayName("[L1-TD-04] duplicate JOIN FETCH orders are deduplicated by ID") void duplicateOrdersAreDeduplicated(){Store s=store(1);Route route=route(10);RouteStop mapping=rs(1,route,s,1);com.elog.entity.Order o=order(1,s,"1","10");when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of(o,o));map(s,route,mapping,List.of(mapping));saveAnswers();service.consolidate(date);verify(orders).updateTripDraftId(argThat(ids->ids.equals(List.of(1L))),anyLong());}
 @Test @DisplayName("[L1-TD-05] no accepted orders returns an empty response") void noOrdersReturnsEmpty(){when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of());ConsolidateResponse r=service.consolidate(date);assertAll(()->assertEquals(0,r.getTripDraftsCreatedOrUpdated()),()->assertTrue(r.getTripDrafts().isEmpty()));}
 @Test @DisplayName("[L1-TD-06] orders at stores without route mapping are skipped") void unmappedOrdersAreSkipped(){Store s=store(1);when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of(order(1,s,"1","10")));when(routeStops.findFirstByStoreId(1L)).thenReturn(Optional.empty());assertTrue(service.consolidate(date).getTripDrafts().isEmpty());verifyNoInteractions(drafts);}
 @Test @DisplayName("[L1-TD-07] existing DRAFT is updated instead of duplicated") void existingDraftIsUpdated(){Store s=store(1);Route route=route(10);RouteStop mapping=rs(1,route,s,1);TripDraft existing=TripDraft.builder().id(77L).status("DRAFT").stops(new ArrayList<>()).build();when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of(order(1,s,"1","10")));when(routeStops.findFirstByStoreId(1L)).thenReturn(Optional.of(mapping));when(routes.findById(10L)).thenReturn(Optional.of(route));when(routeStops.findByRouteIdOrderBySequenceOrderAsc(10L)).thenReturn(List.of(mapping));when(drafts.findByRouteIdAndDeliveryDate(10L,date)).thenReturn(Optional.of(existing));saveAnswers();TripDraftResponse r=service.consolidate(date).getTripDrafts().getFirst();assertEquals(77L,r.getId());verify(drafts).save(existing);}
 @Test @DisplayName("[L1-TD-08] orders for multiple stores on one route are grouped") void multipleStoresAreGrouped(){Store a=store(1),b=store(2);Route route=route(10);RouteStop rsa=rs(1,route,a,1),rsb=rs(2,route,b,2);when(orders.findByDeliveryDateAndStatus(date,"ACCEPTED")).thenReturn(List.of(order(1,a,"1","10"),order(2,b,"1","10")));when(routeStops.findFirstByStoreId(1L)).thenReturn(Optional.of(rsa));when(routeStops.findFirstByStoreId(2L)).thenReturn(Optional.of(rsb));when(routes.findById(10L)).thenReturn(Optional.of(route));when(routeStops.findByRouteIdOrderBySequenceOrderAsc(10L)).thenReturn(List.of(rsa,rsb));when(drafts.findByRouteIdAndDeliveryDate(10L,date)).thenReturn(Optional.empty());saveAnswers();ConsolidateResponse r=service.consolidate(date);assertAll(()->assertEquals(1,r.getTripDraftsCreatedOrUpdated()),()->assertEquals(2,r.getTripDrafts().getFirst().getActiveStopCount()));verify(orders).updateTripDraftId(argThat(ids->ids.equals(List.of(1L,2L))),anyLong());}
 @Test @DisplayName("[L1-TD-09] ETA recalculation maps collaborator stops into response") void etaResultIsMapped(){StopEtaResponse stop=StopEtaResponse.builder().tripDraftStopId(2L).sequenceNo(1).storeCode("S1").plannedEta(LocalDateTime.of(date,LocalTime.of(9,0))).build();when(eta.calculateAndPersist(1L,LocalTime.of(7,0))).thenReturn(List.of(stop));RecalculateEtaResponse r=service.recalculateEta(1L,new RecalculateEtaRequest(LocalTime.of(7,0)));assertAll(()->assertEquals(1L,r.getTripDraftId()),()->assertEquals(List.of(stop),r.getStops()),()->assertTrue(r.getMessage().contains("1 active stops")));}
 @Test @DisplayName("[L1-TD-10] ETA collaborator failure is propagated unchanged") void etaFailurePropagates(){IllegalStateException failure=new IllegalStateException("maps unavailable");when(eta.calculateAndPersist(1L,LocalTime.NOON)).thenThrow(failure);assertSame(failure,assertThrows(IllegalStateException.class,()->service.recalculateEta(1L,new RecalculateEtaRequest(LocalTime.NOON))));}
 @Test @DisplayName("[L1-TD-11] empty ETA collaborator result creates empty stop response") void emptyEtaResultMapsEmpty(){when(eta.calculateAndPersist(1L,LocalTime.NOON)).thenReturn(List.of());RecalculateEtaResponse r=service.recalculateEta(1L,new RecalculateEtaRequest(LocalTime.NOON));assertAll(()->assertTrue(r.getStops().isEmpty()),()->assertTrue(r.getMessage().contains("0 active stops")));}
}

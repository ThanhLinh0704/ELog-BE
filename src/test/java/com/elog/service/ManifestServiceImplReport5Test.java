package com.elog.service;

import com.elog.dto.response.ManifestResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.ManifestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManifestServiceImplReport5Test {
    @Mock TripDraftRepository tripDraftRepo; @Mock TripDraftStopRepository tripDraftStopRepo; @Mock OrderItemRepository orderItemRepo;
    @Mock ManifestRepository manifestRepo; @Mock ManifestLineRepository manifestLineRepo; @Mock UserRepository userRepo;
    ManifestServiceImpl service; TripDraft draft; User user;

    @BeforeEach void setUp(){service=new ManifestServiceImpl(tripDraftRepo,tripDraftStopRepo,orderItemRepo,manifestRepo,manifestLineRepo,userRepo);draft=TripDraft.builder().id(1L).route(Route.builder().id(2L).code("RT-01").build()).deliveryDate(LocalDate.of(2026,8,14)).status("VALIDATED").build();user=User.builder().id(3L).username("warehouse").fullName("Warehouse").build();}
    private TripDraftStop stop(int sequence){return TripDraftStop.builder().id((long)sequence).sequenceNo(sequence).isActive(true).store(Store.builder().id(100L+sequence).code("S"+sequence).name("Store "+sequence).build()).build();}
    private OrderItem item(int n){BigDecimal weight=BigDecimal.valueOf(n);BigDecimal volume=BigDecimal.valueOf(n,2);return OrderItem.builder().id((long)n).product(Product.builder().id((long)n).sku("SKU-"+n).productName("Product "+n).build()).quantity(n).unitWeightKg(BigDecimal.ONE).unitVolumeM3(new BigDecimal("0.01")).lineWeightKg(weight).lineVolumeM3(volume).build();}
    private List<ManifestLine> captureGenerated(List<TripDraftStop> stops, java.util.function.Function<TripDraftStop,List<OrderItem>> items){when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));when(tripDraftStopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(stops);when(userRepo.findByUsername("warehouse")).thenReturn(Optional.of(user));for(TripDraftStop s:stops)when(orderItemRepo.findByStopForManifest(s.getStore().getId(),1L)).thenReturn(items.apply(s));when(manifestRepo.save(any())).thenAnswer(i->i.getArgument(0));service.generateManifest(1L,"warehouse");ArgumentCaptor<List<ManifestLine>> lines=ArgumentCaptor.forClass(List.class);verify(manifestLineRepo).saveAll(lines.capture());return lines.getValue();}

    @Test @DisplayName("[L1-MF-01] four delivery stops generate reversed LIFO lines and totals")
    void fourStopsGenerateReversedLifo(){List<TripDraftStop> stops=IntStream.rangeClosed(1,4).mapToObj(this::stop).toList();List<ManifestLine> lines=captureGenerated(stops,s->List.of(item(s.getSequenceNo())));
        assertEquals(List.of(4,3,2,1),lines.stream().map(ManifestLine::getStopSequenceNo).toList());assertEquals(List.of(1,2,3,4),lines.stream().map(ManifestLine::getLifoSequence).toList());
        ArgumentCaptor<Manifest> manifest=ArgumentCaptor.forClass(Manifest.class);verify(manifestRepo,times(2)).save(manifest.capture());Manifest saved=manifest.getAllValues().getLast();assertAll(()->assertEquals(new BigDecimal("10"),saved.getTotalWeightKg()),()->assertEquals(new BigDecimal("0.10"),saved.getTotalVolumeM3()),()->assertEquals(4,saved.getTotalLines()));}

    @Test @DisplayName("[L1-MF-02] manifest preserves complete five-item store breakdown")
    void manifestPreservesItemBreakdown(){TripDraftStop stop=stop(1);List<OrderItem> items=IntStream.rangeClosed(1,5).mapToObj(this::item).toList();List<ManifestLine> lines=captureGenerated(List.of(stop),s->items);assertEquals(5,lines.size());
        for(int i=0;i<5;i++){int n=i+1;ManifestLine line=lines.get(i);assertAll(()->assertEquals("SKU-"+n,line.getProductCode()),()->assertEquals(n,line.getQuantity()),()->assertEquals(BigDecimal.valueOf(n,2),line.getLineVolumeM3()),()->assertEquals("S1",line.getStoreCode()));}}

    @Test @DisplayName("[L1-MF-03] generated manifest header is persisted as an immutable record")
    void manifestHeaderIsPersisted(){TripDraftStop stop=stop(1);captureGenerated(List.of(stop),s->List.of(item(1)));ArgumentCaptor<Manifest> saved=ArgumentCaptor.forClass(Manifest.class);verify(manifestRepo,times(2)).save(saved.capture());assertSame(draft,saved.getAllValues().getFirst().getTripDraft());assertSame(user,saved.getAllValues().getFirst().getGeneratedBy());assertNotNull(saved.getAllValues().getFirst().getGeneratedAt());}

    @Test @DisplayName("[L1-MF-04] missing trip draft blocks manifest writes")
    void missingDraftBlocksGeneration(){when(tripDraftRepo.findById(999L)).thenReturn(Optional.empty());BusinessException e=assertThrows(BusinessException.class,()->service.generateManifest(999L,"warehouse"));assertEquals(ErrorCode.TRIP_DRAFT_NOT_FOUND,e.getErrorCode());verifyNoInteractions(manifestRepo,manifestLineRepo);}

    @Test @DisplayName("[L1-MF-05] non-validated trip draft blocks manifest writes")
    void nonValidatedDraftBlocksGeneration(){draft.setStatus("PLANNED");when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));BusinessException e=assertThrows(BusinessException.class,()->service.generateManifest(1L,"warehouse"));assertEquals(ErrorCode.TRIP_DRAFT_NOT_VALIDATED,e.getErrorCode());verifyNoInteractions(manifestRepo,manifestLineRepo);}

    @Test @DisplayName("[L1-MF-06] existing manifest returns lines in LIFO loading sequence")
    void existingManifestReturnsLifoLines(){Manifest manifest=Manifest.builder().manifestId(10L).tripDraft(draft).generatedBy(user).generatedAt(LocalDateTime.now()).totalLines(2).totalWeightKg(BigDecimal.TEN).totalVolumeM3(BigDecimal.ONE).build();ManifestLine first=ManifestLine.builder().lifoSequence(1).stopSequenceNo(4).storeCode("S4").productCode("SKU-4").quantity(4).build();ManifestLine second=ManifestLine.builder().lifoSequence(2).stopSequenceNo(1).storeCode("S1").productCode("SKU-1").quantity(1).build();when(manifestRepo.findByTripDraftIdWithDetails(1L)).thenReturn(Optional.of(manifest));when(manifestLineRepo.findByManifestManifestIdOrderByLifoSequenceAsc(10L)).thenReturn(List.of(first,second));
        ManifestResponse response=service.getManifest(1L);assertEquals(List.of(1,2),response.getLines().stream().map(l->l.getLifoSequence()).toList());assertEquals(List.of(4,1),response.getLines().stream().map(l->l.getStopSequenceNo()).toList());}

    @Test @DisplayName("[L1-MF-07] missing manifest returns MANIFEST_NOT_FOUND")
    void missingManifestIsRejected(){when(manifestRepo.findByTripDraftIdWithDetails(1L)).thenReturn(Optional.empty());BusinessException e=assertThrows(BusinessException.class,()->service.getManifest(1L));assertEquals(ErrorCode.MANIFEST_NOT_FOUND,e.getErrorCode());}
}

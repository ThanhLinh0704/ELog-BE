package com.elog.service;

import com.elog.dto.response.ManifestByStopResponse;
import com.elog.dto.response.ManifestResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.impl.ManifestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManifestServiceImplTest {

    @Mock
    private TripDraftRepository tripDraftRepo;

    @Mock
    private TripDraftStopRepository tripDraftStopRepo;

    @Mock
    private OrderItemRepository orderItemRepo;

    @Mock
    private ManifestRepository manifestRepo;

    @Mock
    private ManifestLineRepository manifestLineRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private ManifestServiceImpl manifestService;

    private TripDraft draft;
    private Route route;
    private User generator;
    private TripDraftStop stop1;
    private TripDraftStop stop2;
    private Store store1;
    private Store store2;
    private Product product;
    private OrderItem item1;
    private OrderItem item2;

    @BeforeEach
    void setUp() {
        route = Route.builder().id(1L).code("RT-001").name("Route 1").build();

        draft = TripDraft.builder()
                .id(1L)
                .route(route)
                .deliveryDate(LocalDate.now())
                .status("VALIDATED")
                .totalVolumeM3(new BigDecimal("5.0"))
                .totalWeightKg(new BigDecimal("500.0"))
                .build();

        generator = User.builder()
                .id(2L)
                .username("warehouse01")
                .fullName("Warehouse Staff")
                .build();

        store1 = Store.builder().id(10L).code("ST-001").name("Store One").build();
        store2 = Store.builder().id(11L).code("ST-002").name("Store Two").build();

        stop1 = TripDraftStop.builder().id(20L).store(store1).sequenceNo(1).isActive(true).build();
        stop2 = TripDraftStop.builder().id(21L).store(store2).sequenceNo(2).isActive(true).build();

        product = Product.builder().id(30L).sku("PROD-1").productName("Product 1").build();

        item1 = OrderItem.builder()
                .id(40L)
                .product(product)
                .quantity(10)
                .unitWeightKg(new BigDecimal("10.0"))
                .unitVolumeM3(new BigDecimal("0.1"))
                .lineWeightKg(new BigDecimal("100.0"))
                .lineVolumeM3(new BigDecimal("1.0"))
                .build();

        item2 = OrderItem.builder()
                .id(41L)
                .product(product)
                .quantity(20)
                .unitWeightKg(new BigDecimal("5.0"))
                .unitVolumeM3(new BigDecimal("0.05"))
                .lineWeightKg(new BigDecimal("100.0"))
                .lineVolumeM3(new BigDecimal("1.0"))
                .build();
    }

    @Test
    void generateManifest_tripDraftNotFound_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manifestService.generateManifest(1L, "warehouse01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void generateManifest_tripDraftNotValidated_throwsException() {
        draft.setStatus("PLANNED");
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));

        assertThatThrownBy(() -> manifestService.generateManifest(1L, "warehouse01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_VALIDATED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void generateManifest_alreadyExists_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(manifestRepo.existsByTripDraftId(1L)).thenReturn(true);

        assertThatThrownBy(() -> manifestService.generateManifest(1L, "warehouse01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MANIFEST_ALREADY_EXISTS)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void generateManifest_noActiveStops_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(manifestRepo.existsByTripDraftId(1L)).thenReturn(false);
        when(tripDraftStopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> manifestService.generateManifest(1L, "warehouse01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_ACTIVE_STOP)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void generateManifest_userNotFound_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(manifestRepo.existsByTripDraftId(1L)).thenReturn(false);
        when(tripDraftStopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L))
                .thenReturn(Arrays.asList(stop1, stop2));
        when(userRepo.findByUsername("warehouse01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manifestService.generateManifest(1L, "warehouse01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND);
    }

    @Test
    void generateManifest_success_lifoOrdering() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draft));
        when(manifestRepo.existsByTripDraftId(1L)).thenReturn(false);
        when(tripDraftStopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L))
                .thenReturn(Arrays.asList(stop1, stop2));
        when(userRepo.findByUsername("warehouse01")).thenReturn(Optional.of(generator));
        
        // Mock LIFO order loading (stop2 is reverse of stop1, loaded first)
        when(orderItemRepo.findByStopForManifest(store2.getId(), 1L))
                .thenReturn(Collections.singletonList(item2));
        when(orderItemRepo.findByStopForManifest(store1.getId(), 1L))
                .thenReturn(Collections.singletonList(item1));

        ManifestResponse response = manifestService.generateManifest(1L, "warehouse01");

        assertThat(response).isNotNull();
        assertThat(response.getTripDraftId()).isEqualTo(1L);
        assertThat(response.getTotalLines()).isEqualTo(2);
        assertThat(response.getTotalWeightKg()).isEqualByComparingTo("200.0");
        assertThat(response.getTotalVolumeM3()).isEqualByComparingTo("2.0");

        verify(manifestRepo, times(2)).save(any(Manifest.class));
        verify(manifestLineRepo).saveAll(anyList());
    }

    @Test
    void getManifest_notFound_throwsException() {
        when(manifestRepo.findByTripDraftIdWithDetails(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> manifestService.getManifest(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MANIFEST_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void getManifest_success() {
        Manifest m = Manifest.builder()
                .manifestId(100L)
                .tripDraft(draft)
                .generatedBy(generator)
                .generatedAt(LocalDateTime.now())
                .totalLines(2)
                .totalWeightKg(new BigDecimal("200.0"))
                .totalVolumeM3(new BigDecimal("2.0"))
                .build();

        ManifestLine line1 = ManifestLine.builder()
                .lifoSequence(1)
                .stopSequenceNo(2)
                .storeCode("ST-002")
                .storeName("Store Two")
                .productCode("PROD-1")
                .productName("Product 1")
                .quantity(20)
                .unitWeightKg(new BigDecimal("5.0"))
                .unitVolumeM3(new BigDecimal("0.05"))
                .lineWeightKg(new BigDecimal("100.0"))
                .lineVolumeM3(new BigDecimal("1.0"))
                .build();

        ManifestLine line2 = ManifestLine.builder()
                .lifoSequence(2)
                .stopSequenceNo(1)
                .storeCode("ST-001")
                .storeName("Store One")
                .productCode("PROD-1")
                .productName("Product 1")
                .quantity(10)
                .unitWeightKg(new BigDecimal("10.0"))
                .unitVolumeM3(new BigDecimal("0.1"))
                .lineWeightKg(new BigDecimal("100.0"))
                .lineVolumeM3(new BigDecimal("1.0"))
                .build();

        when(manifestRepo.findByTripDraftIdWithDetails(1L)).thenReturn(Optional.of(m));
        when(manifestLineRepo.findByManifestManifestIdOrderByLifoSequenceAsc(100L))
                .thenReturn(Arrays.asList(line1, line2));

        ManifestResponse response = manifestService.getManifest(1L);

        assertThat(response).isNotNull();
        assertThat(response.getLines()).hasSize(2);
        assertThat(response.getLines().get(0).getLoadingNote()).isEqualTo("Xếp VÀO xe đầu tiên — nằm sâu nhất");
        assertThat(response.getLines().get(1).getLoadingNote()).isEqualTo("Xếp VÀO xe cuối cùng — gần cửa xe nhất");
    }

    @Test
    void getManifestByStop_success() {
        Manifest m = Manifest.builder()
                .manifestId(100L)
                .tripDraft(draft)
                .generatedBy(generator)
                .generatedAt(LocalDateTime.now())
                .totalLines(2)
                .totalWeightKg(new BigDecimal("200.0"))
                .totalVolumeM3(new BigDecimal("2.0"))
                .build();

        ManifestLine line1 = ManifestLine.builder()
                .lifoSequence(2)
                .stopSequenceNo(1)
                .storeCode("ST-001")
                .storeName("Store One")
                .productCode("PROD-1")
                .productName("Product 1")
                .quantity(10)
                .unitWeightKg(new BigDecimal("10.0"))
                .unitVolumeM3(new BigDecimal("0.1"))
                .lineWeightKg(new BigDecimal("100.0"))
                .lineVolumeM3(new BigDecimal("1.0"))
                .build();

        ManifestLine line2 = ManifestLine.builder()
                .lifoSequence(1)
                .stopSequenceNo(2)
                .storeCode("ST-002")
                .storeName("Store Two")
                .productCode("PROD-1")
                .productName("Product 1")
                .quantity(20)
                .unitWeightKg(new BigDecimal("5.0"))
                .unitVolumeM3(new BigDecimal("0.05"))
                .lineWeightKg(new BigDecimal("100.0"))
                .lineVolumeM3(new BigDecimal("1.0"))
                .build();

        when(manifestRepo.findByTripDraftIdWithDetails(1L)).thenReturn(Optional.of(m));
        when(manifestLineRepo.findByManifestManifestIdOrderByStopSequenceNoAscLifoSequenceAsc(100L))
                .thenReturn(Arrays.asList(line1, line2));

        ManifestByStopResponse response = manifestService.getManifestByStop(1L);

        assertThat(response).isNotNull();
        assertThat(response.getStops()).hasSize(2);
        // Groups should be sorted by LIFO loading order (descending stop_sequence_no = 2 first, then 1)
        assertThat(response.getStops().get(0).getStopSequenceNo()).isEqualTo(2);
        assertThat(response.getStops().get(0).getLoadingNote()).isEqualTo("Nhóm này xếp VÀO xe ĐẦU TIÊN");
        assertThat(response.getStops().get(1).getStopSequenceNo()).isEqualTo(1);
        assertThat(response.getStops().get(1).getLoadingNote()).isEqualTo("Nhóm này xếp VÀO xe CUỐI CÙNG — gần cửa");
    }
}


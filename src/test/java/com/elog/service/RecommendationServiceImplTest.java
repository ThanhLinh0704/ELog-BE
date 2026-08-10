package com.elog.service;

import com.elog.dto.response.RecommendationResultResponse;
import com.elog.entity.*;
import com.elog.repository.*;
import com.elog.service.impl.RecommendationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private TripDraftRepository tripDraftRepo;
    @Mock
    private TripDraftStopRepository tripDraftStopRepo;
    @Mock
    private VehicleRepository vehicleRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private TripRepository tripRepo;
    @Mock
    private TripExecutionRepository tripExecutionRepo;
    @Mock
    private OrderItemRepository orderItemRepo;
    @Mock
    private OrderRepository orderRepo;
    @Mock
    private SystemConfigRepository configRepo;
    @Mock
    private PlanningHistoryService planningHistoryService;

    @Spy
    private ConstraintValidationService constraintValidationService = new com.elog.service.impl.ConstraintValidationServiceImpl();

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    private TripDraft testDraft;
    private Vehicle largeVehicle;
    private Store testStore;
    private TripDraftStop stop1;

    @BeforeEach
    void setUp() {
        testStore = Store.builder()
                .id(100L)
                .code("ST-001")
                .name("Store 1")
                .allowedDeliveryHours("08:00-17:00")
                .isActive(true)
                .build();

        testDraft = TripDraft.builder()
                .id(1L)
                .deliveryDate(LocalDate.now())
                .status("PLANNED")
                .totalVolumeM3(BigDecimal.valueOf(5.0))
                .totalWeightKg(BigDecimal.valueOf(1000.0))
                .plannedDepartureTime(LocalTime.of(8, 0))
                .build();

        stop1 = TripDraftStop.builder()
                .id(200L)
                .tripDraft(testDraft)
                .store(testStore)
                .sequenceNo(1)
                .isActive(true)
                .plannedEta(LocalDateTime.of(LocalDate.now(), LocalTime.of(8, 30)))
                .build();

        testDraft.setStops(List.of(stop1));

        // Fleet has a vehicle with plenty of capacity
        largeVehicle = Vehicle.builder()
                .id(10L)
                .plateNumber("88X-900.30")
                .vehicleType("15 TONS")
                .maxVolumeM3(BigDecimal.valueOf(50.0))
                .payloadKg(BigDecimal.valueOf(15000.0))
                .status(VehicleStatus.AVAILABLE)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("getRecommendations khi vi phạm order.deliveryTimeWindow: chẩn đoán đúng TIME_WINDOW thay vì báo thiếu tải")
    void getRecommendations_whenOrderTimeWindowViolated_attributesToTimeWindowNotCapacity() {
        // ETA is 08:30, but order delivery window requires 13:00 - 17:00
        Order orderWithTimeWindow = Order.builder()
                .id(500L)
                .orderRef("DH-20260808-173A")
                .store(testStore)
                .deliveryTimeWindow("13:00 - 17:00")
                .build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(tripDraftStopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(1L)).thenReturn(List.of(stop1));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(List.of(largeVehicle));
        when(orderRepo.findByTripDraftId(1L)).thenReturn(List.of(orderWithTimeWindow));
        when(userRepo.findAll()).thenReturn(List.of());

        RecommendationResultResponse response = recommendationService.recommendTop3(1L);

        assertThat(response).isNotNull();
        assertThat(response.getPlanType()).isEqualTo("NO_PLAN");
        assertThat(response.getViolatedConstraints()).isNotEmpty();
        assertThat(response.getViolatedConstraints().get(0))
                .contains("vi phạm khung giờ giao hàng (TIME_WINDOW)");

        // Assert no duplicate TIME_WINDOW reasons exist
        long timeWindowReasonCount = response.getViolatedConstraints().stream()
                .filter(r -> r != null && r.contains("TIME_WINDOW"))
                .count();
        assertThat(timeWindowReasonCount).isEqualTo(1);
    }
}

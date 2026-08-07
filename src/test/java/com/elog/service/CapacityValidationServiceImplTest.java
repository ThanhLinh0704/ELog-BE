package com.elog.service;

import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.repository.OrderRepository;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.UserRepository;
import com.elog.repository.VehicleRepository;
import com.elog.service.impl.CapacityValidationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapacityValidationServiceImplTest {

    @Mock
    private TripDraftRepository tripDraftRepo;

    @Mock
    private VehicleRepository vehicleRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private RecommendationService recommendationService;

    @InjectMocks
    private CapacityValidationServiceImpl capacityValidationService;

    private User testUser;
    private TripDraft testDraft;
    private Route testRoute;
    private Vehicle testVehicle;
    private Store testStore;
    private TripDraftStop testStop;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).username("dispatcher").fullName("Dispatcher Test").build();

        testRoute = Route.builder().id(10L).code("RT-010").name("Route 10").isActive(true).build();

        testStore = Store.builder()
                .id(100L)
                .code("ST-001")
                .name("Store 1")
                .maxAllowedVehicleWeight(BigDecimal.valueOf(10000))
                .allowedDeliveryHours("08:00-17:00")
                .isActive(true)
                .build();

        testDraft = TripDraft.builder()
                .id(1L)
                .route(testRoute)
                .deliveryDate(LocalDate.now())
                .status("PLANNED")
                .totalVolumeM3(BigDecimal.valueOf(5.0))
                .totalWeightKg(BigDecimal.valueOf(2000.0))
                .volumeCheckResult(ConstraintResult.NOT_CHECKED)
                .weightCheckResult(ConstraintResult.NOT_CHECKED)
                .build();

        testStop = TripDraftStop.builder()
                .id(200L)
                .tripDraft(testDraft)
                .store(testStore)
                .isActive(true)
                .sequenceNo(1)
                .plannedEta(LocalDateTime.of(LocalDate.now(), java.time.LocalTime.of(10, 0)))
                .build();

        testDraft.setStops(Arrays.asList(testStop));

        testVehicle = Vehicle.builder()
                .id(1L)
                .plateNumber("29A-12345")
                .vehicleType("1.25 TONS")
                .maxVolumeM3(BigDecimal.valueOf(10.0))
                .payloadKg(BigDecimal.valueOf(3000.0))
                .isActive(true)
                .build();
    }

    @Test
    void validate_success_eligibleVehicle() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(testVehicle));
        when(userRepo.findByUsername("dispatcher")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(Collections.emptyList());

        CapacityValidationResultResponse response = capacityValidationService.validate(1L, "dispatcher");

        assertThat(response.isValidationPassed()).isTrue();
        assertThat(response.getEligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles()).isEmpty();
        assertThat(response.getNewStatus()).isEqualTo("VALIDATED");

        verify(tripDraftRepo).save(testDraft);
    }

    @Test
    void validate_fails_whenVolumeExceedsSafetyBuffer() {
        // Safe volume buffer is 10.0 * 0.9 = 9.0. Let's make draft volume 9.5
        testDraft.setTotalVolumeM3(BigDecimal.valueOf(9.5));

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(testVehicle));
        when(userRepo.findByUsername("dispatcher")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(Collections.emptyList());

        CapacityValidationResultResponse response = capacityValidationService.validate(1L, "dispatcher");

        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getFailureReason()).contains("Volume exceeds safety limit");
    }

    @Test
    void validate_fails_whenWeightExceedsStoreLimit() {
        // Store max allowed vehicle weight is 10000. Let's set vehicle weight capacity to 12000, which exceeds store limit.
        testVehicle.setPayloadKg(BigDecimal.valueOf(12000.0));

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(testVehicle));
        when(userRepo.findByUsername("dispatcher")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(Collections.emptyList());

        CapacityValidationResultResponse response = capacityValidationService.validate(1L, "dispatcher");

        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getFailureReason()).contains("exceeds store ST-001 limit");
    }

    @Test
    void validate_fails_whenEtaOutsideStoreAllowedHours() {
        // Allowed hours: 08:00-17:00. Let's set plannedEta to 20:00.
        testStop.setPlannedEta(LocalDateTime.of(LocalDate.now(), java.time.LocalTime.of(20, 0)));

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(testVehicle));
        when(userRepo.findByUsername("dispatcher")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(Collections.emptyList());

        CapacityValidationResultResponse response = capacityValidationService.validate(1L, "dispatcher");

        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getFailureReason()).contains("is outside store ST-001 allowed delivery hours");
    }

    @Test
    void validate_fails_whenEtaOutsideOrderTimeWindow() {
        // ETA is 10:00. Order requires delivery before 09:00.
        Order testOrder = Order.builder()
                .id(500L)
                .orderRef("ORD-999")
                .store(testStore)
                .deliveryTimeWindow("Trước 9h")
                .build();

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(testVehicle));
        when(userRepo.findByUsername("dispatcher")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(Collections.singletonList(testOrder));

        CapacityValidationResultResponse response = capacityValidationService.validate(1L, "dispatcher");

        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getFailureReason()).contains("violates delivery window (Trước 9h) for order ORD-999");
    }

    @Test
    void validate_success_viaTwoVehicleFallback_whenNoSingleVehicleFitsButPairDoes() {
        // 9.5 m³ exceeds safety buffer of single vehicle (10.0 * 0.9 = 9.0) -> no single vehicle fits,
        // but Recommendation Engine confirms a two-vehicle split is feasible (BR-07).
        testDraft.setTotalVolumeM3(BigDecimal.valueOf(9.5));

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(testVehicle));
        when(userRepo.findByUsername("dispatcher")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(Collections.emptyList());
        when(recommendationService.isTwoVehicleFeasible(1L)).thenReturn(true);

        CapacityValidationResultResponse response = capacityValidationService.validate(1L, "dispatcher");

        assertThat(response.isValidationPassed()).isTrue();
        assertThat(response.getNewStatus()).isEqualTo("VALIDATED");
        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getSuggestion()).contains("two-vehicle split is feasible");
        assertThat(response.getMessage()).contains("two-vehicle split");
        assertThat(testDraft.getStatus()).isEqualTo("VALIDATED");
        assertThat(testDraft.getValidatedAt()).isNotNull();

        verify(recommendationService).isTwoVehicleFeasible(1L);
        verify(tripDraftRepo).save(testDraft);
    }

    @Test
    void validate_fails_whenNeitherSingleNorTwoVehicleFits() {
        // Same draft exceeding single vehicle limit, but this time Recommendation Engine also says
        // no two-vehicle pair fits -> must stay at PLANNED, must NOT accidentally pass.
        testDraft.setTotalVolumeM3(BigDecimal.valueOf(9.5));

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(testDraft));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(testVehicle));
        when(userRepo.findByUsername("dispatcher")).thenReturn(Optional.of(testUser));
        when(orderRepository.findByTripDraftId(1L)).thenReturn(Collections.emptyList());
        when(recommendationService.isTwoVehicleFeasible(1L)).thenReturn(false);

        CapacityValidationResultResponse response = capacityValidationService.validate(1L, "dispatcher");

        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getNewStatus()).isEqualTo("PLANNED");
        assertThat(response.getBindingConstraint()).isEqualTo("VOLUME");
        assertThat(response.getSuggestion()).contains("no two-vehicle split is feasible either");
        assertThat(testDraft.getStatus()).isEqualTo("PLANNED");

        verify(recommendationService).isTwoVehicleFeasible(1L);
    }
}

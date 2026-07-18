package com.elog.service;

import com.elog.dto.response.CapacityValidationResultResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
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
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CapacityValidationServiceImplTest {

    @Mock
    private TripDraftRepository tripDraftRepo;

    @Mock
    private VehicleRepository vehicleRepo;

    @Mock
    private UserRepository userRepo;

    @InjectMocks
    private CapacityValidationServiceImpl validationService;

    private TripDraft draftPlanned;
    private Vehicle vehicleSmall;
    private Vehicle vehicleLarge;
    private User validator;
    private Route route;

    @BeforeEach
    void setUp() {
        route = Route.builder().id(1L).code("RT-001").name("Route 1").build();

        draftPlanned = TripDraft.builder()
                .id(1L)
                .route(route)
                .deliveryDate(LocalDate.now())
                .status("PLANNED")
                .totalVolumeM3(new BigDecimal("5.0"))
                .totalWeightKg(new BigDecimal("500.0"))
                .volumeCheckResult(ConstraintResult.NOT_CHECKED)
                .weightCheckResult(ConstraintResult.NOT_CHECKED)
                .build();

        vehicleSmall = Vehicle.builder()
                .id(10L)
                .plateNumber("29A-12345")
                .maxVolumeM3(new BigDecimal("4.0"))
                .maxWeightKg(new BigDecimal("400.0"))
                .isActive(true)
                .build();

        vehicleLarge = Vehicle.builder()
                .id(11L)
                .plateNumber("29A-67890")
                .maxVolumeM3(new BigDecimal("10.0"))
                .maxWeightKg(new BigDecimal("1000.0"))
                .isActive(true)
                .build();

        validator = User.builder()
                .id(1L)
                .username("dispatcher01")
                .fullName("Dispatcher One")
                .build();
    }

    @Test
    void validate_tripDraftNotFound_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.validate(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void validate_statusNotPlannedAndAlreadyValidated_throwsException() {
        draftPlanned.setStatus("VALIDATED");
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));

        assertThatThrownBy(() -> validationService.validate(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ALREADY_VALIDATED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.CONFLICT);
    }

    @Test
    void validate_statusNotPlannedAndDRAFT_throwsException() {
        draftPlanned.setStatus("DRAFT");
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));

        assertThatThrownBy(() -> validationService.validate(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_CONFIRMED)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void validate_zeroVolumeAndWeight_throwsException() {
        draftPlanned.setTotalVolumeM3(BigDecimal.ZERO);
        draftPlanned.setTotalWeightKg(BigDecimal.ZERO);
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));

        assertThatThrownBy(() -> validationService.validate(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_ITEMS_TO_VALIDATE)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.BAD_REQUEST);
    }

    @Test
    void validate_noActiveVehicles_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> validationService.validate(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NO_ACTIVE_VEHICLE)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void validate_userNotFound_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Collections.singletonList(vehicleLarge));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.validate(1L, "dispatcher01"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.RESOURCE_NOT_FOUND)
                .hasFieldOrPropertyWithValue("httpStatus", HttpStatus.NOT_FOUND);
    }

    @Test
    void validate_success_validationPassed() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Arrays.asList(vehicleSmall, vehicleLarge));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.of(validator));
        when(tripDraftRepo.save(any(TripDraft.class))).thenReturn(draftPlanned);

        CapacityValidationResultResponse response = validationService.validate(1L, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(response.isValidationPassed()).isTrue();
        assertThat(response.getNewStatus()).isEqualTo("VALIDATED");
        assertThat(response.getEligibleVehicles()).hasSize(1);
        assertThat(response.getEligibleVehicles().get(0).getVehicleId()).isEqualTo(11L); // vehicleLarge
        assertThat(response.getIneligibleVehicles()).hasSize(1);
        assertThat(response.getIneligibleVehicles().get(0).getVehicleId()).isEqualTo(10L); // vehicleSmall
        assertThat(response.getVolumeCheckResult()).isEqualTo(ConstraintResult.PASS);
        assertThat(response.getWeightCheckResult()).isEqualTo(ConstraintResult.PASS);

        verify(tripDraftRepo).save(draftPlanned);
    }

    @Test
    void validate_failed_noEligibleVehicles() {
        draftPlanned.setTotalVolumeM3(new BigDecimal("12.0"));
        draftPlanned.setTotalWeightKg(new BigDecimal("1200.0"));

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Arrays.asList(vehicleSmall, vehicleLarge));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.of(validator));
        when(tripDraftRepo.save(any(TripDraft.class))).thenReturn(draftPlanned);

        CapacityValidationResultResponse response = validationService.validate(1L, "dispatcher01");

        assertThat(response).isNotNull();
        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getNewStatus()).isEqualTo("PLANNED");
        assertThat(response.getEligibleVehicles()).isEmpty();
        assertThat(response.getIneligibleVehicles()).hasSize(2);
        assertThat(response.getVolumeCheckResult()).isEqualTo(ConstraintResult.FAIL);
        assertThat(response.getWeightCheckResult()).isEqualTo(ConstraintResult.FAIL);
        assertThat(response.getBindingConstraint()).isEqualTo("BOTH");
    }

    @Test
    void validate_failed_volumeConstraintOnly() {
        draftPlanned.setTotalVolumeM3(new BigDecimal("12.0"));
        draftPlanned.setTotalWeightKg(new BigDecimal("300.0")); // Weight ok, Volume exceeds all

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Arrays.asList(vehicleSmall, vehicleLarge));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.of(validator));

        CapacityValidationResultResponse response = validationService.validate(1L, "dispatcher01");

        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getVolumeCheckResult()).isEqualTo(ConstraintResult.FAIL);
        assertThat(response.getWeightCheckResult()).isEqualTo(ConstraintResult.PASS);
        assertThat(response.getBindingConstraint()).isEqualTo("VOLUME");
    }

    @Test
    void validate_failed_weightConstraintOnly() {
        draftPlanned.setTotalVolumeM3(new BigDecimal("3.0"));
        draftPlanned.setTotalWeightKg(new BigDecimal("1200.0")); // Volume ok, Weight exceeds all

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Arrays.asList(vehicleSmall, vehicleLarge));
        when(userRepo.findByUsername("dispatcher01")).thenReturn(Optional.of(validator));

        CapacityValidationResultResponse response = validationService.validate(1L, "dispatcher01");

        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getVolumeCheckResult()).isEqualTo(ConstraintResult.PASS);
        assertThat(response.getWeightCheckResult()).isEqualTo(ConstraintResult.FAIL);
        assertThat(response.getBindingConstraint()).isEqualTo("WEIGHT");
    }

    @Test
    void getValidationResult_tripDraftNotFound_throwsException() {
        when(tripDraftRepo.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> validationService.getValidationResult(1L))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TRIP_DRAFT_NOT_FOUND);
    }

    @Test
    void getValidationResult_success_validatedStatus() {
        draftPlanned.setStatus("VALIDATED");
        draftPlanned.setVolumeCheckResult(ConstraintResult.PASS);
        draftPlanned.setWeightCheckResult(ConstraintResult.PASS);
        draftPlanned.setValidatedBy(validator);

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Arrays.asList(vehicleSmall, vehicleLarge));

        CapacityValidationResultResponse response = validationService.getValidationResult(1L);

        assertThat(response).isNotNull();
        assertThat(response.isValidationPassed()).isTrue();
        assertThat(response.getValidatedBy()).isNotNull();
        assertThat(response.getValidatedBy().getFullName()).isEqualTo("Dispatcher One");
        assertThat(response.getEligibleVehicles()).hasSize(1);
    }

    @Test
    void getValidationResult_notCheckedStatus() {
        draftPlanned.setStatus("PLANNED");
        draftPlanned.setVolumeCheckResult(ConstraintResult.NOT_CHECKED);
        draftPlanned.setWeightCheckResult(ConstraintResult.NOT_CHECKED);

        when(tripDraftRepo.findById(1L)).thenReturn(Optional.of(draftPlanned));
        when(vehicleRepo.findByIsActiveTrue()).thenReturn(Arrays.asList(vehicleSmall, vehicleLarge));

        CapacityValidationResultResponse response = validationService.getValidationResult(1L);

        assertThat(response).isNotNull();
        assertThat(response.isValidationPassed()).isFalse();
        assertThat(response.getMessage()).contains("Chưa có kết quả kiểm tra");
    }
}


package com.elog.service.impl;

import com.elog.dto.response.*;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.UserRepository;
import com.elog.repository.VehicleRepository;
import com.elog.service.CapacityValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapacityValidationServiceImpl implements CapacityValidationService {

    private final TripDraftRepository tripDraftRepo;
    private final VehicleRepository vehicleRepo;
    private final UserRepository userRepo;

    @Override
    @Transactional
    public CapacityValidationResultResponse validate(Long tripDraftId, String currentUsername) {
        TripDraft draft = tripDraftRepo.findById(tripDraftId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found with id: " + tripDraftId,
                        HttpStatus.NOT_FOUND));

        if (!"PLANNED".equals(draft.getStatus())) {
            if ("VALIDATED".equals(draft.getStatus())) {
                throw new BusinessException(
                        ErrorCode.ALREADY_VALIDATED,
                        "Trip Draft " + tripDraftId + " has already been validated (status=VALIDATED). View result via GET /validation-result.",
                        HttpStatus.CONFLICT);
            }
            throw new BusinessException(
                    ErrorCode.TRIP_DRAFT_NOT_CONFIRMED,
                    "Trip Draft " + tripDraftId + " must be confirmed by Dispatcher (status=PLANNED) before capacity validation. Current status: " + draft.getStatus() + ".",
                    HttpStatus.BAD_REQUEST);
        }

        if (draft.getTotalVolumeM3().compareTo(BigDecimal.ZERO) == 0
                && draft.getTotalWeightKg().compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(
                    ErrorCode.NO_ITEMS_TO_VALIDATE,
                    "Trip Draft has zero total volume and weight — nothing to validate.",
                    HttpStatus.BAD_REQUEST);
        }

        List<Vehicle> activeVehicles = vehicleRepo.findByIsActiveTrue();
        if (activeVehicles.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.NO_ACTIVE_VEHICLE,
                    "No active vehicles found in fleet. Please contact Admin to configure vehicle master data.",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        User validator = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found: " + currentUsername,
                        HttpStatus.NOT_FOUND));

        List<EligibleVehicleDto> eligibleVehicles = new ArrayList<>();
        List<IneligibleVehicleDto> ineligibleVehicles = new ArrayList<>();

        for (Vehicle v : activeVehicles) {
            if (v.getMaxVolumeM3() == null || v.getMaxWeightKg() == null) {
                log.warn("Vehicle {} missing capacity data — skipped in validation", v.getId());
                continue;
            }

            boolean volumeOk = draft.getTotalVolumeM3().compareTo(v.getMaxVolumeM3()) <= 0;
            boolean weightOk = draft.getTotalWeightKg().compareTo(v.getMaxWeightKg()) <= 0;

            if (volumeOk && weightOk) {
                eligibleVehicles.add(EligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getMaxWeightKg())
                        .remainingVolumeM3(v.getMaxVolumeM3().subtract(draft.getTotalVolumeM3()))
                        .remainingWeightKg(v.getMaxWeightKg().subtract(draft.getTotalWeightKg()))
                        .build());
            } else {
                StringBuilder reason = new StringBuilder();
                if (!volumeOk) {
                    reason.append("Volume exceeds capacity (")
                          .append(draft.getTotalVolumeM3()).append(" m³ > ").append(v.getMaxVolumeM3()).append(" m³)");
                }
                if (!weightOk) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append("Weight exceeds capacity (")
                          .append(draft.getTotalWeightKg()).append(" kg > ").append(v.getMaxWeightKg()).append(" kg)");
                }

                ineligibleVehicles.add(IneligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getMaxWeightKg())
                        .volumeCheckResult(volumeOk ? ConstraintResult.PASS : ConstraintResult.FAIL)
                        .weightCheckResult(weightOk ? ConstraintResult.PASS : ConstraintResult.FAIL)
                        .failureReason(reason.toString())
                        .build());
            }
        }

        ConstraintResult overallVolume;
        ConstraintResult overallWeight;
        String newStatus;
        String bindingConstraint = null;
        String suggestion = null;

        if (!eligibleVehicles.isEmpty()) {
            overallVolume = ConstraintResult.PASS;
            overallWeight = ConstraintResult.PASS;
            newStatus = "VALIDATED";

            // Sắp xếp các xe đủ tải theo maxVolumeM3 tăng dần
            eligibleVehicles.sort(Comparator.comparing(EligibleVehicleDto::getMaxVolumeM3));
        } else {
            boolean anyVolumeOk = activeVehicles.stream()
                    .anyMatch(v -> v.getMaxVolumeM3() != null && draft.getTotalVolumeM3().compareTo(v.getMaxVolumeM3()) <= 0);
            boolean anyWeightOk = activeVehicles.stream()
                    .anyMatch(v -> v.getMaxWeightKg() != null && draft.getTotalWeightKg().compareTo(v.getMaxWeightKg()) <= 0);

            overallVolume = anyVolumeOk ? ConstraintResult.PASS : ConstraintResult.FAIL;
            overallWeight = anyWeightOk ? ConstraintResult.PASS : ConstraintResult.FAIL;
            newStatus = "PLANNED";

            if (overallVolume == ConstraintResult.FAIL && overallWeight == ConstraintResult.PASS) {
                bindingConstraint = "VOLUME";
            } else if (overallVolume == ConstraintResult.PASS && overallWeight == ConstraintResult.FAIL) {
                bindingConstraint = "WEIGHT";
            } else {
                bindingConstraint = "BOTH";
            }

            suggestion = "No single vehicle can accommodate this load. Consider trip splitting in Vehicle Assignment (BR-07).";
        }

        draft.setVolumeCheckResult(overallVolume);
        draft.setWeightCheckResult(overallWeight);
        if ("VALIDATED".equals(newStatus)) {
            draft.setStatus("VALIDATED");
            draft.setValidatedAt(LocalDateTime.now());
            draft.setValidatedBy(validator);
        }
        tripDraftRepo.save(draft);

        ConfirmedByDto validatorDto = ConfirmedByDto.builder()
                .userId(validator.getId())
                .fullName(validator.getFullName())
                .build();

        String message = "VALIDATED".equals(newStatus)
                ? "Capacity validation passed. " + eligibleVehicles.size() + " eligible vehicles available. Proceed to Vehicle Assignment."
                : "Capacity validation failed. Total volume " + draft.getTotalVolumeM3() + " m³ exceeds all available vehicle capacities.";

        return CapacityValidationResultResponse.builder()
                .tripDraftId(draft.getId())
                .fixedRouteCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate().toString())
                .newStatus(newStatus)
                .totalVolumeM3(draft.getTotalVolumeM3())
                .totalWeightKg(draft.getTotalWeightKg())
                .validationPassed(!eligibleVehicles.isEmpty())
                .volumeCheckResult(overallVolume)
                .weightCheckResult(overallWeight)
                .eligibleVehicles(eligibleVehicles)
                .ineligibleVehicles(ineligibleVehicles)
                .bindingConstraint(bindingConstraint)
                .suggestion(suggestion)
                .validatedAt(draft.getValidatedAt())
                .validatedBy(validatorDto)
                .message(message)
                .build();
      }

    @Override
    @Transactional(readOnly = true)
    public CapacityValidationResultResponse getValidationResult(Long tripDraftId) {
        TripDraft draft = tripDraftRepo.findById(tripDraftId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found with id: " + tripDraftId,
                        HttpStatus.NOT_FOUND));

        List<Vehicle> activeVehicles = vehicleRepo.findByIsActiveTrue();
        List<EligibleVehicleDto> eligibleVehicles = new ArrayList<>();
        List<IneligibleVehicleDto> ineligibleVehicles = new ArrayList<>();

        boolean validationPassed = "VALIDATED".equals(draft.getStatus());

        if (draft.getVolumeCheckResult() != ConstraintResult.NOT_CHECKED && !activeVehicles.isEmpty()) {
            for (Vehicle v : activeVehicles) {
                if (v.getMaxVolumeM3() == null || v.getMaxWeightKg() == null) {
                    continue;
                }

                boolean volumeOk = draft.getTotalVolumeM3().compareTo(v.getMaxVolumeM3()) <= 0;
                boolean weightOk = draft.getTotalWeightKg().compareTo(v.getMaxWeightKg()) <= 0;

                if (volumeOk && weightOk) {
                    eligibleVehicles.add(EligibleVehicleDto.builder()
                            .vehicleId(v.getId())
                            .plateNumber(v.getPlateNumber())
                            .vehicleType(v.getVehicleType())
                            .maxVolumeM3(v.getMaxVolumeM3())
                            .maxWeightKg(v.getMaxWeightKg())
                            .remainingVolumeM3(v.getMaxVolumeM3().subtract(draft.getTotalVolumeM3()))
                            .remainingWeightKg(v.getMaxWeightKg().subtract(draft.getTotalWeightKg()))
                            .build());
                } else {
                    StringBuilder reason = new StringBuilder();
                    if (!volumeOk) {
                        reason.append("Volume exceeds capacity (")
                              .append(draft.getTotalVolumeM3()).append(" m³ > ").append(v.getMaxVolumeM3()).append(" m³)");
                    }
                    if (!weightOk) {
                        if (reason.length() > 0) reason.append(" and ");
                        reason.append("Weight exceeds capacity (")
                              .append(draft.getTotalWeightKg()).append(" kg > ").append(v.getMaxWeightKg()).append(" kg)");
                      }

                    ineligibleVehicles.add(IneligibleVehicleDto.builder()
                            .vehicleId(v.getId())
                            .plateNumber(v.getPlateNumber())
                            .vehicleType(v.getVehicleType())
                            .maxVolumeM3(v.getMaxVolumeM3())
                            .maxWeightKg(v.getMaxWeightKg())
                            .volumeCheckResult(volumeOk ? ConstraintResult.PASS : ConstraintResult.FAIL)
                            .weightCheckResult(weightOk ? ConstraintResult.PASS : ConstraintResult.FAIL)
                            .failureReason(reason.toString())
                            .build());
                }
            }

            if (!eligibleVehicles.isEmpty()) {
                eligibleVehicles.sort(Comparator.comparing(EligibleVehicleDto::getMaxVolumeM3));
            }
        }

        String bindingConstraint = null;
        String suggestion = null;
        if (draft.getVolumeCheckResult() != ConstraintResult.NOT_CHECKED && eligibleVehicles.isEmpty()) {
            if (draft.getVolumeCheckResult() == ConstraintResult.FAIL && draft.getWeightCheckResult() == ConstraintResult.PASS) {
                bindingConstraint = "VOLUME";
            } else if (draft.getVolumeCheckResult() == ConstraintResult.PASS && draft.getWeightCheckResult() == ConstraintResult.FAIL) {
                bindingConstraint = "WEIGHT";
            } else {
                bindingConstraint = "BOTH";
            }
            suggestion = "No single vehicle can accommodate this load. Consider trip splitting in Vehicle Assignment (BR-07).";
        }

        ConfirmedByDto validatorDto = null;
        if (draft.getValidatedBy() != null) {
            validatorDto = ConfirmedByDto.builder()
                    .userId(draft.getValidatedBy().getId())
                    .fullName(draft.getValidatedBy().getFullName())
                    .build();
        }

        String message = validationPassed
                ? "Capacity validation passed. " + eligibleVehicles.size() + " eligible vehicles available. Proceed to Vehicle Assignment."
                : (draft.getVolumeCheckResult() == ConstraintResult.NOT_CHECKED
                    ? "Chưa có kết quả kiểm tra. Bấm Kiểm tra để bắt đầu."
                    : "Capacity validation failed. Total volume " + draft.getTotalVolumeM3() + " m³ exceeds all available vehicle capacities.");

        return CapacityValidationResultResponse.builder()
                .tripDraftId(draft.getId())
                .fixedRouteCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate().toString())
                .newStatus(draft.getStatus())
                .totalVolumeM3(draft.getTotalVolumeM3())
                .totalWeightKg(draft.getTotalWeightKg())
                .validationPassed(validationPassed)
                .volumeCheckResult(draft.getVolumeCheckResult())
                .weightCheckResult(draft.getWeightCheckResult())
                .eligibleVehicles(eligibleVehicles)
                .ineligibleVehicles(ineligibleVehicles)
                .bindingConstraint(bindingConstraint)
                .suggestion(suggestion)
                .validatedAt(draft.getValidatedAt())
                .validatedBy(validatorDto)
                .message(message)
                .build();
    }
}

package com.elog.service.impl;

import com.elog.dto.response.common.ConfirmedByDto;
import com.elog.dto.response.vehicle.CapacityValidationResultResponse;
import com.elog.dto.response.vehicle.EligibleVehicleDto;
import com.elog.dto.response.vehicle.IneligibleVehicleDto;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.OrderRepository;
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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CapacityValidationServiceImpl implements CapacityValidationService {

    private static final BigDecimal SAFETY_BUFFER = BigDecimal.valueOf(0.90);

    private final TripDraftRepository tripDraftRepo;
    private final VehicleRepository vehicleRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepository;
    private final com.elog.service.RecommendationService recommendationService;
    private final com.elog.service.ConstraintValidationService constraintValidationService;

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

        // Loại xe Bảo dưỡng/Ngừng hoạt động (không bao giờ liệt kê). Xe IN_USE được giữ lại vì
        // trạng thái IN_USE trong DB là trạng thái tĩnh — xe có thể chỉ bận vào 1 ngày cụ thể
        // (ví dụ: 28/08) nhưng vẫn rảnh cho ngày đang xét (ví dụ: 25/08). Xung đột theo ngày
        // do RecommendationServiceImpl.checkHardConstraints() HC-2 xử lý.
        List<Vehicle> activeVehicles = vehicleRepo.findByIsActiveTrue().stream()
                .filter(v -> v.getStatus() != VehicleStatus.MAINTENANCE && v.getStatus() != VehicleStatus.OUT_OF_SERVICE)
                .toList();
        if (activeVehicles.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.NO_ACTIVE_VEHICLE,
                    "No available vehicles in fleet (all are in use, under maintenance, or out of service).",
                    HttpStatus.SERVICE_UNAVAILABLE);
        }

        User validator = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found: " + currentUsername,
                        HttpStatus.NOT_FOUND));

        List<EligibleVehicleDto> eligibleVehicles = new ArrayList<>();
        List<IneligibleVehicleDto> ineligibleVehicles = new ArrayList<>();
        List<Order> draftOrders = orderRepository.findByTripDraftId(draft.getId());

        for (Vehicle v : activeVehicles) {
            if (v.getMaxVolumeM3() == null || v.getPayloadKg() == null) {
                log.warn("Vehicle {} missing capacity data — skipped in validation", v.getId());
                continue;
            }

            BigDecimal effectiveMaxVolume = v.getMaxVolumeM3().multiply(SAFETY_BUFFER);
            BigDecimal effectiveMaxWeight = v.getPayloadKg().multiply(SAFETY_BUFFER);

            boolean volumeOk = draft.getTotalVolumeM3().compareTo(effectiveMaxVolume) <= 0;
            boolean weightOk = draft.getTotalWeightKg().compareTo(effectiveMaxWeight) <= 0;

            // Route capacity limits: Max Allowed Vehicle Weight
            boolean weightAllowedOnRoute = true;
            String routeWeightViolationReason = null;
            for (TripDraftStop stop : draft.getStops()) {
                if (stop.getIsActive()) {
                    Store store = stop.getStore();
                    if (store.getMaxAllowedVehicleWeight() != null && v.getPayloadKg().compareTo(store.getMaxAllowedVehicleWeight()) > 0) {
                        weightAllowedOnRoute = false;
                        routeWeightViolationReason = "Vehicle weight (" + v.getPayloadKg() + " kg) exceeds store " + store.getCode() + " limit (" + store.getMaxAllowedVehicleWeight() + " kg)";
                        break;
                    }
                }
            }

            // Delivery Time Window: ETA check
            boolean etaAllowed = true;
            String etaViolationReason = null;
            for (TripDraftStop stop : draft.getStops()) {
                String violation = constraintValidationService.validateStopEta(stop, draftOrders);
                if (violation != null) {
                    etaAllowed = false;
                    etaViolationReason = violation;
                    break;
                }
            }

            if (volumeOk && weightOk && weightAllowedOnRoute && etaAllowed) {
                eligibleVehicles.add(EligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getPayloadKg())
                        .remainingVolumeM3(v.getMaxVolumeM3().subtract(draft.getTotalVolumeM3()))
                        .remainingWeightKg(v.getPayloadKg().subtract(draft.getTotalWeightKg()))
                        .build());
            } else {
                StringBuilder reason = new StringBuilder();
                if (!volumeOk) {
                    reason.append("Volume exceeds safety limit (")
                          .append(draft.getTotalVolumeM3()).append(" m³ > ").append(effectiveMaxVolume.setScale(3, RoundingMode.HALF_UP)).append(" m³)");
                }
                if (!weightOk) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append("Weight exceeds safety limit (")
                          .append(draft.getTotalWeightKg()).append(" kg > ").append(effectiveMaxWeight.setScale(2, RoundingMode.HALF_UP)).append(" kg)");
                }
                if (!weightAllowedOnRoute) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append(routeWeightViolationReason);
                }
                if (!etaAllowed) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append(etaViolationReason);
                }

                ineligibleVehicles.add(IneligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getPayloadKg())
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
                    .anyMatch(v -> v.getMaxVolumeM3() != null && draft.getTotalVolumeM3().compareTo(v.getMaxVolumeM3().multiply(SAFETY_BUFFER)) <= 0);
            boolean anyWeightOk = activeVehicles.stream()
                    .anyMatch(v -> v.getPayloadKg() != null && draft.getTotalWeightKg().compareTo(v.getPayloadKg().multiply(SAFETY_BUFFER)) <= 0);

            overallVolume = anyVolumeOk ? ConstraintResult.PASS : ConstraintResult.FAIL;
            overallWeight = anyWeightOk ? ConstraintResult.PASS : ConstraintResult.FAIL;

            // ELOG-139: no single vehicle fits — ask Recommendation Engine if a two-vehicle
            // split is feasible (BR-07) before giving up.
            boolean twoVehicleFeasible = recommendationService.isTwoVehicleFeasible(tripDraftId);

            if (twoVehicleFeasible) {
                newStatus = "VALIDATED";
                suggestion = "No single vehicle can accommodate this load, but a two-vehicle split is feasible. "
                        + "Proceed to Vehicle Assignment (Split) to assign 2 vehicles (BR-07).";
            } else {
                newStatus = "PLANNED";

                if (overallVolume == ConstraintResult.FAIL && overallWeight == ConstraintResult.PASS) {
                    bindingConstraint = "VOLUME";
                } else if (overallVolume == ConstraintResult.PASS && overallWeight == ConstraintResult.FAIL) {
                    bindingConstraint = "WEIGHT";
                } else if (overallVolume == ConstraintResult.FAIL && overallWeight == ConstraintResult.FAIL) {
                    bindingConstraint = "BOTH";
                } else {
                    boolean hasEtaViolation = ineligibleVehicles.stream().anyMatch(v -> v.getFailureReason() != null && (v.getFailureReason().contains("Planned ETA") || v.getFailureReason().contains("violates delivery window") || v.getFailureReason().contains("allowed delivery hours") || v.getFailureReason().contains("exceeds closing time")));
                    if (hasEtaViolation) {
                        bindingConstraint = "TIME_WINDOW";
                    } else {
                        bindingConstraint = "ROUTE_CONSTRAINT";
                    }
                }

                suggestion = "No single vehicle can accommodate this load, and no two-vehicle split is feasible either. "
                        + "Consider checking route weight/ETA limits or fleet availability.";
            }
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

        String message;
        if ("VALIDATED".equals(newStatus)) {
            message = eligibleVehicles.isEmpty()
                    ? "Capacity validation passed via two-vehicle split. Proceed to Vehicle Assignment (Split) — BR-07."
                    : "Capacity validation passed. " + eligibleVehicles.size() + " eligible vehicles available. Proceed to Vehicle Assignment.";
        } else {
            message = "Capacity validation failed. Total volume " + draft.getTotalVolumeM3()
                    + " m³ exceeds all available vehicle capacities or violates route limits, and no two-vehicle split is feasible either.";
        }

        return CapacityValidationResultResponse.builder()
                .tripDraftId(draft.getId())
                .fixedRouteCode(draft.getRoute().getCode())
                .deliveryDate(draft.getDeliveryDate().toString())
                .newStatus(newStatus)
                .totalVolumeM3(draft.getTotalVolumeM3())
                .totalWeightKg(draft.getTotalWeightKg())
                .validationPassed("VALIDATED".equals(newStatus))
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

        List<Vehicle> activeVehicles = vehicleRepo.findByIsActiveTrue().stream()
                .filter(v -> v.getStatus() != VehicleStatus.MAINTENANCE && v.getStatus() != VehicleStatus.OUT_OF_SERVICE)
                .toList();
        List<EligibleVehicleDto> eligibleVehicles = new ArrayList<>();
        List<IneligibleVehicleDto> ineligibleVehicles = new ArrayList<>();
        List<Order> draftOrders = orderRepository.findByTripDraftId(draft.getId());

        boolean validationPassed = "VALIDATED".equals(draft.getStatus());

        if (draft.getVolumeCheckResult() != ConstraintResult.NOT_CHECKED && !activeVehicles.isEmpty()) {
            for (Vehicle v : activeVehicles) {
                if (v.getMaxVolumeM3() == null || v.getPayloadKg() == null) {
                    continue;
                }

                BigDecimal effectiveMaxVolume = v.getMaxVolumeM3().multiply(SAFETY_BUFFER);
                BigDecimal effectiveMaxWeight = v.getPayloadKg().multiply(SAFETY_BUFFER);

                boolean volumeOk = draft.getTotalVolumeM3().compareTo(effectiveMaxVolume) <= 0;
                boolean weightOk = draft.getTotalWeightKg().compareTo(effectiveMaxWeight) <= 0;

                // Route capacity limits: Max Allowed Vehicle Weight
                boolean weightAllowedOnRoute = true;
                String routeWeightViolationReason = null;
                for (TripDraftStop stop : draft.getStops()) {
                    if (stop.getIsActive()) {
                        Store store = stop.getStore();
                        if (store.getMaxAllowedVehicleWeight() != null && v.getPayloadKg().compareTo(store.getMaxAllowedVehicleWeight()) > 0) {
                            weightAllowedOnRoute = false;
                            routeWeightViolationReason = "Vehicle weight (" + v.getPayloadKg() + " kg) exceeds store " + store.getCode() + " limit (" + store.getMaxAllowedVehicleWeight() + " kg)";
                            break;
                        }
                    }
                }

                // Delivery Time Window: ETA check
                boolean etaAllowed = true;
                String etaViolationReason = null;
                for (TripDraftStop stop : draft.getStops()) {
                    String violation = constraintValidationService.validateStopEta(stop, draftOrders);
                    if (violation != null) {
                        etaAllowed = false;
                        etaViolationReason = violation;
                        break;
                    }
                }

                if (volumeOk && weightOk && weightAllowedOnRoute && etaAllowed) {
                    eligibleVehicles.add(EligibleVehicleDto.builder()
                            .vehicleId(v.getId())
                            .plateNumber(v.getPlateNumber())
                            .vehicleType(v.getVehicleType())
                            .maxVolumeM3(v.getMaxVolumeM3())
                            .maxWeightKg(v.getPayloadKg())
                            .remainingVolumeM3(v.getMaxVolumeM3().subtract(draft.getTotalVolumeM3()))
                            .remainingWeightKg(v.getPayloadKg().subtract(draft.getTotalWeightKg()))
                            .build());
                } else {
                    StringBuilder reason = new StringBuilder();
                    if (!volumeOk) {
                        reason.append("Volume exceeds safety limit (")
                              .append(draft.getTotalVolumeM3()).append(" m³ > ").append(effectiveMaxVolume.setScale(3, RoundingMode.HALF_UP)).append(" m³)");
                    }
                    if (!weightOk) {
                        if (reason.length() > 0) reason.append(" and ");
                        reason.append("Weight exceeds safety limit (")
                              .append(draft.getTotalWeightKg()).append(" kg > ").append(effectiveMaxWeight.setScale(2, RoundingMode.HALF_UP)).append(" kg)");
                    }
                    if (!weightAllowedOnRoute) {
                        if (reason.length() > 0) reason.append(" and ");
                        reason.append(routeWeightViolationReason);
                    }
                    if (!etaAllowed) {
                        if (reason.length() > 0) reason.append(" and ");
                        reason.append(etaViolationReason);
                    }

                    ineligibleVehicles.add(IneligibleVehicleDto.builder()
                            .vehicleId(v.getId())
                            .plateNumber(v.getPlateNumber())
                            .vehicleType(v.getVehicleType())
                            .maxVolumeM3(v.getMaxVolumeM3())
                            .maxWeightKg(v.getPayloadKg())
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
            } else if (draft.getVolumeCheckResult() == ConstraintResult.FAIL && draft.getWeightCheckResult() == ConstraintResult.FAIL) {
                bindingConstraint = "BOTH";
            } else {
                boolean hasEtaViolation = ineligibleVehicles.stream().anyMatch(v -> v.getFailureReason() != null && (v.getFailureReason().contains("Planned ETA") || v.getFailureReason().contains("violates delivery window") || v.getFailureReason().contains("allowed delivery hours") || v.getFailureReason().contains("exceeds closing time")));
                if (hasEtaViolation) {
                    bindingConstraint = "TIME_WINDOW";
                } else {
                    bindingConstraint = "ROUTE_CONSTRAINT";
                }
            }

            if (validationPassed) {
                suggestion = "No single vehicle can accommodate this load, but a two-vehicle split is feasible. Proceed to Vehicle Assignment (Split) to assign 2 vehicles (BR-07).";
            } else {
                suggestion = "No single vehicle can accommodate this load, and no two-vehicle split is feasible either. Consider checking route weight/ETA limits or fleet availability.";
            }
        }

        ConfirmedByDto validatorDto = null;
        if (draft.getValidatedBy() != null) {
            validatorDto = ConfirmedByDto.builder()
                    .userId(draft.getValidatedBy().getId())
                    .fullName(draft.getValidatedBy().getFullName())
                    .build();
        }

        String message;
        if (validationPassed) {
            message = eligibleVehicles.isEmpty()
                    ? "Capacity validation passed via two-vehicle split. Proceed to Vehicle Assignment (Split) — BR-07."
                    : "Capacity validation passed. " + eligibleVehicles.size() + " eligible vehicles available. Proceed to Vehicle Assignment.";
        } else {
            message = (draft.getVolumeCheckResult() == ConstraintResult.NOT_CHECKED)
                    ? "Chưa có kết quả kiểm tra. Bấm Kiểm tra để bắt đầu."
                    : "Capacity validation failed. Total volume " + draft.getTotalVolumeM3() + " m³ exceeds all available vehicle capacities or violates route limits, and no two-vehicle split is feasible either.";
        }

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


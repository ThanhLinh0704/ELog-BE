package com.elog.service.impl;

import com.elog.dto.response.RecommendationResultResponse;
import com.elog.dto.response.VehicleRecommendationResponse;
import com.elog.dto.response.VehicleRecommendationResponse.RecommendedVehicleDto;
import com.elog.dto.response.VehicleRecommendationResponse.SubTripDto;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.ConstraintValidationService;
import com.elog.service.RecommendationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Recommendation Engine — FT-06 (Single-Vehicle) + FT-07 (Two-Vehicle Fallback).
 * <p>
 * Flow: recommendTop3() → try single-vehicle → if empty → fallback two-vehicle → if empty → NO_PLAN.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {

    // ── Constants ─────────────────────────────────────────────────────────────
    private static final BigDecimal SAFETY_BUFFER = new BigDecimal("0.90");
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final BigDecimal MULTI_VEHICLE_PENALTY = new BigDecimal("15.0");
    private static final int TOP_N = 3;

    // Soft-scoring weights (SRS v2.5.0 Baseline: 70% Capacity Utilization + 30% Operating Cost Efficiency)
    private static final double W_CAPACITY = 0.70;
    private static final double W_COST = 0.30;

    // ── Dependencies ──────────────────────────────────────────────────────────
    private final TripDraftRepository tripDraftRepo;
    private final TripDraftStopRepository tripDraftStopRepo;
    private final VehicleRepository vehicleRepo;
    private final TripRepository tripRepo;
    private final UserRepository userRepo;
    private final OrderRepository orderRepo;
    private final SystemConfigRepository configRepo;
    private final ConstraintValidationService constraintValidationService;
    private final TripExecutionRepository tripExecutionRepo;
    private final com.elog.service.PlanningHistoryService planningHistoryService;

    // ── Internal helper classes ───────────────────────────────────────────────

    /** Per-stop cargo summary for splitting. */
    private record StopCargo(TripDraftStop stop, BigDecimal volumeM3, BigDecimal weightKg) {}

    /** Paired driver resolution result. */
    private record PairedDriverInfo(User driver, boolean isTemporary) {}

    /** Scored candidate for single-vehicle. */
    private record ScoredVehicle(Vehicle vehicle, BigDecimal score, String explanation,
                                 PairedDriverInfo driverInfo) {}

    /** Scored candidate for two-vehicle pair. */
    private record ScoredPair(Vehicle vehicleA, Vehicle vehicleB,
                              PairedDriverInfo driverInfoA, PairedDriverInfo driverInfoB,
                              int splitPoint,
                              List<StopCargo> subA, List<StopCargo> subB,
                              BigDecimal scoreA, BigDecimal scoreB,
                              BigDecimal pairScore,
                              String explanation) {}

    // ══════════════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public RecommendationResultResponse recommendTop3(Long tripDraftId) {
        TripDraft draft = tripDraftRepo.findById(tripDraftId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found: " + tripDraftId,
                        HttpStatus.NOT_FOUND));

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.RECOMMENDATION_RUN,
                com.elog.entity.PlanningActorType.RECOMMENDATION_ENGINE, null,
                draft.getStatus(), draft.getStatus(), "Khởi chạy thuật toán đề xuất xe", null, null, null, null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        List<TripDraftStop> activeStops = tripDraftStopRepo
                .findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);

        if (activeStops.isEmpty()) {
            return RecommendationResultResponse.builder()
                    .tripDraftId(tripDraftId)
                    .planType("NO_PLAN")
                    .recommendations(List.of())
                    .message("Trip Draft has no active stops.")
                    .build();
        }

        // ── Step 1: Try single-vehicle ────────────────────────────────────
        List<ScoredVehicle> singleResults = recommendSingleVehicle(draft, activeStops);

        if (!singleResults.isEmpty()) {
            List<VehicleRecommendationResponse> top3 = singleResults.stream()
                    .limit(TOP_N)
                    .map(sv -> toSingleVehicleResponse(sv, draft))
                    .toList();

            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    tripDraftId, null, com.elog.entity.PlanningEventType.RECOMMENDATION_LIST_GENERATED,
                    com.elog.entity.PlanningActorType.RECOMMENDATION_ENGINE, null,
                    draft.getStatus(), draft.getStatus(), "Sinh " + top3.size() + " phương án 1 xe đề xuất", top3, null, null, "SINGLE_VEHICLE",
                    draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

            return RecommendationResultResponse.builder()
                    .tripDraftId(tripDraftId)
                    .planType("SINGLE_VEHICLE")
                    .recommendations(top3)
                    .message("Tìm thấy " + singleResults.size() + " phương án 1 xe khả thi, hiển thị Top-"
                            + Math.min(TOP_N, singleResults.size()) + ".")
                    .build();
        }

        // ── Step 2: Fallback two-vehicle ──────────────────────────────────
        log.warn("No single vehicle fits trip draft {}, triggering 2-vehicle fallback", tripDraftId);

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.SINGLE_VEHICLE_NOT_FOUND,
                com.elog.entity.PlanningActorType.RECOMMENDATION_ENGINE, null,
                draft.getStatus(), draft.getStatus(), "Không tìm thấy phương án 1 xe thỏa mãn tải trọng", null, null, null, null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, null, com.elog.entity.PlanningEventType.TWO_VEHICLE_FALLBACK_TRIGGERED,
                com.elog.entity.PlanningActorType.RECOMMENDATION_ENGINE, null,
                draft.getStatus(), draft.getStatus(), "Kích hoạt thuật toán chia tải đề xuất 2 xe", null, null, null, null,
                draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

        List<ScoredPair> pairResults = recommendTwoVehicles(draft, activeStops);

        if (!pairResults.isEmpty()) {
            List<VehicleRecommendationResponse> top3 = pairResults.stream()
                    .limit(TOP_N)
                    .map(sp -> toTwoVehicleResponse(sp))
                    .toList();

            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    tripDraftId, null, com.elog.entity.PlanningEventType.RECOMMENDATION_LIST_GENERATED,
                    com.elog.entity.PlanningActorType.RECOMMENDATION_ENGINE, null,
                    draft.getStatus(), draft.getStatus(), "Sinh " + top3.size() + " phương án 2 xe đề xuất", top3, null, null, "TWO_VEHICLE",
                    draft.getRoute() != null ? draft.getRoute().getCode() : null, draft.getDeliveryDate()));

            return RecommendationResultResponse.builder()
                    .tripDraftId(tripDraftId)
                    .planType("TWO_VEHICLE")
                    .recommendations(top3)
                    .message("Không có xe đơn lẻ phù hợp. Tìm thấy " + pairResults.size()
                            + " phương án 2 xe, hiển thị Top-" + Math.min(TOP_N, pairResults.size()) + ".")
                    .build();
        }

        // ── Step 3: No feasible plan ──────────────────────────────────────
        List<String> reasons = collectInfeasibilityReasons(draft, activeStops);
        return RecommendationResultResponse.builder()
                .tripDraftId(tripDraftId)
                .planType("NO_PLAN")
                .recommendations(List.of())
                .message("Không có phương án khả thi cho Trip Draft " + tripDraftId + ".")
                .violatedConstraints(reasons)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SINGLE-VEHICLE ENGINE (FT-06)
    // ══════════════════════════════════════════════════════════════════════════

    private List<ScoredVehicle> recommendSingleVehicle(TripDraft draft, List<TripDraftStop> stops) {
        List<Vehicle> allVehicles = vehicleRepo.findByIsActiveTrue();
        LocalDate deliveryDate = draft.getDeliveryDate();
        List<Store> storesOnRoute = stops.stream().map(TripDraftStop::getStore).toList();

        // Pre-load available drivers
        List<User> allDrivers = findActiveDrivers();
        List<TripStatus> busyStatuses = List.of(TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);

        // Compute max cost/speed across fleet for normalization
        double maxCostPerKm = allVehicles.stream()
                .map(Vehicle::getCostPerKm)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .max().orElse(1.0);
        double maxSpeedKmh = allVehicles.stream()
                .map(Vehicle::getAverageSpeedKmh)
                .filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue)
                .max().orElse(1.0);

        List<ScoredVehicle> candidates = new ArrayList<>();

        for (Vehicle v : allVehicles) {
            // ── Tier 1: Hard Constraints ──────────────────────────────────
            List<String> hardFails = checkHardConstraints(v, draft, stops, storesOnRoute, deliveryDate, busyStatuses);
            if (!hardFails.isEmpty()) {
                continue;
            }

            // Resolve driver pairing (assigned driver first, fallback to temporary driver)
            PairedDriverInfo driverInfo = resolveDriverForVehicle(v, deliveryDate, allDrivers, busyStatuses);
            if (driverInfo == null) {
                continue; // No driver with compatible license available
            }

            // ── Tier 2: Soft Scoring ──────────────────────────────────────
            List<User> eligibleDrivers = List.of(driverInfo.driver());
            BigDecimal score = calculateSoftScore(v, draft, stops, maxCostPerKm, maxSpeedKmh, eligibleDrivers);
            String explanation = buildSingleExplanation(v, draft, score);

            candidates.add(new ScoredVehicle(v, score, explanation, driverInfo));
        }

        // Sort descending by score
        candidates.sort(Comparator.comparing(ScoredVehicle::score).reversed());
        return candidates;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TWO-VEHICLE ENGINE (FT-07)
    // ══════════════════════════════════════════════════════════════════════════

    private List<ScoredPair> recommendTwoVehicles(TripDraft draft, List<TripDraftStop> stops) {
        // Build per-stop cargo data
        List<StopCargo> stopCargos = buildStopCargos(draft.getId(), stops);

        // Find valid split points (store integrity: don't split same store across sub-trips)
        List<Integer> validSplitPoints = findValidSplitPoints(stopCargos);

        if (validSplitPoints.isEmpty()) {
            log.warn("No valid split points for trip draft {}", draft.getId());
            return List.of();
        }

        List<Vehicle> allVehicles = vehicleRepo.findByIsActiveTrue();
        LocalDate deliveryDate = draft.getDeliveryDate();
        List<User> allDrivers = findActiveDrivers();
        List<TripStatus> busyStatuses = List.of(TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);

        // Normalization params
        double maxCostPerKm = allVehicles.stream()
                .map(Vehicle::getCostPerKm).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).max().orElse(1.0);
        double maxSpeedKmh = allVehicles.stream()
                .map(Vehicle::getAverageSpeedKmh).filter(Objects::nonNull)
                .mapToDouble(BigDecimal::doubleValue).max().orElse(1.0);

        List<ScoredPair> allPairs = new ArrayList<>();

        for (int k : validSplitPoints) {
            List<StopCargo> subA = stopCargos.subList(0, k);
            List<StopCargo> subB = stopCargos.subList(k, stopCargos.size());

            BigDecimal subAVolume = sumVolume(subA);
            BigDecimal subAWeight = sumWeight(subA);
            BigDecimal subBVolume = sumVolume(subB);
            BigDecimal subBWeight = sumWeight(subB);

            List<TripDraftStop> stopsA = subA.stream().map(StopCargo::stop).toList();
            List<TripDraftStop> stopsB = subB.stream().map(StopCargo::stop).toList();
            List<Store> storesA = stopsA.stream().map(TripDraftStop::getStore).toList();
            List<Store> storesB = stopsB.stream().map(TripDraftStop::getStore).toList();

            // Find vehicles that pass hard constraints for subA
            List<Vehicle> candidatesA = new ArrayList<>();
            for (Vehicle v : allVehicles) {
                if (passesHardConstraintsForSub(v, subAVolume, subAWeight, stopsA, storesA, deliveryDate, busyStatuses)) {
                    candidatesA.add(v);
                }
            }

            // Find vehicles that pass hard constraints for subB
            List<Vehicle> candidatesB = new ArrayList<>();
            for (Vehicle v : allVehicles) {
                if (passesHardConstraintsForSub(v, subBVolume, subBWeight, stopsB, storesB, deliveryDate, busyStatuses)) {
                    candidatesB.add(v);
                }
            }

            // Pair up VA != VB, both must have eligible drivers
            for (Vehicle va : candidatesA) {
                PairedDriverInfo driverInfoA = resolveDriverForVehicle(va, deliveryDate, allDrivers, busyStatuses);
                if (driverInfoA == null) continue;

                for (Vehicle vb : candidatesB) {
                    if (va.getId().equals(vb.getId())) continue;

                    PairedDriverInfo driverInfoB = resolveDriverForVehicle(vb, deliveryDate, allDrivers, busyStatuses);
                    if (driverInfoB == null) continue;

                    // Cannot assign same driver to both sub-trips
                    if (driverInfoA.driver().getId().equals(driverInfoB.driver().getId())) continue;

                    // Score each sub-trip
                    BigDecimal scoreA = calculateSoftScoreForSub(va, subAVolume, subAWeight, stopsA,
                            maxCostPerKm, maxSpeedKmh, List.of(driverInfoA.driver()));
                    BigDecimal scoreB = calculateSoftScoreForSub(vb, subBVolume, subBWeight, stopsB,
                            maxCostPerKm, maxSpeedKmh, List.of(driverInfoB.driver()));

                    // PairScore = (scoreA + scoreB) / 2 - penalty
                    BigDecimal pairScore = scoreA.add(scoreB)
                            .divide(BigDecimal.valueOf(2), 2, RoundingMode.HALF_UP)
                            .subtract(MULTI_VEHICLE_PENALTY);

                    String explanation = String.format(
                            "Cặp xe %s + %s | Chia tại điểm dừng %d | ScoreA=%.1f, ScoreB=%.1f, Penalty=%.1f",
                            va.getPlateNumber(), vb.getPlateNumber(), k,
                            scoreA.doubleValue(), scoreB.doubleValue(), MULTI_VEHICLE_PENALTY.doubleValue());

                    allPairs.add(new ScoredPair(va, vb, driverInfoA, driverInfoB, k, subA, subB, scoreA, scoreB, pairScore, explanation));
                }
            }
        }

        // Sort descending by pairScore, take top-N
        allPairs.sort(Comparator.comparing(ScoredPair::pairScore).reversed());
        return allPairs.stream().limit(TOP_N * 2L).toList(); // keep more for dedup, then limit at response
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HARD CONSTRAINTS CHECK (Tier 1 — FT-05)
    // ══════════════════════════════════════════════════════════════════════════

    private List<String> checkHardConstraints(Vehicle v, TripDraft draft,
                                              List<TripDraftStop> stops, List<Store> stores,
                                              LocalDate deliveryDate, List<TripStatus> busyStatuses) {
        List<String> fails = new ArrayList<>();

        // HC-1: Vehicle status
        if (v.getStatus() != VehicleStatus.AVAILABLE) {
            fails.add("Vehicle status is " + v.getStatus() + ", not AVAILABLE");
            return fails; // fast-fail
        }

        // HC-2: Schedule conflict & unreturned check
        if (tripRepo.existsByVehicleIdAndDeliveryDateAndStatusIn(v.getId(), deliveryDate, busyStatuses)) {
            fails.add("Vehicle has conflicting trip on " + deliveryDate);
            return fails;
        }

        if (!tripExecutionRepo.findUnreturnedByVehicleId(v.getId()).isEmpty()) {
            fails.add("Vehicle is IN_USE and has not confirmed return to warehouse yet");
            return fails;
        }

        // HC-3: Dual capacity with safety buffer
        BigDecimal safetyBuffer = getSafetyBufferRatio();
        BigDecimal effectiveVolume = v.getMaxVolumeM3().multiply(safetyBuffer);
        BigDecimal effectiveWeight = v.getPayloadKg().multiply(safetyBuffer);

        if (draft.getTotalVolumeM3().compareTo(effectiveVolume) > 0) {
            fails.add("Volume " + draft.getTotalVolumeM3() + " m³ > effective capacity "
                    + effectiveVolume + " m³ (" + safetyBuffer.multiply(BigDecimal.valueOf(100)) + "% of " + v.getMaxVolumeM3() + ")");
        }
        if (draft.getTotalWeightKg().compareTo(effectiveWeight) > 0) {
            fails.add("Weight " + draft.getTotalWeightKg() + " kg > effective capacity "
                    + effectiveWeight + " kg (" + safetyBuffer.multiply(BigDecimal.valueOf(100)) + "% of " + v.getPayloadKg() + ")");
        }
        if (!fails.isEmpty()) return fails;

        // HC-4: Store infrastructure constraints
        List<String> storeViolations = constraintValidationService.validateTripVehicleStops(stores, v);
        if (!storeViolations.isEmpty()) {
            fails.addAll(storeViolations);
            return fails;
        }

        // HC-5: Time window check (ETA_i <= store_i.closingTime)
        for (TripDraftStop stop : stops) {
            if (stop.getPlannedEta() != null && stop.getStore().getTimeWindowEnd() != null) {
                LocalTime eta = stop.getPlannedEta().toLocalTime();
                LocalTime closing = stop.getStore().getTimeWindowEnd();
                if (eta.isAfter(closing)) {
                    fails.add("ETA " + eta + " exceeds closing time " + closing
                            + " at store " + stop.getStore().getCode());
                }
            }
        }

        return fails;
    }

    /**
     * Simplified hard constraints check for a sub-trip (two-vehicle splitting).
     */
    private boolean passesHardConstraintsForSub(Vehicle v, BigDecimal subVolume, BigDecimal subWeight,
                                                 List<TripDraftStop> subStops, List<Store> subStores,
                                                 LocalDate deliveryDate, List<TripStatus> busyStatuses) {
        if (v.getStatus() != VehicleStatus.AVAILABLE) return false;
        if (tripRepo.existsByVehicleIdAndDeliveryDateAndStatusIn(v.getId(), deliveryDate, busyStatuses)) return false;
        if (!tripExecutionRepo.findUnreturnedByVehicleId(v.getId()).isEmpty()) return false;

        BigDecimal safetyBuffer = getSafetyBufferRatio();
        BigDecimal effectiveVolume = v.getMaxVolumeM3().multiply(safetyBuffer);
        BigDecimal effectiveWeight = v.getPayloadKg().multiply(safetyBuffer);
        if (subVolume.compareTo(effectiveVolume) > 0) return false;
        if (subWeight.compareTo(effectiveWeight) > 0) return false;

        List<String> storeViolations = constraintValidationService.validateTripVehicleStops(subStores, v);
        if (!storeViolations.isEmpty()) return false;

        // Time window check for sub-stops
        for (TripDraftStop stop : subStops) {
            if (stop.getPlannedEta() != null && stop.getStore().getTimeWindowEnd() != null) {
                if (stop.getPlannedEta().toLocalTime().isAfter(stop.getStore().getTimeWindowEnd())) {
                    return false;
                }
            }
        }

        return true;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SOFT SCORING (Tier 2 — FT-12)
    // ══════════════════════════════════════════════════════════════════════════

    private BigDecimal calculateSoftScore(Vehicle v, TripDraft draft, List<TripDraftStop> stops,
                                           double maxCostPerKm, double maxSpeedKmh,
                                           List<User> eligibleDrivers) {
        return calculateSoftScoreForSub(v,
                draft.getTotalVolumeM3(), draft.getTotalWeightKg(),
                stops, maxCostPerKm, maxSpeedKmh, eligibleDrivers);
    }

    private BigDecimal calculateSoftScoreForSub(Vehicle v, BigDecimal totalVolume, BigDecimal totalWeight,
                                                 List<TripDraftStop> stops,
                                                 double maxCostPerKm, double maxSpeedKmh,
                                                 List<User> eligibleDrivers) {
        // S_capacity: fill ratio (higher = better). Average of volume fill + weight fill.
        double volumeFill = safeDiv(totalVolume, v.getMaxVolumeM3());
        double weightFill = safeDiv(totalWeight, v.getPayloadKg());
        double sCapacity = ((volumeFill + weightFill) / 2.0) * 100.0;

        // S_cost: cheaper is better. Inverse normalized.
        double costPerKm = v.getCostPerKm() != null ? v.getCostPerKm().doubleValue() : maxCostPerKm;
        double sCost = maxCostPerKm > 0 ? (1.0 - costPerKm / maxCostPerKm) * 100.0 : 50.0;

        // S_speed: faster is better. Normalized.
        double speedKmh = v.getAverageSpeedKmh() != null ? v.getAverageSpeedKmh().doubleValue() : 0;
        double sSpeed = maxSpeedKmh > 0 ? (speedKmh / maxSpeedKmh) * 100.0 : 50.0;

        // S_driver: has eligible driver = 100, else 0 (already filtered, so always 100 here)
        double sDriver = eligibleDrivers.isEmpty() ? 0.0 : 100.0;

        // S_time: slack time between last stop ETA and closing time (more slack = better)
        double sTime = calculateSlackScore(stops);

        double totalScore = W_CAPACITY * sCapacity + W_COST * sCost;

        return BigDecimal.valueOf(totalScore).setScale(2, RoundingMode.HALF_UP);
    }

    private double calculateSlackScore(List<TripDraftStop> stops) {
        if (stops.isEmpty()) return 50.0;

        double totalSlackMinutes = 0;
        int countWithWindow = 0;

        for (TripDraftStop stop : stops) {
            if (stop.getPlannedEta() != null && stop.getStore().getTimeWindowEnd() != null) {
                LocalTime eta = stop.getPlannedEta().toLocalTime();
                LocalTime closing = stop.getStore().getTimeWindowEnd();
                long slackMin = java.time.Duration.between(eta, closing).toMinutes();
                totalSlackMinutes += Math.max(0, slackMin);
                countWithWindow++;
            }
        }

        if (countWithWindow == 0) return 50.0; // neutral if no time windows
        double avgSlack = totalSlackMinutes / countWithWindow;
        // Normalize: 0 min slack → 0 score, 120+ min → 100
        return Math.min(100.0, (avgSlack / 120.0) * 100.0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DRIVER ELIGIBILITY
    // ══════════════════════════════════════════════════════════════════════════

    private List<User> findActiveDrivers() {
        return userRepo.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive())
                        && u.getDriverStatus() == DriverStatus.ACTIVE
                        && u.getRoles().stream().anyMatch(r -> "DRIVER".equals(r.getName())))
                .toList();
    }

    private List<User> findEligibleDrivers(Vehicle v, LocalDate date,
                                            List<User> allDrivers, List<TripStatus> busyStatuses) {
        return allDrivers.stream()
                .filter(d -> {
                    // License check
                    if (v.getRequiredLicense() != null) {
                        if (d.getLicenseClass() == null
                                || d.getLicenseClass().ordinal() < v.getRequiredLicense().ordinal()) {
                            return false;
                        }
                    }
                    // Schedule check
                    return !tripRepo.existsByDriverIdAndDeliveryDateAndStatusIn(d.getId(), date, busyStatuses);
                })
                .toList();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // TWO-VEHICLE SPLITTING HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Build per-stop cargo summaries by aggregating order items.
     */
    private List<StopCargo> buildStopCargos(Long tripDraftId, List<TripDraftStop> stops) {
        List<Order> orders = orderRepo.findByTripDraftId(tripDraftId);

        // Group order volume/weight by storeId
        Map<Long, BigDecimal> volumeByStore = new HashMap<>();
        Map<Long, BigDecimal> weightByStore = new HashMap<>();
        for (Order order : orders) {
            Long storeId = order.getStore().getId();
            BigDecimal orderVolume = BigDecimal.ZERO;
            BigDecimal orderWeight = BigDecimal.ZERO;
            for (OrderItem item : order.getItems()) {
                orderVolume = orderVolume.add(item.getLineVolumeM3());
                orderWeight = orderWeight.add(item.getLineWeightKg());
            }
            volumeByStore.merge(storeId, orderVolume, BigDecimal::add);
            weightByStore.merge(storeId, orderWeight, BigDecimal::add);
        }

        List<StopCargo> result = new ArrayList<>();
        for (TripDraftStop stop : stops) {
            Long storeId = stop.getStore().getId();
            BigDecimal vol = volumeByStore.getOrDefault(storeId, BigDecimal.ZERO);
            BigDecimal wgt = weightByStore.getOrDefault(storeId, BigDecimal.ZERO);
            result.add(new StopCargo(stop, vol, wgt));
        }
        return result;
    }

    /**
     * Find valid split points (1-indexed from 1 to N-1) that don't split orders
     * from the same store across two sub-trips (Store Integrity — FR-07-03).
     */
    private List<Integer> findValidSplitPoints(List<StopCargo> stopCargos) {
        List<Integer> validPoints = new ArrayList<>();
        Set<Long> seenStoreIds = new HashSet<>();

        for (int k = 1; k < stopCargos.size(); k++) {
            // Collect store IDs in subA (0..k-1) and subB (k..N-1)
            seenStoreIds.clear();
            boolean valid = true;

            // Store IDs in sub-trip A
            for (int i = 0; i < k; i++) {
                seenStoreIds.add(stopCargos.get(i).stop().getStore().getId());
            }

            // Check no store in sub-trip B appears in sub-trip A
            for (int j = k; j < stopCargos.size(); j++) {
                if (seenStoreIds.contains(stopCargos.get(j).stop().getStore().getId())) {
                    valid = false;
                    break;
                }
            }

            if (valid) {
                validPoints.add(k);
            }
        }

        return validPoints;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // INFEASIBILITY DIAGNOSTICS
    // ══════════════════════════════════════════════════════════════════════════

    private List<String> collectInfeasibilityReasons(TripDraft draft, List<TripDraftStop> stops) {
        List<String> reasons = new ArrayList<>();
        List<Vehicle> allVehicles = vehicleRepo.findByIsActiveTrue();

        if (allVehicles.isEmpty()) {
            reasons.add("Không có xe nào đang hoạt động trong đội xe.");
            return reasons;
        }

        // Find the largest vehicle capacity
        BigDecimal maxFleetVolume = allVehicles.stream()
                .map(v -> v.getMaxVolumeM3().multiply(SAFETY_BUFFER))
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);
        BigDecimal maxFleetWeight = allVehicles.stream()
                .map(v -> v.getPayloadKg().multiply(SAFETY_BUFFER))
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        if (draft.getTotalVolumeM3().compareTo(maxFleetVolume) > 0) {
            reasons.add("Tổng thể tích " + draft.getTotalVolumeM3() + " m³ vượt quá xe lớn nhất ("
                    + maxFleetVolume + " m³ @ 90% buffer).");
        }
        if (draft.getTotalWeightKg().compareTo(maxFleetWeight) > 0) {
            reasons.add("Tổng trọng lượng " + draft.getTotalWeightKg() + " kg vượt quá xe lớn nhất ("
                    + maxFleetWeight + " kg @ 90% buffer).");
        }

        // Check time window violations
        long lateStops = stops.stream()
                .filter(s -> "TIME_WINDOW_LATE".equals(s.getViolationCode()))
                .count();
        if (lateStops > 0) {
            reasons.add(lateStops + " điểm dừng có ETA vượt quá giờ đóng cửa (TIME_WINDOW_LATE).");
        }

        // Check driver availability
        List<User> drivers = findActiveDrivers();
        if (drivers.isEmpty()) {
            reasons.add("Không có tài xế nào đang rảnh.");
        }

        if (reasons.isEmpty()) {
            reasons.add("Không tìm được cặp xe + tài xế nào đáp ứng đồng thời tất cả ràng buộc.");
        }

        return reasons;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESPONSE BUILDERS
    // ══════════════════════════════════════════════════════════════════════════

    private PairedDriverInfo resolveDriverForVehicle(Vehicle v, LocalDate date, List<User> allDrivers, List<TripStatus> busyStatuses) {
        // Step 1: Try fixed assigned driver first
        User assigned = v.getAssignedDriver();
        if (assigned != null && Boolean.TRUE.equals(assigned.getIsActive())) {
            boolean licenseOk = (v.getRequiredLicense() == null) ||
                    (assigned.getLicenseClass() != null && assigned.getLicenseClass().ordinal() >= v.getRequiredLicense().ordinal());
            boolean busy = tripRepo.existsByDriverIdAndDeliveryDateAndStatusIn(assigned.getId(), date, busyStatuses)
                    || !tripExecutionRepo.findUnreturnedByDriverId(assigned.getId()).isEmpty();

            if (licenseOk && !busy) {
                return new PairedDriverInfo(assigned, false);
            }
        }

        // Step 2: Fallback to temporary substitute driver
        List<User> eligibleSubstitutes = findEligibleDrivers(v, date, allDrivers, busyStatuses);
        if (!eligibleSubstitutes.isEmpty()) {
            return new PairedDriverInfo(eligibleSubstitutes.get(0), true);
        }

        return null;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RESPONSE BUILDERS
    // ══════════════════════════════════════════════════════════════════════════

    private VehicleRecommendationResponse toSingleVehicleResponse(ScoredVehicle sv, TripDraft draft) {
        BigDecimal volPct = BigDecimal.valueOf(safeDiv(draft.getTotalVolumeM3(), sv.vehicle().getMaxVolumeM3()) * 100).setScale(1, RoundingMode.HALF_UP);
        BigDecimal wgtPct = BigDecimal.valueOf(safeDiv(draft.getTotalWeightKg(), sv.vehicle().getPayloadKg()) * 100).setScale(1, RoundingMode.HALF_UP);

        List<String> warnings = new ArrayList<>();
        if (Boolean.TRUE.equals(sv.driverInfo().isTemporary())) {
            warnings.add("Tài xế " + sv.driverInfo().driver().getFullName() + " là tài xế thay thế tạm thời cho xe " + sv.vehicle().getPlateNumber() + ".");
        }
        if (volPct.doubleValue() < 30.0 || wgtPct.doubleValue() < 30.0) {
            warnings.add("Tỷ lệ lấp đầy thấp (<30%).");
        }

        return VehicleRecommendationResponse.builder()
                .planType("SINGLE_VEHICLE")
                .vehicles(List.of(toVehicleDto(sv.vehicle(), sv.driverInfo())))
                .subTrips(null)
                .totalScore(sv.score())
                .explanation(sv.explanation())
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    private VehicleRecommendationResponse toTwoVehicleResponse(ScoredPair sp) {
        BigDecimal volPctA = BigDecimal.valueOf(safeDiv(sumVolume(sp.subA()), sp.vehicleA().getMaxVolumeM3()) * 100).setScale(1, RoundingMode.HALF_UP);
        BigDecimal wgtPctA = BigDecimal.valueOf(safeDiv(sumWeight(sp.subA()), sp.vehicleA().getPayloadKg()) * 100).setScale(1, RoundingMode.HALF_UP);

        List<String> warningsA = new ArrayList<>();
        if (Boolean.TRUE.equals(sp.driverInfoA().isTemporary())) {
            warningsA.add("Tài xế " + sp.driverInfoA().driver().getFullName() + " là tài xế thay thế tạm thời cho xe " + sp.vehicleA().getPlateNumber() + ".");
        }
        if (volPctA.doubleValue() < 30.0 || wgtPctA.doubleValue() < 30.0) {
            warningsA.add("Tỷ lệ lấp đầy thấp (<30%).");
        }

        SubTripDto subTripA = SubTripDto.builder()
                .label("Sub-trip A")
                .vehicleId(sp.vehicleA().getId())
                .stopSequenceNos(sp.subA().stream().map(sc -> sc.stop().getSequenceNo()).toList())
                .subTotalVolumeM3(sumVolume(sp.subA()))
                .subTotalWeightKg(sumWeight(sp.subA()))
                .volumeUtilizationPct(volPctA)
                .weightUtilizationPct(wgtPctA)
                .subScore(sp.scoreA())
                .warnings(warningsA.isEmpty() ? null : warningsA)
                .build();

        BigDecimal volPctB = BigDecimal.valueOf(safeDiv(sumVolume(sp.subB()), sp.vehicleB().getMaxVolumeM3()) * 100).setScale(1, RoundingMode.HALF_UP);
        BigDecimal wgtPctB = BigDecimal.valueOf(safeDiv(sumWeight(sp.subB()), sp.vehicleB().getPayloadKg()) * 100).setScale(1, RoundingMode.HALF_UP);

        List<String> warningsB = new ArrayList<>();
        if (Boolean.TRUE.equals(sp.driverInfoB().isTemporary())) {
            warningsB.add("Tài xế " + sp.driverInfoB().driver().getFullName() + " là tài xế thay thế tạm thời cho xe " + sp.vehicleB().getPlateNumber() + ".");
        }
        if (volPctB.doubleValue() < 30.0 || wgtPctB.doubleValue() < 30.0) {
            warningsB.add("Tỷ lệ lấp đầy thấp (<30%).");
        }

        SubTripDto subTripB = SubTripDto.builder()
                .label("Sub-trip B")
                .vehicleId(sp.vehicleB().getId())
                .stopSequenceNos(sp.subB().stream().map(sc -> sc.stop().getSequenceNo()).toList())
                .subTotalVolumeM3(sumVolume(sp.subB()))
                .subTotalWeightKg(sumWeight(sp.subB()))
                .volumeUtilizationPct(volPctB)
                .weightUtilizationPct(wgtPctB)
                .subScore(sp.scoreB())
                .warnings(warningsB.isEmpty() ? null : warningsB)
                .build();

        List<String> planWarnings = new ArrayList<>();
        if (!warningsA.isEmpty()) planWarnings.addAll(warningsA);
        if (!warningsB.isEmpty()) planWarnings.addAll(warningsB);

        return VehicleRecommendationResponse.builder()
                .planType("TWO_VEHICLE")
                .vehicles(List.of(toVehicleDto(sp.vehicleA(), sp.driverInfoA()), toVehicleDto(sp.vehicleB(), sp.driverInfoB())))
                .subTrips(List.of(subTripA, subTripB))
                .totalScore(sp.pairScore())
                .explanation(sp.explanation())
                .warnings(planWarnings.isEmpty() ? null : planWarnings)
                .build();
    }

    private RecommendedVehicleDto toVehicleDto(Vehicle v, PairedDriverInfo driverInfo) {
        User driver = driverInfo != null ? driverInfo.driver() : null;
        Boolean isTemp = driverInfo != null ? driverInfo.isTemporary() : null;

        return RecommendedVehicleDto.builder()
                .vehicleId(v.getId())
                .vehicleCode(v.getVehicleCode())
                .plateNumber(v.getPlateNumber())
                .vehicleType(v.getVehicleType())
                .payloadKg(v.getPayloadKg())
                .maxVolumeM3(v.getMaxVolumeM3())
                .costPerKm(v.getCostPerKm())
                .averageSpeedKmh(v.getAverageSpeedKmh())
                .driverId(driver != null ? driver.getId() : null)
                .driverName(driver != null ? driver.getFullName() : null)
                .driverPhone(driver != null ? driver.getEmail() : null)
                .driverLicenseClass(driver != null ? driver.getLicenseClass() : null)
                .isTemporaryDriver(isTemp)
                .build();
    }

    private String buildSingleExplanation(Vehicle v, TripDraft draft, BigDecimal score) {
        double volFill = safeDiv(draft.getTotalVolumeM3(), v.getMaxVolumeM3()) * 100;
        double wgtFill = safeDiv(draft.getTotalWeightKg(), v.getPayloadKg()) * 100;
        return String.format("Xe %s (%s) | Lấp đầy: %.0f%% thể tích, %.0f%% tải trọng | Điểm: %.1f",
                v.getPlateNumber(), v.getVehicleType(), volFill, wgtFill, score.doubleValue());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ══════════════════════════════════════════════════════════════════════════

    private double safeDiv(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) return 0;
        return numerator.divide(denominator, 4, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal sumVolume(List<StopCargo> cargos) {
        return cargos.stream().map(StopCargo::volumeM3).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumWeight(List<StopCargo> cargos) {
        return cargos.stream().map(StopCargo::weightKg).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getSafetyBufferRatio() {
        return configRepo.findByConfigKey("CAPACITY_SAFETY_BUFFER_RATIO")
                .map(c -> {
                    try {
                        return new BigDecimal(c.getConfigValue());
                    } catch (Exception e) {
                        return new BigDecimal("0.90");
                    }
                })
                .orElse(new BigDecimal("0.90"));
    }
}

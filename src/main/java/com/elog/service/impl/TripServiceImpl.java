package com.elog.service.impl;

import com.elog.dto.request.trip.CancelTripRequest;
import com.elog.dto.request.trip.TripAssignmentPatchRequest;
import com.elog.dto.request.trip.TripAssignRequest;
import com.elog.dto.request.trip.TripSplitAssignRequest;
import com.elog.dto.response.trip.TripResponse;
import com.elog.dto.response.trip.TripSplitResponse;
import com.elog.dto.response.trip.TripStopResponse;
import com.elog.dto.response.user.AvailableDriverResponse;
import com.elog.dto.response.user.DriverTripCalendarDayResponse;
import com.elog.dto.response.vehicle.EligibleVehicleDto;
import com.elog.dto.response.vehicle.EligibleVehiclesResponse;
import com.elog.dto.response.vehicle.FleetCapacityCheckResponse;
import com.elog.dto.response.vehicle.IneligibleVehicleDto;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.TripService;
import com.elog.service.TripStateMachine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripServiceImpl implements TripService {

    private static final BigDecimal SAFETY_BUFFER = BigDecimal.valueOf(0.90);

    private final TripRepository tripRepository;
    private final TripStopRepository tripStopRepository;
    private final TripDraftRepository tripDraftRepository;
    private final TripDraftStopRepository tripDraftStopRepository;
    private final VehicleRepository vehicleRepository;
    private final UserRepository userRepository;
    private final ManifestRepository manifestRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;
    private final TripStateMachine tripStateMachine;
    private final TripExecutionRepository tripExecutionRepository;
    private final com.elog.service.PlanningHistoryService planningHistoryService;
    private final com.elog.service.TripOutcomeHistoryService tripOutcomeHistoryService;
    private final com.elog.service.ConstraintValidationService constraintValidationService;
    private final SystemConfigRepository systemConfigRepository;

    // ── US-15 TASK-02 ──────────────────────────────────────────────

    private void validateAssignmentEligibility(TripDraft td) {
        boolean isValidated = "VALIDATED".equals(td.getStatus());
        boolean isPlannedAndChecked = "PLANNED".equals(td.getStatus())
                && td.getVolumeCheckResult() != ConstraintResult.NOT_CHECKED
                && !tripRepository.existsByTripDraftIdAndStatusNot(td.getId(), TripStatus.CANCELLED);

        if (!isValidated && !isPlannedAndChecked) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_NOT_VALIDATED,
                    "Trip Draft must be VALIDATED or PLANNED (with volume check completed) before assignment.", HttpStatus.BAD_REQUEST);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public EligibleVehiclesResponse getEligibleVehicles(Long tripDraftId) {
        TripDraft td = findTripDraftOrThrow(tripDraftId);
        validateAssignmentEligibility(td);

        return evaluateVehiclesForVolumeAndWeight(
                td.getTotalVolumeM3(), td.getTotalWeightKg(), td.getStops(), td.getDeliveryDate());
    }

    @Override
    @Transactional(readOnly = true)
    public EligibleVehiclesResponse getEligibleVehiclesForStops(Long tripDraftId, List<Long> stopIds) {
        TripDraft td = findTripDraftOrThrow(tripDraftId);
        validateAssignmentEligibility(td);

        if (stopIds == null || stopIds.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "stopIds không được để trống.", HttpStatus.BAD_REQUEST);
        }

        List<TripDraftStop> selectedStops = td.getStops().stream()
                .filter(s -> stopIds.contains(s.getId()))
                .toList();

        if (selectedStops.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "stopIds không hợp lệ hoặc không thuộc Trip Draft này.", HttpStatus.BAD_REQUEST);
        }

        BigDecimal groupVolume = BigDecimal.ZERO;
        BigDecimal groupWeight = BigDecimal.ZERO;
        for (TripDraftStop stop : selectedStops) {
            List<OrderItem> items = orderItemRepository.findByStopForManifest(
                    stop.getStore().getId(), tripDraftId);
            BigDecimal stopVolume = items.stream()
                    .map(OrderItem::getLineVolumeM3)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal stopWeight = items.stream()
                    .map(OrderItem::getLineWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            groupVolume = groupVolume.add(stopVolume);
            groupWeight = groupWeight.add(stopWeight);
        }

        List<TripDraftStop> recalculatedStops = calculateGroupEtas(td, selectedStops, td.getPlannedDepartureTime());
        return evaluateVehiclesForVolumeAndWeight(groupVolume, groupWeight, recalculatedStops, td.getDeliveryDate());
    }

    private EligibleVehiclesResponse evaluateVehiclesForVolumeAndWeight(
            BigDecimal totalVolume, BigDecimal totalWeight, List<TripDraftStop> stops, LocalDate deliveryDate) {
        // Bảo dưỡng/Ngừng hoạt động: chặn hẳn (mọi ngày), khớp Guard 0 của
        // validateVehicleAndDriverAvailability() — xe này không bao giờ được liệt kê ở đây, dù
        // "đủ tải" hay "không đủ tải" (giữ đúng convention RecommendationServiceImpl HC-1 đang dùng).
        List<Vehicle> activeVehicles = vehicleRepository.findByIsActiveTrue().stream()
                .filter(v -> v.getStatus() != VehicleStatus.MAINTENANCE && v.getStatus() != VehicleStatus.OUT_OF_SERVICE)
                .toList();
        List<EligibleVehicleDto> eligibleVehicles = new ArrayList<>();
        List<IneligibleVehicleDto> ineligibleVehicles = new ArrayList<>();

        List<Order> draftOrders = Collections.emptyList();
        if (stops != null && !stops.isEmpty()) {
            Long draftId = stops.get(0).getTripDraft() != null ? stops.get(0).getTripDraft().getId() : null;
            if (draftId != null) {
                draftOrders = orderRepository.findByTripDraftId(draftId);
            }
        }

        List<TripStatus> busyStatuses = List.of(TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);
        java.util.Set<Long> busyDriverIdsOnDate = (deliveryDate != null)
                ? new java.util.HashSet<>(tripRepository.findBusyDriverIdsOnDate(deliveryDate, busyStatuses))
                : java.util.Collections.emptySet();

        List<TripExecution> allUnreturned = tripExecutionRepository.findAllUnreturnedExecutions();
        java.util.Map<Long, List<TripExecution>> unreturnedByDriverId = allUnreturned.stream()
                .filter(te -> te.getDriver() != null && (te.getTrip() == null || te.getTrip().getDeliveryDate() == null || deliveryDate == null || !te.getTrip().getDeliveryDate().isAfter(deliveryDate)))
                .collect(java.util.stream.Collectors.groupingBy(te -> te.getDriver().getId()));

        // Vehicle-side per-date conflict — mirrors Guard 1/3 of validateVehicleAndDriverAvailability()
        // (the real gate at confirm-time) so this preview list matches what "Xác nhận" will actually
        // accept. NOTE: busy set here intentionally includes VALIDATED (unlike the driver busyStatuses
        // above) — a vehicle already earmarked (VALIDATED, not yet dispatched) for another trip on this
        // exact date is a real conflict, same as the confirm-time check.
        List<TripStatus> vehicleBusyStatuses = List.of(TripStatus.VALIDATED, TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);
        java.util.Set<Long> busyVehicleIdsOnDate = (deliveryDate != null)
                ? new java.util.HashSet<>(tripRepository.findBusyVehicleIdsOnDate(deliveryDate, vehicleBusyStatuses))
                : java.util.Collections.emptySet();
        java.util.Map<Long, List<TripExecution>> unreturnedByVehicleId = allUnreturned.stream()
                .filter(te -> te.getTrip() != null && te.getTrip().getVehicle() != null
                        && (te.getTrip().getDeliveryDate() == null || deliveryDate == null || !te.getTrip().getDeliveryDate().isAfter(deliveryDate)))
                .collect(java.util.stream.Collectors.groupingBy(te -> te.getTrip().getVehicle().getId()));

        for (Vehicle v : activeVehicles) {
            if (v.getMaxVolumeM3() == null || v.getPayloadKg() == null) {
                continue;
            }

            BigDecimal effectiveMaxVolume = v.getMaxVolumeM3().multiply(SAFETY_BUFFER);
            BigDecimal effectiveMaxWeight = v.getPayloadKg().multiply(SAFETY_BUFFER);

            boolean volumeOk = totalVolume.compareTo(effectiveMaxVolume) <= 0;
            boolean weightOk = totalWeight.compareTo(effectiveMaxWeight) <= 0;
            String storeWeightViolationReason = validateVehicleStoreWeight(v, stops);
            boolean routeWeightOk = (storeWeightViolationReason == null);

            boolean etaAllowed = true;
            String etaViolationReason = null;
            if (stops != null) {
                for (TripDraftStop stop : stops) {
                    String violation = constraintValidationService.validateStopEta(stop, draftOrders);
                    if (violation != null) {
                        etaAllowed = false;
                        etaViolationReason = violation;
                        break;
                    }
                }
            }

            boolean vehicleAvailable = true;
            String vehicleBusyReason = null;
            if (busyVehicleIdsOnDate.contains(v.getId())) {
                vehicleAvailable = false;
                vehicleBusyReason = "Vehicle already assigned to another trip on " + deliveryDate;
            } else if (!unreturnedByVehicleId.getOrDefault(v.getId(), Collections.emptyList()).isEmpty()) {
                vehicleAvailable = false;
                vehicleBusyReason = "Vehicle is IN_USE and has not confirmed return to warehouse yet";
            }

            Long assignedDriverId = null;
            String assignedDriverName = null;
            Boolean assignedDriverAvailable = null;
            String assignedDriverBusyReason = null;

            if (v.getAssignedDriver() != null) {
                User driver = v.getAssignedDriver();
                assignedDriverId = driver.getId();
                assignedDriverName = driver.getFullName() != null ? driver.getFullName() : driver.getUsername();

                if (!Boolean.TRUE.equals(driver.getIsActive()) || driver.getDriverStatus() != DriverStatus.ACTIVE) {
                    assignedDriverAvailable = false;
                    assignedDriverBusyReason = "Tài xế cố định (" + assignedDriverName + ") hiện đang nghỉ/không hoạt động";
                } else {
                    boolean busyOnDate = busyDriverIdsOnDate.contains(driver.getId());
                    List<TripExecution> unreturned = unreturnedByDriverId.getOrDefault(driver.getId(), Collections.emptyList());

                    if (busyOnDate) {
                        assignedDriverAvailable = false;
                        assignedDriverBusyReason = "Tài xế cố định (" + assignedDriverName + ") đã được gán chuyến bận ngày " + deliveryDate;
                    } else if (!unreturned.isEmpty()) {
                        assignedDriverAvailable = false;
                        assignedDriverBusyReason = "Tài xế cố định (" + assignedDriverName + ") chưa xác nhận về kho cho chuyến trước";
                    } else {
                        assignedDriverAvailable = true;
                        assignedDriverBusyReason = null;
                    }
                }
            }

            if (volumeOk && weightOk && routeWeightOk && etaAllowed && vehicleAvailable) {
                eligibleVehicles.add(EligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getPayloadKg())
                        .remainingVolumeM3(v.getMaxVolumeM3().subtract(totalVolume))
                        .remainingWeightKg(v.getPayloadKg().subtract(totalWeight))
                        .assignedDriverId(assignedDriverId)
                        .assignedDriverName(assignedDriverName)
                        .assignedDriverAvailable(assignedDriverAvailable)
                        .assignedDriverBusyReason(assignedDriverBusyReason)
                        .build());
            } else {
                StringBuilder reason = new StringBuilder();
                if (!volumeOk) {
                    reason.append("Volume exceeds safety limit (")
                          .append(totalVolume).append(" m³ > ")
                          .append(effectiveMaxVolume.setScale(3, RoundingMode.HALF_UP)).append(" m³)");
                }
                if (!weightOk) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append("Weight exceeds safety limit (")
                          .append(totalWeight).append(" kg > ")
                          .append(effectiveMaxWeight.setScale(2, RoundingMode.HALF_UP)).append(" kg)");
                }
                if (!routeWeightOk) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append(storeWeightViolationReason);
                }
                if (!etaAllowed) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append(etaViolationReason);
                }
                if (!vehicleAvailable) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append(vehicleBusyReason);
                }

                ineligibleVehicles.add(IneligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getPayloadKg())
                        .volumeCheckResult(volumeOk ? ConstraintResult.PASS : ConstraintResult.FAIL)
                        .weightCheckResult((weightOk && routeWeightOk && etaAllowed && vehicleAvailable) ? ConstraintResult.PASS : ConstraintResult.FAIL)
                        .failureReason(reason.toString())
                        .build());
            }
        }

        eligibleVehicles.sort(Comparator.comparing(EligibleVehicleDto::getMaxVolumeM3));

        return EligibleVehiclesResponse.builder()
                .eligibleVehicles(eligibleVehicles)
                .ineligibleVehicles(ineligibleVehicles)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AvailableDriverResponse> getAvailableDrivers(LocalDate date) {
        List<User> drivers = userRepository.findAll().stream()
                .filter(u -> u.getIsActive() && u.getDriverStatus() == DriverStatus.ACTIVE && u.getRoles().stream()
                        .anyMatch(r -> "DRIVER".equals(r.getName())))
                .toList();

        List<TripStatus> busyStatuses = List.of(TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);

        // 1. Batch fetch busy driver IDs on target date
        java.util.Set<Long> busyDriverIdsOnDate = new java.util.HashSet<>(
                tripRepository.findBusyDriverIdsOnDate(date, busyStatuses)
        );

        // 2. Batch fetch unreturned executions and group by driverId
        List<TripExecution> allUnreturned = tripExecutionRepository.findAllUnreturnedExecutions();
        java.util.Map<Long, List<TripExecution>> unreturnedByDriverId = allUnreturned.stream()
                .filter(te -> te.getDriver() != null && (te.getTrip() == null || te.getTrip().getDeliveryDate() == null || !te.getTrip().getDeliveryDate().isAfter(date)))
                .collect(java.util.stream.Collectors.groupingBy(te -> te.getDriver().getId()));

        return drivers.stream().map(d -> {
            boolean busyOnDate = busyDriverIdsOnDate.contains(d.getId());
            List<TripExecution> unreturned = unreturnedByDriverId.getOrDefault(d.getId(), java.util.Collections.emptyList());

            boolean busy = busyOnDate || !unreturned.isEmpty();
            String busyReason;
            if (busyOnDate) {
                busyReason = "Already assigned to an active trip on " + date;
            } else if (!unreturned.isEmpty()) {
                TripExecution te = unreturned.get(0);
                Long conflictTripId = te.getTrip() != null ? te.getTrip().getTripId() : te.getId();
                busyReason = "Currently active on trip #" + conflictTripId
                        + " and has not confirmed return to warehouse yet.";
            } else {
                busyReason = null;
            }

            return AvailableDriverResponse.builder()
                    .userId(d.getId())
                    .fullName(d.getFullName())
                    .email(d.getEmail())
                    .available(!busy)
                    .busyReason(busyReason)
                    .build();
        }).toList();
    }

    @Override
    @Transactional
    public TripResponse assignVehicleAndDriver(Long tripDraftId, TripAssignRequest request,
                                                String currentUsername) {
        TripDraft td = findTripDraftOrThrow(tripDraftId);

        // CANCELLED Trip không tính là "đã gán" — cho phép gán lại xe khác. Chỉ chặn khi còn 1 Trip
        // nào khác đang tồn tại (VALIDATED/DISPATCHED/IN_PROGRESS/COMPLETED).
        if (tripRepository.existsByTripDraftIdAndStatusNot(tripDraftId, TripStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED,
                    "Trip Draft has already been assigned.", HttpStatus.CONFLICT);
        }

        // Guard 1: TripDraft must be VALIDATED or PLANNED (with volume check completed)
        validateAssignmentEligibility(td);

        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND,
                        "Vehicle not found.", HttpStatus.NOT_FOUND));

        Long driverId = request.getDriverId();
        if (driverId == null) {
            if (vehicle.getAssignedDriver() != null) {
                driverId = vehicle.getAssignedDriver().getId();
            } else {
                throw new BusinessException(ErrorCode.FIELD_REQUIRED,
                        "Driver ID is required because vehicle has no assigned fixed driver.", HttpStatus.BAD_REQUEST);
            }
        }

        User driver = findDriverOrThrow(driverId);
        User dispatcher = findUserByUsernameOrThrow(currentUsername);

        if ("PLANNED".equals(td.getStatus())) {
            td.setStatus("VALIDATED");
            td.setValidatedAt(LocalDateTime.now());
            td.setValidatedBy(dispatcher);
            tripDraftRepository.save(td);

            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    tripDraftId, null, com.elog.entity.PlanningEventType.OPTION_SELECTED,
                    com.elog.entity.PlanningActorType.USER, dispatcher.getUsername(),
                    "PLANNED", "VALIDATED", "Dispatcher xác nhận tải trọng thủ công (auto 1-xe/2-xe không khả thi) và phân xe cho tuyến",
                    null, null, null, null,
                    td.getRoute() != null ? td.getRoute().getCode() : null, td.getDeliveryDate()));
        }

        // Guard 2: Vehicle must be eligible (dual-constraint with 90% safety buffer, route weight limit, & stop ETA)
        BigDecimal effectiveMaxVolume = vehicle.getMaxVolumeM3().multiply(SAFETY_BUFFER);
        BigDecimal effectiveMaxWeight = vehicle.getPayloadKg().multiply(SAFETY_BUFFER);
        if (effectiveMaxVolume.compareTo(td.getTotalVolumeM3()) < 0
                || effectiveMaxWeight.compareTo(td.getTotalWeightKg()) < 0) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                    "Vehicle " + vehicle.getPlateNumber()
                            + " does not meet dual-constraint requirements (90% safety buffer) for this trip.",
                    HttpStatus.BAD_REQUEST);
        }
        String storeWeightViolation = validateVehicleStoreWeight(vehicle, td.getStops());
        if (storeWeightViolation != null) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                    "Vehicle " + vehicle.getPlateNumber() + " violates route constraints: " + storeWeightViolation,
                    HttpStatus.BAD_REQUEST);
        }
        List<Order> draftOrders = orderRepository.findByTripDraftId(tripDraftId);
        if (td.getStops() != null) {
            for (TripDraftStop stop : td.getStops()) {
                String violation = constraintValidationService.validateStopEta(stop, draftOrders);
                if (violation != null) {
                    throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                            "Vehicle " + vehicle.getPlateNumber() + " violates time window constraint: " + violation,
                            HttpStatus.BAD_REQUEST);
                }
            }
        }

        // Guard 2.5: Driver license class must be compatible with vehicle's required license
        if (vehicle.getRequiredLicense() != null) {
            if (driver.getLicenseClass() == null || driver.getLicenseClass().ordinal() < vehicle.getRequiredLicense().ordinal()) {
                throw new BusinessException(ErrorCode.DRIVER_LICENSE_INCOMPATIBLE,
                        "Driver " + driver.getFullName() + " license class (" + (driver.getLicenseClass() != null ? driver.getLicenseClass() : "None") +
                        ") is insufficient for vehicle required license (" + vehicle.getRequiredLicense() + ").",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Guard 3 & 4: Comprehensive vehicle & driver availability check (unreturned, active trip, time overlap)
        validateVehicleAndDriverAvailability(vehicle, driver, td.getDeliveryDate(), td.getPlannedDepartureTime(), null);

        // Create Trip
        Trip trip = buildTrip(td, vehicle, driver, dispatcher,
                td.getTotalWeightKg(), td.getTotalVolumeM3());
        trip = tripRepository.save(trip);

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, trip.getTripId(), com.elog.entity.PlanningEventType.OPTION_SELECTED,
                com.elog.entity.PlanningActorType.USER, dispatcher.getUsername(),
                td.getStatus(), td.getStatus(), "Phân công xe " + vehicle.getPlateNumber() + " cho Trip " + trip.getTripId(),
                vehicle.getPlateNumber(), null, null, null,
                td.getRoute() != null ? td.getRoute().getCode() : null, td.getDeliveryDate()));

        planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                tripDraftId, trip.getTripId(), com.elog.entity.PlanningEventType.DRIVER_ASSIGNED,
                com.elog.entity.PlanningActorType.USER, dispatcher.getUsername(),
                td.getStatus(), td.getStatus(), "Phân công tài xế " + driver.getFullName() + " cho Trip " + trip.getTripId(),
                null, null, null, null,
                td.getRoute() != null ? td.getRoute().getCode() : null, td.getDeliveryDate()));

        // Create TripStops from active TripDraftStops
        List<TripDraftStop> activeStops =
                tripDraftStopRepository.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);
        createTripStops(trip, activeStops);

        // Link manifest
        linkManifest(tripDraftId, trip.getTripId());

        log.info("Trip {} created from TripDraft {} with vehicle {} and driver {}",
                trip.getTripId(), tripDraftId, vehicle.getPlateNumber(), driver.getFullName());

        return buildTripResponse(trip, "Trip created successfully. Proceed to Dispatch (US-16).");
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getTripsByTripDraftId(Long tripDraftId) {
        List<Trip> trips = tripRepository.findByTripDraftIdWithDetails(tripDraftId);
        return trips.stream()
                .map(t -> buildTripResponse(t, null))
                .toList();
    }

    // ── US-15 TASK-03 — Split ──────────────────────────────────────

    @Override
    @Transactional
    public TripSplitResponse assignSplit(Long tripDraftId, TripSplitAssignRequest request,
                                          String currentUsername) {
        TripDraft td = findTripDraftOrThrow(tripDraftId);

        // CANCELLED Trip không tính là "đã gán" — xem giải thích ở assignVehicleAndDriver.
        if (tripRepository.existsByTripDraftIdAndStatusNot(tripDraftId, TripStatus.CANCELLED)) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED,
                    "Trip Draft has already been assigned.", HttpStatus.CONFLICT);
        }

        validateAssignmentEligibility(td);

        User dispatcher = findUserByUsernameOrThrow(currentUsername);

        if ("PLANNED".equals(td.getStatus())) {
            td.setStatus("VALIDATED");
            td.setValidatedAt(LocalDateTime.now());
            td.setValidatedBy(dispatcher);
            tripDraftRepository.save(td);

            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    tripDraftId, null, com.elog.entity.PlanningEventType.OPTION_SELECTED,
                    com.elog.entity.PlanningActorType.USER, dispatcher.getUsername(),
                    "PLANNED", "VALIDATED", "Dispatcher xác nhận tải trọng thủ công (auto 1-xe/2-xe không khả thi) và phân N xe cho tuyến",
                    null, null, null, null,
                    td.getRoute() != null ? td.getRoute().getCode() : null, td.getDeliveryDate()));
        }

        List<TripSplitResponse.TripSummary> summaries = new ArrayList<>();

        // BR-PLAN-03: Validate complete, non-duplicate, non-extraneous stop coverage for this trip draft
        List<TripDraftStop> allActiveStops = tripDraftStopRepository.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);
        Set<Long> expectedStopIds = allActiveStops.stream().map(TripDraftStop::getId).collect(java.util.stream.Collectors.toSet());

        List<Long> requestedStopIds = request.getAssignments().stream()
                .flatMap(a -> a.getStopIds().stream())
                .toList();
        Set<Long> requestedStopIdSet = new HashSet<>(requestedStopIds);

        if (requestedStopIds.size() != requestedStopIdSet.size()) {
            throw new BusinessException(ErrorCode.SPLIT_PLAN_STOP_DUPLICATED,
                    "One or more stops are assigned to more than one vehicle in this split plan.", HttpStatus.BAD_REQUEST);
        }

        if (!requestedStopIdSet.equals(expectedStopIds)) {
            throw new BusinessException(ErrorCode.SPLIT_PLAN_STOP_INCOMPLETE,
                    "Split plan must cover exactly all active stops of the Trip Draft — no stop may be missing or extraneous.", HttpStatus.BAD_REQUEST);
        }

        for (TripSplitAssignRequest.SplitAssignment assignment : request.getAssignments()) {
            Vehicle vehicle = vehicleRepository.findById(assignment.getVehicleId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND,
                            "Vehicle not found.", HttpStatus.NOT_FOUND));

            Long driverId = assignment.getDriverId();
            User driver;
            if (driverId != null) {
                driver = findDriverOrThrow(driverId);
            } else {
                if (vehicle.getAssignedDriver() != null) {
                    driver = vehicle.getAssignedDriver();
                    if (!Boolean.TRUE.equals(driver.getIsActive()) || driver.getDriverStatus() != DriverStatus.ACTIVE) {
                        throw new BusinessException(ErrorCode.FIELD_REQUIRED,
                                "Tài xế cố định (" + (driver.getFullName() != null ? driver.getFullName() : driver.getUsername())
                                        + ") của xe " + vehicle.getPlateNumber() + " hiện đang nghỉ/không hoạt động. Vui lòng chọn tài xế khác.",
                                HttpStatus.BAD_REQUEST);
                    }
                    List<TripStatus> busyStatuses = List.of(TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);
                    boolean busyOnDate = tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(driver.getId(), td.getDeliveryDate(), busyStatuses);
                    List<TripExecution> unreturned = tripExecutionRepository.findUnreturnedByDriverId(driver.getId()).stream()
                            .filter(te -> te.getTrip() == null || te.getTrip().getDeliveryDate() == null || !te.getTrip().getDeliveryDate().isAfter(td.getDeliveryDate()))
                            .toList();
                    if (busyOnDate || !unreturned.isEmpty()) {
                        String reason = busyOnDate ? "đã được gán chuyến bận ngày " + td.getDeliveryDate() : "chưa xác nhận về kho cho chuyến trước";
                        throw new BusinessException(ErrorCode.FIELD_REQUIRED,
                                "Tài xế cố định (" + (driver.getFullName() != null ? driver.getFullName() : driver.getUsername())
                                        + ") của xe " + vehicle.getPlateNumber() + " " + reason + ". Vui lòng chọn tài xế khác.",
                                HttpStatus.BAD_REQUEST);
                    }
                } else {
                    throw new BusinessException(ErrorCode.FIELD_REQUIRED,
                            "Xe " + vehicle.getPlateNumber() + " không có tài xế cố định. Vui lòng chọn tài xế cho chuyến.",
                            HttpStatus.BAD_REQUEST);
                }
            }

            // Vehicle & driver availability check (unreturned, active trip, time overlap)
            validateVehicleAndDriverAvailability(vehicle, driver, td.getDeliveryDate(), td.getPlannedDepartureTime(), null);

            // Get the specific stops for this split group and recalculate group ETAs from warehouse
            List<TripDraftStop> fetchedStops = tripDraftStopRepository.findAllById(assignment.getStopIds());
            if (fetchedStops.size() != assignment.getStopIds().size()) {
                throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "One or more TripDraftStops not found.", HttpStatus.NOT_FOUND);
            }
            List<TripDraftStop> rawGroupStops = fetchedStops.stream()
                    .sorted(Comparator.comparing(TripDraftStop::getSequenceNo))
                    .toList();

            List<TripDraftStop> groupStops = calculateGroupEtas(td, rawGroupStops, td.getPlannedDepartureTime());

            // Calculate group totals via batch query
            List<Long> groupStoreIds = groupStops.stream()
                    .map(stop -> stop.getStore() != null ? stop.getStore().getId() : null)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();

            BigDecimal groupWeight = BigDecimal.ZERO;
            BigDecimal groupVolume = BigDecimal.ZERO;
            if (!groupStoreIds.isEmpty()) {
                List<OrderItem> items = orderItemRepository.findByStoreIdInAndTripDraftId(groupStoreIds, tripDraftId);
                for (OrderItem item : items) {
                    if (item.getLineWeightKg() != null) groupWeight = groupWeight.add(item.getLineWeightKg());
                    if (item.getLineVolumeM3() != null) groupVolume = groupVolume.add(item.getLineVolumeM3());
                }
            }

            // Validate vehicle capacity for this group (dual-constraint with 90% safety buffer, store weight limit & stop ETA)
            BigDecimal effectiveMaxVolume = vehicle.getMaxVolumeM3().multiply(SAFETY_BUFFER);
            BigDecimal effectiveMaxWeight = vehicle.getPayloadKg().multiply(SAFETY_BUFFER);
            if (effectiveMaxVolume.compareTo(groupVolume) < 0
                    || effectiveMaxWeight.compareTo(groupWeight) < 0) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                        "Vehicle " + vehicle.getPlateNumber()
                                + " cannot carry the assigned stops under 90% safety buffer (volume: " + groupVolume
                                + " m³, weight: " + groupWeight + " kg).",
                        HttpStatus.BAD_REQUEST);
            }

            String storeWeightViolation = validateVehicleStoreWeight(vehicle, groupStops);
            if (storeWeightViolation != null) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                        "Vehicle " + vehicle.getPlateNumber() + " violates route constraints for group stops: " + storeWeightViolation,
                        HttpStatus.BAD_REQUEST);
            }

            List<Order> draftOrders = orderRepository.findByTripDraftId(tripDraftId);
            for (TripDraftStop stop : groupStops) {
                String violation = constraintValidationService.validateStopEta(stop, draftOrders);
                if (violation != null) {
                    throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                            "Vehicle " + vehicle.getPlateNumber() + " violates time window constraint: " + violation,
                            HttpStatus.BAD_REQUEST);
                }
            }

            // Driver license class compatibility check
            if (vehicle.getRequiredLicense() != null) {
                if (driver.getLicenseClass() == null || driver.getLicenseClass().ordinal() < vehicle.getRequiredLicense().ordinal()) {
                    throw new BusinessException(ErrorCode.DRIVER_LICENSE_INCOMPATIBLE,
                            "Driver " + driver.getFullName() + " license class (" + (driver.getLicenseClass() != null ? driver.getLicenseClass() : "None") +
                            ") is insufficient for vehicle required license (" + vehicle.getRequiredLicense() + ").",
                            HttpStatus.BAD_REQUEST);
                }
            }

            Trip trip = buildTrip(td, vehicle, driver, dispatcher, groupWeight, groupVolume);
            trip = tripRepository.save(trip);
            createTripStops(trip, groupStops);

            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    tripDraftId, trip.getTripId(), com.elog.entity.PlanningEventType.OPTION_SELECTED,
                    com.elog.entity.PlanningActorType.USER, dispatcher.getUsername(),
                    td.getStatus(), td.getStatus(), "Phân công " + request.getAssignments().size() + " xe: Xe " + vehicle.getPlateNumber() + " cho sub-trip " + trip.getTripId(),
                    vehicle.getPlateNumber(), null, null, null,
                    td.getRoute() != null ? td.getRoute().getCode() : null, td.getDeliveryDate()));

            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    tripDraftId, trip.getTripId(), com.elog.entity.PlanningEventType.DRIVER_ASSIGNED,
                    com.elog.entity.PlanningActorType.USER, dispatcher.getUsername(),
                    td.getStatus(), td.getStatus(), "Phân công tài xế " + driver.getFullName() + " cho sub-trip " + trip.getTripId(),
                    null, null, null, null,
                    td.getRoute() != null ? td.getRoute().getCode() : null, td.getDeliveryDate()));

            summaries.add(TripSplitResponse.TripSummary.builder()
                    .tripId(trip.getTripId())
                    .plateNumber(vehicle.getPlateNumber())
                    .vehicleType(vehicle.getVehicleType())
                    .stopCount(groupStops.size())
                    .totalVolumeM3(groupVolume)
                    .totalWeightKg(groupWeight)
                    .build());
        }

        return TripSplitResponse.builder()
                .tripDraftId(tripDraftId)
                .tripsCreated(summaries.size())
                .trips(summaries)
                .message("Load split into " + summaries.size() + " trips per BR-07.")
                .build();
    }

    // ── US-15 TASK-03 — Fleet Shortfall Check (BR-08) ──────────────

    @Override
    @Transactional(readOnly = true)
    public FleetCapacityCheckResponse checkFleetCapacity(LocalDate date) {
        BigDecimal fleetVolume = vehicleRepository.sumActiveMaxVolumeM3();
        BigDecimal fleetWeight = vehicleRepository.sumActiveMaxWeightKg();
        if (fleetVolume == null) fleetVolume = BigDecimal.ZERO;
        if (fleetWeight == null) fleetWeight = BigDecimal.ZERO;

        // Sum all VALIDATED trip drafts for the day
        List<TripDraft> dayDrafts = tripDraftRepository.findByDeliveryDate(date,
                org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                .filter(td -> "VALIDATED".equals(td.getStatus()))
                .toList();

        BigDecimal dayVolume = dayDrafts.stream()
                .map(TripDraft::getTotalVolumeM3)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal dayWeight = dayDrafts.stream()
                .map(TripDraft::getTotalWeightKg)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String volResult = dayVolume.compareTo(fleetVolume) <= 0 ? "PASS" : "FAIL";
        String wgtResult = dayWeight.compareTo(fleetWeight) <= 0 ? "PASS" : "FAIL";
        boolean canDispatch = "PASS".equals(volResult) && "PASS".equals(wgtResult);

        return FleetCapacityCheckResponse.builder()
                .deliveryDate(date)
                .fleetTotalVolumeM3(fleetVolume)
                .fleetTotalWeightKg(fleetWeight)
                .dayTotalVolumeM3(dayVolume)
                .dayTotalWeightKg(dayWeight)
                .volumeCheckResult(volResult)
                .weightCheckResult(wgtResult)
                .canDispatch(canDispatch)
                .message(canDispatch
                        ? "Fleet capacity sufficient for " + date + "."
                        : "Fleet capacity insufficient for " + date + ". Cannot dispatch.")
                .build();
    }

    // ── US-16 TASK-02 — Dispatch ───────────────────────────────────

    @Override
    @Transactional
    public TripResponse dispatchTrip(Long tripId, String currentUsername) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND,
                        "Trip not found.", HttpStatus.NOT_FOUND));

        // Guard: already locked?
        if (trip.getLockedAt() != null) {
            throw new BusinessException(ErrorCode.TRIP_LOCKED,
                    "Trip " + tripId + " is already dispatched (locked at " + trip.getLockedAt() + ").",
                    HttpStatus.CONFLICT);
        }

        // Guard: BR-08 fleet shortfall (last check before lock)
        FleetCapacityCheckResponse fleetCheck = checkFleetCapacity(trip.getDeliveryDate());
        if (!fleetCheck.isCanDispatch()) {
            throw new BusinessException(ErrorCode.FLEET_CAPACITY_SHORTFALL,
                    fleetCheck.getMessage(), HttpStatus.SERVICE_UNAVAILABLE);
        }

        User dispatcher = findUserByUsernameOrThrow(currentUsername);

        // State machine transition: VALIDATED → DISPATCHED
        tripStateMachine.transition(trip, TripStatus.DISPATCHED, dispatcher.getId());
        trip.setLockedBy(dispatcher);
        tripRepository.save(trip);

        if (trip.getTripDraft() != null) {
            TripDraft td = trip.getTripDraft();
            td.setStatus("DISPATCHED");
            tripDraftRepository.save(td);
        }

        // Create & save TripExecution (FT-09 lifecycle) for the driver if not already existing
        if (tripExecutionRepository.findByTripId(trip.getTripId()).isEmpty()) {
            TripExecution execution = TripExecution.builder()
                    .trip(trip)
                    .driver(trip.getDriver())
                    .build();

            if (trip.getTripDraft() != null) {
                List<TripStop> myTripStops = tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(trip.getTripId());
                List<Order> draftOrders = orderRepository.findByTripDraftId(trip.getTripDraft().getId());

                if (!myTripStops.isEmpty()) {
                    Map<Long, TripDraftStop> storeToStopMap = new HashMap<>();
                    for (TripStop ts : myTripStops) {
                        TripDraftStop ds = ts.getTripDraftStop();
                        if (ds != null && ds.getStore() != null) {
                            storeToStopMap.putIfAbsent(ds.getStore().getId(), ds);
                        }
                    }
                    for (Order order : draftOrders) {
                        TripDraftStop stop = (order.getStore() != null) ? storeToStopMap.get(order.getStore().getId()) : null;
                        if (stop != null) {
                            execution.getOrderResults().add(DeliveryOrderResult.builder()
                                    .tripExecution(execution)
                                    .order(order)
                                    .stop(stop)
                                    .status("PENDING")
                                    .build());
                        }
                    }
                } else {
                    List<TripDraftStop> draftStops = tripDraftStopRepository.findByTripDraftIdOrderBySequenceNoAsc(trip.getTripDraft().getId());
                    Map<Long, TripDraftStop> storeToStopMap = new HashMap<>();
                    for (TripDraftStop s : draftStops) {
                        if (s.getStore() != null) {
                            storeToStopMap.putIfAbsent(s.getStore().getId(), s);
                        }
                    }
                    for (Order order : draftOrders) {
                        TripDraftStop stop = (order.getStore() != null) ? storeToStopMap.get(order.getStore().getId()) : null;
                        if (stop == null && !draftStops.isEmpty()) {
                            stop = draftStops.get(0);
                        }
                        if (stop != null) {
                            execution.getOrderResults().add(DeliveryOrderResult.builder()
                                    .tripExecution(execution)
                                    .order(order)
                                    .stop(stop)
                                    .status("PENDING")
                                    .build());
                        }
                    }
                }
            }

            tripExecutionRepository.save(execution);

            tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                    execution.getId(), trip.getTripId(),
                    com.elog.entity.TripOutcomeEventType.TRIP_EXECUTION_CREATED,
                    com.elog.entity.PlanningActorType.SYSTEM, currentUsername,
                    null, "ASSIGNED",
                    null, null, null, null, null, null, null, null,
                    trip.getRoute() != null ? trip.getRoute().getCode() : null,
                    trip.getDeliveryDate(),
                    trip.getDriver() != null ? trip.getDriver().getUsername() : null
            ));
        }

        log.info("Trip {} dispatched by {}", tripId, currentUsername);

        TripResponse response = buildTripResponse(trip,
                "Trip dispatched and locked. Handover slip ready.");
        response.setHandoverSlipUrl("/api/v1/trips/" + tripId + "/handover-slip");
        return response;
    }

    // ── Cancel trip (DISPATCHED, not yet started) ───────────────────
    @Override
    @Transactional
    public TripResponse cancelTrip(Long tripId, CancelTripRequest request, String currentUsername) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND,
                        "Trip not found.", HttpStatus.NOT_FOUND));

        User actor = findUserByUsernameOrThrow(currentUsername);
        String reason = request != null ? request.getReason() : null;

        // State machine guards: only DISPATCHED->CANCELLED is valid; any other current status throws
        // the right BusinessException (TRIP_COMPLETED / INVALID_TRIP_TRANSITION) — see TripStateMachine.
        tripStateMachine.transition(trip, TripStatus.CANCELLED, actor.getId());
        trip.setCancelledBy(actor);
        trip.setCancelReason(reason);
        tripRepository.save(trip);

        if (trip.getTripDraft() != null) {
            TripDraft td = trip.getTripDraft();
            td.setStatus("VALIDATED");
            tripDraftRepository.save(td);
        }

        // Trip is guaranteed not-started here (DISPATCHED->CANCELLED only), so the driver never left
        // the warehouse — close the execution outright instead of waiting for a return confirmation.
        tripExecutionRepository.findByTripId(trip.getTripId()).ifPresent(execution -> {
            execution.setStatus("CANCELLED");
            tripExecutionRepository.save(execution);

            tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                    execution.getId(), trip.getTripId(),
                    com.elog.entity.TripOutcomeEventType.CANCELLED_BY_DISPATCHER,
                    com.elog.entity.PlanningActorType.USER, currentUsername,
                    "DISPATCHED", "CANCELLED",
                    null, null, null, null, null, reason, null, null,
                    trip.getRoute() != null ? trip.getRoute().getCode() : null,
                    trip.getDeliveryDate(),
                    trip.getDriver() != null ? trip.getDriver().getUsername() : null
            ));
        });

        // Explicit save — defensive, matching the adminOverrideTripExecution pattern rather than
        // relying solely on persistence-context dirty-checking.
        if (trip.getVehicle() != null) {
            vehicleRepository.save(trip.getVehicle());
        }

        log.info("Trip {} cancelled by dispatcher {} (reason: {})", tripId, currentUsername, reason);

        return buildTripResponse(trip, "Trip cancelled. Vehicle and driver released.");
    }

    @Override
    @Transactional(readOnly = true)
    public String getHandoverSlipHtml(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND,
                        "Trip not found.", HttpStatus.NOT_FOUND));

        if (trip.getStatus() == TripStatus.VALIDATED) {
            throw new BusinessException(ErrorCode.HANDOVER_SLIP_NOT_AVAILABLE,
                    "Trip must be dispatched before generating handover slip.",
                    HttpStatus.BAD_REQUEST);
        }

        List<TripStop> stops = tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(tripId);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");
        DateTimeFormatter dtTimeFmt = DateTimeFormatter.ofPattern("HH:mm:ss");

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>");
        html.append("<title>Handover Slip - Trip #").append(tripId).append("</title>");
        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;max-width:800px;margin:0 auto;padding:20px}");
        html.append("table{width:100%;border-collapse:collapse;margin:10px 0}");
        html.append("th,td{border:1px solid #333;padding:8px;text-align:left}");
        html.append("th{background:#f0f0f0}h1{text-align:center;font-size:18px}");
        html.append(".header{text-align:center;margin-bottom:20px}");
        html.append(".sig{margin-top:40px}.sig-line{border-bottom:1px solid #333;width:250px;display:inline-block;margin-right:40px}");
        html.append("@media print{body{margin:0}@page{margin:1cm}}");
        html.append("</style></head><body>");
        html.append("<div class='header'><h1>PHIẾU GIAO HÀNG (HANDOVER SLIP)</h1>");
        html.append("<p>ELog Delivery System — SEP490_G104</p></div>");

        html.append("<table><tr><td><b>Ngày giao:</b> ")
                .append(trip.getDeliveryDate().format(dtf)).append("</td>");
        html.append("<td><b>Tuyến:</b> ").append(trip.getRoute().getCode()).append("</td></tr>");
        html.append("<tr><td><b>Xe:</b> ").append(trip.getVehicle().getPlateNumber())
                .append(" (").append(trip.getVehicle().getVehicleType()).append(")</td>");
        html.append("<td><b>Tài xế:</b> ").append(trip.getDriver().getFullName()).append("</td></tr>");
        html.append("<tr><td><b>Xuất phát:</b> ")
                .append(trip.getPlannedDepartureTime() != null
                        ? trip.getPlannedDepartureTime().format(timeFmt) : "N/A")
                .append("</td>");
        html.append("<td><b>Điều phối lúc:</b> ")
                .append(trip.getLockedAt() != null ? trip.getLockedAt().format(dtTimeFmt) : "N/A")
                .append("</td></tr></table>");

        html.append("<h3>DANH SÁCH ĐIỂM GIAO (theo thứ tự tuyến)</h3>");
        html.append("<table><tr><th>#</th><th>Điểm giao</th></tr>");
        for (TripStop stop : stops) {
            html.append("<tr><td>").append(stop.getSequenceOrder()).append("</td>");
            html.append("<td>").append(stop.getRouteStop().getStore().getName()).append("</td></tr>");
        }
        html.append("</table>");

        html.append("<p><b>Tổng tải:</b> ")
                .append(trip.getTotalWeightKg()).append(" kg / ")
                .append(trip.getTotalVolumeM3()).append(" m³</p>");

        if (trip.getLockedBy() != null) {
            html.append("<p><b>Dispatcher:</b> ").append(trip.getLockedBy().getFullName()).append("</p>");
        }

        html.append("<div class='sig'>");
        html.append("<p>Chữ ký xác nhận: <span class='sig-line'>&nbsp;</span> Ngày: ___/___/______</p>");
        html.append("<p>Tài xế nhận slip: <span class='sig-line'>&nbsp;</span> Ngày: ___/___/______</p>");
        html.append("</div></body></html>");

        return html.toString();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TripResponse> getDriverTrips(String username, LocalDate date, String status) {
        User driver = findUserByUsernameOrThrow(username);
        List<Trip> trips;
        if (status != null && !status.isBlank()) {
            TripStatus tripStatus = TripStatus.valueOf(status.toUpperCase());
            trips = tripRepository.findByDriverIdAndDeliveryDateAndStatus(driver.getId(), date, tripStatus);
        } else {
            trips = tripRepository.findAll().stream()
                    .filter(t -> t.getDriver().getId().equals(driver.getId())
                              && t.getDeliveryDate().equals(date))
                    .toList();
        }
        return trips.stream().map(t -> buildTripResponse(t, null)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DriverTripCalendarDayResponse> getDriverTripCalendar(String username, YearMonth month) {
        User driver = findUserByUsernameOrThrow(username);
        LocalDate start = month.atDay(1);
        LocalDate end = month.atEndOfMonth();

        List<Trip> trips = tripRepository.findByDriverIdAndDeliveryDateBetween(driver.getId(), start, end);

        return trips.stream()
                .collect(Collectors.groupingBy(Trip::getDeliveryDate))
                .entrySet().stream()
                .map(e -> new DriverTripCalendarDayResponse(
                        e.getKey(),
                        e.getValue().stream().allMatch(t -> t.getStatus() == TripStatus.COMPLETED)))
                .sorted(Comparator.comparing(DriverTripCalendarDayResponse::getDate))
                .toList();
    }


    // ── Private helpers ────────────────────────────────────────────

    private TripDraft findTripDraftOrThrow(Long id) {
        return tripDraftRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "Trip Draft not found.", HttpStatus.NOT_FOUND));
    }

    private User findDriverOrThrow(Long driverId) {
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND,
                        "Driver not found.", HttpStatus.NOT_FOUND));
        boolean isDriver = driver.getRoles().stream()
                .anyMatch(r -> "DRIVER".equals(r.getName()));
        if (!isDriver) {
            throw new BusinessException(ErrorCode.DRIVER_NOT_FOUND,
                    "User " + driverId + " is not a driver.", HttpStatus.BAD_REQUEST);
        }
        if (driver.getDriverStatus() == DriverStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.DRIVER_INACTIVE,
                    "Driver " + driver.getFullName() + " is currently INACTIVE and cannot be assigned to trips.", HttpStatus.BAD_REQUEST);
        }
        return driver;
    }

    private User findUserByUsernameOrThrow(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found: " + username, HttpStatus.NOT_FOUND));
    }

    private Trip buildTrip(TripDraft td, Vehicle vehicle, User driver, User dispatcher,
                           BigDecimal totalWeight, BigDecimal totalVolume) {
        return Trip.builder()
                .tripDraft(td)
                .route(td.getRoute())
                .vehicle(vehicle)
                .driver(driver)
                .deliveryDate(td.getDeliveryDate())
                .status(TripStatus.VALIDATED)
                .totalWeightKg(totalWeight)
                .totalVolumeM3(totalVolume)
                .totalDistanceKm(td.getTotalDistanceKm())
                .routePolyline(td.getRoutePolyline())
                .plannedDepartureTime(td.getPlannedDepartureTime())
                .createdBy(dispatcher)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private void createTripStops(Trip trip, List<TripDraftStop> draftStops) {
        List<TripStop> tripStops = new ArrayList<>();
        for (TripDraftStop ds : draftStops) {
            // Calculate stop weight/volume from order items
            List<OrderItem> items = orderItemRepository.findByStopForManifest(
                    ds.getStore().getId(), ds.getTripDraft().getId());
            BigDecimal stopWeight = items.stream()
                    .map(OrderItem::getLineWeightKg)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal stopVolume = items.stream()
                    .map(OrderItem::getLineVolumeM3)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            TripStop ts = TripStop.builder()
                    .trip(trip)
                    .routeStop(ds.getRouteStop())
                    .tripDraftStop(ds)
                    .sequenceOrder(ds.getSequenceNo())
                    .plannedEta(ds.getPlannedEta())
                    .distanceFromPrevKm(ds.getDistanceFromPrevKm())
                    .travelTimeFromPrevMin(ds.getTravelTimeFromPrevMin())
                    .stopWeightKg(stopWeight)
                    .stopVolumeM3(stopVolume)
                    .status(TripStopStatus.PENDING)
                    .build();
            tripStops.add(ts);
        }
        tripStopRepository.saveAll(tripStops);
    }

    private void linkManifest(Long tripDraftId, Long tripId) {
        manifestRepository.findByTripDraftIdWithDetails(tripDraftId)
                .ifPresent(manifest -> {
                    manifest.setTripId(tripId);
                    manifestRepository.save(manifest);
                });
    }

    private TripResponse buildTripResponse(Trip trip, String message) {
        List<TripStop> stops = tripStopRepository.findByTripTripIdOrderBySequenceOrderAsc(trip.getTripId());
        Long manifestId = manifestRepository.findByTripDraftIdWithDetails(trip.getTripDraft().getId())
                .map(Manifest::getManifestId)
                .orElse(null);

        Optional<TripExecution> executionOpt = tripExecutionRepository.findByTripId(trip.getTripId());
        Long executionId = executionOpt.map(TripExecution::getId).orElse(null);
        LocalDateTime returnedToWarehouseAt = executionOpt.map(TripExecution::getReturnedToWarehouseAt).orElse(null);

        return TripResponse.builder()
                .tripId(trip.getTripId())
                .tripDraftId(trip.getTripDraft().getId())
                .executionId(executionId)
                .returnedToWarehouseAt(returnedToWarehouseAt)
                .fixedRouteCode(trip.getRoute().getCode())
                .deliveryDate(trip.getDeliveryDate())
                .status(trip.getStatus().name())
                .vehicle(TripResponse.VehicleInfo.builder()
                        .vehicleId(trip.getVehicle().getId())
                        .plateNumber(trip.getVehicle().getPlateNumber())
                        .vehicleType(trip.getVehicle().getVehicleType())
                        .build())
                .driver(TripResponse.DriverInfo.builder()
                        .userId(trip.getDriver().getId())
                        .fullName(trip.getDriver().getFullName())
                        .build())
                .totalWeightKg(trip.getTotalWeightKg())
                .totalVolumeM3(trip.getTotalVolumeM3())
                .totalDistanceKm(trip.getTotalDistanceKm())
                .routePolyline(trip.getRoutePolyline())
                .plannedDepartureTime(trip.getPlannedDepartureTime())
                .lockedAt(trip.getLockedAt())
                .lockedBy(trip.getLockedBy() != null
                        ? TripResponse.DriverInfo.builder()
                        .userId(trip.getLockedBy().getId())
                        .fullName(trip.getLockedBy().getFullName())
                        .build() : null)
                .completedAt(trip.getCompletedAt())
                .cancelledAt(trip.getCancelledAt())
                .daysOverdue(computeDaysOverdue(trip))
                .tripStopCount(stops.size())
                .manifestId(manifestId)
                .tripStops(stops.stream().map(this::buildTripStopResponse).toList())
                .message(message)
                .build();
    }

    /** Số ngày quá hạn deliveryDate khi trip vẫn DISPATCHED chưa bắt đầu — null nếu chưa quá ngưỡng. */
    private Integer computeDaysOverdue(Trip trip) {
        if (trip.getStatus() != TripStatus.DISPATCHED) {
            return null;
        }
        int thresholdDays = systemConfigRepository.findByConfigKey("TRIP_STALE_THRESHOLD_DAYS")
                .map(c -> Integer.parseInt(c.getConfigValue()))
                .orElse(3);
        long diff = ChronoUnit.DAYS.between(trip.getDeliveryDate(), LocalDate.now());
        return diff >= thresholdDays ? (int) diff : null;
    }

    private TripStopResponse buildTripStopResponse(TripStop ts) {
        return TripStopResponse.builder()
                .tripStopId(ts.getTripStopId())
                .routeStopId(ts.getRouteStop().getId())
                // trip_draft_stop_id is nullable on trip_stops (see TripStop.tripDraftStop) — guard
                // like storeCode/storeName below instead of NPE-ing on an orphaned stop.
                .tripDraftStopId(ts.getTripDraftStop() != null ? ts.getTripDraftStop().getId() : null)
                .sequenceOrder(ts.getSequenceOrder())
                .storeCode(ts.getRouteStop().getStore() != null
                        ? ts.getRouteStop().getStore().getCode() : null)
                .storeName(ts.getRouteStop().getStore() != null
                        ? ts.getRouteStop().getStore().getName() : null)
                .plannedEta(ts.getPlannedEta())
                .status(ts.getStatus().name())
                .stopWeightKg(ts.getStopWeightKg())
                .stopVolumeM3(ts.getStopVolumeM3())
                .distanceFromPrevKm(ts.getDistanceFromPrevKm())
                .travelTimeFromPrevMin(ts.getTravelTimeFromPrevMin())
                .notes(ts.getNotes())
                .build();
    }


    @Override
    @Transactional(readOnly = true)
    public TripResponse getTripById(Long tripId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND,
                        "Trip not found with id: " + tripId, HttpStatus.NOT_FOUND));

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            boolean isDriver = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> "ROLE_DRIVER".equals(a.getAuthority()) || "trip:execute".equals(a.getAuthority()));

            if (isDriver && currentUsername != null && !"anonymousUser".equals(currentUsername)) {
                if (trip.getDriver() == null || !currentUsername.equals(trip.getDriver().getUsername())) {
                    throw new BusinessException(ErrorCode.NOT_YOUR_TRIP,
                            "Bạn không có quyền xem thông tin chuyến xe của tài xế khác", HttpStatus.FORBIDDEN);
                }
            }
        }

        return buildTripResponse(trip, null);
    }

    @Override
    @Transactional
    public TripResponse updateAssignment(Long tripId, TripAssignmentPatchRequest request, String currentUsername) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_NOT_FOUND,
                        "Trip not found with id: " + tripId, HttpStatus.NOT_FOUND));

        // Guard 1: Status must be VALIDATED
        if (trip.getStatus() != TripStatus.VALIDATED) {
            throw new BusinessException(ErrorCode.TRIP_LOCKED,
                    "Trip " + tripId + " is already dispatched or completed and cannot be modified.",
                    HttpStatus.CONFLICT);
        }

        // Fetch new vehicle and driver
        Vehicle vehicle = vehicleRepository.findById(request.getVehicleId())
                .orElseThrow(() -> new BusinessException(ErrorCode.VEHICLE_NOT_FOUND,
                        "Vehicle not found with id: " + request.getVehicleId(), HttpStatus.NOT_FOUND));

        User driver = userRepository.findById(request.getDriverId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND,
                        "Driver not found with id: " + request.getDriverId(), HttpStatus.NOT_FOUND));

        // Check if driver has role DRIVER
        boolean isDriver = driver.getRoles().stream()
                .anyMatch(r -> "ROLE_DRIVER".equals(r.getName()) || "DRIVER".equals(r.getName()));
        if (!isDriver) {
            throw new BusinessException(ErrorCode.DRIVER_NOT_FOUND,
                    "User " + request.getDriverId() + " is not a driver.", HttpStatus.BAD_REQUEST);
        }
        if (driver.getDriverStatus() == DriverStatus.INACTIVE) {
            throw new BusinessException(ErrorCode.DRIVER_INACTIVE,
                    "Driver " + driver.getFullName() + " is currently INACTIVE and cannot be assigned to trips.", HttpStatus.BAD_REQUEST);
        }

        // Guard 2: Capacity check & store weight limit
        if (vehicle.getMaxVolumeM3().compareTo(trip.getTotalVolumeM3()) < 0
                || vehicle.getPayloadKg().compareTo(trip.getTotalWeightKg()) < 0) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                    "Vehicle " + vehicle.getPlateNumber() + " capacity is insufficient for the trip load.",
                    HttpStatus.BAD_REQUEST);
        }
        if (trip.getTripDraft() != null) {
            String storeWeightViolation = validateVehicleStoreWeight(vehicle, trip.getTripDraft().getStops());
            if (storeWeightViolation != null) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                        "Vehicle " + vehicle.getPlateNumber() + " violates route constraints: " + storeWeightViolation,
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Guard 2.5: Driver license class compatibility check
        if (vehicle.getRequiredLicense() != null) {
            if (driver.getLicenseClass() == null || driver.getLicenseClass().ordinal() < vehicle.getRequiredLicense().ordinal()) {
                throw new BusinessException(ErrorCode.DRIVER_LICENSE_INCOMPATIBLE,
                        "Driver " + driver.getFullName() + " license class (" + (driver.getLicenseClass() != null ? driver.getLicenseClass() : "None") +
                        ") is insufficient for vehicle required license (" + vehicle.getRequiredLicense() + ").",
                        HttpStatus.BAD_REQUEST);
            }
        }

        // Guard 3 & 4: Comprehensive vehicle & driver availability check (excluding current trip)
        validateVehicleAndDriverAvailability(vehicle, driver, trip.getDeliveryDate(), trip.getPlannedDepartureTime(), tripId);

        String oldPlate = trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null;
        String oldDriverName = trip.getDriver() != null ? trip.getDriver().getFullName() : null;

        // Update and save
        trip.setVehicle(vehicle);
        trip.setDriver(driver);
        tripRepository.save(trip);

        if (oldPlate != null && !oldPlate.equals(vehicle.getPlateNumber())) {
            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    trip.getTripDraft() != null ? trip.getTripDraft().getId() : null, trip.getTripId(),
                    com.elog.entity.PlanningEventType.OPTION_CHANGED,
                    com.elog.entity.PlanningActorType.USER, currentUsername,
                    trip.getStatus().name(), trip.getStatus().name(),
                    "Đổi xe từ " + oldPlate + " sang " + vehicle.getPlateNumber() + " cho Trip " + tripId,
                    vehicle.getPlateNumber(), null, null, null,
                    trip.getRoute() != null ? trip.getRoute().getCode() : null, trip.getDeliveryDate()));
        }

        if (oldDriverName != null && !oldDriverName.equals(driver.getFullName())) {
            planningHistoryService.record(new com.elog.service.PlanningHistoryService.PlanningEventInput(
                    trip.getTripDraft() != null ? trip.getTripDraft().getId() : null, trip.getTripId(),
                    com.elog.entity.PlanningEventType.DRIVER_CHANGED,
                    com.elog.entity.PlanningActorType.USER, currentUsername,
                    trip.getStatus().name(), trip.getStatus().name(),
                    "Đổi tài xế từ " + oldDriverName + " sang " + driver.getFullName() + " cho Trip " + tripId,
                    null, null, null, null,
                    trip.getRoute() != null ? trip.getRoute().getCode() : null, trip.getDeliveryDate()));
        }

        log.info("Trip {} assignment updated by {}: Vehicle={}, Driver={}",
                tripId, currentUsername, vehicle.getPlateNumber(), driver.getFullName());

        return buildTripResponse(trip, null);
    }

    private void validateVehicleAndDriverAvailability(Vehicle vehicle, User driver, LocalDate deliveryDate, LocalTime plannedDepartureTime, Long excludeTripId) {
        // 0. Vehicle must be administratively usable — Maintenance/OutOfService/deactivated vehicles
        // are never assignable, regardless of date (unlike the IN_USE case below, which is a
        // per-date busy check, not a blanket block — a vehicle busy today can still be planned
        // for a future date).
        if (vehicle.getStatus() == VehicleStatus.MAINTENANCE || vehicle.getStatus() == VehicleStatus.OUT_OF_SERVICE) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                    "Vehicle " + vehicle.getPlateNumber() + " is currently " + vehicle.getStatus()
                            + " and cannot be assigned to a trip.",
                    HttpStatus.BAD_REQUEST);
        }
        if (!Boolean.TRUE.equals(vehicle.getIsActive())) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                    "Vehicle " + vehicle.getPlateNumber() + " has been deactivated and cannot be assigned to a trip.",
                    HttpStatus.BAD_REQUEST);
        }

        List<TripStatus> busyStatuses = List.of(TripStatus.VALIDATED, TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);

        // 1. Vehicle active trip check on same day
        if (excludeTripId == null) {
            if (tripRepository.existsByVehicleIdAndDeliveryDateAndStatusIn(vehicle.getId(), deliveryDate, busyStatuses)) {
                throw new BusinessException(ErrorCode.VEHICLE_CONFLICT,
                        "Vehicle " + vehicle.getPlateNumber() + " already has an active trip on " + deliveryDate + ".",
                        HttpStatus.CONFLICT);
            }
        } else {
            if (tripRepository.existsByVehicleIdAndDeliveryDateAndStatusInAndTripIdNot(vehicle.getId(), deliveryDate, busyStatuses, excludeTripId)) {
                throw new BusinessException(ErrorCode.VEHICLE_CONFLICT,
                        "Vehicle " + vehicle.getPlateNumber() + " already has an active trip on " + deliveryDate + ".",
                        HttpStatus.CONFLICT);
            }
        }

        // 2. Driver active trip check on same day
        if (excludeTripId == null) {
            if (tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(driver.getId(), deliveryDate, busyStatuses)) {
                throw new BusinessException(ErrorCode.DRIVER_CONFLICT,
                        "Driver " + driver.getFullName() + " already assigned to another trip on " + deliveryDate + ".",
                        HttpStatus.CONFLICT);
            }
        } else {
            if (tripRepository.existsByDriverIdAndDeliveryDateAndStatusInAndTripIdNot(driver.getId(), deliveryDate, busyStatuses, excludeTripId)) {
                throw new BusinessException(ErrorCode.DRIVER_CONFLICT,
                        "Driver " + driver.getFullName() + " already assigned to another trip on " + deliveryDate + ".",
                        HttpStatus.CONFLICT);
            }
        }

        // 3. Vehicle unreturned check
        List<TripExecution> unreturnedVehicles = tripExecutionRepository.findUnreturnedByVehicleId(vehicle.getId());
        for (TripExecution te : unreturnedVehicles) {
            if (excludeTripId != null && te.getTrip() != null && te.getTrip().getTripId().equals(excludeTripId)) {
                continue;
            }
            if (te.getTrip() != null && te.getTrip().getDeliveryDate() != null && te.getTrip().getDeliveryDate().isAfter(deliveryDate)) {
                continue;
            }
            throw new BusinessException(ErrorCode.VEHICLE_CONFLICT,
                    "Vehicle " + vehicle.getPlateNumber() + " is currently IN_USE on trip #" 
                            + (te.getTrip() != null ? te.getTrip().getTripId() : te.getId()) 
                            + " and has not confirmed return to warehouse yet.",
                    HttpStatus.CONFLICT);
        }

        // 4. Driver unreturned check
        List<TripExecution> unreturnedDrivers = tripExecutionRepository.findUnreturnedByDriverId(driver.getId());
        for (TripExecution te : unreturnedDrivers) {
            if (excludeTripId != null && te.getTrip() != null && te.getTrip().getTripId().equals(excludeTripId)) {
                continue;
            }
            if (te.getTrip() != null && te.getTrip().getDeliveryDate() != null && te.getTrip().getDeliveryDate().isAfter(deliveryDate)) {
                continue;
            }
            throw new BusinessException(ErrorCode.DRIVER_CONFLICT,
                    "Driver " + driver.getFullName() + " is currently active on trip #" 
                            + (te.getTrip() != null ? te.getTrip().getTripId() : te.getId()) 
                            + " and has not confirmed return to warehouse yet.",
                    HttpStatus.CONFLICT);
        }

        // 5. Time window overlap check (vs previous returned trips)
        if (plannedDepartureTime != null) {
            LocalDateTime newDeparture = LocalDateTime.of(deliveryDate, plannedDepartureTime);

            List<TripExecution> vehicleHistory = tripExecutionRepository.findByVehicleId(vehicle.getId());
            for (TripExecution te : vehicleHistory) {
                if (te.getReturnedToWarehouseAt() != null) {
                    if (te.getTrip() != null && te.getTrip().getDeliveryDate() != null && te.getTrip().getDeliveryDate().isAfter(deliveryDate)) {
                        continue;
                    }
                    LocalDateTime earliestAvailable = te.getReturnedToWarehouseAt().plusMinutes(30);
                    if (newDeparture.isBefore(earliestAvailable)) {
                        throw new BusinessException(ErrorCode.VEHICLE_CONFLICT,
                                "Xe " + vehicle.getPlateNumber() + " về kho lúc "
                                        + te.getReturnedToWarehouseAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + ". Giờ xuất phát dự kiến " + newDeparture.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + " vi phạm thời gian giãn cách sau khi về kho (cần tối thiểu 30 phút).",
                                HttpStatus.CONFLICT);
                    }
                }
            }

            List<TripExecution> driverHistory = tripExecutionRepository.findByDriverId(driver.getId());
            for (TripExecution te : driverHistory) {
                if (te.getReturnedToWarehouseAt() != null) {
                    if (te.getTrip() != null && te.getTrip().getDeliveryDate() != null && te.getTrip().getDeliveryDate().isAfter(deliveryDate)) {
                        continue;
                    }
                    LocalDateTime earliestAvailable = te.getReturnedToWarehouseAt().plusMinutes(30);
                    if (newDeparture.isBefore(earliestAvailable)) {
                        throw new BusinessException(ErrorCode.DRIVER_CONFLICT,
                                "Tài xế " + driver.getFullName() + " về kho lúc "
                                        + te.getReturnedToWarehouseAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + ". Giờ xuất phát dự kiến " + newDeparture.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + " vi phạm thời gian giãn cách sau khi về kho (cần tối thiểu 30 phút).",
                                HttpStatus.CONFLICT);
                    }
                }
            }
        }
    }

    private String validateVehicleStoreWeight(Vehicle vehicle, List<TripDraftStop> stops) {
        if (stops == null || vehicle == null || vehicle.getPayloadKg() == null) {
            return null;
        }
        for (TripDraftStop stop : stops) {
            if (Boolean.TRUE.equals(stop.getIsActive()) && stop.getStore() != null) {
                Store store = stop.getStore();
                if (store.getMaxAllowedVehicleWeight() != null
                        && vehicle.getPayloadKg().compareTo(store.getMaxAllowedVehicleWeight()) > 0) {
                    return "Vehicle weight (" + vehicle.getPayloadKg() + " kg) exceeds store "
                            + store.getCode() + " limit (" + store.getMaxAllowedVehicleWeight() + " kg)";
                }
            }
        }
        return null;
    }

    private List<TripDraftStop> calculateGroupEtas(TripDraft draft, List<TripDraftStop> groupStops, LocalTime departureTime) {
        if (groupStops == null || groupStops.isEmpty()) {
            return Collections.emptyList();
        }

        List<TripDraftStop> sortedGroup = new ArrayList<>(groupStops);
        sortedGroup.sort(Comparator.comparingInt(TripDraftStop::getSequenceNo));

        LocalDate deliveryDate = draft.getDeliveryDate() != null ? draft.getDeliveryDate() : LocalDate.now();
        LocalTime depTime = departureTime != null ? departureTime : (draft.getPlannedDepartureTime() != null ? draft.getPlannedDepartureTime() : LocalTime.of(7, 0));
        LocalDateTime currentEta = LocalDateTime.of(deliveryDate, depTime);

        double warehouseLat = getSystemConfigDouble("WAREHOUSE_LAT", 21.028511);
        double warehouseLng = getSystemConfigDouble("WAREHOUSE_LNG", 105.804817);
        double avgSpeedKmh = getSystemConfigDouble("AVERAGE_SPEED_KMH", 30.0);

        double prevLat = warehouseLat;
        double prevLng = warehouseLng;

        List<TripDraftStop> recalculatedStops = new ArrayList<>();
        List<Order> groupDraftOrders = orderRepository.findByTripDraftId(draft.getId());

        for (int i = 0; i < sortedGroup.size(); i++) {
            TripDraftStop original = sortedGroup.get(i);
            Store store = original.getStore();

            double stopLat = (store != null && store.getLatitude() != null) ? store.getLatitude() : prevLat;
            double stopLng = (store != null && store.getLongitude() != null) ? store.getLongitude() : prevLng;

            if (i > 0) {
                TripDraftStop prev = sortedGroup.get(i - 1);
                int prevServiceMin = (prev.getRouteStop() != null && prev.getRouteStop().getAvgServiceTimeMin() != null)
                        ? prev.getRouteStop().getAvgServiceTimeMin() : 15;
                currentEta = currentEta.plusMinutes(prevServiceMin);
            }

            double distanceKm = haversineDistance(prevLat, prevLng, stopLat, stopLng);
            long travelMinutes = Math.round((distanceKm / avgSpeedKmh) * 60);
            currentEta = currentEta.plusMinutes(travelMinutes);

            BigDecimal distKmBd = BigDecimal.valueOf(distanceKm).setScale(2, RoundingMode.HALF_UP);
            int travelMinInt = (int) travelMinutes;

            LocalTime twStart = store != null ? store.getTimeWindowStart() : null;
            int waitingTimeMin = 0;
            LocalTime arrivalTime = currentEta.toLocalTime();
            if (twStart != null && arrivalTime.isBefore(twStart)) {
                long waitMin = java.time.temporal.ChronoUnit.MINUTES.between(arrivalTime, twStart);
                waitingTimeMin = (int) waitMin;
                if (waitMin <= 30) {
                    currentEta = LocalDateTime.of(currentEta.toLocalDate(), twStart);
                }
            }

            String violationCode = constraintValidationService.validateStopEta(
                    TripDraftStop.builder()
                            .id(original.getId())
                            .store(store)
                            .routeStop(original.getRouteStop())
                            .sequenceNo(original.getSequenceNo())
                            .plannedEta(currentEta)
                            .plannedWaitingTimeMin(waitingTimeMin)
                            .build(),
                    groupDraftOrders
            );

            TripDraftStop recalculated = TripDraftStop.builder()
                    .id(original.getId())
                    .tripDraft(original.getTripDraft())
                    .store(original.getStore())
                    .routeStop(original.getRouteStop())
                    .sequenceNo(original.getSequenceNo())
                    .orderCount(original.getOrderCount())
                    .isActive(original.getIsActive())
                    .overrideNote(original.getOverrideNote())
                    .plannedEta(currentEta)
                    .distanceFromPrevKm(distKmBd)
                    .travelTimeFromPrevMin(travelMinInt)
                    .plannedWaitingTimeMin(waitingTimeMin)
                    .violationCode(violationCode)
                    .build();

            recalculatedStops.add(recalculated);

            prevLat = stopLat;
            prevLng = stopLng;
        }

        return recalculatedStops;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double getSystemConfigDouble(String key, double defaultValue) {
        try {
            return systemConfigRepository.findByConfigKey(key)
                    .map(sc -> Double.parseDouble(sc.getConfigValue()))
                    .orElse(defaultValue);
        } catch (Exception e) {
            return defaultValue;
        }
    }
}


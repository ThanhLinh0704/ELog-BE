package com.elog.service.impl;

import com.elog.dto.request.TripAssignRequest;
import com.elog.dto.request.TripSplitAssignRequest;
import com.elog.dto.request.TripAssignmentPatchRequest;
import com.elog.dto.response.*;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripServiceImpl implements TripService {

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

    // ── US-15 TASK-02 ──────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public EligibleVehiclesResponse getEligibleVehicles(Long tripDraftId) {
        TripDraft td = findTripDraftOrThrow(tripDraftId);
        if (!"VALIDATED".equals(td.getStatus())) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_NOT_VALIDATED,
                    "Trip Draft must be VALIDATED before assignment.", HttpStatus.BAD_REQUEST);
        }

        List<Vehicle> activeVehicles = vehicleRepository.findByIsActiveTrue();
        List<EligibleVehicleDto> eligibleVehicles = new ArrayList<>();
        List<IneligibleVehicleDto> ineligibleVehicles = new ArrayList<>();

        for (Vehicle v : activeVehicles) {
            if (v.getMaxVolumeM3() == null || v.getPayloadKg() == null) {
                continue;
            }

            boolean volumeOk = v.getMaxVolumeM3().compareTo(td.getTotalVolumeM3()) >= 0;
            boolean weightOk = v.getPayloadKg().compareTo(td.getTotalWeightKg()) >= 0;
            String storeWeightViolationReason = validateVehicleStoreWeight(v, td.getStops());
            boolean routeWeightOk = (storeWeightViolationReason == null);

            if (volumeOk && weightOk && routeWeightOk) {
                eligibleVehicles.add(EligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getPayloadKg())
                        .remainingVolumeM3(v.getMaxVolumeM3().subtract(td.getTotalVolumeM3()))
                        .remainingWeightKg(v.getPayloadKg().subtract(td.getTotalWeightKg()))
                        .build());
            } else {
                StringBuilder reason = new StringBuilder();
                if (!volumeOk) {
                    reason.append("Volume exceeds capacity (")
                          .append(td.getTotalVolumeM3()).append(" m³ > ").append(v.getMaxVolumeM3()).append(" m³)");
                }
                if (!weightOk) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append("Weight exceeds capacity (")
                          .append(td.getTotalWeightKg()).append(" kg > ").append(v.getPayloadKg()).append(" kg)");
                }
                if (!routeWeightOk) {
                    if (reason.length() > 0) reason.append(" and ");
                    reason.append(storeWeightViolationReason);
                }

                ineligibleVehicles.add(IneligibleVehicleDto.builder()
                        .vehicleId(v.getId())
                        .plateNumber(v.getPlateNumber())
                        .vehicleType(v.getVehicleType())
                        .maxVolumeM3(v.getMaxVolumeM3())
                        .maxWeightKg(v.getPayloadKg())
                        .volumeCheckResult(volumeOk ? ConstraintResult.PASS : ConstraintResult.FAIL)
                        .weightCheckResult((weightOk && routeWeightOk) ? ConstraintResult.PASS : ConstraintResult.FAIL)
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

        return drivers.stream().map(d -> {
            boolean busyOnDate = tripRepository.existsByDriverIdAndDeliveryDateAndStatusIn(
                    d.getId(), date, busyStatuses);

            // Must match DRIVER_CONFLICT condition in validateVehicleAndDriverAvailability():
            // driver with unreturned TripExecution (returnedToWarehouseAt IS NULL) is NOT available,
            // regardless of delivery date.
            List<TripExecution> unreturned = tripExecutionRepository.findUnreturnedByDriverId(d.getId());

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

        if (tripRepository.existsByTripDraftId(tripDraftId)) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED,
                    "Trip Draft has already been assigned.", HttpStatus.CONFLICT);
        }

        // Guard 1: TripDraft must be VALIDATED
        if (!"VALIDATED".equals(td.getStatus())) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_NOT_VALIDATED,
                    "Trip Draft must be VALIDATED before assignment.", HttpStatus.BAD_REQUEST);
        }

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

        // Guard 2: Vehicle must be eligible (dual-constraint & route weight limit)
        if (vehicle.getMaxVolumeM3().compareTo(td.getTotalVolumeM3()) < 0
                || vehicle.getPayloadKg().compareTo(td.getTotalWeightKg()) < 0) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                    "Vehicle " + vehicle.getPlateNumber()
                            + " does not meet dual-constraint requirements for this trip.",
                    HttpStatus.BAD_REQUEST);
        }
        String storeWeightViolation = validateVehicleStoreWeight(vehicle, td.getStops());
        if (storeWeightViolation != null) {
            throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                    "Vehicle " + vehicle.getPlateNumber() + " violates route constraints: " + storeWeightViolation,
                    HttpStatus.BAD_REQUEST);
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

        if (tripRepository.existsByTripDraftId(tripDraftId)) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_ALREADY_ASSIGNED,
                    "Trip Draft has already been assigned.", HttpStatus.CONFLICT);
        }

        if (!"VALIDATED".equals(td.getStatus())) {
            throw new BusinessException(ErrorCode.TRIP_DRAFT_NOT_VALIDATED,
                    "Trip Draft must be VALIDATED before assignment.", HttpStatus.BAD_REQUEST);
        }

        User dispatcher = findUserByUsernameOrThrow(currentUsername);
        List<TripSplitResponse.TripSummary> summaries = new ArrayList<>();
        List<TripStatus> busyStatuses = List.of(TripStatus.VALIDATED, TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);

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
            User driver = findDriverOrThrow(assignment.getDriverId());

            // Vehicle & driver availability check (unreturned, active trip, time overlap)
            validateVehicleAndDriverAvailability(vehicle, driver, td.getDeliveryDate(), td.getPlannedDepartureTime(), null);

            // Get the specific stops for this split group
            List<TripDraftStop> groupStops = assignment.getStopIds().stream()
                    .map(stopId -> tripDraftStopRepository.findById(stopId)
                            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                                    "TripDraftStop " + stopId + " not found.", HttpStatus.NOT_FOUND)))
                    .sorted(Comparator.comparing(TripDraftStop::getSequenceNo))
                    .toList();

            // Calculate group totals
            BigDecimal groupWeight = BigDecimal.ZERO;
            BigDecimal groupVolume = BigDecimal.ZERO;
            for (TripDraftStop stop : groupStops) {
                List<OrderItem> items = orderItemRepository.findByStopForManifest(
                        stop.getStore().getId(), tripDraftId);
                for (OrderItem item : items) {
                    groupWeight = groupWeight.add(item.getLineWeightKg());
                    groupVolume = groupVolume.add(item.getLineVolumeM3());
                }
            }

            // Validate vehicle capacity for this group
            if (vehicle.getMaxVolumeM3().compareTo(groupVolume) < 0
                    || vehicle.getPayloadKg().compareTo(groupWeight) < 0) {
                throw new BusinessException(ErrorCode.VEHICLE_NOT_ELIGIBLE,
                        "Vehicle " + vehicle.getPlateNumber()
                                + " cannot carry the assigned stops (volume: " + groupVolume
                                + " m³, weight: " + groupWeight + " kg).",
                        HttpStatus.BAD_REQUEST);
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
                    td.getStatus(), td.getStatus(), "Phân công 2 xe: Xe " + vehicle.getPlateNumber() + " cho sub-trip " + trip.getTripId(),
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
        response.setHandoverSlipUrl("/api/trips/" + tripId + "/handover-slip");
        return response;
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
        html.append("<td><b>Dispatch lúc:</b> ")
                .append(trip.getLockedAt() != null ? trip.getLockedAt().format(dtTimeFmt) : "N/A")
                .append("</td></tr></table>");

        html.append("<h3>DANH SÁCH ĐIỂM GIAO (theo thứ tự tuyến)</h3>");
        html.append("<table><tr><th>#</th><th>Điểm giao</th><th>ETA</th><th>Trạng thái</th></tr>");
        for (TripStop stop : stops) {
            html.append("<tr><td>").append(stop.getSequenceOrder()).append("</td>");
            html.append("<td>").append(stop.getRouteStop().getStore().getName()).append("</td>");
            html.append("<td>").append(stop.getPlannedEta() != null
                    ? stop.getPlannedEta().format(timeFmt) : "N/A").append("</td>");
            html.append("<td>").append(stop.getStatus()).append("</td></tr>");
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
                .tripStopCount(stops.size())
                .manifestId(manifestId)
                .tripStops(stops.stream().map(this::buildTripStopResponse).toList())
                .message(message)
                .build();
    }

    private TripStopResponse buildTripStopResponse(TripStop ts) {
        return TripStopResponse.builder()
                .tripStopId(ts.getTripStopId())
                .routeStopId(ts.getRouteStop().getId())
                .tripDraftStopId(ts.getTripDraftStop().getId())
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
                    LocalDateTime earliestAvailable = te.getReturnedToWarehouseAt().plusMinutes(30);
                    if (newDeparture.isBefore(earliestAvailable)) {
                        throw new BusinessException(ErrorCode.VEHICLE_CONFLICT,
                                "Vehicle " + vehicle.getPlateNumber() + " returned to warehouse at "
                                        + te.getReturnedToWarehouseAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + ". New departure at " + newDeparture.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + " violates return window.",
                                HttpStatus.CONFLICT);
                    }
                }
            }

            List<TripExecution> driverHistory = tripExecutionRepository.findByDriverId(driver.getId());
            for (TripExecution te : driverHistory) {
                if (te.getReturnedToWarehouseAt() != null) {
                    LocalDateTime earliestAvailable = te.getReturnedToWarehouseAt().plusMinutes(30);
                    if (newDeparture.isBefore(earliestAvailable)) {
                        throw new BusinessException(ErrorCode.DRIVER_CONFLICT,
                                "Driver " + driver.getFullName() + " returned to warehouse at "
                                        + te.getReturnedToWarehouseAt().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + ". New departure at " + newDeparture.format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                                        + " violates return window.",
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
}


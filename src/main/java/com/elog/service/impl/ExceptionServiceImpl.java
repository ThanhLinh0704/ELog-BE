package com.elog.service.impl;

import com.elog.dto.response.exception.DeliveryExceptionResponse.ReporterInfo;
import com.elog.dto.response.exception.ExceptionListResponse.ExceptionItem;
import com.elog.dto.request.exception.RejectStopRequest;
import com.elog.dto.request.exception.ResolveExceptionRequest;
import com.elog.dto.response.exception.DeliveryExceptionResponse;
import com.elog.dto.response.exception.ExceptionListResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.*;
import com.elog.service.ExceptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ExceptionServiceImpl implements ExceptionService {

    private static final Long   SYSTEM_USER_ID   = 1L;
    private static final String SYSTEM_USER_NAME  = "System";
    private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final TripStopRepository          tripStopRepo;
    private final TripRepository              tripRepo;
    private final UserRepository              userRepo;
    private final DeliveryExceptionRepository deliveryExceptionRepo;
    private final TripExecutionRepository     tripExecutionRepo;
    private final DeliveryOrderResultRepository deliveryOrderResultRepo;

    @Override
    public DeliveryExceptionResponse rejectStop(Long tripStopId, RejectStopRequest request, String currentUsername) {
        TripStop stop = tripStopRepo.findById(tripStopId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_STOP_NOT_FOUND,
                        "TripStop " + tripStopId + " not found", HttpStatus.NOT_FOUND));

        Trip trip = stop.getTrip();

        if (!trip.getDriver().getUsername().equals(currentUsername)) {
            throw new BusinessException(ErrorCode.NOT_YOUR_TRIP,
                    "You are not the driver of this trip.", HttpStatus.FORBIDDEN);
        }

        if (stop.getStatus() != TripStopStatus.IN_PROGRESS) {
            throw new BusinessException(ErrorCode.STOP_NOT_IN_PROGRESS,
                    "Please call /arrive first before recording a rejection. Current status: " + stop.getStatus(),
                    HttpStatus.CONFLICT);
        }

        if (deliveryExceptionRepo.existsByTripStopIdAndExceptionTypeAndResolvedAtIsNull(
                tripStopId, ExceptionType.DELIVERY_REJECTION)) {
            Long existingId = deliveryExceptionRepo.findByTripStopIdOrderByCreatedAtDesc(tripStopId).stream()
                    .filter(e -> e.getExceptionType() == ExceptionType.DELIVERY_REJECTION)
                    .map(DeliveryException::getExceptionId)
                    .findFirst().orElse(null);
            throw new BusinessException(ErrorCode.REJECTION_ALREADY_RECORDED,
                    "A rejection has already been recorded for this stop. Existing exception id: " + existingId,
                    HttpStatus.CONFLICT);
        }

        String rejectionType = request.getRejectionType().toUpperCase().trim();
        if ("OTHER".equals(rejectionType) &&
                (request.getDescription() == null || request.getDescription().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "description is required when rejectionType is OTHER", HttpStatus.BAD_REQUEST);
        }

        User driver = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND,
                        "Driver not found: " + currentUsername, HttpStatus.NOT_FOUND));

        DeliveryException ex = DeliveryException.builder()
                .tripStopId(tripStopId)
                .exceptionType(ExceptionType.DELIVERY_REJECTION)
                .reportedBy(driver.getId())
                .description(buildDescription(rejectionType, request.getDescription()))
                .build();
        ex = deliveryExceptionRepo.save(ex);

        stop.setStatus(TripStopStatus.EXCEPTION);
        tripStopRepo.save(stop);

        if (allStopsDone(trip)) {
            trip.setStatus(TripStatus.COMPLETED);
            trip.setCompletedAt(LocalDateTime.now());
            tripRepo.save(trip);
            log.info("Trip {} auto-completed after DELIVERY_REJECTION on stop {}", trip.getTripId(), tripStopId);
        }

        log.info("DELIVERY_REJECTION recorded (exceptionId={}) for TripStop {} by driver {}",
                ex.getExceptionId(), tripStopId, currentUsername);

        return buildDetailResponse(ex, stop, driver, null,
                "Rejection recorded. Goods remain on truck for return to warehouse.");
    }

    @Override
    @Transactional(readOnly = true)
    public ExceptionListResponse listExceptions(LocalDate date, String type, String resolved) {
        ExceptionType exceptionType = parseExceptionType(type);
        Boolean resolvedFlag = parseResolved(resolved);

        List<DeliveryException> exceptions = deliveryExceptionRepo.findByFilters(date, exceptionType, resolvedFlag);

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();
            boolean isDriver = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                    .anyMatch(a -> "ROLE_DRIVER".equals(a.getAuthority()) || "trip:execute".equals(a.getAuthority()));

            if (isDriver && currentUsername != null && !"anonymousUser".equals(currentUsername)) {
                User driverUser = userRepo.findByUsername(currentUsername).orElse(null);
                Long driverId = driverUser != null ? driverUser.getId() : null;

                List<Long> stopIds = exceptions.stream().map(DeliveryException::getTripStopId).filter(Objects::nonNull).distinct().toList();
                List<Long> executionIds = exceptions.stream().map(DeliveryException::getTripExecutionId).filter(Objects::nonNull).distinct().toList();

                Map<Long, TripStop> stopMap = stopIds.isEmpty() ? Collections.emptyMap() : tripStopRepo.findAllById(stopIds).stream().collect(Collectors.toMap(TripStop::getTripStopId, s -> s));
                Map<Long, TripExecution> executionMap = executionIds.isEmpty() ? Collections.emptyMap() : tripExecutionRepo.findAllById(executionIds).stream().collect(Collectors.toMap(TripExecution::getId, e -> e));

                exceptions = exceptions.stream().filter(e -> {
                    if (driverId != null && driverId.equals(e.getReportedBy())) {
                        return true;
                    }
                    if (e.getTripStopId() != null) {
                        TripStop ts = stopMap.get(e.getTripStopId());
                        if (ts != null && ts.getTrip() != null && ts.getTrip().getDriver() != null) {
                            return currentUsername.equals(ts.getTrip().getDriver().getUsername());
                        }
                    }
                    if (e.getTripExecutionId() != null) {
                        TripExecution te = executionMap.get(e.getTripExecutionId());
                        if (te != null && te.getDriver() != null) {
                            return currentUsername.equals(te.getDriver().getUsername());
                        }
                    }
                    return false;
                }).toList();
            }
        }

        long unresolvedCount = exceptions.stream().filter(e -> e.getResolvedAt() == null).count();

        return ExceptionListResponse.builder()
                .date(date != null ? date.toString() : LocalDate.now().toString())
                .totalCount(exceptions.size())
                .unresolvedCount((int) unresolvedCount)
                .exceptions(exceptions.stream().map(this::buildListItem).toList())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DeliveryExceptionResponse getException(Long exceptionId) {
        DeliveryException ex = findExceptionOrThrow(exceptionId);
        TripStop stop = ex.getTripStopId() != null
                ? tripStopRepo.findById(ex.getTripStopId()).orElse(null)
                : null;
        User reporter = resolveUser(ex.getReportedBy());
        User resolver = ex.getResolvedBy() != null ? resolveUser(ex.getResolvedBy()) : null;
        return buildDetailResponse(ex, stop, reporter, resolver, null);
    }

    @Override
    public DeliveryExceptionResponse resolveException(Long exceptionId, ResolveExceptionRequest request, String currentUsername) {
        DeliveryException ex = findExceptionOrThrow(exceptionId);

        if (ex.getResolvedAt() != null) {
            throw new BusinessException(ErrorCode.EXCEPTION_ALREADY_RESOLVED,
                    "Exception " + exceptionId + " was already resolved at " + ex.getResolvedAt().format(DT_FMT),
                    HttpStatus.CONFLICT);
        }

        User manager = userRepo.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND,
                        "User not found: " + currentUsername, HttpStatus.NOT_FOUND));

        ex.setResolvedAt(LocalDateTime.now());
        ex.setResolvedBy(manager.getId());
        ex.setResolutionNotes(request.getResolutionNotes());
        deliveryExceptionRepo.save(ex);

        log.info("Exception {} resolved by {} at {}", exceptionId, currentUsername, ex.getResolvedAt().format(DT_FMT));

        TripStop stop = ex.getTripStopId() != null
                ? tripStopRepo.findById(ex.getTripStopId()).orElse(null)
                : null;

        return buildDetailResponse(ex, stop, resolveUser(ex.getReportedBy()), manager, "Exception resolved.");
    }

    private DeliveryException findExceptionOrThrow(Long exceptionId) {
        return deliveryExceptionRepo.findById(exceptionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EXCEPTION_NOT_FOUND,
                        "Exception " + exceptionId + " not found", HttpStatus.NOT_FOUND));
    }

    /** Format: "[STORE_CLOSED] userDescription" — prefix sẽ được parse lại khi response */
    private String buildDescription(String rejectionType, String userDescription) {
        String prefix = "[" + rejectionType + "]";
        return (userDescription != null && !userDescription.isBlank())
                ? prefix + " " + userDescription.trim()
                : prefix;
    }

    /** Parse "[TYPE]" prefix từ description — null nếu không phải DELIVERY_REJECTION */
    private String parseRejectionType(DeliveryException ex) {
        if (ex.getExceptionType() != ExceptionType.DELIVERY_REJECTION) return null;
        String desc = ex.getDescription();
        if (desc != null && desc.startsWith("[") && desc.contains("]")) {
            return desc.substring(1, desc.indexOf(']'));
        }
        return null;
    }

    /** Trả về description sau khi bỏ prefix "[TYPE] " */
    private String cleanDescription(DeliveryException ex) {
        String desc = ex.getDescription();
        if (desc == null) return null;
        if (desc.startsWith("[") && desc.contains("] ")) {
            return desc.substring(desc.indexOf("] ") + 2);
        }
        return desc;
    }

    private boolean allStopsDone(Trip trip) {
        return trip.getStops().stream()
                .allMatch(s -> s.getStatus() == TripStopStatus.COMPLETED
                            || s.getStatus() == TripStopStatus.EXCEPTION);
    }

    private ExceptionType parseExceptionType(String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) return null;
        try {
            return ExceptionType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Invalid type filter: " + type + ". Use ALL, TIME_EXCEPTION, or DELIVERY_REJECTION",
                    HttpStatus.BAD_REQUEST);
        }
    }

    private Boolean parseResolved(String resolved) {
        if (resolved == null || "all".equalsIgnoreCase(resolved)) return null;
        if ("true".equalsIgnoreCase(resolved))  return Boolean.TRUE;
        if ("false".equalsIgnoreCase(resolved)) return Boolean.FALSE;
        throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                "Invalid resolved filter: " + resolved + ". Use true, false, or all", HttpStatus.BAD_REQUEST);
    }

    private User resolveUser(Long userId) {
        if (userId == null) return null;
        return userRepo.findById(userId).orElse(null);
    }

    private String getStoreCode(TripStop stop) {
        return stop.getRouteStop() != null && stop.getRouteStop().getStore() != null
                ? stop.getRouteStop().getStore().getCode()
                : "STOP-" + stop.getTripStopId();
    }

    private String getStoreName(TripStop stop) {
        return stop.getRouteStop() != null && stop.getRouteStop().getStore() != null
                ? stop.getRouteStop().getStore().getName()
                : null;
    }

    private DeliveryExceptionResponse.ReporterInfo buildReporterInfo(User user) {
        if (user == null) {
            return DeliveryExceptionResponse.ReporterInfo.builder()
                    .userId(SYSTEM_USER_ID).fullName(SYSTEM_USER_NAME).build();
        }
        return DeliveryExceptionResponse.ReporterInfo.builder()
                .userId(user.getId()).fullName(user.getFullName()).build();
    }

    private Long computeDelayMinutes(TripStop stop) {
        if (stop == null || stop.getActualArrivalTime() == null || stop.getPlannedEta() == null) return null;
        long delay = ChronoUnit.MINUTES.between(stop.getPlannedEta(), stop.getActualArrivalTime());
        return delay > 0 ? delay : null;
    }

    private DeliveryExceptionResponse buildDetailResponse(
            DeliveryException ex, TripStop stop, User reporter, User resolver, String message) {
        Long tripId = null;
        String fixedRouteCode = null;
        String vehicleCode = null;
        String storeCode = null;
        String storeName = null;
        String tripStopStatus = null;
        String plannedEta = null;
        String actualArrivalTime = null;
        Long delayMinutes = null;

        if (stop != null) {
            Trip trip = stop.getTrip();
            if (trip != null) {
                tripId = trip.getTripId();
                if (trip.getRoute() != null) fixedRouteCode = trip.getRoute().getCode();
                if (trip.getVehicle() != null) vehicleCode = trip.getVehicle().getPlateNumber();
            }
            storeCode = getStoreCode(stop);
            storeName = getStoreName(stop);
            if (stop.getStatus() != null) tripStopStatus = stop.getStatus().name();
            if (stop.getPlannedEta() != null) plannedEta = stop.getPlannedEta().format(DT_FMT);
            if (stop.getActualArrivalTime() != null) actualArrivalTime = stop.getActualArrivalTime().format(DT_FMT);
            delayMinutes = computeDelayMinutes(stop);
        } else if (ex.getTripExecutionId() != null) {
            TripExecution te = tripExecutionRepo.findById(ex.getTripExecutionId()).orElse(null);
            if (te != null && te.getTrip() != null) {
                Trip trip = te.getTrip();
                tripId = trip.getTripId();
                if (trip.getRoute() != null) fixedRouteCode = trip.getRoute().getCode();
                if (trip.getVehicle() != null) vehicleCode = trip.getVehicle().getPlateNumber();
            }
            if (ex.getOrderId() != null) {
                DeliveryOrderResult res = deliveryOrderResultRepo
                        .findByTripExecutionIdAndOrderId(ex.getTripExecutionId(), ex.getOrderId()).orElse(null);
                if (res != null && res.getStop() != null) {
                    TripDraftStop tds = res.getStop();
                    if (tds.getStore() != null) {
                        storeCode = tds.getStore().getCode();
                        storeName = tds.getStore().getName();
                    }
                    if (tds.getPlannedEta() != null) {
                        plannedEta = tds.getPlannedEta().format(DT_FMT);
                    }
                }
            }
        }

        return DeliveryExceptionResponse.builder()
                .exceptionId(ex.getExceptionId())
                .exceptionType(ex.getExceptionType().name())
                .rejectionType(parseRejectionType(ex))
                .tripStopId(stop != null ? stop.getTripStopId() : null)
                .storeCode(storeCode)
                .storeName(storeName)
                .tripStopStatus(tripStopStatus)
                .tripId(tripId)
                .fixedRouteCode(fixedRouteCode)
                .vehicleCode(vehicleCode)
                .plannedEta(plannedEta)
                .actualArrivalTime(actualArrivalTime)
                .delayMinutes(delayMinutes)
                .description(cleanDescription(ex))
                .reportedBy(buildReporterInfo(reporter))
                .createdAt(ex.getCreatedAt().format(DT_FMT))
                .resolvedAt(ex.getResolvedAt() != null ? ex.getResolvedAt().format(DT_FMT) : null)
                .resolvedBy(resolver != null ? buildReporterInfo(resolver) : null)
                .resolutionNotes(ex.getResolutionNotes())
                .message(message)
                .build();
    }

    private ExceptionListResponse.ExceptionItem buildListItem(DeliveryException ex) {
        TripStop stop = ex.getTripStopId() != null ? tripStopRepo.findById(ex.getTripStopId()).orElse(null) : null;

        Long tripId = null;
        String fixedRouteCode = null;
        String vehicleCode = null;
        String driverName = null;
        Long tripStopId = null;
        String storeCode = null;
        String storeName = null;
        String plannedEta = null;
        String actualArrivalTime = null;
        Long delayMinutes = null;

        if (stop != null) {
            Trip trip = stop.getTrip();
            if (trip != null) {
                tripId = trip.getTripId();
                if (trip.getRoute() != null) fixedRouteCode = trip.getRoute().getCode();
                if (trip.getVehicle() != null) vehicleCode = trip.getVehicle().getPlateNumber();
                if (trip.getDriver() != null) driverName = trip.getDriver().getFullName();
            }
            tripStopId = stop.getTripStopId();
            storeCode = getStoreCode(stop);
            storeName = getStoreName(stop);
            if (stop.getPlannedEta() != null) plannedEta = stop.getPlannedEta().format(DT_FMT);
            if (stop.getActualArrivalTime() != null) actualArrivalTime = stop.getActualArrivalTime().format(DT_FMT);
            delayMinutes = computeDelayMinutes(stop);
        } else if (ex.getTripExecutionId() != null) {
            TripExecution te = tripExecutionRepo.findById(ex.getTripExecutionId()).orElse(null);
            if (te != null) {
                if (te.getTrip() != null) {
                    Trip trip = te.getTrip();
                    tripId = trip.getTripId();
                    if (trip.getRoute() != null) fixedRouteCode = trip.getRoute().getCode();
                    if (trip.getVehicle() != null) vehicleCode = trip.getVehicle().getPlateNumber();
                    if (trip.getDriver() != null) driverName = trip.getDriver().getFullName();
                } else if (te.getDriver() != null) {
                    driverName = te.getDriver().getFullName();
                }
            }
            if (ex.getOrderId() != null) {
                DeliveryOrderResult res = deliveryOrderResultRepo
                        .findByTripExecutionIdAndOrderId(ex.getTripExecutionId(), ex.getOrderId()).orElse(null);
                if (res != null && res.getStop() != null) {
                    TripDraftStop tds = res.getStop();
                    if (tds.getStore() != null) {
                        storeCode = tds.getStore().getCode();
                        storeName = tds.getStore().getName();
                    }
                    if (tds.getPlannedEta() != null) {
                        plannedEta = tds.getPlannedEta().format(DT_FMT);
                    }
                }
            }
        }

        String reporterName = SYSTEM_USER_ID.equals(ex.getReportedBy())
                ? SYSTEM_USER_NAME
                : (resolveUser(ex.getReportedBy()) != null ? resolveUser(ex.getReportedBy()).getFullName() : "Unknown");
        String resolverName = ex.getResolvedBy() != null
                ? (resolveUser(ex.getResolvedBy()) != null ? resolveUser(ex.getResolvedBy()).getFullName() : "Unknown")
                : null;

        return ExceptionListResponse.ExceptionItem.builder()
                .exceptionId(ex.getExceptionId())
                .exceptionType(ex.getExceptionType().name())
                .rejectionType(parseRejectionType(ex))
                .tripId(tripId)
                .fixedRouteCode(fixedRouteCode)
                .vehicleCode(vehicleCode)
                .driverName(driverName)
                .tripStopId(tripStopId)
                .storeCode(storeCode)
                .storeName(storeName)
                .plannedEta(plannedEta)
                .actualArrivalTime(actualArrivalTime)
                .delayMinutes(delayMinutes)
                .description(cleanDescription(ex))
                .reportedBy(reporterName)
                .createdAt(ex.getCreatedAt().format(DT_FMT))
                .resolvedAt(ex.getResolvedAt() != null ? ex.getResolvedAt().format(DT_FMT) : null)
                .resolvedBy(resolverName)
                .resolutionNotes(ex.getResolutionNotes())
                .build();
    }
}

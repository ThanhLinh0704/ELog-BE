package com.elog.service.impl;

import com.elog.dto.request.DriverStatusUpdateRequest;
import com.elog.dto.response.ActiveTripWarningResponse;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.DriverResponse;
import com.elog.dto.response.DriverStatusHistoryResponse;
import com.elog.entity.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.DriverStatusHistoryRepository;
import com.elog.repository.TripRepository;
import com.elog.repository.UserRepository;
import com.elog.service.DriverStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DriverStatusServiceImpl implements DriverStatusService {

    private final UserRepository userRepository;
    private final DriverStatusHistoryRepository driverStatusHistoryRepository;
    private final TripRepository tripRepository;

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<DriverResponse>> getDrivers(String keyword, String status, Pageable pageable) {
        List<User> allUsers = userRepository.findAll();
        List<User> drivers = allUsers.stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> "DRIVER".equals(r.getName())))
                .filter(u -> {
                    if (status != null && !status.isBlank()) {
                        return u.getDriverStatus() != null && u.getDriverStatus().name().equalsIgnoreCase(status);
                    }
                    return true;
                })
                .filter(u -> {
                    if (keyword != null && !keyword.isBlank()) {
                        String kw = keyword.toLowerCase();
                        boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(kw);
                        boolean matchPhone = u.getPhoneNumber() != null && u.getPhoneNumber().toLowerCase().contains(kw);
                        boolean matchUsername = u.getUsername() != null && u.getUsername().toLowerCase().contains(kw);
                        return matchName || matchPhone || matchUsername;
                    }
                    return true;
                })
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), drivers.size());
        List<User> pageContent = (start <= drivers.size()) ? drivers.subList(start, end) : List.of();

        List<DriverResponse> responses = pageContent.stream()
                .map(this::toDriverResponse)
                .toList();

        return ApiResponse.success(responses, pageable.getPageNumber(), pageable.getPageSize(), drivers.size());
    }

    @Override
    @Transactional(readOnly = true)
    public DriverResponse getDriverById(Long driverId) {
        User driver = findDriverUserOrThrow(driverId);
        return toDriverResponse(driver);
    }

    @Override
    @Transactional
    public DriverResponse updateStatus(Long driverId, DriverStatusUpdateRequest request, String currentUsername) {
        User driver = findDriverUserOrThrow(driverId);
        User currentActor = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "Current user not found"));

        if (request.getStatus() == DriverStatus.INACTIVE) {
            if (request.getReasonCode() == null) {
                throw new BusinessException(ErrorCode.DRIVER_STATUS_REASON_REQUIRED,
                        "Reason code is required when deactivating a driver", HttpStatus.BAD_REQUEST);
            }
            if (request.getReasonCode() == DriverInactiveReasonCode.OTHER &&
                    (request.getReasonNote() == null || request.getReasonNote().isBlank())) {
                throw new BusinessException(ErrorCode.DRIVER_STATUS_REASON_REQUIRED,
                        "Reason note is required when reason code is OTHER", HttpStatus.BAD_REQUEST);
            }
        }

        DriverStatus statusBefore = driver.getDriverStatus();
        DriverStatus statusAfter = request.getStatus();

        if (statusBefore != statusAfter) {
            driver.setDriverStatus(statusAfter);
            if (statusAfter == DriverStatus.INACTIVE) {
                driver.setDriverInactiveReasonCode(request.getReasonCode());
                driver.setDriverInactiveReasonNote(request.getReasonNote());
            } else {
                driver.setDriverInactiveReasonCode(null);
                driver.setDriverInactiveReasonNote(null);
            }

            driver.setDriverStatusUpdatedAt(LocalDateTime.now());
            driver.setDriverStatusUpdatedBy(currentActor);
            userRepository.save(driver);

            DriverStatusHistory history = DriverStatusHistory.builder()
                    .driverId(driver.getId())
                    .statusBefore(statusBefore)
                    .statusAfter(statusAfter)
                    .reasonCode(statusAfter == DriverStatus.INACTIVE ? request.getReasonCode() : null)
                    .reasonNote(statusAfter == DriverStatus.INACTIVE ? request.getReasonNote() : null)
                    .changedBy(currentActor.getId())
                    .changedAt(LocalDateTime.now())
                    .build();

            driverStatusHistoryRepository.save(history);
        }

        return toDriverResponse(driver);
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<DriverStatusHistoryResponse>> getStatusHistory(Long driverId, Pageable pageable) {
        findDriverUserOrThrow(driverId);
        Page<DriverStatusHistory> page = driverStatusHistoryRepository.findByDriverId(driverId, pageable);

        List<DriverStatusHistoryResponse> responses = page.getContent().stream()
                .map(h -> {
                    String changerName = userRepository.findById(h.getChangedBy())
                            .map(User::getUsername)
                            .orElse("System");
                    return DriverStatusHistoryResponse.builder()
                            .id(h.getId())
                            .statusBefore(h.getStatusBefore() != null ? h.getStatusBefore().name() : null)
                            .statusAfter(h.getStatusAfter().name())
                            .reasonCode(h.getReasonCode() != null ? h.getReasonCode().name() : null)
                            .reasonNote(h.getReasonNote())
                            .changedByName(changerName)
                            .changedAt(h.getChangedAt())
                            .build();
                })
                .toList();

        return ApiResponse.success(responses, pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements());
    }

    private User findDriverUserOrThrow(Long driverId) {
        User user = userRepository.findById(driverId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRIVER_NOT_FOUND, "Driver not found with ID: " + driverId));
        boolean isDriver = user.getRoles().stream().anyMatch(r -> "DRIVER".equals(r.getName()));
        if (!isDriver) {
            throw new BusinessException(ErrorCode.DRIVER_NOT_FOUND, "User with ID " + driverId + " is not a driver");
        }
        return user;
    }

    private DriverResponse toDriverResponse(User driver) {
        List<ActiveTripWarningResponse> warnings = new ArrayList<>();
        if (driver.getDriverStatus() == DriverStatus.INACTIVE) {
            List<TripStatus> uncompletedStatuses = List.of(TripStatus.VALIDATED, TripStatus.DISPATCHED, TripStatus.IN_PROGRESS);
            List<Trip> activeTrips = tripRepository.findByDriverIdAndStatusIn(driver.getId(), uncompletedStatuses);
            warnings = activeTrips.stream()
                    .map(t -> ActiveTripWarningResponse.builder()
                            .tripId(t.getTripId())
                            .status(t.getStatus().name())
                            .deliveryDate(t.getDeliveryDate())
                            .routeCode(t.getRoute() != null ? t.getRoute().getCode() : null)
                            .build())
                    .toList();
        }

        String updatedByName = driver.getDriverStatusUpdatedBy() != null ? driver.getDriverStatusUpdatedBy().getUsername() : null;

        return DriverResponse.builder()
                .id(driver.getId())
                .fullName(driver.getFullName())
                .phoneNumber(driver.getPhoneNumber())
                .email(driver.getEmail())
                .driverStatus(driver.getDriverStatus() != null ? driver.getDriverStatus().name() : DriverStatus.ACTIVE.name())
                .reasonCode(driver.getDriverInactiveReasonCode() != null ? driver.getDriverInactiveReasonCode().name() : null)
                .reasonNote(driver.getDriverInactiveReasonNote())
                .statusUpdatedAt(driver.getDriverStatusUpdatedAt())
                .statusUpdatedByName(updatedByName)
                .activeTripsWarning(warnings)
                .build();
    }
}

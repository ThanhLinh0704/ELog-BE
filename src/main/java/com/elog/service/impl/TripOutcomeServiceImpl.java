package com.elog.service.impl;

import com.elog.dto.response.TripOutcomeResponse;
import com.elog.entity.Trip;
import com.elog.entity.TripExecution;
import com.elog.entity.TripOutcome;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.TripOutcomeRepository;
import com.elog.service.TripOutcomeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TripOutcomeServiceImpl implements TripOutcomeService {

    private final TripOutcomeRepository tripOutcomeRepo;
    private final com.elog.service.TripOutcomeHistoryService tripOutcomeHistoryService;

    @Override
    @Transactional(readOnly = true)
    public List<TripOutcomeResponse> getSubmittedOutcomes() {
        return tripOutcomeRepo.findByStatus("SUBMITTED").stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TripOutcomeResponse validateOutcome(Long outcomeId, String dispatcherUsername) {
        TripOutcome outcome = tripOutcomeRepo.findById(outcomeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy Trip Outcome với ID: " + outcomeId,
                        HttpStatus.NOT_FOUND));

        if (!"SUBMITTED".equals(outcome.getStatus()) && !"NEEDS_CORRECTION".equals(outcome.getStatus())) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Trip Outcome đã ở trạng thái: " + outcome.getStatus(),
                    HttpStatus.BAD_REQUEST);
        }

        String statusBefore = outcome.getStatus();
        outcome.setStatus("VALIDATED");
        outcome.setValidatedAt(LocalDateTime.now());
        outcome.setValidatedBy(dispatcherUsername);
        tripOutcomeRepo.save(outcome);

        TripExecution exec = outcome.getTripExecution();
        Trip trip = exec != null ? exec.getTrip() : null;

        tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                exec != null ? exec.getId() : null, trip != null ? trip.getTripId() : null,
                com.elog.entity.TripOutcomeEventType.OUTCOME_VALIDATED,
                com.elog.entity.PlanningActorType.USER, dispatcherUsername,
                statusBefore, "VALIDATED",
                null, null, null, null, null, null, null, null,
                trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                trip != null ? trip.getDeliveryDate() : null,
                exec != null && exec.getDriver() != null ? exec.getDriver().getUsername() : null
        ));

        log.info("Dispatcher {} validated Trip Outcome ID {}", dispatcherUsername, outcomeId);
        return toResponse(outcome);
    }

    @Override
    @Transactional
    public TripOutcomeResponse amendOutcome(Long outcomeId, String amendmentReason, String dispatcherUsername) {
        TripOutcome outcome = tripOutcomeRepo.findById(outcomeId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        "Không tìm thấy Trip Outcome với ID: " + outcomeId,
                        HttpStatus.NOT_FOUND));

        if (amendmentReason == null || amendmentReason.isBlank()) {
            throw new BusinessException(
                    ErrorCode.VALIDATION_FAILED,
                    "Bắt buộc nhập lý do điều chỉnh (amendmentReason)",
                    HttpStatus.BAD_REQUEST);
        }

        String statusBefore = outcome.getStatus();
        TripExecution exec = outcome.getTripExecution();
        Trip trip = exec != null ? exec.getTrip() : null;

        tripOutcomeHistoryService.record(new com.elog.service.TripOutcomeHistoryService.OutcomeEventInput(
                exec != null ? exec.getId() : null, trip != null ? trip.getTripId() : null,
                com.elog.entity.TripOutcomeEventType.OUTCOME_AMENDED,
                com.elog.entity.PlanningActorType.USER, dispatcherUsername,
                statusBefore, "NEEDS_CORRECTION",
                null, null, null, null, null, null, null, amendmentReason,
                trip != null && trip.getRoute() != null ? trip.getRoute().getCode() : null,
                trip != null ? trip.getDeliveryDate() : null,
                exec != null && exec.getDriver() != null ? exec.getDriver().getUsername() : null
        ));

        outcome.setStatus("NEEDS_CORRECTION");
        outcome.setAmendmentReason(amendmentReason);
        outcome.setVersion(outcome.getVersion() + 1);
        tripOutcomeRepo.save(outcome);

        log.info("Dispatcher {} amended Trip Outcome ID {}, new version {}", dispatcherUsername, outcomeId, outcome.getVersion());
        return toResponse(outcome);
    }

    private TripOutcomeResponse toResponse(TripOutcome outcome) {
        TripExecution exec = outcome.getTripExecution();
        Trip trip = exec != null ? exec.getTrip() : null;
        String tripCodeStr = trip != null ? "TRIP-" + trip.getTripId() : null;

        return TripOutcomeResponse.builder()
                .id(outcome.getId())
                .executionId(exec != null ? exec.getId() : null)
                .tripId(trip != null ? trip.getTripId() : null)
                .tripCode(tripCodeStr)
                .driverName(exec != null && exec.getDriver() != null ? exec.getDriver().getFullName() : null)
                .vehiclePlate(trip != null && trip.getVehicle() != null ? trip.getVehicle().getPlateNumber() : null)
                .status(outcome.getStatus())
                .totalOrders(outcome.getTotalOrders())
                .deliveredCount(outcome.getDeliveredCount())
                .failedCount(outcome.getFailedCount())
                .partialCount(outcome.getPartialCount())
                .submittedAt(outcome.getSubmittedAt())
                .validatedAt(outcome.getValidatedAt())
                .validatedBy(outcome.getValidatedBy())
                .amendmentReason(outcome.getAmendmentReason())
                .version(outcome.getVersion())
                .build();
    }
}

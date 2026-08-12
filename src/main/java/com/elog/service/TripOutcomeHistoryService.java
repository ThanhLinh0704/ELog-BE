package com.elog.service;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.TripOutcomeEventResponse;
import com.elog.entity.PlanningActorType;
import com.elog.entity.TripOutcomeEventType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface TripOutcomeHistoryService {

    void record(OutcomeEventInput input);

    ApiResponse<List<TripOutcomeEventResponse>> search(OutcomeHistoryFilter filter, Pageable pageable, String currentUsername, boolean isDriver);

    record OutcomeEventInput(
            Long tripExecutionId,
            Long tripId,
            TripOutcomeEventType eventType,
            PlanningActorType actorType,
            String actorUsername,
            String statusBefore,
            String statusAfter,
            Long orderId,
            String orderRef,
            Long stopId,
            String storeCode,
            String deliveryResult,
            String reasonCode,
            String exceptionText,
            String validationNote,
            String routeCode,
            LocalDate deliveryDate,
            String driverUsername
    ) {}

    record OutcomeHistoryFilter(
            Long tripId,
            String driverUsername,
            String routeCode,
            LocalDate deliveryDate,
            String storeCode,
            String deliveryResult,
            TripOutcomeEventType eventType,
            LocalDateTime fromDate,
            LocalDateTime toDate
    ) {
        public OutcomeHistoryFilter withDriverUsername(String newDriverUsername) {
            return new OutcomeHistoryFilter(tripId, newDriverUsername, routeCode, deliveryDate, storeCode, deliveryResult, eventType, fromDate, toDate);
        }
    }
}

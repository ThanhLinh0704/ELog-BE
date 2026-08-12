package com.elog.service;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.PlanningEventResponse;
import com.elog.entity.PlanningActorType;
import com.elog.entity.PlanningEventType;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PlanningHistoryService {

    void record(PlanningEventInput input);

    ApiResponse<List<PlanningEventResponse>> search(PlanningHistoryFilter filter, Pageable pageable);

    record PlanningEventInput(
            Long tripDraftId,
            Long tripId,
            PlanningEventType eventType,
            PlanningActorType actorType,
            String actorUsername,
            String statusBefore,
            String statusAfter,
            String changeSummary,
            Object changeDetail,
            String note,
            Integer planVersion,
            String optionCode,
            String routeCode,
            LocalDate deliveryDate
    ) {}

    record PlanningHistoryFilter(
            Long tripDraftId,
            Long tripId,
            String routeCode,
            LocalDate deliveryDate,
            PlanningEventType eventType,
            String actorUsername,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            String status
    ) {}
}

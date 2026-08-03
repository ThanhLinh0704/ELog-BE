package com.elog.service.impl;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.PlanningEventResponse;
import com.elog.entity.PlanningActorType;
import com.elog.entity.TripPlanningEvent;
import com.elog.entity.User;
import com.elog.repository.TripPlanningEventRepository;
import com.elog.repository.UserRepository;
import com.elog.repository.specification.TripPlanningEventSpecification;
import com.elog.service.PlanningHistoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlanningHistoryServiceImpl implements PlanningHistoryService {

    private final TripPlanningEventRepository tripPlanningEventRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(PlanningEventInput input) {
        try {
            Long actorId = null;
            String actorRole = null;

            if (input.actorType() == PlanningActorType.USER && input.actorUsername() != null) {
                User user = userRepository.findByUsername(input.actorUsername()).orElse(null);
                if (user != null) {
                    actorId = user.getId();
                    actorRole = user.getRoles().stream()
                            .findFirst()
                            .map(r -> r.getName())
                            .orElse(null);
                }
            }

            String detailJson = null;
            if (input.changeDetail() != null) {
                detailJson = objectMapper.writeValueAsString(input.changeDetail());
            }

            TripPlanningEvent event = TripPlanningEvent.builder()
                    .tripDraftId(input.tripDraftId())
                    .tripId(input.tripId())
                    .eventType(input.eventType())
                    .actorType(input.actorType())
                    .actorId(actorId)
                    .actorUsername(input.actorUsername())
                    .actorRole(actorRole)
                    .statusBefore(input.statusBefore())
                    .statusAfter(input.statusAfter())
                    .changeSummary(input.changeSummary())
                    .changeDetail(detailJson)
                    .note(input.note())
                    .planVersion(input.planVersion())
                    .optionCode(input.optionCode())
                    .routeCode(input.routeCode())
                    .deliveryDate(input.deliveryDate())
                    .build();

            tripPlanningEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record trip planning history event: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<PlanningEventResponse>> search(PlanningHistoryFilter filter, Pageable pageable) {
        Specification<TripPlanningEvent> spec = Specification.where(TripPlanningEventSpecification.hasTripDraftId(filter.tripDraftId()))
                .and(TripPlanningEventSpecification.hasTripId(filter.tripId()))
                .and(TripPlanningEventSpecification.hasRouteCode(filter.routeCode()))
                .and(TripPlanningEventSpecification.hasDeliveryDate(filter.deliveryDate()))
                .and(TripPlanningEventSpecification.hasEventType(filter.eventType()))
                .and(TripPlanningEventSpecification.hasActor(filter.actorUsername()))
                .and(TripPlanningEventSpecification.occurredBetween(filter.fromDate(), filter.toDate()))
                .and(TripPlanningEventSpecification.hasStatusAfter(filter.status()));

        Page<TripPlanningEvent> page = tripPlanningEventRepository.findAll(spec, pageable);

        List<PlanningEventResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.success(responses, pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements());
    }

    private PlanningEventResponse toResponse(TripPlanningEvent event) {
        Object parsedDetail = null;
        if (event.getChangeDetail() != null) {
            try {
                parsedDetail = objectMapper.readValue(event.getChangeDetail(), Object.class);
            } catch (Exception e) {
                parsedDetail = event.getChangeDetail();
            }
        }

        return PlanningEventResponse.builder()
                .id(event.getId())
                .tripDraftId(event.getTripDraftId())
                .tripId(event.getTripId())
                .eventType(event.getEventType().name())
                .actorType(event.getActorType().name())
                .actorUsername(event.getActorUsername())
                .actorRole(event.getActorRole())
                .occurredAt(event.getOccurredAt())
                .statusBefore(event.getStatusBefore())
                .statusAfter(event.getStatusAfter())
                .changeSummary(event.getChangeSummary())
                .changeDetail(parsedDetail)
                .note(event.getNote())
                .planVersion(event.getPlanVersion())
                .optionCode(event.getOptionCode())
                .routeCode(event.getRouteCode())
                .deliveryDate(event.getDeliveryDate())
                .build();
    }
}

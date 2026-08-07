package com.elog.service.impl;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.TripOutcomeEventResponse;
import com.elog.entity.PlanningActorType;
import com.elog.entity.TripOutcomeEvent;
import com.elog.entity.User;
import com.elog.repository.TripOutcomeEventRepository;
import com.elog.repository.UserRepository;
import com.elog.repository.specification.TripOutcomeEventSpecification;
import com.elog.service.TripOutcomeHistoryService;
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
public class TripOutcomeHistoryServiceImpl implements TripOutcomeHistoryService {

    private final TripOutcomeEventRepository tripOutcomeEventRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(OutcomeEventInput input) {
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

            TripOutcomeEvent event = TripOutcomeEvent.builder()
                    .tripExecutionId(input.tripExecutionId())
                    .tripId(input.tripId())
                    .eventType(input.eventType())
                    .actorType(input.actorType())
                    .actorId(actorId)
                    .actorUsername(input.actorUsername())
                    .actorRole(actorRole)
                    .statusBefore(input.statusBefore())
                    .statusAfter(input.statusAfter())
                    .orderId(input.orderId())
                    .orderRef(input.orderRef())
                    .stopId(input.stopId())
                    .storeCode(input.storeCode())
                    .deliveryResult(input.deliveryResult())
                    .reasonCode(input.reasonCode())
                    .exceptionText(input.exceptionText())
                    .validationNote(input.validationNote())
                    .routeCode(input.routeCode())
                    .deliveryDate(input.deliveryDate())
                    .driverUsername(input.driverUsername())
                    .build();

            tripOutcomeEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to record trip outcome history event: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ApiResponse<List<TripOutcomeEventResponse>> search(OutcomeHistoryFilter filter, Pageable pageable, String currentUsername, boolean isDriver) {
        OutcomeHistoryFilter effectiveFilter = filter;
        if (isDriver) {
            effectiveFilter = filter.withDriverUsername(currentUsername);
        }

        Specification<TripOutcomeEvent> spec = Specification.where(TripOutcomeEventSpecification.hasTripId(effectiveFilter.tripId()))
                .and(TripOutcomeEventSpecification.hasDriverUsername(effectiveFilter.driverUsername()))
                .and(TripOutcomeEventSpecification.hasRouteCode(effectiveFilter.routeCode()))
                .and(TripOutcomeEventSpecification.hasDeliveryDate(effectiveFilter.deliveryDate()))
                .and(TripOutcomeEventSpecification.hasStoreCode(effectiveFilter.storeCode()))
                .and(TripOutcomeEventSpecification.hasDeliveryResult(effectiveFilter.deliveryResult()))
                .and(TripOutcomeEventSpecification.hasEventType(effectiveFilter.eventType()))
                .and(TripOutcomeEventSpecification.occurredBetween(effectiveFilter.fromDate(), effectiveFilter.toDate()));

        Page<TripOutcomeEvent> page = tripOutcomeEventRepository.findAll(spec, pageable);

        List<TripOutcomeEventResponse> responses = page.getContent().stream()
                .map(this::toResponse)
                .toList();

        return ApiResponse.success(responses, pageable.getPageNumber(), pageable.getPageSize(), page.getTotalElements());
    }

    private TripOutcomeEventResponse toResponse(TripOutcomeEvent event) {
        return TripOutcomeEventResponse.builder()
                .id(event.getId())
                .tripExecutionId(event.getTripExecutionId())
                .tripId(event.getTripId())
                .eventType(event.getEventType().name())
                .actorType(event.getActorType().name())
                .actorUsername(event.getActorUsername())
                .actorRole(event.getActorRole())
                .occurredAt(event.getOccurredAt())
                .statusBefore(event.getStatusBefore())
                .statusAfter(event.getStatusAfter())
                .orderId(event.getOrderId())
                .orderRef(event.getOrderRef())
                .stopId(event.getStopId())
                .storeCode(event.getStoreCode())
                .deliveryResult(event.getDeliveryResult())
                .reasonCode(event.getReasonCode())
                .exceptionText(event.getExceptionText())
                .validationNote(event.getValidationNote())
                .routeCode(event.getRouteCode())
                .deliveryDate(event.getDeliveryDate())
                .driverUsername(event.getDriverUsername())
                .build();
    }
}

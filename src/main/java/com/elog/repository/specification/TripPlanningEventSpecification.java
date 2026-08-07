package com.elog.repository.specification;

import com.elog.entity.PlanningEventType;
import com.elog.entity.TripPlanningEvent;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TripPlanningEventSpecification {

    private TripPlanningEventSpecification() {}

    public static Specification<TripPlanningEvent> hasTripDraftId(Long tripDraftId) {
        return (root, query, cb) -> tripDraftId == null ? null : cb.equal(root.get("tripDraftId"), tripDraftId);
    }

    public static Specification<TripPlanningEvent> hasTripId(Long tripId) {
        return (root, query, cb) -> tripId == null ? null : cb.equal(root.get("tripId"), tripId);
    }

    public static Specification<TripPlanningEvent> hasRouteCode(String routeCode) {
        return (root, query, cb) -> (routeCode == null || routeCode.isBlank()) ? null
                : cb.equal(root.get("routeCode"), routeCode);
    }

    public static Specification<TripPlanningEvent> hasDeliveryDate(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.equal(root.get("deliveryDate"), date);
    }

    public static Specification<TripPlanningEvent> hasEventType(PlanningEventType type) {
        return (root, query, cb) -> type == null ? null : cb.equal(root.get("eventType"), type);
    }

    public static Specification<TripPlanningEvent> hasActor(String username) {
        return (root, query, cb) -> (username == null || username.isBlank()) ? null
                : cb.equal(root.get("actorUsername"), username);
    }

    public static Specification<TripPlanningEvent> occurredBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("occurredAt"), from, to);
            return from != null ? cb.greaterThanOrEqualTo(root.get("occurredAt"), from)
                                 : cb.lessThanOrEqualTo(root.get("occurredAt"), to);
        };
    }

    public static Specification<TripPlanningEvent> hasStatusAfter(String status) {
        return (root, query, cb) -> (status == null || status.isBlank()) ? null
                : cb.equal(root.get("statusAfter"), status);
    }
}

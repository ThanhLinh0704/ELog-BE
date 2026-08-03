package com.elog.repository.specification;

import com.elog.entity.TripOutcomeEvent;
import com.elog.entity.TripOutcomeEventType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TripOutcomeEventSpecification {

    private TripOutcomeEventSpecification() {}

    public static Specification<TripOutcomeEvent> hasTripId(Long tripId) {
        return (root, query, cb) -> tripId == null ? null : cb.equal(root.get("tripId"), tripId);
    }

    public static Specification<TripOutcomeEvent> hasDriverUsername(String driverUsername) {
        return (root, query, cb) -> (driverUsername == null || driverUsername.isBlank()) ? null
                : cb.equal(root.get("driverUsername"), driverUsername);
    }

    public static Specification<TripOutcomeEvent> hasRouteCode(String routeCode) {
        return (root, query, cb) -> (routeCode == null || routeCode.isBlank()) ? null
                : cb.equal(root.get("routeCode"), routeCode);
    }

    public static Specification<TripOutcomeEvent> hasDeliveryDate(LocalDate date) {
        return (root, query, cb) -> date == null ? null : cb.equal(root.get("deliveryDate"), date);
    }

    public static Specification<TripOutcomeEvent> hasStoreCode(String storeCode) {
        return (root, query, cb) -> (storeCode == null || storeCode.isBlank()) ? null
                : cb.equal(root.get("storeCode"), storeCode);
    }

    public static Specification<TripOutcomeEvent> hasDeliveryResult(String deliveryResult) {
        return (root, query, cb) -> (deliveryResult == null || deliveryResult.isBlank()) ? null
                : cb.equal(root.get("deliveryResult"), deliveryResult);
    }

    public static Specification<TripOutcomeEvent> hasEventType(TripOutcomeEventType eventType) {
        return (root, query, cb) -> eventType == null ? null : cb.equal(root.get("eventType"), eventType);
    }

    public static Specification<TripOutcomeEvent> occurredBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            if (from == null && to == null) return null;
            if (from != null && to != null) return cb.between(root.get("occurredAt"), from, to);
            return from != null ? cb.greaterThanOrEqualTo(root.get("occurredAt"), from)
                                 : cb.lessThanOrEqualTo(root.get("occurredAt"), to);
        };
    }
}

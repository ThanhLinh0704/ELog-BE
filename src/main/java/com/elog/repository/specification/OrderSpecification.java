package com.elog.repository.specification;

import com.elog.entity.Order;
import com.elog.entity.RouteStop;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    private OrderSpecification() {}

    public static Specification<Order> filterOrders(
            LocalDate deliveryDate,
            String status,
            Long routeId,
            String routeCode,
            Long batchId,
            String search) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Active import batch condition
            predicates.add(cb.equal(root.get("importBatch").get("isActive"), true));

            // Delivery Date filter
            if (deliveryDate != null) {
                predicates.add(cb.equal(root.get("deliveryDate"), deliveryDate));
            }

            // Status filter
            if (status != null && !status.isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("status")), status.trim().toUpperCase()));
            }

            // Import Batch ID filter
            if (batchId != null) {
                predicates.add(cb.equal(root.get("importBatch").get("id"), batchId));
            }

            // Route filter (matches tripDraft route OR store route)
            if (routeId != null || (routeCode != null && !routeCode.isBlank())) {
                Predicate tripDraftRouteMatch;
                if (routeId != null) {
                    tripDraftRouteMatch = cb.equal(root.get("tripDraft").get("route").get("id"), routeId);
                } else {
                    tripDraftRouteMatch = cb.equal(cb.upper(root.get("tripDraft").get("route").get("code")), routeCode.trim().toUpperCase());
                }

                // Subquery for store route stops
                Subquery<Long> subquery = query.subquery(Long.class);
                Root<RouteStop> rsRoot = subquery.from(RouteStop.class);
                subquery.select(rsRoot.get("store").get("id"));
                if (routeId != null) {
                    subquery.where(cb.equal(rsRoot.get("route").get("id"), routeId));
                } else {
                    subquery.where(cb.equal(cb.upper(rsRoot.get("route").get("code")), routeCode.trim().toUpperCase()));
                }
                Predicate storeRouteMatch = root.get("store").get("id").in(subquery);

                predicates.add(cb.or(tripDraftRouteMatch, storeRouteMatch));
            }

            // Keyword Search filter
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate searchPredicate = cb.or(
                        cb.like(cb.lower(root.get("orderRef")), pattern),
                        cb.like(cb.lower(root.get("store").get("code")), pattern),
                        cb.like(cb.lower(root.get("store").get("name")), pattern),
                        cb.like(cb.lower(root.get("recipientName")), pattern),
                        cb.like(cb.lower(root.get("recipientPhone")), pattern)
                );
                predicates.add(searchPredicate);
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

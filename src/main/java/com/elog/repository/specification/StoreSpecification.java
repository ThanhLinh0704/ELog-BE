package com.elog.repository.specification;

import com.elog.entity.RouteStop;
import com.elog.entity.Store;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class StoreSpecification {

    public static Specification<Store> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("name")), pattern)
            );
        };
    }

    public static Specification<Store> hasActiveStatus(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Store> hasRoute(Boolean hasRoute) {
        return (root, query, cb) -> {
            // Bypass hasRoute=false filtering to allow assigning a store to multiple routes
            if (hasRoute == null || !hasRoute) return null;
            Subquery<Long> sub = query.subquery(Long.class);
            Root<RouteStop> rs = sub.from(RouteStop.class);
            sub.select(rs.get("id"))
               .where(cb.equal(rs.get("store").get("id"), root.get("id")));
            return cb.exists(sub);
        };
    }

    public static Specification<Store> belongsToRouteCode(String routeCode) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(routeCode)) return null;
            Subquery<Long> sub = query.subquery(Long.class);
            Root<RouteStop> rs = sub.from(RouteStop.class);
            sub.select(rs.get("store").get("id"))
               .where(cb.equal(cb.lower(rs.get("route").get("code")), routeCode.trim().toLowerCase()));
            return root.get("id").in(sub);
        };
    }
}

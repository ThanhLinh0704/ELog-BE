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
            if (hasRoute == null) return null;
            Subquery<Long> sub = query.subquery(Long.class);
            Root<RouteStop> rs = sub.from(RouteStop.class);
            sub.select(rs.get("id"))
               .where(cb.equal(rs.get("store").get("id"), root.get("id")));
            return hasRoute ? cb.exists(sub) : cb.not(cb.exists(sub));
        };
    }
}

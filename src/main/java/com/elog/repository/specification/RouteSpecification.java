package com.elog.repository.specification;

import com.elog.entity.Route;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class RouteSpecification {

    public static Specification<Route> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("name")), pattern)
            );
        };
    }

    public static Specification<Route> hasActiveStatus(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return cb.equal(root.get("isActive"), isActive);
        };
    }
}

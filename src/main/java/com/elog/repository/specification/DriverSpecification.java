package com.elog.repository.specification;

import com.elog.entity.DriverStatus;
import com.elog.entity.Role;
import com.elog.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class DriverSpecification {

    public static Specification<User> isDriver() {
        return (root, query, cb) -> {
            Join<User, Role> roleJoin = root.join("roles");
            return cb.equal(roleJoin.get("name"), "DRIVER");
        };
    }

    public static Specification<User> hasDriverStatus(String status) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(status)) return null;
            return cb.equal(root.get("driverStatus"), DriverStatus.valueOf(status.toUpperCase()));
        };
    }

    public static Specification<User> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("fullName")), pattern),
                    cb.like(cb.lower(root.get("phoneNumber")), pattern),
                    cb.like(cb.lower(root.get("username")), pattern)
            );
        };
    }
}

package com.elog.repository.specification;

import com.elog.entity.Role;
import com.elog.entity.User;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

public class UserSpecification {

    // Lọc theo từ khóa (username, fullName, email)
    public static Specification<User> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(keyword)) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                cb.like(cb.lower(root.get("username")), pattern),
                cb.like(cb.lower(root.get("fullName")), pattern),
                cb.like(cb.lower(root.get("email")), pattern)
            );
        };
    }

    // Lọc theo vai trò (Role Name)
    public static Specification<User> hasRole(String roleName) {
        return (root, query, cb) -> {
            if (!StringUtils.hasText(roleName)) return null;
            Join<User, Role> roleJoin = root.join("roles");
            return cb.equal(roleJoin.get("name"), roleName);
        };
    }

    // Lọc theo trạng thái hoạt động (isActive)
    public static Specification<User> hasActiveStatus(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return cb.equal(root.get("isActive"), isActive);
        };
    }
}

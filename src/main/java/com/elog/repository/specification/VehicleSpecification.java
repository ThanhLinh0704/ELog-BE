package com.elog.repository.specification;

import com.elog.entity.Vehicle;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class VehicleSpecification {

    private VehicleSpecification() {}

    public static Specification<Vehicle> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("plateNumber")), pattern),
                    cb.like(cb.lower(root.get("vehicleType")), pattern)
            );
        };
    }

    public static Specification<Vehicle> hasActiveStatus(Boolean isActive) {
        return (root, query, cb) -> {
            if (isActive == null) return null;
            return cb.equal(root.get("isActive"), isActive);
        };
    }

    public static Specification<Vehicle> hasMinimumWeight(BigDecimal minWeightKg) {
        return (root, query, cb) -> {
            if (minWeightKg == null) return null;
            return cb.greaterThanOrEqualTo(root.get("maxWeightKg"), minWeightKg);
        };
    }

    public static Specification<Vehicle> hasMinimumVolume(BigDecimal minVolumeM3) {
        return (root, query, cb) -> {
            if (minVolumeM3 == null) return null;
            return cb.greaterThanOrEqualTo(root.get("maxVolumeM3"), minVolumeM3);
        };
    }
}

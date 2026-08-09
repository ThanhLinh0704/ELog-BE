package com.elog.service.impl;

import com.elog.entity.Order;
import com.elog.entity.Store;
import com.elog.entity.TripDraftStop;
import com.elog.entity.Vehicle;
import com.elog.service.ConstraintValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ConstraintValidationServiceImpl implements ConstraintValidationService {

    private static final int GRACE_PERIOD_MINUTES = 20;

    @Override
    public List<String> validateStoreVehicle(Store store, Vehicle vehicle) {
        List<String> violations = new ArrayList<>();
        if (store == null || vehicle == null) {
            return violations;
        }

        // 1. Weight Constraint
        if (store.getMaxAllowedVehicleWeight() != null) {
            BigDecimal maxAllowedTons = store.getMaxAllowedVehicleWeight();
            BigDecimal vehicleWeightKg = vehicle.getGrossVehicleWeightKg() != null 
                    ? vehicle.getGrossVehicleWeightKg() 
                    : vehicle.getPayloadKg();

            if (vehicleWeightKg != null) {
                BigDecimal vehicleTons = vehicleWeightKg.divide(new BigDecimal("1000"), 3, java.math.RoundingMode.HALF_UP);
                if (vehicleTons.compareTo(maxAllowedTons) > 0) {
                    violations.add("ERR_WEIGHT_EXCEEDED: Cửa hàng " + store.getCode() + 
                            " giới hạn xe <= " + maxAllowedTons + " tấn, nhưng xe " + 
                            vehicle.getPlateNumber() + " nặng " + vehicleTons + " tấn");
                }
            }
        }

        // 2. Restricted Vehicle Type
        if (store.getRestrictedVehicleTypes() != null && !store.getRestrictedVehicleTypes().isBlank()) {
            String vehicleType = vehicle.getVehicleType();
            if (vehicleType != null) {
                String[] restrictedTypes = store.getRestrictedVehicleTypes().split(",");
                for (String restricted : restrictedTypes) {
                    if (vehicleType.trim().equalsIgnoreCase(restricted.trim())) {
                        violations.add("ERR_RESTRICTED_VEHICLE: Cửa hàng " + store.getCode() + 
                                " cấm xe loại " + vehicleType);
                        break;
                    }
                }
            }
        }

        return violations;
    }

    @Override
    public List<String> validateTripVehicleStops(List<Store> stores, Vehicle vehicle) {
        List<String> allViolations = new ArrayList<>();
        if (stores == null || vehicle == null) return allViolations;

        for (Store store : stores) {
            List<String> violations = validateStoreVehicle(store, vehicle);
            allViolations.addAll(violations);
        }
        return allViolations;
    }

    @Override
    public boolean isTimeWithinAllowedHours(LocalTime time, String allowedHours) {
        if (allowedHours == null || allowedHours.trim().isEmpty() || "All".equalsIgnoreCase(allowedHours.trim())) {
            return true;
        }
        try {
            String[] intervals = allowedHours.split(",");
            for (String interval : intervals) {
                String[] parts = interval.trim().split("-");
                if (parts.length == 2) {
                    LocalTime start = LocalTime.parse(parts[0].trim());
                    LocalTime end = LocalTime.parse(parts[1].trim());
                    if (start.isAfter(end)) {
                        if (!time.isBefore(start) || !time.isAfter(end)) {
                            return true;
                        }
                    } else {
                        if (!time.isBefore(start) && !time.isAfter(end)) {
                            return true;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Invalid allowed delivery hours format: '{}'", allowedHours, e);
            return true;
        }
        return false;
    }

    @Override
    public boolean isTimeWithinOrderWindow(LocalTime time, String window) {
        if (window == null || window.trim().isEmpty()) {
            return true;
        }
        String cleanWindow = window.trim().toLowerCase();
        
        if (cleanWindow.contains("hành chính") || cleanWindow.contains("hanh chinh")) {
            LocalTime start = LocalTime.of(8, 0);
            LocalTime end = LocalTime.of(17, 0).plusMinutes(GRACE_PERIOD_MINUTES);
            return !time.isBefore(start) && !time.isAfter(end);
        }
        
        if (cleanWindow.contains("trước") || cleanWindow.contains("truoc")) {
            LocalTime limit = parseTimeFromString(cleanWindow.replaceAll("trước|truoc", "").trim());
            if (limit != null) {
                return !time.isAfter(limit.plusMinutes(GRACE_PERIOD_MINUTES));
            }
            return true;
        }
        
        if (cleanWindow.contains("sau")) {
            LocalTime limit = parseTimeFromString(cleanWindow.replaceAll("sau", "").trim());
            if (limit != null) {
                return !time.isBefore(limit);
            }
            return true;
        }
        
        if (cleanWindow.contains("-")) {
            String[] parts = cleanWindow.split("-");
            if (parts.length == 2) {
                LocalTime start = parseTimeFromString(parts[0].trim());
                LocalTime end = parseTimeFromString(parts[1].trim());
                if (start != null && end != null) {
                    LocalTime endWithGrace = end.plusMinutes(GRACE_PERIOD_MINUTES);
                    if (start.isAfter(endWithGrace)) {
                        return !time.isBefore(start) || !time.isAfter(endWithGrace);
                    } else {
                        return !time.isBefore(start) && !time.isAfter(endWithGrace);
                    }
                }
            }
        }
        
        LocalTime directTime = parseTimeFromString(cleanWindow);
        if (directTime != null) {
            return !time.isAfter(directTime.plusMinutes(GRACE_PERIOD_MINUTES));
        }
        
        return true;
    }

    private LocalTime parseTimeFromString(String s) {
        if (s == null) return null;
        s = s.trim().replaceAll("\\s+", "");
        if (s.isEmpty()) return null;
        
        try {
            if (s.contains(":")) {
                String[] parts = s.split(":");
                int hour = Integer.parseInt(parts[0]);
                int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                return LocalTime.of(hour, minute);
            }
            
            if (s.contains("h")) {
                String[] parts = s.split("h");
                int hour = Integer.parseInt(parts[0]);
                int minute = (parts.length > 1 && !parts[1].isEmpty()) ? Integer.parseInt(parts[1]) : 0;
                return LocalTime.of(hour, minute);
            }
            
            if (s.matches("\\d+")) {
                int hour = Integer.parseInt(s);
                if (hour >= 0 && hour <= 23) {
                    return LocalTime.of(hour, 0);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to parse time string: '{}'", s, e);
        }
        return null;
    }

    @Override
    public String validateStopEta(TripDraftStop stop, List<Order> draftOrders) {
        if (stop == null || Boolean.FALSE.equals(stop.getIsActive()) || stop.getPlannedEta() == null) {
            return null;
        }
        Store store = stop.getStore();
        if (store == null) {
            return null;
        }
        LocalTime etaTime = stop.getPlannedEta().toLocalTime();

        // 1. Store allowed delivery hours check
        if (!isTimeWithinAllowedHours(etaTime, store.getAllowedDeliveryHours())) {
            return "Planned ETA (" + etaTime + ") is outside store " + store.getCode() 
                    + " allowed delivery hours (" + store.getAllowedDeliveryHours() + ")";
        }

        // 2. Store closing time check
        if (store.getTimeWindowEnd() != null && etaTime.isAfter(store.getTimeWindowEnd())) {
            return "ETA " + etaTime + " exceeds closing time " + store.getTimeWindowEnd() 
                    + " at store " + store.getCode();
        }

        // 3. Order delivery time window check
        if (draftOrders != null) {
            for (Order order : draftOrders) {
                if (order.getStore() != null && order.getStore().getId().equals(store.getId())) {
                    if (Boolean.TRUE.equals(order.getIsDeliveryTimeOverridden())) {
                        continue;
                    }
                    if (order.getDeliveryTimeWindow() != null && !order.getDeliveryTimeWindow().trim().isEmpty()) {
                        if (!isTimeWithinOrderWindow(etaTime, order.getDeliveryTimeWindow())) {
                            return "Planned ETA (" + etaTime + ") violates delivery window (" 
                                    + order.getDeliveryTimeWindow() + ") for order " + order.getOrderRef();
                        }
                    }
                }
            }
        }

        return null;
    }
}


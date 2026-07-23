package com.elog.service.impl;

import com.elog.entity.Store;
import com.elog.entity.TripDraft;
import com.elog.entity.TripDraftStop;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.repository.SystemConfigRepository;
import com.elog.repository.TripDraftRepository;
import com.elog.repository.TripDraftStopRepository;
import com.elog.service.DepartureAdjustmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DepartureAdjustmentServiceImpl implements DepartureAdjustmentService {

    private final TripDraftRepository tripDraftRepo;
    private final TripDraftStopRepository stopRepo;
    private final SystemConfigRepository configRepo;

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> calculateOptimalDepartureTime(Long tripDraftId) {
        TripDraft draft = tripDraftRepo.findById(tripDraftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.TRIP_DRAFT_NOT_FOUND,
                        "TripDraft not found: " + tripDraftId, HttpStatus.NOT_FOUND));

        List<TripDraftStop> activeStops = stopRepo.findByTripDraftIdAndIsActiveTrueOrderBySequenceNoAsc(tripDraftId);
        if (activeStops.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_ACTIVE_STOP,
                    "No active stops for TripDraft: " + tripDraftId, HttpStatus.BAD_REQUEST);
        }

        LocalTime currentDeparture = draft.getPlannedDepartureTime() != null 
                ? draft.getPlannedDepartureTime() : LocalTime.of(8, 0);

        // Check if there is an early stop that requires delaying departure
        int maxWaitMin = 0;
        boolean hasEarlyViolation = false;
        boolean hasLateViolation = false;

        for (TripDraftStop stop : activeStops) {
            if ("TIME_WINDOW_EARLY".equals(stop.getViolationCode())) {
                hasEarlyViolation = true;
            }
            if ("TIME_WINDOW_LATE".equals(stop.getViolationCode())) {
                hasLateViolation = true;
            }
            if (stop.getPlannedWaitingTimeMin() != null && stop.getPlannedWaitingTimeMin() > maxWaitMin) {
                maxWaitMin = stop.getPlannedWaitingTimeMin();
            }
        }

        LocalTime suggestedTime = currentDeparture;
        String reason;

        if (hasEarlyViolation || maxWaitMin > 15) {
            suggestedTime = currentDeparture.plusMinutes(maxWaitMin);
            reason = String.format("Đề xuất lùi giờ xuất phát thêm %d phút (từ %s sang %s) để khớp Time Window các cửa hàng.",
                    maxWaitMin, currentDeparture, suggestedTime);
        } else if (hasLateViolation) {
            suggestedTime = currentDeparture.minusMinutes(30);
            if (suggestedTime.isBefore(LocalTime.of(6, 0))) {
                suggestedTime = LocalTime.of(6, 0);
            }
            reason = String.format("Đề xuất xuất phát sớm hơn (lúc %s) để tránh trễ khung giờ giao hàng.", suggestedTime);
        } else {
            reason = "Giờ xuất phát hiện tại đã tối ưu, không có vi phạm Time Window.";
        }

        Map<String, Object> response = new HashMap<>();
        response.put("tripDraftId", tripDraftId);
        response.put("currentDepartureTime", currentDeparture.toString());
        response.put("suggestedDepartureTime", suggestedTime.toString());
        response.put("reason", reason);
        response.put("hasViolations", hasEarlyViolation || hasLateViolation);

        return response;
    }
}

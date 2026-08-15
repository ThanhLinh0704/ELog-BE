package com.elog.service;

import com.elog.dto.response.trip.ActiveTripsResponse;
import com.elog.dto.response.trip.StopArriveResponse;
import com.elog.dto.response.trip.StopCompleteResponse;
import com.elog.dto.response.trip.TripProgressResponse;
import com.elog.dto.response.trip.TripStartResponse;

import java.time.LocalDate;

public interface TripMonitoringService {

    /** DISPATCHED → IN_PROGRESS */
    TripStartResponse startTrip(Long tripId, String currentUsername);

    /** PENDING → IN_PROGRESS. Tạo TIME_EXCEPTION nếu đến trễ quá threshold. */
    StopArriveResponse arriveAtStop(Long tripStopId, String currentUsername);

    /** IN_PROGRESS → COMPLETED. Trip tự động COMPLETED nếu là stop cuối. */
    StopCompleteResponse completeStop(Long tripStopId, String currentUsername);

    /** Tất cả trips DISPATCHED/IN_PROGRESS/COMPLETED của ngày. */
    ActiveTripsResponse getActiveTripsDashboard(LocalDate date);

    /** Chi tiết tiến độ từng stop của 1 trip. */
    TripProgressResponse getTripProgress(Long tripId);
}

package com.elog.service;

import com.elog.dto.request.trip.RecalculateEtaRequest;
import com.elog.dto.request.trip.StopUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.common.ConfirmResponse;
import com.elog.dto.response.goong.RecalculateEtaResponse;
import com.elog.dto.response.trip.ConsolidateResponse;
import com.elog.dto.response.trip.StopOrderItemResponse;
import com.elog.dto.response.trip.TripDraftResponse;
import com.elog.dto.response.trip.TripDraftStopResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TripDraftService {

    ConsolidateResponse consolidate(LocalDate deliveryDate);

    ApiResponse<List<TripDraftResponse>> getTripDrafts(LocalDate deliveryDate, Pageable pageable);

    TripDraftResponse getTripDraftById(Long id);

    // US-11 methods
    TripDraftResponse getStopsForReview(Long tripDraftId);

    TripDraftStopResponse updateStop(Long tripDraftId, Long stopId, StopUpdateRequest request);

    RecalculateEtaResponse recalculateEta(Long tripDraftId, RecalculateEtaRequest request);

    ConfirmResponse confirmTripDraft(Long tripDraftId, String currentUsername);

    void revertToDraft(Long tripDraftId, String currentUsername);

    List<StopOrderItemResponse> getStopOrderItems(Long tripDraftId, Long stopId);

    TripDraftResponse adjustDepartureTime(Long tripDraftId, com.elog.dto.request.trip.AdjustDepartureTimeRequest request);

    void settleDelay(Long tripDraftId, Long orderId, com.elog.dto.request.trip.SettleDelayRequest request, String username);

    void excludeOrder(Long tripDraftId, Long orderId);

    void reIncludeOrder(Long tripDraftId, Long orderId);

    List<StopOrderItemResponse> getExcludedOrders(Long tripDraftId);
}

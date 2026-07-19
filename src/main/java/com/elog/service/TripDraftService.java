package com.elog.service;

import com.elog.dto.request.RecalculateEtaRequest;
import com.elog.dto.request.StopUpdateRequest;
import com.elog.dto.response.*;
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
}

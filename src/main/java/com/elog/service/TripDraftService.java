package com.elog.service;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.ConsolidateResponse;
import com.elog.dto.response.TripDraftResponse;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface TripDraftService {

    ConsolidateResponse consolidate(LocalDate deliveryDate);

    ApiResponse<List<TripDraftResponse>> getTripDrafts(LocalDate deliveryDate, Pageable pageable);

    TripDraftResponse getTripDraftById(Long id);
}

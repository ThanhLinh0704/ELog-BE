package com.elog.dto.response.goong;

import lombok.*;

import java.util.List;

import com.elog.dto.response.trip.StopEtaResponse;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RecalculateEtaResponse {
    private Long tripDraftId;
    private String message;
    private List<StopEtaResponse> stops;
}

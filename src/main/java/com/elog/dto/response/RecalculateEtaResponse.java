package com.elog.dto.response;

import lombok.*;

import java.util.List;

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

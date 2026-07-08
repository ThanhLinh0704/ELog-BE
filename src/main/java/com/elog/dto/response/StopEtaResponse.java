package com.elog.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StopEtaResponse {
    private Long tripDraftStopId;
    private Integer sequenceNo;
    private String storeCode;
    private LocalDateTime plannedEta;
}

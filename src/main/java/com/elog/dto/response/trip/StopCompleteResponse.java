package com.elog.dto.response.trip;

import lombok.Builder;
import lombok.Getter;

/** Response cho POST /api/trip-stops/{id}/complete */
@Getter
@Builder
public class StopCompleteResponse {

    private Long tripStopId;
    private String storeCode;
    private String status;
    private String actualDepartureTime;

    private boolean tripCompleted;
    private String tripStatus;

    private NextStopInfo nextStop;
    private String message;

    @Getter
    @Builder
    public static class NextStopInfo {
        private Long tripStopId;
        private String storeCode;
        private String plannedEta;
        private Integer sequenceOrder;
    }
}

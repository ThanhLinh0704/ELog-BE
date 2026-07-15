package com.elog.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Response cho POST /api/trip-stops/{id}/arrive */
@Getter
@Builder
public class StopArriveResponse {

    private Long tripStopId;
    private String storeCode;
    private String status;
    private String actualArrivalTime;
    private String plannedEta;
    private Long delayMinutes;

    private boolean timeExceptionFlagged;
    private Long exceptionId;

    private String message;
}

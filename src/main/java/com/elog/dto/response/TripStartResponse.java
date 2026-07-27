package com.elog.dto.response;

import lombok.Builder;
import lombok.Getter;


/** Response cho POST /api/trips/{id}/start */
@Getter
@Builder
public class TripStartResponse {

    private Long tripId;
    private String status;
    private String actualDepartureTime;
    private String firstStopCode;
    private String firstStopEta;
    private String message;
}

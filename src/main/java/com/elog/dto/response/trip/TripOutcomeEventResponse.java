package com.elog.dto.response.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripOutcomeEventResponse {
    private Long id;
    private Long tripExecutionId;
    private Long tripId;
    private String eventType;
    private String actorType;
    private String actorUsername;
    private String actorRole;
    private LocalDateTime occurredAt;
    private String statusBefore;
    private String statusAfter;
    private Long orderId;
    private String orderRef;
    private Long stopId;
    private String storeCode;
    private String deliveryResult;
    private String reasonCode;
    private String exceptionText;
    private String validationNote;
    private String routeCode;
    private LocalDate deliveryDate;
    private String driverUsername;
}

package com.elog.dto.response;

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
public class PlanningEventResponse {
    private Long id;
    private Long tripDraftId;
    private Long tripId;
    private String eventType;
    private String actorType;
    private String actorUsername;
    private String actorRole;
    private LocalDateTime occurredAt;
    private String statusBefore;
    private String statusAfter;
    private String changeSummary;
    private Object changeDetail;
    private String note;
    private Integer planVersion;
    private String optionCode;
    private String routeCode;
    private LocalDate deliveryDate;
}

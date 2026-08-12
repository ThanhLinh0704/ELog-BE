package com.elog.dto.response.trip;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripOutcomeResponse {

    private Long id;
    private Long executionId;
    private Long tripId;
    private String tripCode;
    private String driverName;
    private String vehiclePlate;
    private String status; // SUBMITTED, VALIDATED, NEEDS_CORRECTION
    private Integer totalOrders;
    private Integer deliveredCount;
    private Integer failedCount;
    private Integer partialCount;
    private LocalDateTime submittedAt;
    private LocalDateTime validatedAt;
    private String validatedBy;
    private String amendmentReason;
    private Integer version;
}

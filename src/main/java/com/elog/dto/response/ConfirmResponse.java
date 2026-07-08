package com.elog.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmResponse {
    private Long tripDraftId;
    private String fixedRouteCode;
    private LocalDate deliveryDate;
    private String status;
    private LocalDateTime confirmedAt;
    private ConfirmedByDto confirmedBy;
    private Integer activeStopCount;
    private String summary;
}

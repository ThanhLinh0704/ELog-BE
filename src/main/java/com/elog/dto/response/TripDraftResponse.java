package com.elog.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripDraftResponse {
    private Long id;
    private Long routeId;
    private String routeCode;
    private LocalDate deliveryDate;
    private BigDecimal totalVolumeM3;
    private BigDecimal totalWeightKg;
    private Integer activeStopCount;
    private Integer skippedStopCount;
    private String status;
    private List<TripDraftStopResponse> stops;
}

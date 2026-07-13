package com.elog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManifestResponse {
    private Long manifestId;
    private Long tripDraftId;
    private String fixedRouteCode;
    private String deliveryDate;
    private LocalDateTime generatedAt;
    private ConfirmedByDto generatedBy;
    private Integer totalLines;
    private BigDecimal totalWeightKg;
    private BigDecimal totalVolumeM3;
    private String message;
    private List<ManifestLineDto> lines;
}

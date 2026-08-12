package com.elog.dto.response.trip;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManifestByStopResponse {
    private Long manifestId;
    private Long tripDraftId;
    private String fixedRouteCode;
    private String deliveryDate;
    private List<ManifestStopGroup> stops;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ManifestStopGroup {
        private Integer stopSequenceNo;
        private String storeCode;
        private String storeName;
        private String loadingNote;
        private List<ManifestLineDto> items;
        private BigDecimal stopWeightKg;
        private BigDecimal stopVolumeM3;
    }
}

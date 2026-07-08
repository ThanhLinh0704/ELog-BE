package com.elog.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConsolidateResponse {
    private LocalDate deliveryDate;
    private Integer tripDraftsCreatedOrUpdated;
    private List<TripDraftResponse> tripDrafts;
    private List<SkippedRouteInfo> skippedRoutes;

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SkippedRouteInfo {
        private Long routeId;
        private String routeCode;
        private String reason;
    }
}

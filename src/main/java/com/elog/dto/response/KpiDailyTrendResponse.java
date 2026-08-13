package com.elog.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiDailyTrendResponse {

    private KpiSummaryResponse.PeriodInfo period;
    private List<DailyDataPoint> data;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyDataPoint {
        private LocalDate date;
        private Integer tripCount;
        private Double onTimeRatePct;
        private Double volumeUtilPct;
    }
}

package com.elog.dto.response.trip;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Số lượng chuyến theo từng trạng thái Trip trong 1 ngày — dùng cho Fleet Status Dashboard
 * mục "Tổng quan tình trạng chuyến hôm nay". Không bao gồm TripDraft (entity/lifecycle riêng,
 * xem filemd/FLEET-STATUS-DASHBOARD-ADJUSTED-SPEC.md mục 5).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripStatusSummaryResponse {
    private String date;
    private int totalCount;
    private int validatedCount;
    private int dispatchedCount;
    private int inProgressCount;
    private int completedCount;
}

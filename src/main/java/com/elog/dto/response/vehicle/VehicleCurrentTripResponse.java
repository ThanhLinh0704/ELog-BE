package com.elog.dto.response.vehicle;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Chuyến hiện tại (nếu có) của 1 xe — chỉ khác null khi Vehicle.status = IN_USE.
 * {@code phase} là 1 trong: ASSIGNED (đã phân công, chờ dispatch) | DISPATCHED (đã điều phối,
 * chưa xuất phát) | IN_PROGRESS (đang thực hiện chuyến) | RETURNING (đã hoàn thành, chưa xác
 * nhận về kho) — xem filemd/FLEET-STATUS-DASHBOARD-ADJUSTED-SPEC.md mục 0 để biết cách suy ra.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCurrentTripResponse {
    private Long tripId;
    private String routeCode;
    private String driverName;
    private LocalDate deliveryDate;
    private String phase;
    private LocalDateTime estimatedCompletionAt;
}

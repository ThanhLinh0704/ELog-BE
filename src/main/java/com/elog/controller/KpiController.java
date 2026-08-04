package com.elog.controller;

import com.elog.dto.response.*;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.service.KpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/kpi")
@RequiredArgsConstructor
@Tag(name = "KPI Dashboard", description = "US-19 KPI reporting APIs")
public class KpiController {

    private final KpiService kpiService;

    @GetMapping("/summary")
    @Operation(summary = "Get KPI summary for a period")
    @PreAuthorize("hasAuthority('kpi:read')")
    public ResponseEntity<ApiResponse<KpiSummaryResponse>> getSummary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String preset) {

        LocalDate[] resolved = resolveDates(startDate, endDate, preset);
        validateDateRange(resolved[0], resolved[1]);

        KpiSummaryResponse response = kpiService.calculate(resolved[0], resolved[1]);
        // Inject preset into period if provided
        if (preset != null) {
            response.getPeriod().setPreset(preset.toUpperCase());
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/daily-trend")
    @Operation(summary = "Get daily trend data for charts")
    @PreAuthorize("hasAuthority('kpi:read')")
    public ResponseEntity<ApiResponse<KpiDailyTrendResponse>> getDailyTrend(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String preset) {

        LocalDate[] resolved = resolveDates(startDate, endDate, preset);
        validateDateRange(resolved[0], resolved[1]);

        KpiDailyTrendResponse response = kpiService.getDailyTrend(resolved[0], resolved[1]);
        if (preset != null) {
            response.getPeriod().setPreset(preset.toUpperCase());
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/by-route")
    @Operation(summary = "Get KPI breakdown by route")
    @PreAuthorize("hasAuthority('kpi:read')")
    public ResponseEntity<ApiResponse<KpiByRouteResponse>> getByRoute(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String preset) {

        LocalDate[] resolved = resolveDates(startDate, endDate, preset);
        validateDateRange(resolved[0], resolved[1]);

        KpiByRouteResponse response = kpiService.getByRoute(resolved[0], resolved[1]);
        if (preset != null) {
            response.getPeriod().setPreset(preset.toUpperCase());
        }
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── Private helpers ──────────────────────────────────────────────

    private LocalDate[] resolveDates(LocalDate start, LocalDate end, String preset) {
        if (preset != null) {
            return switch (preset.toUpperCase()) {
                case "TODAY" -> new LocalDate[]{LocalDate.now(), LocalDate.now()};
                case "LAST_7_DAYS" -> new LocalDate[]{LocalDate.now().minusDays(6), LocalDate.now()};
                case "LAST_30_DAYS" -> new LocalDate[]{LocalDate.now().minusDays(29), LocalDate.now()};
                default -> throw new BusinessException(
                        ErrorCode.INVALID_REQUEST,
                        "Invalid preset: " + preset + ". Valid values: TODAY, LAST_7_DAYS, LAST_30_DAYS",
                        HttpStatus.BAD_REQUEST);
            };
        }
        LocalDate s = (start != null) ? start : LocalDate.now().minusDays(6);
        LocalDate e = (end != null) ? end : LocalDate.now();
        return new LocalDate[]{s, e};
    }

    private void validateDateRange(LocalDate start, LocalDate end) {
        if (start.isAfter(end)) {
            throw new BusinessException(
                    ErrorCode.INVALID_DATE_RANGE,
                    "startDate must not be after endDate",
                    HttpStatus.BAD_REQUEST);
        }
        if (ChronoUnit.DAYS.between(start, end) > 90) {
            throw new BusinessException(
                    ErrorCode.DATE_RANGE_TOO_WIDE,
                    "Maximum 90 days per query",
                    HttpStatus.BAD_REQUEST);
        }
    }
}

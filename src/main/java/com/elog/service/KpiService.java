package com.elog.service;

import com.elog.dto.response.KpiByDriverResponse;
import com.elog.dto.response.KpiByRouteResponse;
import com.elog.dto.response.KpiByVehicleResponse;
import com.elog.dto.response.KpiDailyTrendResponse;
import com.elog.dto.response.KpiSummaryResponse;

import java.time.LocalDate;

/**
 * US-19 — KPI Dashboard service.
 * Calculates operational KPIs from trip/stop/exception data.
 */
public interface KpiService {

    /**
     * Calculate full KPI summary for a period.
     *
     * @param startDate period start (inclusive)
     * @param endDate   period end (inclusive)
     */
    KpiSummaryResponse calculate(LocalDate startDate, LocalDate endDate);

    /**
     * Daily trend data for chart rendering.
     */
    KpiDailyTrendResponse getDailyTrend(LocalDate startDate, LocalDate endDate);

    /**
     * KPI breakdown per route.
     */
    KpiByRouteResponse getByRoute(LocalDate startDate, LocalDate endDate);

    /**
     * KPI breakdown per vehicle.
     */
    KpiByVehicleResponse getByVehicle(LocalDate startDate, LocalDate endDate);

    /**
     * KPI breakdown per driver.
     */
    KpiByDriverResponse getByDriver(LocalDate startDate, LocalDate endDate);
}

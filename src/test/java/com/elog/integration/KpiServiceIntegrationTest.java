package com.elog.integration;

import com.elog.dto.response.kpi.KpiByDriverResponse;
import com.elog.dto.response.kpi.KpiByRouteResponse;
import com.elog.dto.response.kpi.KpiByVehicleResponse;
import com.elog.dto.response.kpi.KpiDailyTrendResponse;
import com.elog.dto.response.kpi.KpiSummaryResponse;
import com.elog.service.KpiService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class KpiServiceIntegrationTest {

    @Autowired
    private KpiService kpiService;

    @Test
    void l2Kpi01CalculatesOverallKpiSummaryFromDatabase() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(1);

        KpiSummaryResponse summary = kpiService.calculate(startDate, endDate);

        assertThat(summary).isNotNull();
        assertThat(summary.getPeriod()).isNotNull();
        assertThat(summary.getOnTimeDelivery()).isNotNull();
        assertThat(summary.getFleetUtilization()).isNotNull();
        assertThat(summary.getTripCompletion()).isNotNull();
        assertThat(summary.getExceptions()).isNotNull();
    }

    @Test
    void l2Kpi02CalculatesDriverPerformanceBreakdown() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(1);

        KpiByDriverResponse drivers = kpiService.getByDriver(startDate, endDate);

        assertThat(drivers).isNotNull();
    }

    @Test
    void l2Kpi03CalculatesRouteSlaPerformance() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(1);

        KpiByRouteResponse routes = kpiService.getByRoute(startDate, endDate);

        assertThat(routes).isNotNull();
    }

    @Test
    void l2Kpi04CalculatesVehicleCapacityUtilization() {
        LocalDate startDate = LocalDate.now().minusDays(30);
        LocalDate endDate = LocalDate.now().plusDays(1);

        KpiByVehicleResponse vehicles = kpiService.getByVehicle(startDate, endDate);

        assertThat(vehicles).isNotNull();
    }

    @Test
    void l2Kpi05GeneratesDailyTrendTimeSeries() {
        LocalDate startDate = LocalDate.now().minusDays(7);
        LocalDate endDate = LocalDate.now();

        KpiDailyTrendResponse trend = kpiService.getDailyTrend(startDate, endDate);

        assertThat(trend).isNotNull();
        assertThat(trend.getData()).isNotEmpty();
    }
}

package com.elog.service;

import com.elog.dto.response.trip.ConsolidateResponse;
import com.elog.dto.response.trip.RecommendationResultResponse;
import com.elog.dto.response.trip.TripDraftResponse;
import com.elog.service.impl.AutoPipelineRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoPipelineRunnerTest {
    @Mock TripDraftService tripDraftService;
    @Mock EtaCalculationService etaCalculationService;
    @Mock RecommendationService recommendationService;

    AutoPipelineRunner runner;

    @BeforeEach
    void setUp() {
        runner = new AutoPipelineRunner(tripDraftService, etaCalculationService, recommendationService);
    }

    @Test
    @DisplayName("[L1-AP-01] runForDate runs pipeline for consolidated drafts")
    void runForDateRunsPipeline() {
        LocalDate date = LocalDate.now();
        TripDraftResponse draft = TripDraftResponse.builder().id(10L).activeStopCount(3).build();
        ConsolidateResponse consolidateResp = ConsolidateResponse.builder().tripDraftsCreatedOrUpdated(1).tripDrafts(List.of(draft)).build();

        when(tripDraftService.consolidate(date)).thenReturn(consolidateResp);
        when(recommendationService.recommendTop3(10L)).thenReturn(RecommendationResultResponse.builder().planType("RECOMMENDED").build());

        runner.runForDate(date);

        verify(etaCalculationService).calculateAndPersist(eq(10L), any(LocalTime.class));
        verify(recommendationService).recommendTop3(10L);
    }

    @Test
    @DisplayName("[L1-AP-02] runForDate handles empty draft consolidation gracefully")
    void runForDateHandlesEmptyDrafts() {
        LocalDate date = LocalDate.now();
        ConsolidateResponse consolidateResp = ConsolidateResponse.builder().tripDraftsCreatedOrUpdated(0).tripDrafts(List.of()).build();

        when(tripDraftService.consolidate(date)).thenReturn(consolidateResp);

        runner.runForDate(date);

        verifyNoInteractions(etaCalculationService, recommendationService);
    }
}

package com.elog.service.impl;

import com.elog.service.EtaCalculationService;
import com.elog.service.RecommendationService;
import com.elog.service.TripDraftService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Runner component to execute auto-pipeline (order consolidation, ETA calculation & recommendations)
 * in isolated REQUIRES_NEW transactions per delivery date asynchronously.
 * This prevents background processing from delaying the primary Excel import response.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AutoPipelineRunner {

    private final TripDraftService tripDraftService;
    private final EtaCalculationService etaCalculationService;
    private final RecommendationService recommendationService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void runForDate(LocalDate deliveryDate) {
        log.info("Auto-Pipeline: starting isolated transaction for deliveryDate={}", deliveryDate);

        // 1. Consolidate orders into TripDrafts
        var consolidateResult = tripDraftService.consolidate(deliveryDate);
        log.info("Auto-Pipeline: consolidated {} trip drafts for date {}",
                consolidateResult.getTripDraftsCreatedOrUpdated(), deliveryDate);

        if (consolidateResult.getTripDrafts() == null || consolidateResult.getTripDrafts().isEmpty()) {
            log.info("Auto-Pipeline: no trip drafts created, skipping ETA and recommendation for date {}", deliveryDate);
            return;
        }

        LocalTime defaultDeparture = LocalTime.of(7, 0);

        // 2. For each trip draft: calculate ETA → run recommendation
        for (var draftResponse : consolidateResult.getTripDrafts()) {
            try {
                if (draftResponse.getActiveStopCount() == null || draftResponse.getActiveStopCount() == 0) {
                    continue;
                }
                etaCalculationService.calculateAndPersist(draftResponse.getId(), defaultDeparture);
                log.info("Auto-Pipeline: ETA calculated for TripDraft id={}", draftResponse.getId());

                var recommendation = recommendationService.recommendTop3(draftResponse.getId());
                log.info("Auto-Pipeline: recommendation for TripDraft id={}: planType={}",
                        draftResponse.getId(), recommendation.getPlanType());
            } catch (Exception ex) {
                log.warn("Auto-Pipeline: failed for TripDraft id={}: {}",
                        draftResponse.getId(), ex.getMessage(), ex);
            }
        }

        log.info("Auto-Pipeline: completed isolated transaction for deliveryDate={}", deliveryDate);
    }
}

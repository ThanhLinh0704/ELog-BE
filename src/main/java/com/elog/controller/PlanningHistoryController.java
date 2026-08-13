package com.elog.controller;

import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.PlanningEventResponse;
import com.elog.entity.PlanningEventType;
import com.elog.service.PlanningHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/planning-events")
@RequiredArgsConstructor
@Tag(name = "Planning History", description = "Audit trail cho Trip Draft / Trip planning lifecycle")
public class PlanningHistoryController {

    private final PlanningHistoryService planningHistoryService;

    @GetMapping
    @Operation(summary = "Search planning history events with filters (paginated)")
    @PreAuthorize("hasAuthority('planning-history:read')")
    public ResponseEntity<ApiResponse<List<PlanningEventResponse>>> search(
            @RequestParam(required = false) Long tripDraftId,
            @RequestParam(required = false) Long tripId,
            @RequestParam(required = false) String routeCode,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deliveryDate,
            @RequestParam(required = false) PlanningEventType eventType,
            @RequestParam(required = false) String actorUsername,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        var filter = new PlanningHistoryService.PlanningHistoryFilter(
                tripDraftId, tripId, routeCode, deliveryDate, eventType, actorUsername, fromDate, toDate, status);
        return ResponseEntity.ok(planningHistoryService.search(filter, pageable));
    }
}

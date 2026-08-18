package com.elog.controller;

import com.elog.dto.request.trip.AdjustDepartureTimeRequest;
import com.elog.dto.request.trip.ConsolidateRequest;
import com.elog.dto.request.trip.RecalculateEtaRequest;
import com.elog.dto.request.trip.SettleDelayRequest;
import com.elog.dto.request.trip.StopUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.common.ConfirmResponse;
import com.elog.dto.response.goong.RecalculateEtaResponse;
import com.elog.dto.response.trip.ConsolidateResponse;
import com.elog.dto.response.trip.ManifestByStopResponse;
import com.elog.dto.response.trip.ManifestResponse;
import com.elog.dto.response.trip.PlanningEventResponse;
import com.elog.dto.response.trip.RecommendationResultResponse;
import com.elog.dto.response.trip.StopOrderItemResponse;
import com.elog.dto.response.trip.TripDraftResponse;
import com.elog.dto.response.trip.TripDraftStopResponse;
import com.elog.dto.response.vehicle.CapacityValidationResultResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.CapacityValidationService;
import com.elog.service.DepartureAdjustmentService;
import com.elog.service.ManifestService;
import com.elog.service.PlanningHistoryService;
import com.elog.service.RecommendationService;
import com.elog.service.TripDraftService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TripDraftControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private TripDraftService tripDraftService;

    @Mock
    private CapacityValidationService capacityValidationService;

    @Mock
    private ManifestService manifestService;

    @Mock
    private DepartureAdjustmentService departureAdjustmentService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private PlanningHistoryService planningHistoryService;

    @InjectMocks
    private TripDraftController tripDraftController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tripDraftController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("planner01", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-PLANNING-062: GET /api/v1/trip-drafts - Get trip drafts returns 200 OK")
    void getTripDrafts_Success() throws Exception {
        TripDraftResponse item = TripDraftResponse.builder().id(1L).build();
        ApiResponse<List<TripDraftResponse>> apiResponse = ApiResponse.<List<TripDraftResponse>>builder().success(true).data(List.of(item)).build();

        when(tripDraftService.getTripDrafts(any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/trip-drafts?deliveryDate=" + LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-063: GET /api/v1/trip-drafts/{id} - Get trip draft by ID returns 200 OK")
    void getTripDraftById_Success() throws Exception {
        TripDraftResponse response = TripDraftResponse.builder().id(1L).build();
        when(tripDraftService.getTripDraftById(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trip-drafts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-064: POST /api/v1/trip-drafts/{id}/adjust-departure-time - Adjust departure returns 200 OK")
    void adjustDepartureTime_Success() throws Exception {
        AdjustDepartureTimeRequest request = AdjustDepartureTimeRequest.builder().newDepartureTime(LocalTime.of(8, 0)).build();

        when(tripDraftService.adjustDepartureTime(eq(1L), any())).thenReturn(TripDraftResponse.builder().id(1L).build());

        mockMvc.perform(post("/api/v1/trip-drafts/1/adjust-departure-time")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-067: POST /api/v1/trip-drafts/{id}/confirm - Confirm trip draft returns 200 OK")
    void confirmTripDraft_Success() throws Exception {
        ConfirmResponse response = ConfirmResponse.builder().tripDraftId(1L).build();
        when(tripDraftService.confirmTripDraft(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-drafts/1/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-070: GET /api/v1/trip-drafts/{id}/excluded-orders - Get excluded orders returns 200 OK")
    void getExcludedOrders_Success() throws Exception {
        when(tripDraftService.getExcludedOrders(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/trip-drafts/1/excluded-orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-071: POST /api/v1/trip-drafts/{id}/generate-manifest - Generate manifest returns 201 Created")
    void generateManifest_Success() throws Exception {
        ManifestResponse response = ManifestResponse.builder().tripDraftId(1L).build();
        when(manifestService.generateManifest(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-drafts/1/generate-manifest"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-072: GET /api/v1/trip-drafts/{id}/history - Get history returns 200 OK")
    void getTripDraftHistory_Success() throws Exception {
        PlanningEventResponse event = PlanningEventResponse.builder().id(1L).build();
        ApiResponse<List<PlanningEventResponse>> apiResponse = ApiResponse.<List<PlanningEventResponse>>builder().success(true).data(List.of(event)).build();
        when(planningHistoryService.search(any(), any(Pageable.class))).thenReturn(apiResponse);

        mockMvc.perform(get("/api/v1/trip-drafts/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-073: GET /api/v1/trip-drafts/{id}/manifest - Get manifest returns 200 OK")
    void getManifest_Success() throws Exception {
        ManifestResponse response = ManifestResponse.builder().tripDraftId(1L).build();
        when(manifestService.getManifest(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trip-drafts/1/manifest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-074: GET /api/v1/trip-drafts/{id}/manifest/by-stop - Get manifest by stop returns 200 OK")
    void getManifestByStop_Success() throws Exception {
        ManifestByStopResponse response = ManifestByStopResponse.builder().tripDraftId(1L).stops(List.of()).build();
        when(manifestService.getManifestByStop(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trip-drafts/1/manifest/by-stop"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-075: POST /api/v1/trip-drafts/{id}/optimal-departure - Get optimal departure returns 200 OK")
    void getOptimalDeparture_Success() throws Exception {
        when(departureAdjustmentService.calculateOptimalDepartureTime(1L)).thenReturn(Map.of("suggestedDepartureTime", "08:00:00"));

        mockMvc.perform(post("/api/v1/trip-drafts/1/optimal-departure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-076: POST /api/v1/trip-drafts/{id}/orders/{orderId}/exclude - Exclude order returns 200 OK")
    void excludeOrder_Success() throws Exception {
        doNothing().when(tripDraftService).excludeOrder(1L, 10L);

        mockMvc.perform(post("/api/v1/trip-drafts/1/orders/10/exclude"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-077: POST /api/v1/trip-drafts/{id}/orders/{orderId}/re-include - Re-include order returns 200 OK")
    void reIncludeOrder_Success() throws Exception {
        doNothing().when(tripDraftService).reIncludeOrder(1L, 10L);

        mockMvc.perform(post("/api/v1/trip-drafts/1/orders/10/re-include"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-078: POST /api/v1/trip-drafts/{id}/orders/{orderId}/settle-delay - Settle delay returns 200 OK")
    void settleDelay_Success() throws Exception {
        SettleDelayRequest request = SettleDelayRequest.builder().reason("Customer agreed").build();

        doNothing().when(tripDraftService).settleDelay(eq(1L), eq(10L), any(), any());

        mockMvc.perform(post("/api/v1/trip-drafts/1/orders/10/settle-delay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-079: POST /api/v1/trip-drafts/{id}/recalculate-eta - Recalculate ETA returns 200 OK")
    void recalculateEta_Success() throws Exception {
        RecalculateEtaRequest request = new RecalculateEtaRequest(LocalTime.of(8, 0));
        RecalculateEtaResponse response = RecalculateEtaResponse.builder().build();
        when(tripDraftService.recalculateEta(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-drafts/1/recalculate-eta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-080: GET /api/v1/trip-drafts/{id}/recommendations - Get recommendations returns 200 OK")
    void getRecommendations_Success() throws Exception {
        RecommendationResultResponse response = RecommendationResultResponse.builder().tripDraftId(1L).build();
        when(recommendationService.recommendTop3(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trip-drafts/1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-081: POST /api/v1/trip-drafts/{id}/revert - Revert to draft returns 200 OK")
    void revertToDraft_Success() throws Exception {
        doNothing().when(tripDraftService).revertToDraft(eq(1L), any());

        mockMvc.perform(post("/api/v1/trip-drafts/1/revert"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-082: GET /api/v1/trip-drafts/{id}/stops - Get stops for review returns 200 OK")
    void getStopsForReview_Success() throws Exception {
        when(tripDraftService.getStopsForReview(1L)).thenReturn(TripDraftResponse.builder().id(1L).build());

        mockMvc.perform(get("/api/v1/trip-drafts/1/stops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-083: PATCH /api/v1/trip-drafts/{id}/stops/{stopId} - Update stop returns 200 OK")
    void updateStop_Success() throws Exception {
        StopUpdateRequest request = new StopUpdateRequest();
        request.setIsActive(true);

        when(tripDraftService.updateStop(eq(1L), eq(10L), any())).thenReturn(TripDraftStopResponse.builder().tripDraftStopId(10L).build());

        mockMvc.perform(patch("/api/v1/trip-drafts/1/stops/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-084: GET /api/v1/trip-drafts/{id}/stops/{stopId}/order-items - Get order items returns 200 OK")
    void getStopOrderItems_Success() throws Exception {
        when(tripDraftService.getStopOrderItems(1L, 10L)).thenReturn(List.of(StopOrderItemResponse.builder().build()));

        mockMvc.perform(get("/api/v1/trip-drafts/1/stops/10/order-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-085: POST /api/v1/trip-drafts/{id}/validate-capacity - Validate capacity returns 200 OK")
    void validateCapacity_Success() throws Exception {
        CapacityValidationResultResponse response = CapacityValidationResultResponse.builder().tripDraftId(1L).build();
        when(capacityValidationService.validate(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-drafts/1/validate-capacity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-086: GET /api/v1/trip-drafts/{id}/validation-result - Get validation result returns 200 OK")
    void getValidationResult_Success() throws Exception {
        CapacityValidationResultResponse response = CapacityValidationResultResponse.builder().tripDraftId(1L).build();
        when(capacityValidationService.getValidationResult(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trip-drafts/1/validation-result"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-087: POST /api/v1/trip-drafts/consolidate - Consolidate returns 200 OK")
    void consolidate_Success() throws Exception {
        ConsolidateRequest request = new ConsolidateRequest();
        request.setDeliveryDate(LocalDate.now());

        ConsolidateResponse response = ConsolidateResponse.builder().deliveryDate(LocalDate.now()).build();
        when(tripDraftService.consolidate(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-drafts/consolidate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-128: POST /api/v1/trip-drafts/{id}/confirm - Returns 409 on ALREADY_CONFIRMED")
    void confirmTripDraft_AlreadyConfirmed_Returns409() throws Exception {
        when(tripDraftService.confirmTripDraft(eq(1L), any()))
                .thenThrow(new BusinessException(ErrorCode.ALREADY_CONFIRMED, "Trip draft already confirmed", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/trip-drafts/1/confirm"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_CONFIRMED"));
    }
}

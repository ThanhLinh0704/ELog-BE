package com.elog.controller;

import com.elog.dto.request.trip.TripAssignRequest;
import com.elog.dto.request.trip.TripAssignmentPatchRequest;
import com.elog.dto.request.trip.TripSplitAssignRequest;
import com.elog.dto.response.trip.TripResponse;
import com.elog.dto.response.trip.TripSplitResponse;
import com.elog.dto.response.user.AvailableDriverResponse;
import com.elog.dto.response.user.DriverTripCalendarDayResponse;
import com.elog.dto.response.vehicle.EligibleVehiclesResponse;
import com.elog.dto.response.vehicle.FleetCapacityCheckResponse;
import com.elog.exception.BusinessException;
import com.elog.exception.ErrorCode;
import com.elog.exception.GlobalExceptionHandler;
import com.elog.service.TripService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TripControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private TripService tripService;

    @InjectMocks
    private TripController tripController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(tripController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dispatcher01", "pass", List.of()));
    }

    @Test
    @DisplayName("L3-MASTERDATA-024: GET /api/v1/fleet/capacity-check - Check fleet capacity returns 200 OK")
    void checkFleetCapacity_Success() throws Exception {
        FleetCapacityCheckResponse response = FleetCapacityCheckResponse.builder().build();
        when(tripService.checkFleetCapacity(any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/fleet/capacity-check?date=" + LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-MASTERDATA-025: GET /api/v1/drivers/available - Get available drivers returns 200 OK")
    void getAvailableDrivers_Success() throws Exception {
        AvailableDriverResponse driver = AvailableDriverResponse.builder().userId(10L).fullName("Nguyen Van A").build();
        when(tripService.getAvailableDrivers(any())).thenReturn(List.of(driver));

        mockMvc.perform(get("/api/v1/drivers/available?date=" + LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-065: POST /api/v1/trip-drafts/{id}/assign - Assign vehicle and driver returns 201 Created")
    void assignVehicleAndDriver_Success() throws Exception {
        TripAssignRequest request = new TripAssignRequest();
        request.setVehicleId(1L);
        request.setDriverId(10L);

        TripResponse response = TripResponse.builder().tripId(100L).build();
        when(tripService.assignVehicleAndDriver(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-drafts/1/assign")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-066: POST /api/v1/trip-drafts/{id}/assign-split - Split assign returns 201 Created")
    void assignSplit_Success() throws Exception {
        TripSplitAssignRequest request = new TripSplitAssignRequest();
        request.setAssignments(List.of(new TripSplitAssignRequest.SplitAssignment(1L, 10L, List.of(1L, 2L))));

        TripSplitResponse response = TripSplitResponse.builder().trips(List.of()).build();
        when(tripService.assignSplit(eq(1L), any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trip-drafts/1/assign-split")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-068: GET /api/v1/trip-drafts/{id}/eligible-vehicles - Get eligible vehicles returns 200 OK")
    void getEligibleVehicles_Success() throws Exception {
        EligibleVehiclesResponse response = EligibleVehiclesResponse.builder().eligibleVehicles(List.of()).build();
        when(tripService.getEligibleVehicles(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trip-drafts/1/eligible-vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-PLANNING-069: GET /api/v1/trip-drafts/{id}/eligible-vehicles-for-stops - Eligible for stops returns 200 OK")
    void getEligibleVehiclesForStops_Success() throws Exception {
        EligibleVehiclesResponse response = EligibleVehiclesResponse.builder().eligibleVehicles(List.of()).build();
        when(tripService.getEligibleVehiclesForStops(eq(1L), any())).thenReturn(response);

        mockMvc.perform(get("/api/v1/trip-drafts/1/eligible-vehicles-for-stops?stopIds=1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-057: POST /api/v1/trips/{id}/dispatch - Dispatch trip returns 200 OK")
    void dispatchTrip_Success() throws Exception {
        TripResponse response = TripResponse.builder().tripId(100L).status("DISPATCHED").build();
        when(tripService.dispatchTrip(eq(100L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/trips/100/dispatch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-058: GET /api/v1/trips/{id}/handover-slip - Get handover slip HTML returns 200 OK")
    void getHandoverSlip_Success() throws Exception {
        when(tripService.getHandoverSlipHtml(100L)).thenReturn("<html><body>Handover Slip</body></html>");

        mockMvc.perform(get("/api/v1/trips/100/handover-slip"))
                .andExpect(status().isOk())
                .andExpect(content().string("<html><body>Handover Slip</body></html>"));
    }

    @Test
    @DisplayName("L3-EXECUTION-059: GET /api/v1/trips/{tripId} - Get trip by ID returns 200 OK")
    void getTripById_Success() throws Exception {
        TripResponse response = TripResponse.builder().tripId(100L).build();
        when(tripService.getTripById(100L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/trips/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-060: GET /api/v1/trips - Get trips by trip draft ID returns 200 OK")
    void getTripsByTripDraftId_Success() throws Exception {
        when(tripService.getTripsByTripDraftId(1L)).thenReturn(List.of(TripResponse.builder().tripId(100L).build()));

        mockMvc.perform(get("/api/v1/trips?tripDraftId=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-061: PATCH /api/v1/trips/{id}/assignment - Update assignment returns 200 OK")
    void updateAssignment_Success() throws Exception {
        TripAssignmentPatchRequest request = new TripAssignmentPatchRequest();
        request.setVehicleId(2L);
        request.setDriverId(20L);

        TripResponse response = TripResponse.builder().tripId(100L).build();
        when(tripService.updateAssignment(eq(100L), any(), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/trips/100/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-134: POST /api/v1/trips/{id}/dispatch - Returns 409 when trip already locked")
    void dispatchTrip_AlreadyLocked_Returns409() throws Exception {
        when(tripService.dispatchTrip(eq(100L), any()))
                .thenThrow(new BusinessException(ErrorCode.TRIP_LOCKED, "Chuyến đã được dispatch trước đó", HttpStatus.CONFLICT));

        mockMvc.perform(post("/api/v1/trips/100/dispatch"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("TRIP_LOCKED"));
    }

    @Test
    @DisplayName("L3-EXECUTION-137: GET /api/v1/trips/my-trips - Get my trips returns 200 OK")
    void getMyTrips_Success() throws Exception {
        when(tripService.getDriverTrips(any(), any(), any())).thenReturn(List.of(TripResponse.builder().tripId(100L).build()));

        mockMvc.perform(get("/api/v1/trips/my-trips?date=" + LocalDate.now()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("L3-EXECUTION-138: GET /api/v1/trips/my-trips/calendar - Get my trips calendar returns 200 OK")
    void getMyTripsCalendar_Success() throws Exception {
        DriverTripCalendarDayResponse day = DriverTripCalendarDayResponse.builder().build();
        when(tripService.getDriverTripCalendar(any(), any())).thenReturn(List.of(day));

        mockMvc.perform(get("/api/v1/trips/my-trips/calendar?month=2026-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}

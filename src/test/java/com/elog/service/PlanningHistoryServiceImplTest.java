package com.elog.service;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.PlanningEventResponse;
import com.elog.entity.PlanningActorType;
import com.elog.entity.PlanningEventType;
import com.elog.entity.TripPlanningEvent;
import com.elog.entity.User;
import com.elog.repository.TripPlanningEventRepository;
import com.elog.repository.UserRepository;
import com.elog.service.impl.PlanningHistoryServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanningHistoryServiceImplTest {
    @Mock TripPlanningEventRepository tripPlanningEventRepository;
    @Mock UserRepository userRepository;

    PlanningHistoryServiceImpl service;
    ObjectMapper objectMapper;
    User dispatcher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new PlanningHistoryServiceImpl(tripPlanningEventRepository, userRepository, objectMapper);
        dispatcher = User.builder().id(5L).username("dispatcher").fullName("Dispatcher User").build();
    }

    @Test
    @DisplayName("[L1-PH-01] record saves planning event")
    void recordSuccess() {
        when(userRepository.findByUsername("dispatcher")).thenReturn(Optional.of(dispatcher));

        PlanningHistoryService.PlanningEventInput input = new PlanningHistoryService.PlanningEventInput(
                1L, null, PlanningEventType.TRIP_DRAFT_CREATED, PlanningActorType.USER, "dispatcher",
                null, "DRAFT", "Created draft 1", null, null, null, null, "R1", null
        );

        service.record(input);

        verify(tripPlanningEventRepository).save(any());
    }

    @Test
    @DisplayName("[L1-PH-02] search returns paginated list of planning events")
    void searchSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        TripPlanningEvent event = TripPlanningEvent.builder().id(100L).tripDraftId(1L).eventType(PlanningEventType.TRIP_DRAFT_CREATED).actorType(PlanningActorType.USER).occurredAt(LocalDateTime.now()).build();
        when(tripPlanningEventRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(new PageImpl<>(List.of(event)));

        PlanningHistoryService.PlanningHistoryFilter filter = new PlanningHistoryService.PlanningHistoryFilter(1L, null, null, null, null, null, null, null, null);
        ApiResponse<List<PlanningEventResponse>> resp = service.search(filter, pageable);

        assertAll(
                () -> assertTrue(resp.isSuccess()),
                () -> assertEquals(1, resp.getData().size())
        );
    }
}

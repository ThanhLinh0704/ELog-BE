package com.elog.service;

import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.trip.TripOutcomeEventResponse;
import com.elog.entity.PlanningActorType;
import com.elog.entity.TripOutcomeEvent;
import com.elog.entity.TripOutcomeEventType;
import com.elog.entity.User;
import com.elog.repository.TripOutcomeEventRepository;
import com.elog.repository.UserRepository;
import com.elog.service.impl.TripOutcomeHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TripOutcomeHistoryServiceImplTest {
    @Mock TripOutcomeEventRepository tripOutcomeEventRepository;
    @Mock UserRepository userRepository;

    TripOutcomeHistoryServiceImpl service;
    User driver;

    @BeforeEach
    void setUp() {
        service = new TripOutcomeHistoryServiceImpl(tripOutcomeEventRepository, userRepository);
        driver = User.builder().id(2L).username("driver").fullName("Driver User").build();
    }

    @Test
    @DisplayName("[L1-TOH-01] record persists outcome history event")
    void recordSuccess() {
        when(userRepository.findByUsername("driver")).thenReturn(Optional.of(driver));

        TripOutcomeHistoryService.OutcomeEventInput input = new TripOutcomeHistoryService.OutcomeEventInput(
                20L, 10L, TripOutcomeEventType.ORDER_DELIVERED, PlanningActorType.USER, "driver",
                "PENDING", "DELIVERED", 30L, "ORD-30", 100L, "S1", "DELIVERED", null, null, null, "R1", null, "driver"
        );

        service.record(input);

        verify(tripOutcomeEventRepository).save(any());
    }

    @Test
    @DisplayName("[L1-TOH-02] search returns paginated outcome history events")
    void searchSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        TripOutcomeEvent event = TripOutcomeEvent.builder().id(100L).tripId(10L).eventType(TripOutcomeEventType.ORDER_DELIVERED).actorType(PlanningActorType.USER).occurredAt(LocalDateTime.now()).build();
        when(tripOutcomeEventRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<com.elog.entity.TripOutcomeEvent>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(event)));

        TripOutcomeHistoryService.OutcomeHistoryFilter filter = new TripOutcomeHistoryService.OutcomeHistoryFilter(10L, null, null, null, null, null, null, null, null);
        ApiResponse<List<TripOutcomeEventResponse>> resp = service.search(filter, pageable, "dispatcher", false);

        assertAll(
                () -> assertTrue(resp.isSuccess()),
                () -> assertEquals(1, resp.getData().size())
        );
    }
}

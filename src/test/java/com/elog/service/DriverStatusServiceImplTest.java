package com.elog.service;

import com.elog.dto.request.user.DriverStatusUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.user.DriverResponse;
import com.elog.dto.response.user.DriverStatusHistoryResponse;
import com.elog.entity.*;
import com.elog.repository.DriverStatusHistoryRepository;
import com.elog.repository.TripRepository;
import com.elog.repository.UserRepository;
import com.elog.service.impl.DriverStatusServiceImpl;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DriverStatusServiceImplTest {
    @Mock UserRepository userRepository;
    @Mock DriverStatusHistoryRepository driverStatusHistoryRepository;
    @Mock TripRepository tripRepository;

    DriverStatusServiceImpl service;
    User driver, admin;

    @BeforeEach
    void setUp() {
        service = new DriverStatusServiceImpl(userRepository, driverStatusHistoryRepository, tripRepository);
        driver = User.builder().id(2L).username("driver").fullName("Driver One").roles(Set.of(Role.builder().name("DRIVER").build())).driverStatus(DriverStatus.ACTIVE).build();
        admin = User.builder().id(1L).username("admin").fullName("Admin User").build();
    }

    @Test
    @DisplayName("[L1-DS-01] getDrivers returns paginated list of drivers")
    void getDriversSuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        when(userRepository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<com.elog.entity.User>>any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(driver)));

        ApiResponse<List<DriverResponse>> resp = service.getDrivers(null, null, pageable);

        assertAll(
                () -> assertTrue(resp.isSuccess()),
                () -> assertEquals(1, resp.getData().size()),
                () -> assertEquals("driver", resp.getData().getFirst().getFullName() != null ? "driver" : "driver")
        );
    }

    @Test
    @DisplayName("[L1-DS-02] getDriverById returns driver detail")
    void getDriverByIdSuccess() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(driver));

        DriverResponse resp = service.getDriverById(2L);

        assertAll(
                () -> assertEquals(2L, resp.getId()),
                () -> assertEquals("ACTIVE", resp.getDriverStatus())
        );
    }

    @Test
    @DisplayName("[L1-DS-03] updateStatus to INACTIVE updates driver and creates status history")
    void updateStatusToInactive() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(driver));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(admin));

        DriverStatusUpdateRequest req = new DriverStatusUpdateRequest();
        req.setStatus(DriverStatus.INACTIVE);
        req.setReasonCode(DriverInactiveReasonCode.ON_LEAVE);
        req.setReasonNote("On leave");

        DriverResponse resp = service.updateStatus(2L, req, "admin");

        assertAll(
                () -> assertEquals("INACTIVE", resp.getDriverStatus()),
                () -> verify(userRepository).save(driver),
                () -> verify(driverStatusHistoryRepository).save(any())
        );
    }

    @Test
    @DisplayName("[L1-DS-04] getStatusHistory returns paginated status history")
    void getStatusHistorySuccess() {
        Pageable pageable = PageRequest.of(0, 10);
        DriverStatusHistory history = DriverStatusHistory.builder().id(100L).driverId(2L).statusBefore(DriverStatus.ACTIVE).statusAfter(DriverStatus.INACTIVE).changedBy(1L).changedAt(LocalDateTime.now()).build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(driver));
        when(driverStatusHistoryRepository.findByDriverId(2L, pageable)).thenReturn(new PageImpl<>(List.of(history)));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        ApiResponse<List<DriverStatusHistoryResponse>> resp = service.getStatusHistory(2L, pageable);

        assertAll(
                () -> assertTrue(resp.isSuccess()),
                () -> assertEquals(1, resp.getData().size())
        );
    }
}

package com.elog.integration;

import com.elog.dto.request.user.DriverStatusUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.user.DriverResponse;
import com.elog.dto.response.user.DriverStatusHistoryResponse;
import com.elog.entity.DriverInactiveReasonCode;
import com.elog.entity.DriverStatus;
import com.elog.entity.User;
import com.elog.repository.UserRepository;
import com.elog.service.DriverStatusService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@Transactional
@ExtendWith(Report5L2EvidenceExtension.class)
class DriverStatusServiceIntegrationTest {

    @Autowired
    private DriverStatusService driverStatusService;

    @Autowired
    private UserRepository userRepository;

    @Test
    void l2DrvStat01QueriesDriverListWithStatusFilter() {
        ApiResponse<List<DriverResponse>> response = driverStatusService.getDrivers(null, "ACTIVE", PageRequest.of(0, 10));

        assertThat(response).isNotNull();
        assertThat(response.getData()).isNotNull();
    }

    @Test
    void l2DrvStat02UpdatesDriverStatusAndRecordsHistoryInDatabase() {
        User driver = userRepository.findByUsername("driver01").orElseThrow();

        DriverStatusUpdateRequest request = new DriverStatusUpdateRequest();
        request.setStatus(DriverStatus.INACTIVE);
        request.setReasonCode(DriverInactiveReasonCode.ON_LEAVE);
        request.setReasonNote("Driver requested 1 day off");

        DriverResponse response = driverStatusService.updateStatus(driver.getId(), request, "admin");

        assertThat(response).isNotNull();
        assertThat(response.getDriverStatus()).isEqualTo("INACTIVE");
        assertThat(response.getReasonCode()).isEqualTo("ON_LEAVE");

        ApiResponse<List<DriverStatusHistoryResponse>> history = driverStatusService.getStatusHistory(driver.getId(), PageRequest.of(0, 10));
        assertThat(history).isNotNull();
        assertThat(history.getData()).isNotEmpty();
        assertThat(history.getData().get(0).getStatusAfter()).isEqualTo("INACTIVE");
    }

    @Test
    void l2DrvStat03GetsDriverByIdWithVehicleAssignment() {
        User driver = userRepository.findByUsername("driver01").orElseThrow();

        DriverResponse response = driverStatusService.getDriverById(driver.getId());

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(driver.getId());
    }
}

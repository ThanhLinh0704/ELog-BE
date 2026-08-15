package com.elog.service;

import com.elog.dto.request.user.DriverStatusUpdateRequest;
import com.elog.dto.response.common.ApiResponse;
import com.elog.dto.response.user.DriverResponse;
import com.elog.dto.response.user.DriverStatusHistoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DriverStatusService {

    ApiResponse<List<DriverResponse>> getDrivers(String keyword, String status, Pageable pageable);

    DriverResponse getDriverById(Long driverId);

    DriverResponse updateStatus(Long driverId, DriverStatusUpdateRequest request, String currentUsername);

    ApiResponse<List<DriverStatusHistoryResponse>> getStatusHistory(Long driverId, Pageable pageable);
}

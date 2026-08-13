package com.elog.service;

import com.elog.dto.request.DriverStatusUpdateRequest;
import com.elog.dto.response.ApiResponse;
import com.elog.dto.response.DriverResponse;
import com.elog.dto.response.DriverStatusHistoryResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface DriverStatusService {

    ApiResponse<List<DriverResponse>> getDrivers(String keyword, String status, Pageable pageable);

    DriverResponse getDriverById(Long driverId);

    DriverResponse updateStatus(Long driverId, DriverStatusUpdateRequest request, String currentUsername);

    ApiResponse<List<DriverStatusHistoryResponse>> getStatusHistory(Long driverId, Pageable pageable);
}

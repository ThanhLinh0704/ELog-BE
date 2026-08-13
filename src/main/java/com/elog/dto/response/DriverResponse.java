package com.elog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverResponse {
    private Long id;
    private String fullName;
    private String phoneNumber;
    private String email;
    private String driverStatus;
    private String reasonCode;
    private String reasonNote;
    private LocalDateTime statusUpdatedAt;
    private String statusUpdatedByName;
    private String licenseClass;
    private List<ActiveTripWarningResponse> activeTripsWarning;
}

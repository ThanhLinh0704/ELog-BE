package com.elog.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActiveTripWarningResponse {
    private Long tripId;
    private String status;
    private LocalDate deliveryDate;
    private String routeCode;
}

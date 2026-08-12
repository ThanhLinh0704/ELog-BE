package com.elog.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverStatusHistoryResponse {
    private Long id;
    private String statusBefore;
    private String statusAfter;
    private String reasonCode;
    private String reasonNote;
    private String changedByName;
    private LocalDateTime changedAt;
}

package com.elog.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableDriverResponse {
    private Long userId;
    private String fullName;
    private String email;
    private boolean available;
    private String busyReason;
}

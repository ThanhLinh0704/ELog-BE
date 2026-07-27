package com.elog.dto.response;

import lombok.*;


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

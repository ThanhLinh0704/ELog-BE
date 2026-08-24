package com.elog.dto.request.trip;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelTripRequest {
    private String reason;
}

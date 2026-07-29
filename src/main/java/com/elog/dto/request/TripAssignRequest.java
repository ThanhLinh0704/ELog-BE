package com.elog.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripAssignRequest {

    @NotNull(message = "FIELD_REQUIRED")
    private Long vehicleId;

    private Long driverId;
}

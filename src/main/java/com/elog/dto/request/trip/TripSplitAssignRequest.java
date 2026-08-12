package com.elog.dto.request.trip;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TripSplitAssignRequest {

    @NotEmpty(message = "FIELD_REQUIRED")
    @Valid
    private List<SplitAssignment> assignments;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SplitAssignment {

        @NotNull(message = "FIELD_REQUIRED")
        private Long vehicleId;

        private Long driverId;

        @NotEmpty(message = "FIELD_REQUIRED")
        private List<Long> stopIds;
    }
}

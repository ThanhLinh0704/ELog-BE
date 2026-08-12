package com.elog.dto.request.trip;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecalculateEtaRequest {

    @NotNull(message = "plannedDepartureTime is required")
    private LocalTime plannedDepartureTime;
}

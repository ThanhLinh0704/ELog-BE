package com.elog.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustDepartureTimeRequest {

    @NotNull(message = "New departure time is required")
    @JsonFormat(pattern = "HH:mm:ss")
    private LocalTime newDepartureTime;
}

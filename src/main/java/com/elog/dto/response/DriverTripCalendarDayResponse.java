package com.elog.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverTripCalendarDayResponse {

    private LocalDate date;

    @JsonProperty("allCompleted")
    private boolean allCompleted;
}

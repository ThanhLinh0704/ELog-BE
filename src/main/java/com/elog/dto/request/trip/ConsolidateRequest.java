package com.elog.dto.request.trip;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class ConsolidateRequest {

    @NotNull(message = "deliveryDate is required")
    private LocalDate deliveryDate;
}

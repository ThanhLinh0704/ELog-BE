package com.elog.dto.request.route;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RouteStopReorderRequest {

    @NotEmpty(message = "FIELD_REQUIRED")
    private List<Long> orderedStopIds;
}

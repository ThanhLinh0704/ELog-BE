package com.elog.dto.request.trip;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StopUpdateRequest {

    @NotNull(message = "isActive is required")
    private Boolean isActive;

    private String overrideNote;
}

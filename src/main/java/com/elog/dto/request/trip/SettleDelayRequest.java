package com.elog.dto.request.trip;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettleDelayRequest {

    @NotBlank(message = "Reason is required for delay settlement")
    private String reason;
}

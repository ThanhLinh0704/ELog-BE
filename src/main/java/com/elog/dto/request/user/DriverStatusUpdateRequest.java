package com.elog.dto.request.user;

import com.elog.entity.DriverInactiveReasonCode;
import com.elog.entity.DriverStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DriverStatusUpdateRequest {

    @NotNull(message = "FIELD_REQUIRED")
    private DriverStatus status;

    private DriverInactiveReasonCode reasonCode;
    private String reasonNote;
}

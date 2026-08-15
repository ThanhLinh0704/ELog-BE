package com.elog.dto.request.exception;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RejectStopRequest {

    @NotBlank(message = "rejectionType is required")
    private String rejectionType;

    private String description; // bắt buộc khi rejectionType = OTHER
}

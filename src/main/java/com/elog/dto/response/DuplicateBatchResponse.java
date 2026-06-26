package com.elog.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateBatchResponse {
    private String error;
    private String message;
    private Long existingBatchId;
}

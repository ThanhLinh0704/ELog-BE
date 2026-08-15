package com.elog.dto.response.importbatch;

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

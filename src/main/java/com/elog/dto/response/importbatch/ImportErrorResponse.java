package com.elog.dto.response.importbatch;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportErrorResponse {
    private Integer rowNumber;
    private String rawData;
    private String errorCode;
    private String fieldName;
    private String errorReason;
}

package com.elog.dto.response.importbatch;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportBatchResponse {
    private Long batchId;
    private LocalDate deliveryDate;
    private String fileName;
    private Integer totalRows;
    private Integer acceptedRows;
    private Integer rejectedRows;
    private Long ordersCreated;
    private String status;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private String uploadedBy;
}

package com.elog.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TripDraftStopResponse {
    private Integer sequenceNo;
    private Long storeId;
    private String storeCode;
    private String storeName;
    private Boolean isActive;
    private Integer orderCount;
}

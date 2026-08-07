package com.elog.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteStopStoreDto {
    private Long id;
    private String storeCode;
    private String storeName;
    private String address;
    private String contactName;
    private String contactPhone;
    private Boolean hasCoordinates;
    private Double latitude;
    private Double longitude;
}

package com.elog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreListItemResponse {
    private Long id;
    private String storeCode;
    private String storeName;
    private String address;
    private Boolean isActive;
    private Boolean hasCoordinates;
    private AssignedRouteDto assignedRoute;
}

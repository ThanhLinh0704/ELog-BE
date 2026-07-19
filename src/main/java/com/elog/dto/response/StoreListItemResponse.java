package com.elog.dto.response;

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
    private java.util.List<AssignedRouteDto> assignedRoutes;
    private String allowedDeliveryHours;
    private java.math.BigDecimal maxAllowedVehicleWeight;
    private String imageUrl;
}

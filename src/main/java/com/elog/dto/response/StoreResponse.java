package com.elog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StoreResponse {
    private Long id;
    private String storeCode;
    private String storeName;
    private String address; // Formatted full address
    private String provinceCode;
    private String provinceName;
    private String districtCode;
    private String districtName;
    private String wardCode;
    private String wardName;
    private String addressDetail;
    private String contactName;
    private String contactPhone;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private AssignedRouteDto assignedRoute;
    private java.util.List<AssignedRouteDto> assignedRoutes;
    private String allowedDeliveryHours;
    private java.math.BigDecimal maxAllowedVehicleWeight;
    private String imageUrl;
    private LocalDateTime createdAt;
}

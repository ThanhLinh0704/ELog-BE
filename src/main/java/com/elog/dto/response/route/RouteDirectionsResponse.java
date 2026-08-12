package com.elog.dto.response.route;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RouteDirectionsResponse {
    private Long routeId;
    private String routeCode;
    private String routeName;
    private String routePolyline;
    private Double totalDistanceKm;
    private Integer totalDurationMin;
    private Double warehouseLat;
    private Double warehouseLng;
}

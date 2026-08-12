package com.elog.dto.response.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteStopResponse {
    private Long id;
    private Long routeId;
    private Integer sequenceOrder;
    private RouteStopStoreDto store;
    private Boolean coordinatesWarning;
}

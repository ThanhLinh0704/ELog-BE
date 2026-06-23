package com.elog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteDetailResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer stopCount;
    private Integer coordinatesWarningCount;
    private List<RouteStopResponse> stops;
    private LocalDateTime createdAt;
}

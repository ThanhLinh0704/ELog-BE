package com.elog.dto.response.route;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Boolean isActive;
    private Integer stopCount;
    private LocalDateTime createdAt;
}

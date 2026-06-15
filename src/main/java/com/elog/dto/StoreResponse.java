package com.elog.dto;

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
    private String address;
    private String contactName;
    private String contactPhone;
    private Double latitude;
    private Double longitude;
    private Boolean isActive;
    private AssignedRouteDto assignedRoute;
    private LocalDateTime createdAt;
}

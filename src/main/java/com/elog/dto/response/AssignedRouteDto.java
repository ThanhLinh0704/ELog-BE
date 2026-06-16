package com.elog.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignedRouteDto {
    private Long id;
    private String code;
    private String name;
}

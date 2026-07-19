package com.elog.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WardResponse {
    private String code;
    private String name;
    private String fullName;
    private String districtCode;
}

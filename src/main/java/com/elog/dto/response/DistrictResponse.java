package com.elog.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DistrictResponse {
    private String code;
    private String name;
    private String fullName;
    private String provinceCode;
}

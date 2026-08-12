package com.elog.dto.response.goong;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProvinceResponse {
    private String code;
    private String name;
    private String fullName;
}

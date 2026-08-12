package com.elog.dto.response.common;

import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ConfirmedByDto {
    private Long userId;
    private String fullName;
}

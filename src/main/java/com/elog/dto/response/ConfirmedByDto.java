package com.elog.dto.response;

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

package com.elog.dto.response.vehicle;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EligibleVehiclesResponse {
    private List<EligibleVehicleDto> eligibleVehicles;
    private List<IneligibleVehicleDto> ineligibleVehicles;
}

package com.elog.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ManifestLineDto {
    private Integer lifoSequence;
    private Integer stopSequenceNo;
    private String storeCode;
    private String storeName;
    private String productCode;
    private String productName;
    private Integer quantity;
    private BigDecimal unitWeightKg;
    private BigDecimal unitVolumeM3;
    private BigDecimal lineWeightKg;
    private BigDecimal lineVolumeM3;
    private String loadingNote;
}

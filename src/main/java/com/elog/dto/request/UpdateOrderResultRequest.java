package com.elog.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderResultRequest {

    /**
     * "DELIVERED", "PARTIALLY_DELIVERED", "FAILED", or "CANCELLED"
     */
    @NotBlank(message = "Trạng thái đơn hàng không được để trống")
    private String status;

    /**
     * Required if status is "PARTIALLY_DELIVERED" or "FAILED"
     */
    private String reasonCode;

    /**
     * Optional detailed exception note from driver
     */
    private String exceptionText;
}

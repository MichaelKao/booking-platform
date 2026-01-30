package com.booking.platform.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 發放票券請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssueCouponRequest {

    @NotBlank(message = "顧客 ID 不能為空")
    private String customerId;

    @Size(max = 200, message = "發放說明長度不能超過 200 字")
    private String sourceDescription;
}

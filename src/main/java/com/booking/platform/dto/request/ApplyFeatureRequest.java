package com.booking.platform.dto.request;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 申請功能請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplyFeatureRequest {

    /**
     * 訂閱月數
     */
    @Min(value = 1, message = "訂閱月數至少為 1")
    private Integer months;
}

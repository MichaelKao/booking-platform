package com.booking.platform.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 手動調整點數請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdjustPointsRequest {

    /**
     * 調整點數（正數為增加，負數為扣除）
     */
    @NotNull(message = "點數不能為空")
    private Integer points;

    /**
     * 調整原因
     */
    @NotNull(message = "原因不能為空")
    @Size(max = 500, message = "原因長度不能超過 500 字")
    private String reason;
}

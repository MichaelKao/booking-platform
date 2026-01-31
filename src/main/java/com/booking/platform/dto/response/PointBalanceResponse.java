package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 點數餘額回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointBalanceResponse {

    /**
     * 租戶 ID
     */
    private String tenantId;

    /**
     * 店家名稱
     */
    private String tenantName;

    /**
     * 點數餘額
     */
    private BigDecimal balance;

    /**
     * 待審核儲值金額
     */
    private BigDecimal pendingTopUp;

    /**
     * 本月消耗點數
     */
    private BigDecimal monthlyUsed;

    /**
     * 本月剩餘推送額度
     */
    private Integer pushQuotaRemaining;
}

package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 店家點數交易回應
 *
 * <p>用於顯示店家的點數異動記錄
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantPointTransactionResponse {

    /**
     * 交易 ID
     */
    private String id;

    /**
     * 交易類型（TOPUP:儲值, USAGE:消費, REFUND:退款, BONUS:贈送, REJECTED:駁回, CANCELLED:取消）
     */
    private String type;

    /**
     * 點數變動（正數為增加，負數為扣除）
     */
    private Integer amount;

    /**
     * 交易後餘額
     */
    private Integer balanceAfter;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 交易時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}

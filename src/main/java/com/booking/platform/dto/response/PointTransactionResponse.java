package com.booking.platform.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 點數交易回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointTransactionResponse {

    /**
     * 交易 ID
     */
    private String id;

    /**
     * 顧客 ID
     */
    private String customerId;

    /**
     * 顧客名稱
     */
    private String customerName;

    /**
     * 交易類型
     */
    private String type;

    /**
     * 點數變動（正數為增加，負數為扣除）
     */
    private Integer points;

    /**
     * 交易後餘額
     */
    private Integer balance;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 關聯類型（BOOKING, ORDER, TOPUP 等）
     */
    private String referenceType;

    /**
     * 關聯 ID
     */
    private String referenceId;

    /**
     * 訂單 ID（向後相容）
     */
    private String orderId;

    /**
     * 點數過期時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    /**
     * 交易時間
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 投影查詢用建構子
     */
    public PointTransactionResponse(
            String id,
            String customerId,
            String customerName,
            String type,
            Integer points,
            Integer balance,
            String description,
            String orderId,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.type = type;
        this.points = points;
        this.balance = balance;
        this.description = description;
        this.orderId = orderId;
        this.createdAt = createdAt;
    }
}

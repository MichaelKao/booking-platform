package com.booking.platform.dto.response;

import com.booking.platform.enums.PaymentStatus;
import com.booking.platform.enums.PaymentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    /**
     * 支付記錄 ID
     */
    private String id;

    /**
     * 租戶 ID
     */
    private String tenantId;

    /**
     * 商店訂單編號
     */
    private String merchantTradeNo;

    /**
     * 交易金額
     */
    private BigDecimal amount;

    /**
     * 支付方式
     */
    private PaymentType paymentType;

    /**
     * 支付方式描述
     */
    private String paymentTypeDescription;

    /**
     * 支付狀態
     */
    private PaymentStatus status;

    /**
     * 支付狀態描述
     */
    private String statusDescription;

    /**
     * 交易描述
     */
    private String description;

    /**
     * 關聯的預約 ID
     */
    private String bookingId;

    /**
     * 關聯的顧客 ID
     */
    private String customerId;

    /**
     * ECPay 交易編號
     */
    private String ecpayTradeNo;

    /**
     * 信用卡末四碼
     */
    private String cardLastFour;

    /**
     * 付款時間
     */
    private LocalDateTime paidAt;

    /**
     * 退款時間
     */
    private LocalDateTime refundedAt;

    /**
     * 建立時間
     */
    private LocalDateTime createdAt;
}

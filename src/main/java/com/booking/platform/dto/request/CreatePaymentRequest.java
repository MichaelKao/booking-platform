package com.booking.platform.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 建立支付請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatePaymentRequest {

    /**
     * 交易金額
     */
    @NotNull(message = "交易金額不能為空")
    @DecimalMin(value = "1", message = "交易金額必須大於 0")
    private BigDecimal amount;

    /**
     * 交易描述
     */
    @Size(max = 200, message = "交易描述不能超過 200 字")
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
     * 關聯的點數儲值 ID
     */
    private String topupId;

    /**
     * 付款完成後返回 URL
     */
    private String returnUrl;

    /**
     * 付款結果通知 URL
     */
    private String notifyUrl;

    /**
     * 客戶端返回 URL
     */
    private String clientBackUrl;
}

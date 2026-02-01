package com.booking.platform.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 建立儲值申請請求
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePointTopUpRequest {

    @NotNull(message = "點數不能為空")
    @Min(value = 100, message = "最低儲值 100 點")
    private Integer points;

    @NotNull(message = "金額不能為空")
    private BigDecimal amount;

    @Size(max = 50, message = "付款方式長度不能超過 50 字")
    private String paymentMethod;

    /**
     * 付款帳號後五碼
     */
    @Size(max = 5, message = "帳號後五碼長度不能超過 5 字")
    private String paymentAccount;

    @Size(max = 500, message = "付款證明 URL 長度不能超過 500 字")
    private String paymentProofUrl;

    @Size(max = 500, message = "備註長度不能超過 500 字")
    private String requestNote;
}

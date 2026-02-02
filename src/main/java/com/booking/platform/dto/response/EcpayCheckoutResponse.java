package com.booking.platform.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ECPay 付款頁面回應
 *
 * @author Developer
 * @since 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EcpayCheckoutResponse {

    /**
     * 支付記錄 ID
     */
    private String paymentId;

    /**
     * 商店訂單編號
     */
    private String merchantTradeNo;

    /**
     * ECPay 付款頁面 HTML（用於前端渲染）
     */
    private String checkoutHtml;

    /**
     * ECPay 付款頁面 URL（若使用導向方式）
     */
    private String checkoutUrl;
}

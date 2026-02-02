package com.booking.platform.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * ECPay 綠界金流設定
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "ecpay")
@Getter
@Setter
public class EcpayConfig {

    /**
     * 商店代號
     */
    private String merchantId;

    /**
     * HashKey（用於加密）
     */
    private String hashKey;

    /**
     * HashIV（用於加密）
     */
    private String hashIv;

    /**
     * API URL
     */
    private String apiUrl = "https://payment.ecpay.com.tw/Cashier/AioCheckOut/V5";

    /**
     * 查詢訂單 API URL
     */
    private String queryApiUrl = "https://payment.ecpay.com.tw/Cashier/QueryTradeInfo/V5";

    /**
     * 是否為測試環境
     */
    private boolean sandbox = false;

    /**
     * 測試環境 API URL
     */
    private String sandboxApiUrl = "https://payment-stage.ecpay.com.tw/Cashier/AioCheckOut/V5";

    /**
     * 測試環境查詢 API URL
     */
    private String sandboxQueryApiUrl = "https://payment-stage.ecpay.com.tw/Cashier/QueryTradeInfo/V5";

    /**
     * 取得實際使用的 API URL
     */
    public String getEffectiveApiUrl() {
        return sandbox ? sandboxApiUrl : apiUrl;
    }

    /**
     * 取得實際使用的查詢 API URL
     */
    public String getEffectiveQueryApiUrl() {
        return sandbox ? sandboxQueryApiUrl : queryApiUrl;
    }
}

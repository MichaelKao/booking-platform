package com.booking.platform.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * SMS 設定
 *
 * <p>支援多家 SMS 供應商設定
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "sms")
@Getter
@Setter
public class SmsConfig {

    /**
     * SMS 供應商（mitake, twsms, every8d）
     */
    private String provider = "mitake";

    /**
     * 是否啟用 SMS 功能
     */
    private boolean enabled = false;

    /**
     * 三竹簡訊設定
     */
    private MitakeConfig mitake = new MitakeConfig();

    /**
     * 台灣簡訊設定
     */
    private TwsmsConfig twsms = new TwsmsConfig();

    /**
     * 每一個 8D 設定
     */
    private Every8dConfig every8d = new Every8dConfig();

    /**
     * 三竹簡訊設定
     */
    @Getter
    @Setter
    public static class MitakeConfig {
        /**
         * API URL
         */
        private String apiUrl = "https://smsapi.mitake.com.tw/api/mtk/SmSend";

        /**
         * 使用者名稱
         */
        private String username;

        /**
         * 密碼
         */
        private String password;
    }

    /**
     * 台灣簡訊設定
     */
    @Getter
    @Setter
    public static class TwsmsConfig {
        /**
         * API URL
         */
        private String apiUrl = "https://api.twsms.com/json/sms_send.php";

        /**
         * 使用者名稱
         */
        private String username;

        /**
         * 密碼
         */
        private String password;
    }

    /**
     * 每一個 8D 設定
     */
    @Getter
    @Setter
    public static class Every8dConfig {
        /**
         * API URL
         */
        private String apiUrl = "https://oms.every8d.com/API21/HTTP/sendSMS.ashx";

        /**
         * 使用者 ID
         */
        private String userId;

        /**
         * 密碼
         */
        private String password;
    }
}

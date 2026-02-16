package com.booking.platform.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 追蹤分析設定
 *
 * <p>讀取 GA4 和 Facebook Pixel 的追蹤 ID
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "app.tracking")
@Getter
@Setter
public class TrackingConfig {

    /**
     * Google Analytics 4 Measurement ID
     */
    private String gaMeasurementId;

    /**
     * Facebook Pixel ID
     */
    private String fbPixelId;
}

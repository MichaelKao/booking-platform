package com.booking.platform.common.config;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Thymeleaf 全域變數注入
 *
 * <p>將追蹤 ID 等全域變數注入到所有頁面
 *
 * @author Developer
 * @since 1.0.0
 */
@ControllerAdvice
@RequiredArgsConstructor
public class ThymeleafGlobalConfig {

    private final TrackingConfig trackingConfig;

    /**
     * 注入 GA4 Measurement ID
     */
    @ModelAttribute("gaMeasurementId")
    public String gaMeasurementId() {
        return trackingConfig.getGaMeasurementId();
    }

    /**
     * 注入 Facebook Pixel ID
     */
    @ModelAttribute("fbPixelId")
    public String fbPixelId() {
        return trackingConfig.getFbPixelId();
    }
}

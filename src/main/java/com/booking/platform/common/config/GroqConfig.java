package com.booking.platform.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Groq AI 設定
 *
 * <p>設定 Groq API 連線參數
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "groq")
@Data
public class GroqConfig {

    /**
     * API Key
     */
    private String apiKey;

    /**
     * 模型名稱
     */
    private String model = "llama-3.3-70b-versatile";

    /**
     * API 端點
     */
    private String apiUrl = "https://api.groq.com/openai/v1";

    /**
     * 最大 Token 數
     */
    private int maxTokens = 500;

    /**
     * 溫度（創意度）
     */
    private double temperature = 0.7;

    /**
     * 是否啟用 AI 助手（預設關閉，需設定 GROQ_ENABLED=true 才會啟用）
     */
    private boolean enabled = false;
}

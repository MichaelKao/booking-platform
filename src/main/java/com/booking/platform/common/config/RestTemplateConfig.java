package com.booking.platform.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置
 *
 * <p>配置 HTTP 客戶端用於呼叫外部 API
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 連線逾時時間（毫秒）
     */
    private static final int CONNECT_TIMEOUT = 5000;

    /**
     * 讀取逾時時間（毫秒）
     */
    private static final int READ_TIMEOUT = 30000;

    /**
     * 建立 RestTemplate Bean
     *
     * @return RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);

        return new RestTemplate(factory);
    }
}

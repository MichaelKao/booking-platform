package com.booking.platform.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Jackson 配置
 *
 * <p>配置 JSON 序列化/反序列化規則
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /**
     * 配置 ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();

        // 註冊 Java 8 日期時間模組
        objectMapper.registerModule(new JavaTimeModule());

        // 日期時間不序列化為時間戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 空物件不報錯
        objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        return objectMapper;
    }
}

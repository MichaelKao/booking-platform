package com.booking.platform.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

/**
 * Redis 配置
 *
 * <p>配置 Redis 序列化和連線
 * <p>支援 REDIS_URL 環境變數（Railway 格式）
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class RedisConfig {

    /**
     * 配置 Redis 連線工廠
     *
     * <p>優先從環境變數 REDIS_URL 讀取（Railway 格式）
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();

        // 直接從環境變數讀取 REDIS_URL
        String redisUrl = System.getenv("REDIS_URL");
        log.info("========================================");
        log.info("Redis 配置初始化");
        log.info("REDIS_URL 環境變數：{}", redisUrl != null ? redisUrl.replaceAll(":[^:@]+@", ":****@") : "null");
        log.info("========================================");

        if (redisUrl != null && !redisUrl.isEmpty()) {
            try {
                URI uri = new URI(redisUrl);

                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 6379;

                config.setHostName(host);
                config.setPort(port);

                // 解析密碼（格式：redis://username:password@host:port）
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String password = userInfo.split(":", 2)[1];
                    config.setPassword(password);
                    log.info("Redis 密碼已設定");
                }

                log.info("Redis 連線配置：host={}, port={}", host, port);
            } catch (Exception e) {
                log.error("解析 REDIS_URL 失敗：{}", e.getMessage());
                log.info("使用預設配置：localhost:6379");
                config.setHostName("localhost");
                config.setPort(6379);
            }
        } else {
            log.warn("REDIS_URL 環境變數未設定，使用預設配置：localhost:6379");
            config.setHostName("localhost");
            config.setPort(6379);
        }

        return new LettuceConnectionFactory(config);
    }

    /**
     * 配置 RedisTemplate
     *
     * <p>使用 JSON 序列化，支援 Java 8 時間類型
     *
     * @param connectionFactory Redis 連線工廠
     * @return RedisTemplate
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ========================================
        // 配置 ObjectMapper
        // ========================================

        ObjectMapper objectMapper = new ObjectMapper();

        // 支援 Java 8 時間類型
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 啟用類型資訊（反序列化時需要）
        objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

        // ========================================
        // 配置序列化器
        // ========================================

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        // Key 使用字串序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);

        // Value 使用 JSON 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}

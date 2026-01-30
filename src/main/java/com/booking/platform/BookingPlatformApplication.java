package com.booking.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 預約平台應用程式入口
 *
 * <p>多租戶 SaaS 預約平台系統
 *
 * @author Developer
 * @since 1.0.0
 */
@SpringBootApplication(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@EnableJpaAuditing
@EnableAsync
@EnableScheduling
public class BookingPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookingPlatformApplication.class, args);
    }
}

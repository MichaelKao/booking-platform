package com.booking.platform.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 非同步執行配置
 *
 * <p>配置非同步任務執行緒池
 *
 * @author Developer
 * @since 1.0.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 通知執行緒池
     *
     * <p>用於發送 LINE 訊息等非同步通知
     */
    @Bean("notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 核心執行緒數
        executor.setCorePoolSize(5);

        // 最大執行緒數
        executor.setMaxPoolSize(20);

        // 佇列容量
        executor.setQueueCapacity(500);

        // 執行緒名稱前綴
        executor.setThreadNamePrefix("notification-");

        // 等待所有任務完成後關閉
        executor.setWaitForTasksToCompleteOnShutdown(true);

        // 最大等待時間（秒）
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }

    /**
     * 通用執行緒池
     *
     * <p>用於其他非同步任務
     */
    @Bean("taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(1000);
        executor.setThreadNamePrefix("task-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}

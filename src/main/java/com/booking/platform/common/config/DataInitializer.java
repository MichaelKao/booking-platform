package com.booking.platform.common.config;

import com.booking.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 資料初始化器
 *
 * <p>在應用程式啟動時執行初始化操作
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    // ========================================
    // 依賴注入
    // ========================================

    private final AuthService authService;

    // ========================================
    // 初始化
    // ========================================

    @Override
    public void run(String... args) {
        log.info("========================================");
        log.info("開始資料初始化...");
        log.info("========================================");

        // 初始化預設超級管理員帳號
        initAdminUser();

        log.info("========================================");
        log.info("資料初始化完成");
        log.info("========================================");
    }

    /**
     * 初始化預設超級管理員帳號
     */
    private void initAdminUser() {
        try {
            authService.initDefaultAdminUser();
        } catch (Exception e) {
            log.error("初始化超級管理員帳號失敗：{}", e.getMessage());
        }
    }
}

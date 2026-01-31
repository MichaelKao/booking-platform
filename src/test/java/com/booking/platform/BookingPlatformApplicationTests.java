package com.booking.platform;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 應用程式啟動測試
 *
 * <p>驗證 Spring Boot 應用程式可以正常啟動
 *
 * @author Developer
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingPlatformApplicationTests {

    @Test
    void contextLoads() {
        // 測試 Spring Context 可以正常載入
    }
}

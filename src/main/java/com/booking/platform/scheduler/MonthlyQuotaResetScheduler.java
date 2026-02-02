package com.booking.platform.scheduler;

import com.booking.platform.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 月度額度重置排程器
 *
 * <p>每月 1 日凌晨重置所有店家的推送和 SMS 額度
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MonthlyQuotaResetScheduler {

    private final TenantRepository tenantRepository;

    @Value("${scheduler.quota-reset.enabled:true}")
    private boolean enabled;

    /**
     * 重置月度推送額度
     *
     * <p>每月 1 日凌晨 0:05 執行
     */
    @Scheduled(cron = "${scheduler.quota-reset.cron:0 5 0 1 * *}")
    @Transactional
    public void resetMonthlyQuotas() {
        if (!enabled) {
            log.debug("月度額度重置排程已停用");
            return;
        }

        log.info("開始執行月度額度重置任務");

        try {
            // 重置推送額度
            int pushResetCount = tenantRepository.resetAllMonthlyPushUsed();
            log.info("已重置 {} 個店家的月度推送額度", pushResetCount);

            // 重置 SMS 額度
            int smsResetCount = tenantRepository.resetAllMonthlySmsUsed();
            log.info("已重置 {} 個店家的月度 SMS 額度", smsResetCount);

            log.info("月度額度重置任務完成");

        } catch (Exception e) {
            log.error("月度額度重置任務執行失敗：{}", e.getMessage(), e);
        }
    }
}

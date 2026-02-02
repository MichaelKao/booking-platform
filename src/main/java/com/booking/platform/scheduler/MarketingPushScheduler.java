package com.booking.platform.scheduler;

import com.booking.platform.entity.marketing.MarketingPush;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.repository.MarketingPushRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.MarketingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 行銷推播排程器
 *
 * <p>定時檢查並執行排程推播
 *
 * <p>執行頻率：每分鐘執行一次
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MarketingPushScheduler {

    private final MarketingPushRepository marketingPushRepository;
    private final TenantRepository tenantRepository;
    private final MarketingService marketingService;

    @Value("${scheduler.marketing-push.enabled:true}")
    private boolean enabled;

    /**
     * 執行排程推播任務
     *
     * <p>每分鐘執行一次，檢查是否有需要發送的排程推播
     */
    @Scheduled(cron = "${scheduler.marketing-push.cron:0 * * * * *}")
    @Transactional
    public void executeScheduledPushes() {
        if (!enabled) {
            log.debug("行銷推播排程已停用");
            return;
        }

        log.debug("檢查排程推播任務");

        try {
            // 查詢待執行的排程推播
            List<MarketingPush> pendingPushes = marketingPushRepository
                    .findPendingScheduledPushes(LocalDateTime.now());

            if (pendingPushes.isEmpty()) {
                return;
            }

            log.info("發現 {} 個待執行的排程推播", pendingPushes.size());

            for (MarketingPush push : pendingPushes) {
                try {
                    // 取得店家資訊
                    Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(push.getTenantId())
                            .orElse(null);

                    if (tenant == null) {
                        log.warn("推播所屬店家不存在，ID：{}，店家 ID：{}", push.getId(), push.getTenantId());
                        push.markFailed("店家不存在");
                        marketingPushRepository.save(push);
                        continue;
                    }

                    // 檢查額度
                    int estimatedCount = push.getEstimatedCount() != null ? push.getEstimatedCount() : 0;
                    if (!tenant.hasPushQuota(estimatedCount)) {
                        log.warn("推播額度不足，ID：{}，店家：{}", push.getId(), tenant.getName());
                        push.markFailed("推播額度不足");
                        marketingPushRepository.save(push);
                        continue;
                    }

                    // 開始發送
                    push.startSending();
                    marketingPushRepository.save(push);

                    // 非同步發送
                    marketingService.executePushAsync(push, tenant);

                    log.info("已啟動排程推播，ID：{}，標題：{}", push.getId(), push.getTitle());

                } catch (Exception e) {
                    log.error("執行排程推播失敗，ID：{}，錯誤：{}", push.getId(), e.getMessage(), e);
                    push.markFailed(e.getMessage());
                    marketingPushRepository.save(push);
                }
            }

        } catch (Exception e) {
            log.error("排程推播任務執行失敗：{}", e.getMessage(), e);
        }
    }
}

package com.booking.platform.scheduler;

import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.system.TenantFeature;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.TenantFeatureRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.line.LineMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 顧客喚回通知排程器
 *
 * <p>每天檢查久未到訪的顧客並發送喚回訊息
 *
 * <p>需要店家訂閱 AUTO_RECALL 功能
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomerRecallScheduler {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final TenantFeatureRepository tenantFeatureRepository;
    private final LineMessageService lineMessageService;

    @Value("${scheduler.customer-recall.enabled:true}")
    private boolean enabled;

    @Value("${scheduler.customer-recall.days-threshold:30}")
    private int daysThreshold;

    /**
     * 顧客喚回任務
     *
     * <p>每天下午 2:00 執行，發送喚回訊息給久未到訪的顧客
     */
    @Scheduled(cron = "${scheduler.customer-recall.cron:0 0 14 * * *}")
    @Transactional
    public void sendRecallNotifications() {
        if (!enabled) {
            log.debug("顧客喚回排程已停用");
            return;
        }

        log.info("開始執行顧客喚回任務，閾值：{} 天", daysThreshold);

        try {
            LocalDateTime thresholdDate = LocalDateTime.now().minusDays(daysThreshold);

            // 取得所有啟用 AUTO_RECALL 功能的店家
            List<TenantFeature> enabledFeatures = tenantFeatureRepository
                    .findByFeatureCodeAndDeletedAtIsNull(FeatureCode.AUTO_RECALL)
                    .stream()
                    .filter(TenantFeature::isEffective)
                    .toList();

            log.debug("共 {} 個店家啟用顧客喚回功能", enabledFeatures.size());

            int totalSent = 0;
            int totalFailed = 0;

            for (TenantFeature feature : enabledFeatures) {
                String tenantId = feature.getTenantId();

                try {
                    // 取得店家資訊
                    Tenant tenant = tenantRepository.findByIdAndDeletedAtIsNull(tenantId).orElse(null);
                    if (tenant == null) {
                        continue;
                    }

                    // 查詢久未到訪的顧客
                    List<Customer> inactiveCustomers = customerRepository
                            .findInactiveCustomers(tenantId, thresholdDate);

                    log.debug("店家 {} 有 {} 位顧客需要喚回", tenant.getName(), inactiveCustomers.size());

                    for (Customer customer : inactiveCustomers) {
                        // 只發送給有 LINE 帳號的顧客
                        if (customer.getLineUserId() == null || customer.getLineUserId().isEmpty()) {
                            continue;
                        }

                        // 避免重複發送（檢查上次喚回時間）
                        if (customer.getLastRecallAt() != null &&
                            customer.getLastRecallAt().isAfter(LocalDateTime.now().minusDays(7))) {
                            continue;
                        }

                        try {
                            // 發送喚回訊息
                            String message = buildRecallMessage(tenant.getName(), customer.getName());
                            lineMessageService.pushText(tenantId, customer.getLineUserId(), message);

                            // 更新喚回時間
                            customer.setLastRecallAt(LocalDateTime.now());
                            customerRepository.save(customer);

                            totalSent++;
                            log.debug("已發送喚回訊息給顧客：{}", customer.getName());
                        } catch (Exception e) {
                            log.warn("發送喚回訊息失敗，顧客 ID：{}，錯誤：{}", customer.getId(), e.getMessage());
                            totalFailed++;
                        }
                    }
                } catch (Exception e) {
                    log.error("處理店家 {} 的顧客喚回失敗：{}", tenantId, e.getMessage(), e);
                }
            }

            log.info("顧客喚回任務完成，成功：{}，失敗：{}", totalSent, totalFailed);

        } catch (Exception e) {
            log.error("顧客喚回任務執行失敗：{}", e.getMessage(), e);
        }
    }

    /**
     * 建構喚回訊息
     */
    private String buildRecallMessage(String storeName, String customerName) {
        return String.format(
                "💝 %s 想念您了！\n\n" +
                "親愛的 %s：\n\n" +
                "好久不見！我們很想念您的光臨～\n\n" +
                "最近有許多新服務和優惠活動，\n" +
                "誠摯邀請您再次蒞臨體驗！\n\n" +
                "現在預約還有專屬優惠喔！\n" +
                "期待很快見到您 😊",
                storeName,
                customerName != null ? customerName : "貴賓"
        );
    }
}

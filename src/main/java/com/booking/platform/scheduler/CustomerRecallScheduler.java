package com.booking.platform.scheduler;

import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.marketing.Campaign;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.CampaignType;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.repository.CampaignRepository;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.CampaignPushService;
import com.booking.platform.service.FeatureService;
import com.booking.platform.service.line.LineNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 顧客喚回排程器
 *
 * <p>每日執行，檢查久未到訪的顧客並發送喚回通知
 *
 * <p>執行時間：每天下午 2:00
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
    private final LineNotificationService lineNotificationService;
    private final FeatureService featureService;
    private final CampaignRepository campaignRepository;
    private final CampaignPushService campaignPushService;

    @Value("${scheduler.customer-recall.enabled:true}")
    private boolean enabled;

    /**
     * 每日最大喚回通知數量（避免一次發送過多）
     */
    @Value("${scheduler.customer-recall.max-per-tenant:50}")
    private int maxPerTenant;

    /**
     * 顧客喚回任務
     *
     * <p>每天下午 2:00 執行，檢查各店家久未到訪的顧客並發送喚回通知
     */
    @Scheduled(cron = "${scheduler.customer-recall.cron:0 0 14 * * *}")
    @Transactional
    public void sendRecallNotifications() {
        if (!enabled) {
            log.debug("顧客喚回排程已停用");
            return;
        }

        log.info("開始執行顧客喚回任務");

        try {
            // 取得所有啟用顧客喚回的店家（只查未刪除的）
            List<Tenant> tenants = tenantRepository.findAllByDeletedAtIsNull().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getEnableCustomerRecall()))
                    .filter(t -> featureService.isFeatureEnabled(t.getId(), FeatureCode.AUTO_RECALL))
                    .toList();

            log.debug("共 {} 個店家啟用顧客喚回", tenants.size());

            int totalSent = 0;
            int totalFailed = 0;

            for (Tenant tenant : tenants) {
                try {
                    // 計算閾值日期
                    int recallDays = tenant.getCustomerRecallDays() != null ? tenant.getCustomerRecallDays() : 30;
                    LocalDateTime thresholdDate = LocalDateTime.now().minusDays(recallDays);

                    // 查詢久未到訪的顧客
                    List<Customer> inactiveCustomers = customerRepository.findInactiveCustomers(
                            tenant.getId(), thresholdDate
                    );

                    // 限制每次發送數量
                    List<Customer> customersToNotify = inactiveCustomers.stream()
                            .limit(maxPerTenant)
                            .toList();

                    log.debug("店家 {} 有 {} 位顧客需要喚回（本次發送 {} 位）",
                            tenant.getName(), inactiveCustomers.size(), customersToNotify.size());

                    // 查詢 ACTIVE 的 RECALL 活動（isAutoTrigger=true）
                    List<Campaign> recallCampaigns = campaignRepository
                            .findActiveByTenantIdAndType(tenant.getId(), CampaignType.RECALL, LocalDateTime.now())
                            .stream()
                            .filter(c -> Boolean.TRUE.equals(c.getIsAutoTrigger()))
                            .toList();

                    for (Customer customer : customersToNotify) {
                        try {
                            // 發送喚回通知
                            lineNotificationService.sendRecallNotification(
                                    tenant.getId(),
                                    customer,
                                    tenant.getCustomerRecallMessage()
                            );

                            // 觸發活動獎勵（發票券、送點數）
                            for (Campaign campaign : recallCampaigns) {
                                try {
                                    campaignPushService.triggerForCustomer(tenant.getId(), campaign, customer);
                                } catch (Exception ce) {
                                    log.warn("喚回活動獎勵觸發失敗，活動：{}，顧客：{}，錯誤：{}",
                                            campaign.getName(), customer.getId(), ce.getMessage());
                                }
                            }

                            // 更新最後喚回時間
                            customer.setLastRecallAt(LocalDateTime.now());
                            customerRepository.save(customer);

                            totalSent++;
                        } catch (Exception e) {
                            log.error("發送喚回通知失敗，顧客 ID：{}，錯誤：{}", customer.getId(), e.getMessage());
                            totalFailed++;
                        }
                    }
                } catch (Exception e) {
                    log.error("處理店家 {} 的顧客喚回失敗：{}", tenant.getName(), e.getMessage(), e);
                }
            }

            log.info("顧客喚回任務完成，成功：{}，失敗：{}", totalSent, totalFailed);

        } catch (Exception e) {
            log.error("顧客喚回任務執行失敗：{}", e.getMessage(), e);
        }
    }
}

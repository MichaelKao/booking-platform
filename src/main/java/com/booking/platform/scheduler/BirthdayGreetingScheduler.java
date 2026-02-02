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
import java.util.List;

/**
 * 生日祝福排程器
 *
 * <p>每天早上 9 點檢查今日壽星並發送祝福訊息
 *
 * <p>需要店家訂閱 AUTO_BIRTHDAY 功能
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BirthdayGreetingScheduler {

    private final CustomerRepository customerRepository;
    private final TenantRepository tenantRepository;
    private final TenantFeatureRepository tenantFeatureRepository;
    private final LineMessageService lineMessageService;

    @Value("${scheduler.birthday-greeting.enabled:true}")
    private boolean enabled;

    /**
     * 生日祝福任務
     *
     * <p>每天早上 9:00 執行，發送生日祝福給今日壽星
     */
    @Scheduled(cron = "${scheduler.birthday-greeting.cron:0 0 9 * * *}")
    @Transactional(readOnly = true)
    public void sendBirthdayGreetings() {
        if (!enabled) {
            log.debug("生日祝福排程已停用");
            return;
        }

        log.info("開始執行生日祝福任務");

        try {
            LocalDate today = LocalDate.now();
            int month = today.getMonthValue();
            int day = today.getDayOfMonth();

            // 取得所有啟用 AUTO_BIRTHDAY 功能的店家
            List<TenantFeature> enabledFeatures = tenantFeatureRepository
                    .findByFeatureCodeAndDeletedAtIsNull(FeatureCode.AUTO_BIRTHDAY)
                    .stream()
                    .filter(TenantFeature::isEffective)
                    .toList();

            log.debug("共 {} 個店家啟用生日祝福功能", enabledFeatures.size());

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

                    // 查詢今日壽星
                    List<Customer> birthdayCustomers = customerRepository
                            .findBirthdayCustomers(tenantId, month, day);

                    log.debug("店家 {} 今日有 {} 位壽星", tenant.getName(), birthdayCustomers.size());

                    for (Customer customer : birthdayCustomers) {
                        // 只發送給有 LINE 帳號的顧客
                        if (customer.getLineUserId() == null || customer.getLineUserId().isEmpty()) {
                            continue;
                        }

                        try {
                            // 發送生日祝福訊息
                            String message = buildBirthdayMessage(tenant.getName(), customer.getName());
                            lineMessageService.pushText(tenantId, customer.getLineUserId(), message);

                            totalSent++;
                            log.debug("已發送生日祝福給顧客：{}", customer.getName());
                        } catch (Exception e) {
                            log.warn("發送生日祝福失敗，顧客 ID：{}，錯誤：{}", customer.getId(), e.getMessage());
                            totalFailed++;
                        }
                    }
                } catch (Exception e) {
                    log.error("處理店家 {} 的生日祝福失敗：{}", tenantId, e.getMessage(), e);
                }
            }

            log.info("生日祝福任務完成，成功：{}，失敗：{}", totalSent, totalFailed);

        } catch (Exception e) {
            log.error("生日祝福任務執行失敗：{}", e.getMessage(), e);
        }
    }

    /**
     * 建構生日祝福訊息
     */
    private String buildBirthdayMessage(String storeName, String customerName) {
        return String.format(
                "🎂 %s 祝您生日快樂！🎉\n\n" +
                "親愛的 %s：\n\n" +
                "在這個特別的日子，我們獻上最誠摯的祝福！\n" +
                "願您新的一歲充滿歡樂與美好！\n\n" +
                "感謝您一直以來的支持與信任，\n" +
                "期待在您的特別日子為您服務！\n\n" +
                "祝福您 生日快樂！🎁",
                storeName,
                customerName != null ? customerName : "貴賓"
        );
    }
}

package com.booking.platform.scheduler;

import com.booking.platform.entity.customer.Customer;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.enums.FeatureCode;
import com.booking.platform.repository.CustomerRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.FeatureService;
import com.booking.platform.service.line.LineNotificationService;
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
 * <p>每日執行，檢查當天生日的顧客並發送祝福訊息
 *
 * <p>執行時間：每天早上 9:00
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
    private final LineNotificationService lineNotificationService;
    private final FeatureService featureService;

    @Value("${scheduler.birthday-greeting.enabled:true}")
    private boolean enabled;

    /**
     * 生日祝福任務
     *
     * <p>每天早上 9:00 執行，檢查各店家當天生日的顧客並發送祝福
     */
    @Scheduled(cron = "${scheduler.birthday-greeting.cron:0 0 9 * * *}")
    @Transactional
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

            // 取得所有啟用生日祝福的店家
            List<Tenant> tenants = tenantRepository.findAll().stream()
                    .filter(t -> t.getDeletedAt() == null)
                    .filter(t -> Boolean.TRUE.equals(t.getEnableBirthdayGreeting()))
                    .filter(t -> featureService.isFeatureEnabled(t.getId(), FeatureCode.AUTO_BIRTHDAY))
                    .toList();

            log.debug("共 {} 個店家啟用生日祝福", tenants.size());

            int totalSent = 0;
            int totalFailed = 0;

            for (Tenant tenant : tenants) {
                try {
                    // 查詢今天生日的顧客
                    List<Customer> birthdayCustomers = customerRepository.findBirthdayCustomers(
                            tenant.getId(), month, day
                    );

                    log.debug("店家 {} 有 {} 位顧客今天生日", tenant.getName(), birthdayCustomers.size());

                    for (Customer customer : birthdayCustomers) {
                        try {
                            // 發送生日祝福
                            lineNotificationService.sendBirthdayGreeting(
                                    tenant.getId(),
                                    customer,
                                    tenant.getBirthdayGreetingMessage()
                            );
                            totalSent++;
                        } catch (Exception e) {
                            log.error("發送生日祝福失敗，顧客 ID：{}，錯誤：{}", customer.getId(), e.getMessage());
                            totalFailed++;
                        }
                    }
                } catch (Exception e) {
                    log.error("處理店家 {} 的生日祝福失敗：{}", tenant.getName(), e.getMessage(), e);
                }
            }

            log.info("生日祝福任務完成，成功：{}，失敗：{}", totalSent, totalFailed);

        } catch (Exception e) {
            log.error("生日祝福任務執行失敗：{}", e.getMessage(), e);
        }
    }
}

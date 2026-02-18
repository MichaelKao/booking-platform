package com.booking.platform.scheduler;

import com.booking.platform.entity.booking.Booking;
import com.booking.platform.entity.tenant.Tenant;
import com.booking.platform.repository.BookingRepository;
import com.booking.platform.repository.TenantRepository;
import com.booking.platform.service.line.LineNotificationService;
import com.booking.platform.service.notification.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 預約提醒排程器
 *
 * <p>定時檢查即將到來的預約並發送 LINE 提醒
 *
 * <p>執行頻率：每小時執行一次
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class BookingReminderScheduler {

    private final BookingRepository bookingRepository;
    private final TenantRepository tenantRepository;
    private final LineNotificationService lineNotificationService;
    private final SmsService smsService;

    @Value("${scheduler.booking-reminder.enabled:true}")
    private boolean enabled;

    /**
     * 預約提醒任務
     *
     * <p>每小時執行一次，檢查各店家設定的提醒時間並發送通知
     */
    @Scheduled(cron = "${scheduler.booking-reminder.cron:0 0 * * * *}")
    @Transactional
    public void sendBookingReminders() {
        if (!enabled) {
            log.debug("預約提醒排程已停用");
            return;
        }

        log.info("開始執行預約提醒任務");

        try {
            // 取得所有啟用預約提醒的店家（只查未刪除的）
            List<Tenant> tenants = tenantRepository.findAllByDeletedAtIsNull().stream()
                    .filter(t -> Boolean.TRUE.equals(t.getEnableBookingReminder()))
                    .toList();

            log.debug("共 {} 個店家啟用預約提醒", tenants.size());

            // 依店家的提醒時間設定分組
            Map<Integer, List<Tenant>> tenantsByReminderHours = new HashMap<>();
            for (Tenant tenant : tenants) {
                int hours = tenant.getReminderHoursBefore() != null ? tenant.getReminderHoursBefore() : 24;
                tenantsByReminderHours.computeIfAbsent(hours, k -> new java.util.ArrayList<>()).add(tenant);
            }

            int totalSent = 0;
            int totalFailed = 0;

            // 處理每個提醒時間設定
            for (Map.Entry<Integer, List<Tenant>> entry : tenantsByReminderHours.entrySet()) {
                int reminderHours = entry.getKey();
                List<Tenant> tenantsForHours = entry.getValue();

                // 計算目標時間範圍（當前時間 + 提醒小時數）
                // 使用 LocalDateTime 避免 LocalTime.plusHours() 跨日溢位問題
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime targetStart = now.plusHours(reminderHours);
                LocalDateTime targetEnd = targetStart.plusHours(1);
                LocalDate targetDate = targetStart.toLocalDate();
                LocalTime targetStartTime = targetStart.toLocalTime();
                LocalTime targetEndTime = targetEnd.toLocalTime();

                for (Tenant tenant : tenantsForHours) {
                    try {
                        // 查詢需要發送提醒的預約
                        List<Booking> bookings = bookingRepository.findUpcomingBookingsForReminderByTenant(
                                tenant.getId(),
                                targetDate,
                                targetStartTime,
                                targetEndTime
                        );

                        log.debug("店家 {} 有 {} 筆預約需要提醒", tenant.getName(), bookings.size());

                        for (Booking booking : bookings) {
                            try {
                                // 發送 LINE 提醒
                                lineNotificationService.sendBookingReminder(booking);

                                // 若啟用 SMS，同時發送 SMS 提醒
                                if (Boolean.TRUE.equals(tenant.getSmsEnabled())) {
                                    try {
                                        smsService.sendBookingReminder(booking);
                                    } catch (Exception smsEx) {
                                        log.warn("發送 SMS 提醒失敗，預約 ID：{}，錯誤：{}", booking.getId(), smsEx.getMessage());
                                    }
                                }

                                // 標記已發送
                                booking.markReminderSent();
                                bookingRepository.save(booking);

                                totalSent++;
                            } catch (Exception e) {
                                log.error("發送預約提醒失敗，預約 ID：{}，錯誤：{}", booking.getId(), e.getMessage());
                                totalFailed++;
                            }
                        }
                    } catch (Exception e) {
                        log.error("處理店家 {} 的預約提醒失敗：{}", tenant.getName(), e.getMessage(), e);
                    }
                }
            }

            log.info("預約提醒任務完成，成功：{}，失敗：{}", totalSent, totalFailed);

        } catch (Exception e) {
            log.error("預約提醒任務執行失敗：{}", e.getMessage(), e);
        }
    }
}

# Scheduler 規範

## 標註

```java
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "scheduler.xxx.enabled", havingValue = "true")
```

---

## Scheduler 列表 (5 個)

| Scheduler | Cron | 說明 |
|-----------|------|------|
| BookingReminderScheduler | `0 0 * * * *` | 每小時檢查並發送預約提醒 (LINE/SMS) |
| MonthlyQuotaResetScheduler | `0 5 0 1 * *` | 每月1日重置推送/SMS 額度 |
| MarketingPushScheduler | `0 * * * * *` | 每分鐘檢查排程推播任務 |
| BirthdayGreetingScheduler | `0 0 9 * * *` | 每天 9:00 發送生日祝福 (AUTO_BIRTHDAY) |
| CustomerRecallScheduler | `0 0 14 * * *` | 每天 14:00 發送顧客喚回通知 (AUTO_RECALL) |

---

## 設定

```yaml
scheduler:
  booking-reminder:
    enabled: true
    cron: "0 0 * * * *"
  quota-reset:
    enabled: true
    cron: "0 5 0 1 * *"
  marketing-push:
    enabled: true
    cron: "0 * * * * *"
```

---

## BookingReminderScheduler

預約提醒排程器，根據店家設定的 `reminderHoursBefore` 發送提醒。

```java
@Scheduled(cron = "${scheduler.booking-reminder.cron:0 0 * * * *}")
public void sendBookingReminders() {
    // 1. 查詢所有啟用提醒的店家
    // 2. 查詢各店家未來 N 小時內的預約
    // 3. 發送 LINE/SMS 提醒
}
```

**相關欄位**：
- `Tenant.reminderHoursBefore` - 提前幾小時發送提醒 (預設 24)
- `Tenant.enableBookingReminder` - 是否啟用提醒 (預設 true)

---

## MonthlyQuotaResetScheduler

每月額度重置排程器。

```java
@Scheduled(cron = "${scheduler.quota-reset.cron:0 5 0 1 * *}")
public void resetMonthlyQuotas() {
    // 1. 重置所有店家的月度推送額度
    // 2. 重置所有店家的月度 SMS 額度
}
```

**相關欄位**：
- `Tenant.monthlyPushQuota` - 每月推送額度
- `Tenant.monthlyPushUsed` - 已使用推送數
- `Tenant.monthlySmsQuota` - 每月 SMS 額度
- `Tenant.monthlySmsUsed` - 已使用 SMS 數

---

## MarketingPushScheduler

行銷推播排程器，處理排程推播任務。

```java
@Scheduled(cron = "${scheduler.marketing-push.cron:0 * * * * *}")
public void processScheduledPushes() {
    // 1. 查詢狀態為 SCHEDULED 且 scheduledAt <= now 的推播
    // 2. 依序發送推播
    // 3. 更新狀態為 SENT
}
```

**相關欄位**：
- `MarketingPush.status` - 推播狀態 (DRAFT/SCHEDULED/SENT/CANCELLED)
- `MarketingPush.scheduledAt` - 排程時間
- `MarketingPush.sentAt` - 實際發送時間

---

## 開發測試

開發環境可設定較短週期測試：

```yaml
scheduler:
  booking-reminder:
    enabled: true
    cron: "0 */5 * * * *"  # 每 5 分鐘
```

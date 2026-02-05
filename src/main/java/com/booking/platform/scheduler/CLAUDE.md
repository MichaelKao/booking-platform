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

| Scheduler | Cron | 說明 | 需訂閱功能 |
|-----------|------|------|-----------|
| BookingReminderScheduler | `0 0 * * * *` | 每小時檢查並發送預約提醒 (LINE/SMS) | AUTO_REMINDER |
| MonthlyQuotaResetScheduler | `0 5 0 1 * *` | 每月1日重置推送/SMS 額度 | - |
| MarketingPushScheduler | `0 * * * * *` | 每分鐘檢查排程推播任務 | - |
| BirthdayGreetingScheduler | `0 0 9 * * *` | 每日 9:00 發送生日祝福 | AUTO_BIRTHDAY |
| CustomerRecallScheduler | `0 0 14 * * *` | 每日 14:00 發送顧客喚回通知 | AUTO_RECALL |

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
  birthday-greeting:
    enabled: true
    cron: "0 0 9 * * *"
  customer-recall:
    enabled: true
    cron: "0 0 14 * * *"
    max-per-tenant: 50
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

## BirthdayGreetingScheduler

生日祝福排程器，每日發送當天生日顧客的祝福訊息。

```java
@Scheduled(cron = "${scheduler.birthday-greeting.cron:0 0 9 * * *}")
@Transactional
public void sendBirthdayGreetings() {
    // 1. 查詢啟用生日祝福且訂閱 AUTO_BIRTHDAY 功能的店家
    // 2. 查詢各店家今天生日的顧客
    // 3. 發送 LINE 生日祝福訊息
}
```

**相關欄位**：
- `Tenant.enableBirthdayGreeting` - 是否啟用生日祝福
- `Tenant.birthdayGreetingMessage` - 自訂祝福訊息
- `Customer.birthDate` - 顧客生日

---

## CustomerRecallScheduler

顧客喚回排程器，每日發送久未到訪顧客的喚回通知。

```java
@Scheduled(cron = "${scheduler.customer-recall.cron:0 0 14 * * *}")
@Transactional
public void sendRecallNotifications() {
    // 1. 查詢啟用顧客喚回且訂閱 AUTO_RECALL 功能的店家
    // 2. 查詢各店家超過 N 天未到訪的顧客
    // 3. 發送 LINE 喚回通知（每次最多 50 位）
    // 4. 更新顧客的 lastRecallAt 時間
}
```

**相關欄位**：
- `Tenant.enableCustomerRecall` - 是否啟用顧客喚回
- `Tenant.customerRecallDays` - 未到訪天數閾值（預設 30 天）
- `Tenant.customerRecallMessage` - 自訂喚回訊息
- `Customer.lastRecallAt` - 上次喚回時間

**設定參數**：
- `scheduler.customer-recall.max-per-tenant` - 每次每店家最多發送數量（預設 50）

---

## 開發測試

開發環境可設定較短週期測試：

```yaml
scheduler:
  booking-reminder:
    enabled: true
    cron: "0 */5 * * * *"  # 每 5 分鐘
  birthday-greeting:
    enabled: true
    cron: "0 */10 * * * *"  # 每 10 分鐘
  customer-recall:
    enabled: true
    cron: "0 */15 * * * *"  # 每 15 分鐘
    max-per-tenant: 5
```

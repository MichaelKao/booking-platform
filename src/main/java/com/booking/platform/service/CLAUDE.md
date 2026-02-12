# Service 規範

## 標註

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
```

## 寫入方法

```java
@Transactional  // 單獨標註
public XxxResponse create(...) { }
```

---

## Service 列表 (38 個)

### 核心服務

| Service | 說明 |
|---------|------|
| BookingService | 預約業務 (建立、確認、取消、完成、編輯、自動分配員工) |
| CustomerService | 顧客管理 (建立、更新、點數、狀態) |
| StaffService | 員工管理 (建立、更新、排班、請假) |
| ServiceItemService | 服務項目管理 |

### 行銷與商品

| Service | 說明 |
|---------|------|
| CouponService | 票券定義與發放 |
| CampaignService | 行銷活動管理 |
| ProductService | 商品管理 (庫存、狀態) |
| ProductOrderService | 商品訂單管理 |
| InventoryService | 庫存異動管理 |
| MembershipLevelService | 會員等級管理 |
| MarketingService | 行銷推播管理 (建立、發送、排程) |

### 點數與儲值

| Service | 說明 |
|---------|------|
| PointService | 店家點數管理 (餘額、交易) |
| PointTopUpService | 儲值申請管理 (審核、批准、駁回) |

### 功能與訂閱

| Service | 說明 |
|---------|------|
| FeatureService | 功能定義與租戶功能訂閱 |
| FeatureStoreService | 功能商店 (店家申請功能) |

### 報表與分析

| Service | 說明 |
|---------|------|
| ReportService | 報表生成 (摘要、趨勢、排名) |
| AdminDashboardService | 超級管理儀表板 |

### 設定與配置

| Service | 說明 |
|---------|------|
| TenantService | 租戶管理 (建立、更新、狀態變更) |
| SettingsService | 店家設定管理 |
| SetupStatusService | 店家設定完成狀態 (新手引導) |
| AuthService | 認證與授權 (登入、註冊、密碼重設) |
| I18nService | 多語系服務 (繁中、簡中、英文) |

### LINE 服務

| Service | 說明 |
|---------|------|
| LineConfigService | LINE Bot 設定管理 |
| LineUserService | LINE 用戶管理 |
| LineWebhookService | LINE Webhook 事件處理 |
| LineMessageService | LINE 訊息發送 |
| LineFlexMessageBuilder | Flex Message 構建（主選單、服務選單、日期Carousel等） |
| LineConversationService | 對話狀態管理 |
| LineNotificationService | LINE 通知發送 (預約提醒) |
| LineRichMenuService | Rich Menu 管理（建立、刪除、主題配色、圖片上傳） |

### 通知服務

| Service | 說明 |
|---------|------|
| EmailService | Email 通知發送 |
| SseNotificationService | SSE 即時通知 (預約/商品訂單/票券/顧客推送) |
| SmsService | SMS 介面 |
| MitakeSmsService | 三竹簡訊實作 |

### 匯出服務

| Service | 說明 |
|---------|------|
| ExcelExportService | Excel 匯出 (預約、顧客、報表) |
| PdfExportService | PDF 匯出 (預約、報表) |

### 金流服務

| Service | 說明 |
|---------|------|
| EcpayService | 綠界金流整合 |

### AI 服務

| Service | 說明 |
|---------|------|
| AiAssistantService | AI 智慧客服 (Groq Llama 3.3) |

### 共用服務

| Service | 說明 |
|---------|------|
| EncryptionService | 加密服務 (AES-256-GCM) |

---

## 目錄結構

```
service/
├── admin/              # 超管服務
│   └── AdminDashboardService
├── ai/                 # AI 服務
│   └── AiAssistantService
├── common/             # 共用服務
│   └── EncryptionService
├── export/             # 匯出服務
│   ├── ExcelExportService
│   └── PdfExportService
├── line/               # LINE 相關
│   ├── LineConfigService
│   ├── LineUserService
│   ├── LineWebhookService
│   ├── LineMessageService
│   ├── LineFlexMessageBuilder
│   ├── LineConversationService
│   ├── LineNotificationService
│   └── LineRichMenuService
├── notification/       # 通知服務
│   ├── EmailService
│   ├── SseNotificationService
│   ├── SmsService
│   └── MitakeSmsService
├── payment/            # 金流服務
│   └── EcpayService
└── /                   # 核心服務
    ├── AuthService
    ├── BookingService
    ├── CustomerService
    ├── StaffService
    ├── ServiceItemService
    ├── ProductService
    ├── ProductOrderService
    ├── InventoryService
    ├── CouponService
    ├── CampaignService
    ├── MarketingService
    ├── MembershipLevelService
    ├── PointService
    ├── PointTopUpService
    ├── FeatureService
    ├── FeatureStoreService
    ├── ReportService
    ├── TenantService
    ├── SettingsService
    └── I18nService
```

---

## StaffService 員工管理

```java
// 排班管理
StaffScheduleResponse getSchedule(String staffId);
StaffScheduleResponse updateSchedule(String staffId, StaffScheduleRequest request);

// 請假管理
List<StaffLeaveResponse> getLeaves(String staffId, LocalDate startDate, LocalDate endDate);
List<StaffLeaveResponse> createLeaves(String staffId, CreateStaffLeaveRequest request);
void deleteLeave(String staffId, String leaveId);
void deleteLeaveByDate(String staffId, LocalDate date);
boolean isStaffOnLeave(String staffId, LocalDate date);
```

---

## 時間/日期驗證規則

所有涉及開始/結束時間的 Service 方法都必須驗證：**開始時間必須早於結束時間**。

| Service | 方法 | 驗證項目 |
|---------|------|---------|
| StaffService | updateSchedule() | startTime < endTime、breakStartTime < breakEndTime、休息時間在上班時間範圍內 |
| StaffService | createLeaves() | 半天假 startTime < endTime |
| SettingsService | updateSettings() | businessStartTime < businessEndTime、breakStartTime < breakEndTime（含單邊更新） |
| BookingService | update() | startTime < endTime（直接指定結束時間時） |
| CouponService | create()/update() | validStartAt < validEndAt |
| CampaignService | create()/update() | startAt < endAt |

---

## 方法模板

```java
@Transactional
public XxxResponse create(CreateXxxRequest request) {
    // ========================================
    // 1. 取得租戶
    // ========================================
    String tenantId = TenantContext.getTenantId();

    // ========================================
    // 2. 驗證業務規則
    // ========================================
    validateSomething(request);

    // ========================================
    // 3. 執行主邏輯
    // ========================================
    Xxx entity = mapper.toEntity(request);
    entity.setTenantId(tenantId);
    xxxRepository.save(entity);

    // ========================================
    // 4. 記錄日誌
    // ========================================
    log.info("建立 Xxx，租戶：{}，ID：{}", tenantId, entity.getId());

    return mapper.toResponse(entity);
}
```

---

## LINE 對話狀態

```java
// Redis Key: line:conversation:{tenantId}:{lineUserId}
// TTL: 30 分鐘

// 取得狀態
ConversationContext context = lineConversationService.getContext(tenantId, lineUserId);

// 開始預約
lineConversationService.startBooking(tenantId, lineUserId);

// 設定選擇的服務
lineConversationService.setSelectedService(tenantId, userId, serviceId, serviceName, duration, price);

// 重置狀態
lineConversationService.reset(tenantId, lineUserId);
```

---

## 加密服務

```java
// LINE Token 加密儲存
String encrypted = encryptionService.encrypt(channelAccessToken);
String decrypted = encryptionService.decrypt(encryptedToken);
```

---

## 快取策略

| 資料類型 | TTL | Key 格式 |
|---------|-----|----------|
| 店家設定 | 30 分鐘 | `settings:{tenantId}` |
| 服務列表 | 30 分鐘 | `services:{tenantId}` |
| 員工列表 | 30 分鐘 | `staff:{tenantId}` |
| 對話狀態 | 30 分鐘 | `line:conversation:{tenantId}:{lineUserId}` |

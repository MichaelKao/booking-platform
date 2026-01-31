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

## Service 列表 (25 個)

### 核心服務

| Service | 說明 |
|---------|------|
| BookingService | 預約業務 (建立、確認、取消、完成) |
| CustomerService | 顧客管理 (建立、更新、點數、狀態) |
| StaffService | 員工管理 (建立、更新、班表) |
| ServiceItemService | 服務項目管理 |

### 行銷與商品

| Service | 說明 |
|---------|------|
| CouponService | 票券定義與發放 |
| CouponInstanceService | 票券實例管理 (核銷、使用) |
| CampaignService | 行銷活動管理 |
| ProductService | 商品管理 (庫存、狀態) |
| MembershipLevelService | 會員等級管理 |

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
| AuthService | 認證與授權 (登入、註冊、密碼重設) |

### LINE 服務

| Service | 說明 |
|---------|------|
| LineConfigService | LINE Bot 設定管理 |
| LineUserService | LINE 用戶管理 |
| LineWebhookService | LINE Webhook 事件處理 |
| LineMessageService | LINE 訊息發送 |
| LineFlexMessageBuilder | Flex Message 構建 |
| LineConversationService | 對話狀態管理 |

### 通知與共用

| Service | 說明 |
|---------|------|
| EmailService | Email 通知發送 |
| EncryptionService | 加密服務 (AES-256-GCM) |

---

## 目錄結構

```
service/
├── admin/              # 超管服務
│   ├── AdminDashboardService
│   ├── AdminFeatureService (整合至 FeatureService)
│   └── AdminPointService (整合至 PointTopUpService)
├── line/               # LINE 相關
│   ├── LineConfigService
│   ├── LineUserService
│   ├── LineWebhookService
│   ├── LineMessageService
│   ├── LineFlexMessageBuilder
│   └── LineConversationService
├── notification/       # 通知服務
│   └── EmailService
└── /                   # 核心服務
    ├── AuthService
    ├── BookingService
    ├── CustomerService
    ├── StaffService
    ├── ServiceItemService
    ├── ProductService
    ├── CouponService
    ├── CouponInstanceService
    ├── CampaignService
    ├── MembershipLevelService
    ├── PointService
    ├── PointTopUpService
    ├── FeatureService
    ├── FeatureStoreService
    ├── ReportService
    ├── TenantService
    ├── SettingsService
    └── EncryptionService
```

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
    // 4. 記錄 AuditLog（重要操作）
    // ========================================
    log.info("建立 Xxx，租戶：{}，ID：{}", tenantId, entity.getId());

    // ========================================
    // 5. 清快取（如有）
    // ========================================
    cacheService.evict("xxx:" + tenantId);

    // ========================================
    // 6. 發通知（如需）
    // ========================================
    notificationService.sendXxxNotification(entity);

    return mapper.toResponse(entity);
}
```

---

## LINE 對話狀態

```java
// Redis Key: line:conversation:{tenantId}:{lineUserId}
// TTL: 30 分鐘

// 取得狀態
ConversationContext context = lineConversationService.getState(tenantId, lineUserId);

// 設定狀態
lineConversationService.setState(tenantId, lineUserId, ConversationState.SELECTING_SERVICE, context);

// 清除狀態
lineConversationService.clearState(tenantId, lineUserId);
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

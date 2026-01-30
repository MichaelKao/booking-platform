# Booking Platform - 多租戶預約平台系統

## 專案概述

這是一個 SaaS 多租戶預約平台，讓各種服務業（美髮、美甲、刺青、SPA、寵物美容、健身教練等）可以透過 LINE Bot 提供預約服務。

### 三個角色

1. **超級管理員（我）**
   - 管理所有店家帳號
   - 控制所有功能開關（想開就開、想關就關）
   - 審核店家儲值申請
   - 查看全平台數據

2. **店家（租戶）**
   - 管理自己的預約、顧客、員工、服務、商品
   - 使用平台提供的功能（免費功能 + 付費加值功能）
   - 透過點數使用加值功能

3. **顧客（店家的客戶）**
   - 透過 LINE Bot 預約服務
   - 查看自己的預約、票券、點數
   - 接收通知和行銷訊息

---

## 技術棧

- Java 17
- Spring Boot 3.2
- Maven
- PostgreSQL（主資料庫）
- Redis（快取、對話狀態）
- LINE Messaging API
- Spring Security + JWT
- Thymeleaf + HTML + JavaScript（前端頁面）

---

## 前端技術說明

### 頁面架構

使用 Spring Boot + Thymeleaf 做伺服器端渲染，搭配：
- HTML5
- CSS3（使用 Bootstrap 5 加速開發）
- JavaScript（原生 Fetch API 呼叫後端 REST API）

### 頁面結構
```
src/main/resources/
├── templates/                      # Thymeleaf 模板
│   ├── admin/                     # 超級管理後台頁面
│   │   ├── layout.html           # 後台共用版型
│   │   ├── dashboard.html        # 儀表板
│   │   ├── tenants.html          # 店家管理
│   │   ├── tenant-detail.html    # 店家詳情
│   │   ├── features.html         # 功能管理
│   │   ├── point-topups.html     # 儲值審核
│   │   └── login.html            # 登入頁
│   ├── tenant/                    # 店家後台頁面
│   │   ├── layout.html           # 後台共用版型
│   │   ├── dashboard.html        # 儀表板
│   │   ├── bookings.html         # 預約管理
│   │   ├── calendar.html         # 行事曆檢視
│   │   ├── customers.html        # 顧客管理
│   │   ├── staff.html            # 員工管理
│   │   ├── services.html         # 服務管理
│   │   ├── products.html         # 商品管理
│   │   ├── campaigns.html        # 行銷活動
│   │   ├── coupons.html          # 票券管理
│   │   ├── reports.html          # 報表
│   │   ├── settings.html         # 設定
│   │   ├── feature-store.html    # 功能商店
│   │   └── login.html            # 登入頁
│   ├── fragments/                 # 共用片段
│   │   ├── header.html
│   │   ├── sidebar.html
│   │   └── footer.html
│   └── error/                     # 錯誤頁面
│       ├── 404.html
│       └── 500.html
├── static/                         # 靜態資源
│   ├── css/
│   │   ├── admin.css
│   │   └── tenant.css
│   ├── js/
│   │   ├── admin.js
│   │   ├── tenant.js
│   │   └── common.js
│   └── images/
└── application.yml
```

### 前端框架

使用 Bootstrap 5 做 UI 框架：
```html



```

### JavaScript 互動

使用原生 Fetch API 呼叫後端：
```javascript
// 範例：取得預約列表
async function loadBookings() {
    const response = await fetch('/api/bookings', {
        headers: {
            'Authorization': 'Bearer ' + getToken()
        }
    });
    const result = await response.json();
    if (result.success) {
        renderBookings(result.data);
    }
}
```

---

## 專案結構（傳統分層架構）
```
com.booking.platform
├── common/                         # 共用元件
│   ├── config/                    # 設定類
│   │   ├── SecurityConfig.java
│   │   ├── WebConfig.java
│   │   ├── RedisConfig.java
│   │   └── AsyncConfig.java
│   ├── exception/                 # 例外處理
│   │   ├── GlobalExceptionHandler.java
│   │   ├── BusinessException.java
│   │   └── ErrorCode.java
│   ├── response/                  # 統一回應格式
│   │   ├── ApiResponse.java
│   │   └── PageResponse.java
│   ├── util/                      # 工具類
│   │   ├── DateUtil.java
│   │   ├── JsonUtil.java
│   │   └── EncryptionUtil.java
│   ├── tenant/                    # 多租戶核心
│   │   ├── TenantContext.java
│   │   └── TenantFilter.java
│   └── security/                  # 安全相關
│       ├── JwtTokenProvider.java
│       ├── JwtAuthenticationFilter.java
│       └── UserDetailsServiceImpl.java
│
├── controller/                     # 所有 Controller
│   ├── admin/                     # 超級管理後台 API
│   │   ├── AdminTenantController.java
│   │   ├── AdminFeatureController.java
│   │   ├── AdminPointController.java
│   │   └── AdminDashboardController.java
│   ├── tenant/                    # 店家後台 API
│   │   ├── BookingController.java
│   │   ├── CustomerController.java
│   │   ├── StaffController.java
│   │   ├── ServiceController.java
│   │   ├── ProductController.java
│   │   ├── OrderController.java
│   │   ├── CampaignController.java
│   │   ├── CouponController.java
│   │   ├── ReportController.java
│   │   └── SettingController.java
│   ├── line/                      # LINE Webhook
│   │   └── LineWebhookController.java
│   └── page/                      # 頁面 Controller
│       ├── AdminPageController.java
│       └── TenantPageController.java
│
├── service/                        # 所有 Service
│   ├── admin/                     # 超級管理相關
│   │   ├── AdminTenantService.java
│   │   ├── AdminFeatureService.java
│   │   └── AdminPointService.java
│   ├── tenant/                    # 租戶管理
│   │   ├── TenantService.java
│   │   └── TenantConfigService.java
│   ├── booking/                   # 預約相關
│   │   ├── BookingService.java
│   │   ├── ScheduleService.java
│   │   └── WaitlistService.java
│   ├── customer/                  # 顧客相關
│   │   ├── CustomerService.java
│   │   └── MembershipService.java
│   ├── staff/                     # 員工相關
│   │   ├── StaffService.java
│   │   └── StaffScheduleService.java
│   ├── catalog/                   # 服務與商品
│   │   ├── ServiceItemService.java
│   │   ├── ProductService.java
│   │   └── InventoryService.java
│   ├── order/                     # 訂單結帳
│   │   ├── OrderService.java
│   │   └── PaymentService.java
│   ├── marketing/                 # 行銷相關
│   │   ├── CampaignService.java
│   │   ├── CouponService.java
│   │   └── PointService.java
│   ├── notification/              # 通知相關
│   │   └── NotificationService.java
│   ├── report/                    # 報表相關
│   │   └── ReportService.java
│   ├── cache/                     # 快取相關
│   │   └── CacheService.java
│   └── line/                      # LINE 相關
│       ├── LineConfigService.java
│       ├── LineEventService.java
│       ├── LineMessageService.java
│       └── LineConversationService.java
│
├── repository/                     # 所有 Repository
│   ├── TenantRepository.java
│   ├── TenantConfigRepository.java
│   ├── TenantFeatureRepository.java
│   ├── TenantLineConfigRepository.java
│   ├── BookingRepository.java
│   ├── BookingHistoryRepository.java
│   ├── CustomerRepository.java
│   ├── MembershipRepository.java
│   ├── StaffRepository.java
│   ├── StaffScheduleRepository.java
│   ├── StaffLeaveRepository.java
│   ├── ServiceCategoryRepository.java
│   ├── ServiceItemRepository.java
│   ├── ProductRepository.java
│   ├── ProductCategoryRepository.java
│   ├── InventoryRepository.java
│   ├── OrderRepository.java
│   ├── OrderItemRepository.java
│   ├── CampaignRepository.java
│   ├── CouponRepository.java
│   ├── CouponInstanceRepository.java
│   ├── PointAccountRepository.java
│   ├── PointTransactionRepository.java
│   ├── PointTopUpRepository.java
│   ├── FeatureRepository.java
│   ├── FeatureSubscriptionRepository.java
│   ├── NotificationTemplateRepository.java
│   ├── NotificationLogRepository.java
│   ├── LineUserRepository.java
│   ├── LineConversationStateRepository.java
│   └── AuditLogRepository.java
│
├── entity/                         # 所有資料庫實體
│   ├── base/                      # 基底類別
│   │   └── BaseEntity.java
│   ├── tenant/                    # 租戶相關
│   │   ├── Tenant.java
│   │   ├── TenantConfig.java
│   │   ├── TenantFeature.java
│   │   └── TenantLineConfig.java
│   ├── booking/                   # 預約相關
│   │   ├── Booking.java
│   │   └── BookingHistory.java
│   ├── customer/                  # 顧客相關
│   │   ├── Customer.java
│   │   ├── CustomerPreference.java
│   │   ├── Membership.java
│   │   └── MembershipLevel.java
│   ├── staff/                     # 員工相關
│   │   ├── Staff.java
│   │   ├── StaffSchedule.java
│   │   └── StaffLeave.java
│   ├── catalog/                   # 服務商品
│   │   ├── ServiceCategory.java
│   │   ├── ServiceItem.java
│   │   ├── ProductCategory.java
│   │   ├── Product.java
│   │   └── Inventory.java
│   ├── order/                     # 訂單相關
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   └── Payment.java
│   ├── marketing/                 # 行銷相關
│   │   ├── Campaign.java
│   │   ├── Coupon.java
│   │   ├── CouponInstance.java
│   │   ├── PointAccount.java
│   │   └── PointTransaction.java
│   ├── notification/              # 通知相關
│   │   ├── NotificationTemplate.java
│   │   ├── NotificationSchedule.java
│   │   └── NotificationLog.java
│   ├── line/                      # LINE 相關
│   │   ├── LineUser.java
│   │   └── LineConversationState.java
│   └── system/                    # 系統相關
│       ├── Feature.java
│       ├── FeatureSubscription.java
│       ├── PointTopUp.java
│       └── AuditLog.java
│
├── dto/                            # 所有 DTO
│   ├── request/                   # 請求 DTO
│   │   ├── auth/
│   │   │   └── LoginRequest.java
│   │   ├── tenant/
│   │   │   ├── CreateTenantRequest.java
│   │   │   └── UpdateTenantRequest.java
│   │   ├── booking/
│   │   │   ├── CreateBookingRequest.java
│   │   │   └── UpdateBookingRequest.java
│   │   ├── customer/
│   │   │   ├── CreateCustomerRequest.java
│   │   │   └── UpdateCustomerRequest.java
│   │   ├── staff/
│   │   │   ├── CreateStaffRequest.java
│   │   │   └── UpdateStaffRequest.java
│   │   ├── service/
│   │   │   └── CreateServiceRequest.java
│   │   ├── product/
│   │   │   └── CreateProductRequest.java
│   │   ├── marketing/
│   │   │   ├── CreateCampaignRequest.java
│   │   │   └── CreateCouponRequest.java
│   │   └── line/
│   │       └── LineConfigRequest.java
│   └── response/                  # 回應 DTO
│       ├── auth/
│       │   └── LoginResponse.java
│       ├── tenant/
│       │   ├── TenantResponse.java
│       │   └── TenantDetailResponse.java
│       ├── booking/
│       │   ├── BookingResponse.java
│       │   ├── BookingDetailResponse.java
│       │   └── BookingListItemResponse.java
│       ├── customer/
│       │   └── CustomerResponse.java
│       ├── staff/
│       │   └── StaffResponse.java
│       ├── service/
│       │   └── ServiceResponse.java
│       ├── product/
│       │   └── ProductResponse.java
│       ├── marketing/
│       │   ├── CampaignResponse.java
│       │   └── CouponResponse.java
│       ├── report/
│       │   ├── DashboardResponse.java
│       │   └── RevenueReportResponse.java
│       └── line/
│           └── AvailableSlotResponse.java
│
├── enums/                          # 所有列舉
│   ├── TenantStatus.java
│   ├── BookingStatus.java
│   ├── PaymentMethod.java
│   ├── PaymentStatus.java
│   ├── CouponType.java
│   ├── CouponStatus.java
│   ├── CampaignType.java
│   ├── CampaignStatus.java
│   ├── PointTransactionType.java
│   ├── TopUpStatus.java
│   ├── FeatureCode.java
│   ├── FeatureStatus.java
│   ├── NotificationType.java
│   ├── NotificationStatus.java
│   ├── ConversationState.java
│   ├── ReminderType.java
│   └── StaffRole.java
│
├── mapper/                         # Entity 與 DTO 轉換
│   ├── TenantMapper.java
│   ├── BookingMapper.java
│   ├── CustomerMapper.java
│   ├── StaffMapper.java
│   ├── ServiceMapper.java
│   ├── ProductMapper.java
│   ├── CampaignMapper.java
│   └── CouponMapper.java
│
└── scheduler/                      # 排程任務
    ├── NotificationScheduler.java
    ├── CouponExpireScheduler.java
    ├── BookingReminderScheduler.java
    └── FeatureRenewalScheduler.java
```

---

## 編碼規範

### 命名規範

- **Entity**：單數名詞，如 `Booking`、`Customer`
- **Repository**：`XxxRepository`
- **Service**：`XxxService`
- **Controller**：`XxxController`
- **DTO**：`XxxRequest`、`XxxResponse`
- **Enum 類別**：單數名詞，如 `BookingStatus`
- **Enum 值**：大寫底線，如 `PENDING_CONFIRMATION`
- **常數**：大寫底線，如 `MAX_RETRY_COUNT`
- **變數和方法**：小駝峰，如 `tenantId`、`findByTenantId`

---

### 註解規範（非常重要）

#### 基本原則

1. **註解寫在程式碼上方，絕對不要寫在程式碼後面**
2. **每個類別、方法、重要邏輯都要有詳細註解**
3. **使用繁體中文撰寫註解**
4. **註解要說明「為什麼」，不只是「做什麼」**
5. **複雜邏輯要用分區註解標示流程**

#### 類別註解規範
```java
/**
 * 預約服務類別
 * 
 * 處理所有預約相關的業務邏輯，包含：
 * 
 *   建立預約（含時段衝突檢查）
 *   確認/取消預約
 *   預約狀態流轉
 *   預約提醒排程
 * 
 * 
 * 效能考量：
 * 
 *   使用 Redis 快取熱門時段查詢
 *   批次處理預約提醒發送
 *   非同步發送通知避免阻塞主流程
 * 
 * 
 * @author Developer
 * @since 1.0.0
 * @see BookingRepository
 * @see NotificationService
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {
    // ...
}
```

#### 方法註解規範
```java
/**
 * 建立新預約
 * 
 * 執行流程：
 * 
 *   驗證店家功能是否啟用
 *   檢查員工該時段是否可預約
 *   檢查顧客是否有未完成預約數量限制
 *   建立預約記錄
 *   發送預約通知給店家
 *   排程預約提醒給顧客
 * 
 * 
 * 效能考量：
 * 
 *   時段檢查使用資料庫索引加速查詢
 *   通知發送使用非同步處理，不阻塞主流程
 *   建立完成後清除相關快取
 * 
 * 
 * @param request 預約請求資料，包含服務、員工、時間等資訊
 * @return 建立完成的預約回應
 * @throws BusinessException 當時段已被預約或超過預約上限時拋出
 * @throws FeatureNotEnabledException 當預約功能未啟用時拋出
 */
@Transactional
public BookingResponse createBooking(CreateBookingRequest request) {
    // ...
}
```

#### 程式碼區塊註解規範（重要：使用分區標示）
```java
@Transactional
public BookingResponse createBooking(CreateBookingRequest request) {
    
    // ========================================
    // 1. 取得當前租戶資訊
    // ========================================
    
    // 從 ThreadLocal 取得當前租戶 ID
    String tenantId = TenantContext.getTenantId();
    
    // 查詢租戶設定，用於後續驗證預約規則
    TenantConfig tenantConfig = tenantConfigRepository
            .findByTenantId(tenantId)
            .orElseThrow(() -> new BusinessException(ErrorCode.TENANT_NOT_FOUND));
    
    // ========================================
    // 2. 驗證預約功能是否啟用
    // ========================================
    
    // 檢查該店家是否有啟用基本預約功能
    // 如果功能被超級管理員關閉，則拋出例外
    featureService.checkFeatureEnabled(tenantId, FeatureCode.BASIC_BOOKING);
    
    // ========================================
    // 3. 取得服務項目並計算時間
    // ========================================
    
    // 取得預約的開始時間
    LocalDateTime startTime = request.getStartTime();
    
    // 根據服務項目取得服務時長
    // 服務時長用於計算預約結束時間
    ServiceItem serviceItem = serviceItemRepository
            .findById(request.getServiceId())
            .orElseThrow(() -> new BusinessException(ErrorCode.SERVICE_NOT_FOUND));
    
    // 計算預約結束時間
    // 結束時間 = 開始時間 + 服務時長（分鐘）
    LocalDateTime endTime = startTime.plusMinutes(serviceItem.getDurationMinutes());
    
    // ========================================
    // 4. 檢查員工時段是否可預約
    // ========================================
    
    // 檢查該員工在指定時段是否有其他預約
    // 使用資料庫索引 idx_bookings_staff_time 加速查詢
    // 時段重疊邏輯：新開始 < 舊結束 AND 新結束 > 舊開始
    boolean hasConflict = bookingRepository.existsConflictingBooking(
            tenantId,
            request.getStaffId(),
            startTime,
            endTime
    );
    
    // 如果時段有衝突，拋出業務例外
    if (hasConflict) {
        throw new BusinessException(
                ErrorCode.TIME_SLOT_NOT_AVAILABLE,
                "該時段已被預約，請選擇其他時間"
        );
    }
    
    // ========================================
    // 5. 檢查顧客預約數量限制
    // ========================================
    
    // 取得顧客目前未完成的預約數量
    // 未完成狀態包含：PENDING_CONFIRMATION, CONFIRMED
    long pendingCount = bookingRepository.countPendingBookings(
            tenantId,
            request.getCustomerId()
    );
    
    // 從租戶設定取得每位顧客最大未完成預約數
    int maxPendingBookings = tenantConfig.getMaxPendingBookingsPerCustomer();
    
    // 檢查是否超過上限
    if (pendingCount >= maxPendingBookings) {
        throw new BusinessException(
                ErrorCode.BOOKING_LIMIT_EXCEEDED,
                String.format("您目前有 %d 筆未完成預約，已達上限", pendingCount)
        );
    }
    
    // ========================================
    // 6. 建立預約記錄
    // ========================================
    
    // 建立預約 Entity
    Booking booking = new Booking();
    
    // 設定租戶 ID（多租戶識別）
    booking.setTenantId(tenantId);
    
    // 設定顧客 ID
    booking.setCustomerId(request.getCustomerId());
    
    // 設定服務員工 ID
    booking.setStaffId(request.getStaffId());
    
    // 設定服務項目 ID
    booking.setServiceItemId(request.getServiceId());
    
    // 設定預約開始時間
    booking.setStartTime(startTime);
    
    // 設定預約結束時間（根據服務時長計算）
    booking.setEndTime(endTime);
    
    // 設定預約狀態
    // 根據店家設定決定是「待確認」還是「已確認」
    if (tenantConfig.isAutoConfirmBooking()) {
        // 自動確認模式：直接設為已確認
        // 適用於標準化服務，不需要店家額外確認
        booking.setStatus(BookingStatus.CONFIRMED);
    } else {
        // 審核模式：設為待確認，等店家審核
        // 適用於需要評估的服務，如大型刺青、客製化服務
        booking.setStatus(BookingStatus.PENDING_CONFIRMATION);
    }
    
    // 設定顧客備註
    booking.setCustomerNote(request.getNote());
    
    // 儲存預約記錄到資料庫
    booking = bookingRepository.save(booking);
    
    // ========================================
    // 7. 記錄預約歷史（狀態變更紀錄）
    // ========================================
    
    // 建立預約歷史記錄，用於追蹤狀態變更
    BookingHistory history = new BookingHistory();
    
    // 設定關聯的預約 ID
    history.setBookingId(booking.getId());
    
    // 設定租戶 ID
    history.setTenantId(tenantId);
    
    // 設定變更前狀態（新建立所以是 null）
    history.setFromStatus(null);
    
    // 設定變更後狀態
    history.setToStatus(booking.getStatus());
    
    // 設定變更原因
    history.setReason("顧客建立預約");
    
    // 儲存歷史記錄
    bookingHistoryRepository.save(history);
    
    // ========================================
    // 8. 發送通知（非同步處理）
    // ========================================
    
    // 使用非同步方式發送通知，避免阻塞主流程
    // 即使通知發送失敗，預約仍然會成功建立
    notificationService.sendBookingNotificationAsync(booking);
    
    // ========================================
    // 9. 排程預約提醒
    // ========================================
    
    // 如果店家有啟用自動提醒功能，排程提醒任務
    if (featureService.isFeatureEnabled(tenantId, FeatureCode.AUTO_REMINDER)) {
        // 排程預約前一天的提醒
        reminderScheduler.scheduleReminder(booking, ReminderType.ONE_DAY_BEFORE);
        
        // 排程預約當天早上的提醒
        reminderScheduler.scheduleReminder(booking, ReminderType.SAME_DAY_MORNING);
    }
    
    // ========================================
    // 10. 清除相關快取
    // ========================================
    
    // 清除該員工該日期的可用時段快取
    // 因為已經有新預約，快取的可用時段已經不正確
    cacheService.evictStaffAvailableSlots(
            tenantId,
            request.getStaffId(),
            startTime.toLocalDate()
    );
    
    // ========================================
    // 11. 組裝回應並返回
    // ========================================
    
    // 使用 Mapper 將 Entity 轉換為 Response DTO
    return bookingMapper.toResponse(booking);
}
```

#### 變數宣告註解規範
```java
// 從請求中取得預約開始時間
LocalDateTime startTime = request.getStartTime();

// 計算預約結束時間（開始時間 + 服務時長）
LocalDateTime endTime = startTime.plusMinutes(serviceItem.getDurationMinutes());

// 查詢該時段是否有衝突的預約
// 使用資料庫層級的時間範圍重疊檢查
boolean hasConflict = bookingRepository.existsConflictingBooking(
        tenantId,
        request.getStaffId(),
        startTime,
        endTime
);
```

#### 條件判斷註解規範
```java
// 檢查時段是否有衝突
// 如果有其他預約佔用該時段，則無法預約
if (hasConflict) {
    // 拋出業務例外，告知顧客該時段不可預約
    throw new BusinessException(
            ErrorCode.TIME_SLOT_NOT_AVAILABLE,
            "該時段已被預約，請選擇其他時間"
    );
}

// 檢查預約模式
// 店家可以設定自動確認或需要審核
if (tenantConfig.isAutoConfirmBooking()) {
    // 自動確認模式：預約建立後直接確認
    // 適用於標準化服務，不需要店家額外確認
    booking.setStatus(BookingStatus.CONFIRMED);
} else {
    // 審核模式：預約建立後等待店家確認
    // 適用於需要評估的服務，如大型刺青、客製化服務
    booking.setStatus(BookingStatus.PENDING_CONFIRMATION);
}
```

#### 迴圈註解規範
```java
// 批次處理預約提醒發送
// 每次處理 BATCH_SIZE 筆，避免記憶體溢出
for (List batch : bookingBatches) {
    
    // 遍歷該批次的每筆預約
    for (Booking booking : batch) {
        
        // 檢查預約是否仍然有效（未取消）
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            
            // 發送提醒通知給顧客
            notificationService.sendReminder(booking);
        }
    }
    
    // 每批次處理完後清除 EntityManager 快取
    // 避免大量資料累積在一級快取中
    entityManager.flush();
    entityManager.clear();
}
```

---

### Entity 設計規範

#### BaseEntity（所有 Entity 的基底類別）
```java
/**
 * Entity 基底類別
 * 
 * 所有業務 Entity 都必須繼承此類別，提供：
 * 
 *   UUID 主鍵自動生成
 *   多租戶 tenant_id 自動設定
 *   審計欄位（建立時間、更新時間、建立者、更新者）
 *   軟刪除支援
 * 
 * 
 * @author Developer
 * @since 1.0.0
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
public abstract class BaseEntity {

    /**
     * 主鍵 ID
     * 使用 UUID 格式，36 字元
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * 租戶 ID
     * 用於多租戶資料隔離
     */
    @Column(name = "tenant_id", length = 36)
    private String tenantId;

    /**
     * 建立時間
     * 自動設定，不可更新
     */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 建立者 ID
     */
    @Column(name = "created_by", length = 36)
    private String createdBy;

    /**
     * 更新時間
     * 每次更新自動設定
     */
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 更新者 ID
     */
    @Column(name = "updated_by", length = 36)
    private String updatedBy;

    /**
     * 刪除時間
     * 用於軟刪除，非 null 表示已刪除
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * Entity 儲存前自動執行
     * 設定 UUID 主鍵和租戶 ID
     */
    @PrePersist
    public void prePersist() {
        // 如果沒有設定 ID，自動生成 UUID
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        // 如果沒有設定租戶 ID，從 Context 取得
        if (this.tenantId == null) {
            this.tenantId = TenantContext.getTenantId();
        }
    }

    /**
     * 檢查是否已被軟刪除
     * 
     * @return true 表示已刪除
     */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }

    /**
     * 執行軟刪除
     * 設定刪除時間為當前時間
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}
```

#### Entity 範例（含完整註解和索引）
```java
/**
 * 預約 Entity
 * 
 * 記錄顧客的服務預約資訊，包含預約時間、服務項目、服務人員等。
 * 
 * 資料表：bookings
 * 
 * 索引設計（效能考量）：
 * 
 *   idx_bookings_tenant_status - 用於店家後台列表查詢（依狀態篩選）
 *   idx_bookings_staff_time - 用於時段衝突檢查（高頻查詢）
 *   idx_bookings_customer - 用於顧客預約查詢
 *   idx_bookings_deleted - 用於軟刪除過濾
 * 
 * 
 * @author Developer
 * @since 1.0.0
 */
@Entity
@Table(
        name = "bookings",
        indexes = {
                // 店家後台查詢用索引（依狀態和時間篩選）
                @Index(name = "idx_bookings_tenant_status", columnList = "tenant_id, status, start_time"),
                // 時段衝突檢查用索引（高頻查詢，需要優化）
                @Index(name = "idx_bookings_staff_time", columnList = "tenant_id, staff_id, start_time, end_time"),
                // 顧客預約查詢用索引
                @Index(name = "idx_bookings_customer", columnList = "tenant_id, customer_id, status"),
                // 軟刪除過濾索引
                @Index(name = "idx_bookings_deleted", columnList = "tenant_id, deleted_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends BaseEntity {
    
    /**
     * 顧客 ID
     * 關聯到 customers 表
     */
    @Column(name = "customer_id", nullable = false, length = 36)
    private String customerId;
    
    /**
     * 服務員工 ID
     * 關聯到 staff 表
     * 可為空（表示不指定員工）
     */
    @Column(name = "staff_id", length = 36)
    private String staffId;
    
    /**
     * 服務項目 ID
     * 關聯到 service_items 表
     */
    @Column(name = "service_item_id", nullable = false, length = 36)
    private String serviceItemId;
    
    /**
     * 預約開始時間
     */
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;
    
    /**
     * 預約結束時間
     * 根據服務項目的時長自動計算
     */
    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;
    
    /**
     * 預約狀態
     * 
     * @see BookingStatus
     */
    @Column(name = "status", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private BookingStatus status;
    
    /**
     * 顧客備註
     * 顧客預約時填寫的特殊需求
     */
    @Column(name = "customer_note", length = 500)
    private String customerNote;
    
    /**
     * 店家備註
     * 店家內部備註，顧客看不到
     */
    @Column(name = "shop_note", length = 500)
    private String shopNote;
    
    /**
     * 取消原因
     * 當預約被取消時記錄原因
     */
    @Column(name = "cancel_reason", length = 500)
    private String cancelReason;
    
    /**
     * 實際服務金額
     * 服務完成後記錄實際收費金額
     * 精度：10 位整數，2 位小數
     */
    @Column(name = "actual_amount", precision = 10, scale = 2)
    private BigDecimal actualAmount;
}
```

---

### Repository 設計規範
```java
/**
 * 預約 Repository
 * 
 * 提供預約資料的存取方法
 * 
 * 效能優化原則：
 * 
 *   使用 COUNT 查詢數量，不載入 Entity
 *   使用投影（Projection）只查詢需要的欄位
 *   使用 EXISTS 檢查存在性
 *   所有查詢都要包含 tenant_id 和 deleted_at 條件
 * 
 * 
 * @author Developer
 * @since 1.0.0
 */
public interface BookingRepository extends JpaRepository {
    
    /**
     * 檢查是否存在時段衝突的預約
     * 
     * 使用時間範圍重疊邏輯：
     * 當 (新預約開始時間 < 既有預約結束時間) AND (新預約結束時間 > 既有預約開始時間) 時，
     * 表示兩個預約時段有重疊。
     * 
     * 效能優化：
     * 
     *   使用複合索引 idx_bookings_staff_time
     *   只查詢 COUNT，不載入完整 Entity
     *   限制查詢範圍為有效狀態的預約
     * 
     * 
     * @param tenantId 租戶 ID
     * @param staffId 員工 ID
     * @param startTime 預約開始時間
     * @param endTime 預約結束時間
     * @return 是否存在衝突的預約
     */
    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.staffId = :staffId
            AND b.deletedAt IS NULL
            AND b.status IN ('PENDING_CONFIRMATION', 'CONFIRMED', 'IN_PROGRESS')
            AND b.startTime < :endTime
            AND b.endTime > :startTime
            """)
    boolean existsConflictingBooking(
            @Param("tenantId") String tenantId,
            @Param("staffId") String staffId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
    
    /**
     * 查詢顧客未完成的預約數量
     * 
     * 未完成狀態包含：
     * 
     *   PENDING_CONFIRMATION - 待確認
     *   CONFIRMED - 已確認但尚未服務
     * 
     * 
     * 效能優化：使用 COUNT 查詢，不載入 Entity
     * 
     * @param tenantId 租戶 ID
     * @param customerId 顧客 ID
     * @return 未完成預約數量
     */
    @Query("""
            SELECT COUNT(b)
            FROM Booking b
            WHERE b.tenantId = :tenantId
            AND b.customerId = :customerId
            AND b.deletedAt IS NULL
            AND b.status IN ('PENDING_CONFIRMATION', 'CONFIRMED')
            """)
    long countPendingBookings(
            @Param("tenantId") String tenantId,
            @Param("customerId") String customerId
    );
    
    /**
     * 分頁查詢預約列表（使用投影）
     * 
     * 只查詢列表顯示需要的欄位，減少資料傳輸量
     * 
     * 效能優化：
     * 
     *   使用 DTO 投影，不載入完整 Entity
     *   使用 LEFT JOIN 一次載入關聯資料，避免 N+1
     *   使用 Pageable 分頁
     * 
     * 
     * @param tenantId 租戶 ID
     * @param status 預約狀態（可選）
     * @param staffId 員工 ID（可選）
     * @param startDate 開始日期（可選）
     * @param endDate 結束日期（可選）
     * @param pageable 分頁參數
     * @return 預約分頁結果
     */
    @Query("""
            SELECT new com.booking.platform.dto.response.booking.BookingListItemResponse(
                b.id, b.startTime, b.endTime, b.status,
                c.name, s.name, si.name
            )
            FROM Booking b
            LEFT JOIN Customer c ON b.customerId = c.id
            LEFT JOIN Staff s ON b.staffId = s.id
            LEFT JOIN ServiceItem si ON b.serviceItemId = si.id
            WHERE b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            AND (:status IS NULL OR b.status = :status)
            AND (:staffId IS NULL OR b.staffId = :staffId)
            AND (:startDate IS NULL OR b.startTime >= :startDate)
            AND (:endDate IS NULL OR b.startTime < :endDate)
            ORDER BY b.startTime DESC
            """)
    Page findBookingListItems(
            @Param("tenantId") String tenantId,
            @Param("status") BookingStatus status,
            @Param("staffId") String staffId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
    
    /**
     * 查詢預約詳情（含關聯資料）
     * 
     * 使用 JOIN FETCH 一次載入所有關聯資料，避免 N+1 問題
     * 
     * @param id 預約 ID
     * @param tenantId 租戶 ID
     * @return 預約詳情（含顧客、員工、服務項目資料）
     */
    @Query("""
            SELECT b FROM Booking b
            LEFT JOIN FETCH b.customer
            LEFT JOIN FETCH b.staff
            LEFT JOIN FETCH b.serviceItem
            WHERE b.id = :id
            AND b.tenantId = :tenantId
            AND b.deletedAt IS NULL
            """)
    Optional findByIdWithDetails(
            @Param("id") String id,
            @Param("tenantId") String tenantId
    );
    
    /**
     * 查詢需要發送提醒的預約
     * 
     * 用於排程任務批次發送預約提醒
     * 
     * @param startTime 開始時間
     * @param endTime 結束時間
     * @param pageable 分頁參數
     * @return 預約分頁結果
     */
    @Query("""
            SELECT b FROM Booking b
            WHERE b.status = 'CONFIRMED'
            AND b.deletedAt IS NULL
            AND b.startTime >= :startTime
            AND b.startTime < :endTime
            ORDER BY b.startTime ASC
            """)
    Page findConfirmedBookingsForReminder(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
}
```

---

### API 回應格式

#### 統一回應格式
```java
/**
 * API 統一回應格式
 * 
 * 所有 API 回應都使用此格式包裝，確保一致性
 * 
 * @param  回應資料類型
 */
@Data
@Builder
public class ApiResponse {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 狀態碼
     */
    private String code;
    
    /**
     * 訊息
     */
    private String message;
    
    /**
     * 回應資料
     */
    private T data;

    /**
     * 建立成功回應（含資料）
     * 
     * @param data 回應資料
     * @return 成功回應
     */
    public static  ApiResponse ok(T data) {
        return ApiResponse.builder()
                .success(true)
                .code("SUCCESS")
                .message("操作成功")
                .data(data)
                .build();
    }

    /**
     * 建立成功回應（不含資料）
     * 
     * @return 成功回應
     */
    public static  ApiResponse ok() {
        return ok(null);
    }

    /**
     * 建立錯誤回應
     * 
     * @param code 錯誤碼
     * @param message 錯誤訊息
     * @return 錯誤回應
     */
    public static  ApiResponse error(String code, String message) {
        return ApiResponse.builder()
                .success(false)
                .code(code)
                .message(message)
                .build();
    }
    
    /**
     * 建立錯誤回應（使用 ErrorCode）
     * 
     * @param errorCode 錯誤碼列舉
     * @return 錯誤回應
     */
    public static  ApiResponse error(ErrorCode errorCode) {
        return ApiResponse.builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }
}
```

#### 分頁回應格式
```java
/**
 * 分頁回應格式
 * 
 * @param  內容資料類型
 */
@Data
@Builder
public class PageResponse {
    
    /**
     * 當前頁內容
     */
    private List content;
    
    /**
     * 當前頁碼（從 0 開始）
     */
    private int page;
    
    /**
     * 每頁筆數
     */
    private int size;
    
    /**
     * 總筆數
     */
    private long totalElements;
    
    /**
     * 總頁數
     */
    private int totalPages;
    
    /**
     * 是否為第一頁
     */
    private boolean first;
    
    /**
     * 是否為最後一頁
     */
    private boolean last;
    
    /**
     * 從 Spring Data Page 轉換
     * 
     * @param page Spring Data Page 物件
     * @return PageResponse
     */
    public static  PageResponse from(Page page) {
        return PageResponse.builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
```

回應範例：
```json
{
    "success": true,
    "code": "SUCCESS",
    "message": "操作成功",
    "data": {
        "content": [...],
        "page": 0,
        "size": 20,
        "totalElements": 100,
        "totalPages": 5,
        "first": true,
        "last": false
    }
}
```

---

### 多租戶處理
```java
/**
 * 租戶上下文
 * 
 * 使用 ThreadLocal 儲存當前請求的租戶 ID
 * 每個請求開始時設定，請求結束時清除
 */
public class TenantContext {
    
    /**
     * ThreadLocal 儲存租戶 ID
     */
    private static final ThreadLocal TENANT_ID = new ThreadLocal<>();

    /**
     * 設定當前租戶 ID
     * 
     * @param tenantId 租戶 ID
     */
    public static void setTenantId(String tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 取得當前租戶 ID
     * 
     * @return 租戶 ID
     */
    public static String getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 清除當前租戶 ID
     * 
     * 重要：每個請求結束時必須呼叫，避免記憶體洩漏
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
```

---

## 效能優化規範

### 資料庫效能優化

#### 索引設計原則
```java
/**
 * 索引設計原則：
 * 
 * 1. 為高頻查詢條件建立索引
 * 2. 複合索引的欄位順序：選擇性高的欄位放前面
 * 3. 避免過多索引，影響寫入效能
 * 4. 所有業務表的 tenant_id 都要在索引中
 * 5. 軟刪除的 deleted_at 要考慮加入索引
 */
@Table(
        name = "bookings",
        indexes = {
                // 複合索引：tenant_id 放最前面
                // 因為所有查詢都會帶這個條件
                @Index(
                        name = "idx_bookings_tenant_status",
                        columnList = "tenant_id, status, start_time"
                ),
                
                // 時段衝突檢查專用索引
                // 這是高頻查詢，每次建立預約都會用到
                @Index(
                        name = "idx_bookings_staff_time",
                        columnList = "tenant_id, staff_id, start_time, end_time"
                ),
                
                // 軟刪除過濾索引
                // 大部分查詢都需要過濾已刪除資料
                @Index(
                        name = "idx_bookings_deleted",
                        columnList = "tenant_id, deleted_at"
                )
        }
)
```

#### 查詢優化原則
```java
// ========================================
// 好的做法
// ========================================

// 1. 只查詢數量，不載入 Entity
@Query("SELECT COUNT(b) FROM Booking b WHERE b.tenantId = :tenantId")
long countByTenantId(@Param("tenantId") String tenantId);

// 2. 使用 EXISTS 檢查存在性（比 COUNT > 0 更有效率）
@Query("SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END FROM Booking b WHERE ...")
boolean existsConflictingBooking(...);

// 3. 使用投影只查詢需要的欄位
@Query("SELECT new com.booking.platform.dto.BookingListItemResponse(b.id, b.startTime, ...) FROM Booking b WHERE ...")
Page findBookingListItems(...);

// 4. 使用 JOIN FETCH 避免 N+1 問題
@Query("SELECT b FROM Booking b LEFT JOIN FETCH b.customer LEFT JOIN FETCH b.staff WHERE b.id = :id")
Optional findByIdWithDetails(@Param("id") String id);

// ========================================
// 不好的做法（避免使用）
// ========================================

// 不好：會載入所有欄位，效能差
// List findByTenantId(String tenantId);

// 不好：N+1 問題
// Booking booking = bookingRepository.findById(id);
// Customer customer = customerRepository.findById(booking.getCustomerId()); // 額外查詢
```

### 批次處理
```java
/**
 * 批次處理服務
 * 
 * 處理大量資料時，應該分批處理，避免：
 * 
 *   記憶體溢出（OOM）
 *   長時間鎖定資料表
 *   交易超時
 * 
 */
@Service
@RequiredArgsConstructor
public class NotificationBatchService {
    
    // 每批次處理數量
    private static final int BATCH_SIZE = 100;
    
    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final EntityManager entityManager;
    
    /**
     * 批次發送預約提醒
     * 
     * 效能優化：
     * 
     *   分批處理，每批 100 筆
     *   每批處理完清除 EntityManager 快取
     *   單筆失敗不影響其他筆
     * 
     */
    @Transactional
    public void sendBookingReminders(LocalDate targetDate) {
        
        // 計算查詢時間範圍
        LocalDateTime startOfDay = targetDate.atStartOfDay();
        LocalDateTime endOfDay = targetDate.plusDays(1).atStartOfDay();
        
        // 使用分頁查詢，每次只取 BATCH_SIZE 筆
        int page = 0;
        Page bookingPage;
        
        do {
            // 分頁查詢
            Pageable pageable = PageRequest.of(page, BATCH_SIZE);
            bookingPage = bookingRepository.findConfirmedBookingsForReminder(
                    startOfDay,
                    endOfDay,
                    pageable
            );
            
            // 處理該批次的預約
            for (Booking booking : bookingPage.getContent()) {
                
                // 發送提醒通知
                try {
                    notificationService.sendReminder(booking);
                } catch (Exception e) {
                    // 單筆失敗不影響其他筆
                    // 記錄錯誤日誌，稍後可以重試
                    log.error("發送預約提醒失敗，bookingId: {}", booking.getId(), e);
                }
            }
            
            // 清除 EntityManager 一級快取
            // 避免大量 Entity 累積在記憶體中
            entityManager.flush();
            entityManager.clear();
            
            // 下一頁
            page++;
            
        } while (bookingPage.hasNext());
    }
}
```

### 快取策略
```java
/**
 * 快取服務
 * 
 * 使用 Redis 快取熱點資料，減少資料庫壓力
 * 
 * 快取策略：
 * 
 *   員工可用時段 - TTL 5 分鐘（資料變動頻繁）
 *   服務項目列表 - TTL 30 分鐘（資料較穩定）
 *   店家設定 - TTL 1 小時（資料很少變動）
 * 
 */
@Service
@RequiredArgsConstructor
public class CacheService {
    
    private final RedisTemplate redisTemplate;
    
    // 快取 Key 前綴
    private static final String KEY_PREFIX = "booking:";
    
    // 快取 TTL 設定
    private static final Duration SLOT_CACHE_TTL = Duration.ofMinutes(5);
    private static final Duration SERVICE_CACHE_TTL = Duration.ofMinutes(30);
    private static final Duration CONFIG_CACHE_TTL = Duration.ofHours(1);
    
    /**
     * 取得員工可用時段（優先從快取取）
     * 
     * @param tenantId 租戶 ID
     * @param staffId 員工 ID
     * @param date 日期
     * @return 可用時段列表
     */
    public List getStaffAvailableSlots(
            String tenantId,
            String staffId,
            LocalDate date
    ) {
        // 組合快取 Key
        String cacheKey = KEY_PREFIX + "slots:" + tenantId + ":" + staffId + ":" + date;
        
        // 嘗試從快取取得
        @SuppressWarnings("unchecked")
        List cachedSlots = (List) redisTemplate.opsForValue().get(cacheKey);
        
        // 快取命中，直接返回
        if (cachedSlots != null) {
            return cachedSlots;
        }
        
        // 快取未命中，從資料庫計算
        List slots = calculateAvailableSlots(tenantId, staffId, date);
        
        // 存入快取
        redisTemplate.opsForValue().set(cacheKey, slots, SLOT_CACHE_TTL);
        
        return slots;
    }
    
    /**
     * 清除員工可用時段快取
     * 
     * 當有新預約或取消預約時呼叫
     * 
     * @param tenantId 租戶 ID
     * @param staffId 員工 ID
     * @param date 日期
     */
    public void evictStaffAvailableSlots(String tenantId, String staffId, LocalDate date) {
        String cacheKey = KEY_PREFIX + "slots:" + tenantId + ":" + staffId + ":" + date;
        redisTemplate.delete(cacheKey);
    }
    
    /**
     * 清除店家所有員工的時段快取
     * 
     * 當店家營業時間變更時呼叫
     * 
     * @param tenantId 租戶 ID
     */
    public void evictAllStaffSlots(String tenantId) {
        // 使用 pattern 刪除所有相關快取
        String pattern = KEY_PREFIX + "slots:" + tenantId + ":*";
        Set keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
```

### 非同步處理
```java
/**
 * 非同步服務設定
 * 
 * 將耗時操作放到非同步執行，不阻塞主流程
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    
    /**
     * 設定非同步執行緒池
     * 
     * 參數說明：
     * 
     *   corePoolSize: 核心執行緒數，常駐執行緒
     *   maxPoolSize: 最大執行緒數，高峰期可擴展
     *   queueCapacity: 佇列容量，超過會新建執行緒
     *   threadNamePrefix: 執行緒名稱前綴，方便日誌追蹤
     * 
     */
    @Bean(name = "notificationExecutor")
    public Executor notificationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        
        // 核心執行緒數
        executor.setCorePoolSize(4);
        
        // 最大執行緒數
        executor.setMaxPoolSize(10);
        
        // 佇列容量
        executor.setQueueCapacity(500);
        
        // 執行緒名稱前綴
        executor.setThreadNamePrefix("notification-");
        
        // 拒絕策略：佇列滿時由呼叫者執行
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        
        executor.initialize();
        return executor;
    }
}

/**
 * 通知服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    
    private final LineMessageService lineMessageService;
    private final NotificationLogRepository notificationLogRepository;
    
    /**
     * 非同步發送預約通知
     * 
     * 使用 @Async 標註，方法會在另一個執行緒中執行
     * 主流程不會等待此方法完成
     * 
     * @param booking 預約資訊
     */
    @Async("notificationExecutor")
    public void sendBookingNotificationAsync(Booking booking) {
        try {
            // 發送 LINE 訊息
            lineMessageService.sendBookingConfirmation(booking);
            
            // 記錄發送成功
            saveNotificationLog(booking, NotificationStatus.SUCCESS, null);
            
        } catch (Exception e) {
            // 記錄發送失敗（可以稍後重試）
            saveNotificationLog(booking, NotificationStatus.FAILED, e.getMessage());
            
            // 記錄錯誤日誌
            log.error("發送預約通知失敗，bookingId: {}", booking.getId(), e);
        }
    }
}
```

### 連線池設定
```yaml
# application.yml

spring:
  datasource:
    # HikariCP 連線池設定
    hikari:
      # 最小閒置連線數
      minimum-idle: 5
      
      # 最大連線數
      # 建議值：(CPU 核心數 * 2) + 磁碟數
      maximum-pool-size: 20
      
      # 連線閒置超時時間（毫秒）
      idle-timeout: 300000
      
      # 連線最大存活時間（毫秒）
      max-lifetime: 1200000
      
      # 取得連線的超時時間（毫秒）
      connection-timeout: 30000
      
      # 連線池名稱（方便日誌追蹤）
      pool-name: BookingHikariPool

  data:
    redis:
      # Lettuce 連線池設定
      lettuce:
        pool:
          # 最小閒置連線數
          min-idle: 2
          
          # 最大閒置連線數
          max-idle: 10
          
          # 最大活躍連線數
          max-active: 20
          
          # 取得連線的最大等待時間
          max-wait: 5000ms
```

### 分頁查詢規範
```java
/**
 * 分頁查詢規範
 * 
 * 所有列表查詢都應該使用分頁，避免一次載入過多資料
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
public class BookingController {
    
    // 預設分頁大小
    private static final int DEFAULT_PAGE_SIZE = 20;
    
    // 最大分頁大小（防止惡意請求）
    private static final int MAX_PAGE_SIZE = 100;
    
    private final BookingService bookingService;
    
    /**
     * 查詢預約列表
     * 
     * @param page 頁碼（從 0 開始）
     * @param size 每頁筆數（最大 100）
     * @param status 預約狀態篩選
     * @param staffId 員工篩選
     * @return 分頁結果
     */
    @GetMapping
    public ApiResponse<PageResponse> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) String staffId
    ) {
        // 限制最大分頁大小，防止惡意請求
        if (size > MAX_PAGE_SIZE) {
            size = MAX_PAGE_SIZE;
        }
        
        // 建立分頁請求
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by("startTime").descending()
        );
        
        // 執行分頁查詢
        Page result = bookingService.getBookings(
                status,
                staffId,
                pageable
        );
        
        // 轉換為自訂分頁回應格式
        return ApiResponse.ok(PageResponse.from(result));
    }
}
```

---

## 核心功能模組

### 1. 租戶管理（Tenant）

**Entity**
- `Tenant`：店家基本資料（店名、代碼、聯絡資訊、狀態）
- `TenantConfig`：店家設定（營業時間、預約規則、通知設定）
- `TenantFeature`：店家功能啟用狀態
- `TenantLineConfig`：店家的 LINE 官方帳號設定

**店家狀態**
```java
public enum TenantStatus {
    PENDING,      // 待審核
    ACTIVE,       // 啟用中
    SUSPENDED,    // 已停權
    TERMINATED    // 已終止
}
```

### 2. 預約管理（Booking）

**Entity**
- `Booking`：預約主檔
- `BookingHistory`：預約狀態變更紀錄

**預約狀態流程**
```java
public enum BookingStatus {
    PENDING_CONFIRMATION,  // 待確認
    CONFIRMED,             // 已確認
    IN_PROGRESS,           // 進行中
    COMPLETED,             // 已完成
    CANCELLED_BY_CUSTOMER, // 顧客取消
    CANCELLED_BY_SHOP,     // 店家取消
    NO_SHOW,               // 爽約
    EXPIRED                // 過期未處理
}
```

**預約模式**
- 即時確認模式：預約後立即確認
- 審核確認模式：預約後待店家確認

### 3. 員工管理（Staff）

**Entity**
- `Staff`：員工基本資料
- `StaffSchedule`：員工班表
- `StaffLeave`：員工請假

**員工角色**
```java
public enum StaffRole {
    OWNER,     // 老闆
    MANAGER,   // 店長
    STAFF,     // 一般員工
    ASSISTANT  // 助理
}
```

### 4. 服務與商品（Catalog）

**Entity**
- `ServiceCategory`：服務分類
- `ServiceItem`：服務項目
- `ProductCategory`：商品分類
- `Product`：商品
- `Inventory`：庫存

### 5. 顧客管理（Customer）

**Entity**
- `Customer`：顧客基本資料
- `CustomerPreference`：顧客偏好
- `Membership`：會員資格
- `MembershipLevel`：會員等級

### 6. 訂單結帳（Order）

**Entity**
- `Order`：訂單/結帳單
- `OrderItem`：訂單項目
- `Payment`：付款紀錄

**付款方式（僅紀錄，不串金流）**
```java
public enum PaymentMethod {
    CASH,        // 現金
    CREDIT_CARD, // 信用卡
    LINE_PAY,    // LINE Pay
    TRANSFER,    // 轉帳
    OTHER        // 其他
}
```

### 7. 行銷活動（Marketing）

**Entity**
- `Campaign`：活動
- `Coupon`：票券定義
- `CouponInstance`：票券實例
- `PointAccount`：點數帳戶
- `PointTransaction`：點數異動

**活動類型**
```java
public enum CampaignType {
    BIRTHDAY,           // 生日活動
    NEW_CUSTOMER,       // 新客活動
    SPENDING_THRESHOLD, // 滿額活動
    LIMITED_TIME,       // 限時活動
    RECALL,             // 喚回活動
    REFERRAL            // 推薦活動
}
```

**票券類型**
```java
public enum CouponType {
    DISCOUNT_AMOUNT,  // 折價券（固定金額）
    DISCOUNT_PERCENT, // 折扣券（百分比）
    GIFT,             // 兌換券
    ADDON             // 加價購券
}
```

### 8. 通知系統（Notification）

**Entity**
- `NotificationTemplate`：通知模板
- `NotificationSchedule`：通知排程
- `NotificationLog`：通知紀錄

**通知類型**
```java
public enum NotificationType {
    BOOKING_CONFIRMED,  // 預約確認
    BOOKING_REMINDER,   // 預約提醒
    BOOKING_CANCELLED,  // 預約取消
    BIRTHDAY_GREETING,  // 生日祝福
    COUPON_RECEIVED,    // 獲得票券
    COUPON_EXPIRING,    // 票券即將到期
    CAMPAIGN_PROMOTION, // 活動推播
    RECALL              // 喚回通知
}
```

### 9. 功能控制（Feature）

**Entity**
- `Feature`：功能定義
- `FeatureSubscription`：功能訂閱

**功能代碼**
```java
public enum FeatureCode {
    // 免費功能
    BASIC_BOOKING,      // 基本預約
    BASIC_CUSTOMER,     // 基本顧客管理
    BASIC_STAFF,        // 基本員工管理（限3位）
    BASIC_SERVICE,      // 基本服務管理
    BASIC_REPORT,       // 基本報表

    // 加值功能
    UNLIMITED_STAFF,    // 無限員工
    MULTI_ACCOUNT,      // 多帳號權限
    ADVANCED_REPORT,    // 進階報表
    COUPON_SYSTEM,      // 票券系統
    MEMBERSHIP_SYSTEM,  // 會員等級系統
    POINT_SYSTEM,       // 集點系統
    AUTO_BIRTHDAY,      // 自動生日祝福
    AUTO_REMINDER,      // 自動預約提醒
    AUTO_RECALL,        // 自動喚回通知
    AI_ASSISTANT,       // AI 智慧客服
    ADVANCED_CUSTOMER,  // 進階顧客篩選
    EXTRA_PUSH,         // 推送額度加購
    MULTI_BRANCH,       // 多分店管理
    INVENTORY,          // 庫存管理
    PRODUCT_SALES       // 商品銷售
}
```

**功能狀態（針對特定店家）**
```java
public enum FeatureStatus {
    HIDDEN,     // 隱藏（店家看不到）
    VISIBLE,    // 可見但不可申請
    AVAILABLE,  // 可申請
    PENDING,    // 已申請待審核
    ENABLED,    // 已啟用
    SUSPENDED,  // 已凍結
    EXPIRED     // 已過期
}
```

### 10. 點數儲值（Point）

**Entity**
- `PointTopUp`：儲值申請

**儲值狀態**
```java
public enum TopUpStatus {
    PENDING,   // 待審核
    APPROVED,  // 已通過
    REJECTED,  // 已駁回
    CANCELLED  // 已取消
}
```

---

## LINE 整合

### LINE 官方帳號架構

每個店家（租戶）擁有自己的 LINE 官方帳號：

- **Channel ID**：LINE Channel 識別碼
- **Channel Secret**：用於驗證 Webhook 簽章
- **Channel Access Token**：用於發送訊息

### Entity
```java
/**
 * 店家 LINE 設定 Entity
 */
@Entity
@Table(name = "tenant_line_configs")
@Getter
@Setter
public class TenantLineConfig extends BaseEntity {

    /**
     * 租戶 ID
     */
    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    /**
     * LINE Channel ID
     */
    @Column(name = "channel_id", nullable = false, length = 50)
    private String channelId;

    /**
     * LINE Channel Secret
     * 加密儲存
     */
    @Column(name = "channel_secret", nullable = false, length = 255)
    private String channelSecret;

    /**
     * LINE Channel Access Token
     * 加密儲存
     */
    @Column(name = "channel_access_token", nullable = false, length = 500)
    private String channelAccessToken;

    /**
     * Webhook URL
     */
    @Column(name = "webhook_url", length = 255)
    private String webhookUrl;

    /**
     * 是否啟用
     */
    @Column(name = "is_active")
    private Boolean isActive = true;
}

/**
 * LINE 用戶 Entity
 */
@Entity
@Table(name = "line_users")
@Getter
@Setter
public class LineUser extends BaseEntity {

    /**
     * 租戶 ID
     */
    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    /**
     * 顧客 ID
     * 關聯到 customers 表
     */
    @Column(name = "customer_id", length = 36)
    private String customerId;

    /**
     * LINE User ID
     */
    @Column(name = "line_user_id", nullable = false, length = 50)
    private String lineUserId;

    /**
     * LINE 顯示名稱
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * LINE 頭像 URL
     */
    @Column(name = "picture_url", length = 500)
    private String pictureUrl;

    /**
     * 是否追蹤中
     */
    @Column(name = "is_followed")
    private Boolean isFollowed = true;

    /**
     * 追蹤時間
     */
    @Column(name = "followed_at")
    private LocalDateTime followedAt;

    /**
     * 取消追蹤時間
     */
    @Column(name = "unfollowed_at")
    private LocalDateTime unfollowedAt;
}

/**
 * LINE 對話狀態 Entity
 */
@Entity
@Table(name = "line_conversation_states")
@Getter
@Setter
public class LineConversationState {

    /**
     * 主鍵 ID
     */
    @Id
    @Column(length = 36)
    private String id;

    /**
     * 租戶 ID
     */
    @Column(name = "tenant_id", nullable = false, length = 36)
    private String tenantId;

    /**
     * LINE User ID
     */
    @Column(name = "line_user_id", nullable = false, length = 50)
    private String lineUserId;

    /**
     * 對話狀態
     */
    @Column(name = "state", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private ConversationState state;

    /**
     * 對話上下文資料（JSON 格式）
     */
    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData;

    /**
     * 狀態過期時間
     */
    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
}
```

### 對話狀態
```java
public enum ConversationState {
    IDLE,                    // 閒置
    SELECTING_SERVICE,       // 選擇服務
    SELECTING_STAFF,         // 選擇人員
    SELECTING_DATE,          // 選擇日期
    SELECTING_TIME,          // 選擇時間
    CONFIRMING_BOOKING,      // 確認預約
    WAITING_CUSTOMER_INFO,   // 等待填寫顧客資料
    VIEWING_BOOKINGS,        // 查看預約
    CANCELLING_BOOKING,      // 取消預約流程
    VIEWING_COUPONS,         // 查看票券
    CONTACTING_SHOP          // 聯絡店家
}
```

### Webhook URL 格式
```
https://{domain}/api/line/webhook/{tenantCode}
```

### LINE 訊息服務
```java
/**
 * LINE 訊息服務
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LineMessageService {

    private final TenantLineConfigRepository lineConfigRepository;
    private final EncryptionUtil encryptionUtil;

    /**
     * 回覆訊息
     * 
     * @param tenantId 租戶 ID
     * @param replyToken 回覆 Token
     * @param messages 訊息列表
     */
    public void replyMessage(String tenantId, String replyToken, List messages) {
        // 取得店家的 LINE 設定
        TenantLineConfig config = getLineConfig(tenantId);
        
        // 解密 Access Token
        String accessToken = encryptionUtil.decrypt(config.getChannelAccessToken());
        
        // 發送回覆訊息
        // ... LINE API 呼叫
    }

    /**
     * 推送訊息給單一用戶
     * 
     * @param tenantId 租戶 ID
     * @param lineUserId LINE User ID
     * @param messages 訊息列表
     */
    public void pushMessage(String tenantId, String lineUserId, List messages) {
        // 取得店家的 LINE 設定
        TenantLineConfig config = getLineConfig(tenantId);
        
        // 解密 Access Token
        String accessToken = encryptionUtil.decrypt(config.getChannelAccessToken());
        
        // 發送推送訊息
        // ... LINE API 呼叫
    }

    /**
     * 群發訊息
     * 
     * @param tenantId 租戶 ID
     * @param lineUserIds LINE User ID 列表
     * @param messages 訊息列表
     */
    public void multicast(String tenantId, List lineUserIds, List messages) {
        // ... 實作
    }
}
```

---

## 超級管理員控制權

超級管理員（我）擁有最高權限：

### 店家管理
- 查看所有店家清單和詳情
- 建立店家帳號
- 啟用/停用/終止店家
- 模擬登入店家後台
- 查看店家操作紀錄

### 功能控制
- 對任何店家開通任何功能
- 對任何店家關閉任何功能
- 對特定店家隱藏特定功能
- 設定特定店家的特殊價格
- 設定功能有效期限
- 凍結/解凍功能
- 批次操作

### 點數管理
- 審核儲值申請
- 手動加減店家點數
- 查看所有點數異動紀錄

---

## API 路徑設計

### 認證
```
POST   /api/auth/admin/login          # 超級管理員登入
POST   /api/auth/tenant/login         # 店家登入
POST   /api/auth/refresh              # 刷新 Token
POST   /api/auth/logout               # 登出
```

### 超級管理後台
```
# 店家管理
GET    /api/admin/tenants                              # 店家列表
POST   /api/admin/tenants                              # 建立店家
GET    /api/admin/tenants/{id}                         # 店家詳情
PUT    /api/admin/tenants/{id}                         # 更新店家
PUT    /api/admin/tenants/{id}/status                  # 變更店家狀態
DELETE /api/admin/tenants/{id}                         # 刪除店家

# 功能控制
GET    /api/admin/features                             # 功能列表
POST   /api/admin/tenants/{id}/features/{code}/enable  # 開通功能
POST   /api/admin/tenants/{id}/features/{code}/disable # 關閉功能
PUT    /api/admin/tenants/{id}/features/{code}/config  # 設定功能
POST   /api/admin/tenants/batch/features/enable        # 批次開通
POST   /api/admin/tenants/batch/features/disable       # 批次關閉

# 點數管理
GET    /api/admin/point-topups                         # 儲值申請列表
POST   /api/admin/point-topups/{id}/approve            # 審核通過
POST   /api/admin/point-topups/{id}/reject             # 審核駁回
POST   /api/admin/tenants/{id}/points/adjust           # 手動調整點數

# 數據統計
GET    /api/admin/dashboard                            # 儀表板數據
```

### 店家後台
```
# 預約管理
GET    /api/bookings                       # 預約列表
POST   /api/bookings                       # 建立預約
GET    /api/bookings/{id}                  # 預約詳情
PUT    /api/bookings/{id}                  # 更新預約
PUT    /api/bookings/{id}/confirm          # 確認預約
PUT    /api/bookings/{id}/cancel           # 取消預約
PUT    /api/bookings/{id}/complete         # 完成預約
GET    /api/bookings/calendar              # 行事曆檢視

# 顧客管理
GET    /api/customers                      # 顧客列表
POST   /api/customers                      # 建立顧客
GET    /api/customers/{id}                 # 顧客詳情
PUT    /api/customers/{id}                 # 更新顧客

# 員工管理
GET    /api/staff                          # 員工列表
POST   /api/staff                          # 建立員工
GET    /api/staff/{id}                     # 員工詳情
PUT    /api/staff/{id}                     # 更新員工
GET    /api/staff/{id}/schedule            # 員工班表
PUT    /api/staff/{id}/schedule            # 更新班表

# 服務管理
GET    /api/services                       # 服務列表
POST   /api/services                       # 建立服務
PUT    /api/services/{id}                  # 更新服務

# 商品管理
GET    /api/products                       # 商品列表
POST   /api/products                       # 建立商品
PUT    /api/products/{id}                  # 更新商品

# 行銷活動
GET    /api/campaigns                      # 活動列表
POST   /api/campaigns                      # 建立活動
PUT    /api/campaigns/{id}                 # 更新活動

# 票券管理
GET    /api/coupons                        # 票券列表
POST   /api/coupons                        # 建立票券
POST   /api/coupons/{id}/issue             # 發放票券
POST   /api/coupons/instances/{id}/redeem  # 核銷票券

# 報表
GET    /api/reports/dashboard              # 儀表板
GET    /api/reports/revenue                # 營收報表

# 設定
GET    /api/settings                       # 取得設定
PUT    /api/settings                       # 更新設定
GET    /api/settings/line                  # LINE 設定
PUT    /api/settings/line                  # 更新 LINE 設定

# 功能商店
GET    /api/feature-store                  # 可用功能列表
POST   /api/feature-store/{code}/apply     # 申請功能

# 點數
GET    /api/points/balance                 # 點數餘額
POST   /api/points/topup                   # 申請儲值
```

### LINE Webhook
```
POST   /api/line/webhook/{tenantCode}      # 接收 LINE 訊息
```

---

## 頁面路由

### 超級管理後台
GET    /admin/login                        # 登入頁
GET    /admin/dashboard                    # 儀表板
GET    /admin/tenants                      # 店家管理Continue上午10:09GET    /admin/tenants/{id}                 # 店家詳情
GET    /admin/features                     # 功能管理
GET    /admin/point-topups                 # 儲值審核

### 店家後台
GET    /tenant/login                       # 登入頁
GET    /tenant/dashboard                   # 儀表板
GET    /tenant/bookings                    # 預約管理
GET    /tenant/calendar                    # 行事曆
GET    /tenant/customers                   # 顧客管理
GET    /tenant/staff                       # 員工管理
GET    /tenant/services                    # 服務管理
GET    /tenant/products                    # 商品管理
GET    /tenant/campaigns                   # 行銷活動
GET    /tenant/coupons                     # 票券管理
GET    /tenant/reports                     # 報表
GET    /tenant/settings                    # 設定
GET    /tenant/feature-store               # 功能商店
GET    /tenant/points                      # 點數管理

---

## 資料庫命名規範

- **表名**：小寫底線，複數，如 `bookings`、`customers`
- **欄位**：小寫底線，如 `tenant_id`、`created_at`
- **主鍵**：`id`（UUID 字串，36 字元）
- **外鍵**：`xxx_id`
- **布林值**：`is_xxx`，如 `is_active`
- **時間戳記**：`xxx_at`，如 `created_at`、`expired_at`

---

## 環境設定

### application.yml
```yaml
spring:
  application:
    name: booking-platform
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/booking_platform}
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
    driver-class-name: org.postgresql.Driver
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 300000
      max-lifetime: 1200000
      connection-timeout: 30000
      pool-name: BookingHikariPool

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true

  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_PASSWORD:}
      lettuce:
        pool:
          min-idle: 2
          max-idle: 10
          max-active: 20
          max-wait: 5000ms

  thymeleaf:
    cache: false
    prefix: classpath:/templates/
    suffix: .html

server:
  port: ${SERVER_PORT:8080}

app:
  jwt:
    secret: ${JWT_SECRET:your-secret-key-at-least-32-characters-long}
    expiration: ${JWT_EXPIRATION:86400000}
  encryption:
    key: ${ENCRYPTION_KEY:your-encryption-key-32ch}
  base-url: ${BASE_URL:http://localhost:8080}
  admin:
    username: ${ADMIN_USERNAME:admin}
    password: ${ADMIN_PASSWORD:admin123}
```

### 正式環境需設定的環境變數
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://your-db-host:5432/booking_platform
DB_USERNAME=your_db_user
DB_PASSWORD=your_db_password
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
JWT_SECRET=your-production-jwt-secret-key-at-least-32-characters
ENCRYPTION_KEY=your-production-encryption-key-32
BASE_URL=https://your-domain.com
ADMIN_USERNAME=your_admin_username
ADMIN_PASSWORD=your_secure_admin_password

---

## 開發優先順序

### 第一階段：核心骨架 ✅ 已完成
1. ✅ Spring Boot Maven 專案初始化
2. ✅ 共用元件（ApiResponse、例外處理、多租戶 Context）
3. ✅ 安全設定（JWT 認證）
4. ✅ Tenant 模組（Entity、Repository、Service、Controller）
5. ✅ 基本頁面框架（登入頁、Layout）

### 第二階段：預約核心 ✅ 已完成
1. ✅ Staff 模組
2. ✅ ServiceItem 模組
3. ✅ Booking 模組
4. ⏳ LINE Webhook 基本處理（待實作）
5. ⏳ 預約相關頁面（待實作）

### 第三階段：顧客經營 ✅ 已完成
1. ✅ Customer 模組
2. ✅ 會員系統（MembershipLevel）
3. ⏳ 預約提醒通知（待實作）
4. ⏳ 顧客管理頁面（待實作）

### 第四階段：商業化 ✅ 已完成
1. ✅ Feature 功能開關模組（20 種功能）
2. ✅ Point 點數儲值模組
3. ✅ 超級管理後台完整功能
4. ⏳ 超級管理後台頁面（待實作）

### 第五階段：加值功能 ✅ 已完成
1. ✅ 票券系統（Coupon、CouponInstance）
2. ✅ 行銷活動（Campaign）
3. ✅ 商品系統（Product）
4. ✅ 報表系統（ReportService）
5. ⏳ 對應頁面（待實作）

---

## 已完成的 API 端點

### Phase 1 - 租戶管理
- `GET/POST /api/admin/tenants` - 租戶 CRUD
- `POST /api/admin/tenants/{id}/status` - 變更狀態

### Phase 2 - 預約核心
- `GET/POST /api/staffs` - 員工 CRUD
- `GET/POST /api/services` - 服務 CRUD
- `GET/POST /api/bookings` - 預約 CRUD
- `POST /api/bookings/{id}/confirm|cancel|complete|no-show` - 預約狀態操作

### Phase 3 - 顧客管理
- `GET/POST /api/customers` - 顧客 CRUD
- `GET/POST /api/membership-levels` - 會員等級 CRUD

### Phase 4 - 商業化
- `GET /api/admin/features` - 功能列表
- `POST /api/admin/features/init` - 初始化功能
- `POST /api/admin/tenants/{id}/features/{code}/enable|disable|suspend` - 功能控制
- `GET /api/admin/point-topups` - 儲值申請列表
- `POST /api/admin/point-topups/{id}/approve|reject` - 審核儲值
- `POST /api/admin/tenants/{id}/points/adjust` - 調整點數

### Phase 5 - 加值功能
- `GET/POST /api/coupons` - 票券 CRUD
- `POST /api/coupons/{id}/publish|pause|resume` - 票券狀態
- `POST /api/coupons/{id}/issue` - 發放票券
- `POST /api/coupons/instances/{id}/redeem` - 依 ID 核銷
- `POST /api/coupons/redeem-by-code` - 依代碼核銷
- `GET /api/coupons/customers/{id}` - 顧客票券
- `GET /api/coupons/customers/{id}/usable` - 可用票券
- `GET/POST /api/campaigns` - 活動 CRUD
- `POST /api/campaigns/{id}/publish|pause|resume|end` - 活動狀態
- `GET/POST /api/products` - 商品 CRUD
- `POST /api/products/{id}/on-sale|off-shelf` - 商品上下架
- `POST /api/products/{id}/adjust-stock` - 庫存調整
- `GET /api/reports/summary|today|weekly|monthly` - 報表摘要
- `GET /api/reports/daily` - 每日報表
- `GET /api/reports/top-services|top-staff` - 排名報表

---

## 注意事項

1. 所有業務資料都要有 `tenant_id`
2. 刪除使用軟刪除（設定 `deleted_at`）
3. 重要操作要寫入 `AuditLog`
4. 敏感資料（LINE Token 等）要加密儲存
5. API 要有適當的權限檢查
6. 使用繁體中文撰寫所有註解
7. 每個功能先檢查該店家是否有啟用該功能
8. 查詢效能優化：使用索引、投影、分頁
9. 非同步處理耗時操作（通知發送等）
10. 快取熱點資料減少資料庫壓力
# Booking Platform - 多租戶預約平台系統

## 線上環境

| 環境 | URL | 短網址 |
|------|-----|--------|
| 店家後台 | https://booking-platform-production-1e08.up.railway.app/tenant/login | https://tinyurl.com/26fxodho |
| 超管後台 | https://booking-platform-production-1e08.up.railway.app/admin/login | https://tinyurl.com/2b9fhnm2 |
| 健康檢查 | https://booking-platform-production-1e08.up.railway.app/health | https://tinyurl.com/2cdb5t4p |

## 快速參考

| 項目 | 值 |
|------|-----|
| Java | 17 |
| Spring Boot | 3.2 |
| 資料庫 | PostgreSQL (Railway) |
| 快取 | Redis |
| 前端 | Thymeleaf + Bootstrap 5 |
| 行事曆 | FullCalendar |
| 圖表 | Chart.js |
| 報表匯出 | Apache POI (Excel) + OpenPDF (PDF) |
| 金流 | ECPay 綠界 |
| 郵件 | Resend (HTTP API) |
| SMS | 三竹簡訊 |
| 部署 | Railway (Docker) |

## 三個角色

| 角色 | 說明 |
|------|------|
| ADMIN | 超級管理員，管理所有店家、審核儲值、控制功能開關 |
| TENANT | 店家，管理自己的預約/顧客/員工/服務/商品 |
| 顧客 | 透過 LINE Bot 預約服務、購買商品、領取票券 |

---

## 專案結構

```
com.booking.platform
├── common/                    # 共用元件
│   ├── config/               # 設定 (Security, Redis, Jackson, Async, Locale, Sms, Ecpay)
│   ├── exception/            # 例外 (BusinessException, ErrorCode)
│   ├── response/             # 統一回應 (ApiResponse, PageResponse)
│   ├── security/             # JWT (JwtTokenProvider, JwtAuthenticationFilter)
│   └── tenant/               # 多租戶 (TenantContext, TenantFilter)
├── controller/                # 控制器 (33 個)
│   ├── admin/                # 超管 API (4 個)
│   ├── auth/                 # 認證 API (1 個)
│   ├── line/                 # LINE Webhook + 診斷 (2 個)
│   ├── page/                 # 頁面路由 (3 個)
│   └── tenant/               # 店家 API (23 個)
├── service/                   # 服務層 (38 個)
│   ├── admin/                # 超管服務
│   ├── line/                 # LINE 相關
│   ├── notification/         # 通知服務 (Email, SSE, SMS)
│   ├── payment/              # 金流服務 (ECPay)
│   └── export/               # 匯出服務 (Excel, PDF)
├── scheduler/                 # 排程任務 (5 個)
├── repository/                # 資料存取層 (25 個)
├── entity/                    # 資料庫實體 (25 個)
│   ├── system/               # 系統實體 (含 Payment, SmsLog)
│   ├── staff/                # 員工實體
│   ├── marketing/            # 行銷實體 (含 MarketingPush)
│   └── tenant/               # 租戶實體
├── dto/                       # 資料傳輸物件 (76+ 個)
│   ├── request/              # 請求 DTO
│   └── response/             # 回應 DTO
├── enums/                     # 列舉 (28 個)
└── mapper/                    # 轉換器
```

---

## 命名規範

| 類型 | 格式 | 範例 |
|------|------|------|
| Entity | 單數名詞 | `Booking`, `Customer` |
| Repository | XxxRepository | `BookingRepository` |
| Service | XxxService | `BookingService` |
| Controller | XxxController | `BookingController` |
| Request DTO | CreateXxxRequest / UpdateXxxRequest | `CreateBookingRequest` |
| Response DTO | XxxResponse | `BookingResponse` |
| Enum 類別 | 單數名詞 | `BookingStatus` |
| Enum 值 | 大寫底線 | `PENDING_CONFIRMATION` |

---

## 關鍵規則

1. **多租戶**：所有查詢必須包含 `tenant_id`，用 `TenantContext.getTenantId()`
2. **軟刪除**：用 `deleted_at`，查詢加 `DeletedAtIsNull`
3. **API 回應**：統一用 `ApiResponse.ok(data)` 包裝
4. **註解**：繁體中文，寫在程式碼上方
5. **加密**：LINE Token 用 AES-256-GCM 加密儲存

---

## 認證 API

```
POST /api/auth/login              # 統一登入 (type: ADMIN/TENANT)
POST /api/auth/admin/login        # 超管登入
POST /api/auth/tenant/login       # 店家登入
POST /api/auth/tenant/register    # 店家註冊
POST /api/auth/forgot-password    # 忘記密碼
POST /api/auth/reset-password     # 重設密碼
POST /api/auth/change-password    # 更改密碼
POST /api/auth/refresh            # 刷新 Token
POST /api/auth/logout             # 登出
```

預設管理員：`admin` / `admin123`

---

## API 端點總覽

### 超級管理 API (`/api/admin/`)

| 資源 | 端點 |
|------|------|
| 租戶 | `GET/POST /tenants`, `GET/PUT/DELETE /tenants/{id}` |
| 租戶狀態 | `PUT /tenants/{id}/status`, `POST /tenants/{id}/activate\|suspend\|freeze` |
| 租戶功能 | `POST /tenants/{id}/features/{code}/enable\|disable\|suspend\|unsuspend` |
| 租戶點數 | `POST /tenants/{id}/points/add`, `GET /tenants/{id}/topups` |
| 功能定義 | `GET /features`, `GET /features/free\|paid`, `PUT /features/{code}` |
| 儲值審核 | `GET /point-topups`, `GET /point-topups/pending`, `POST /point-topups/{id}/approve\|reject` |
| 儀表板 | `GET /dashboard` |

### 店家 API (`/api/`)

| 資源 | 端點 |
|------|------|
| 預約 | `GET/POST /bookings`, `GET/PUT /bookings/{id}`, `POST /bookings/{id}/confirm\|complete\|cancel\|no-show` |
| 預約行事曆 | `GET /bookings/calendar`, `GET /bookings/staff/{staffId}/date/{date}` |
| 顧客 | `GET/POST /customers`, `GET/PUT/DELETE /customers/{id}` |
| 顧客操作 | `POST /customers/{id}/points/add\|deduct`, `POST /customers/{id}/block\|unblock` |
| 員工 | `GET/POST /staff`, `GET/PUT/DELETE /staff/{id}`, `GET /staff/bookable` |
| 員工排班 | `GET/PUT /staff/{id}/schedule` |
| 員工請假 | `GET/POST /staff/{id}/leaves`, `DELETE /staff/{id}/leaves/{leaveId}` |
| 服務 | `GET/POST /services`, `GET/PUT/DELETE /services/{id}`, `GET /services/bookable` |
| 服務分類 | `GET/POST /service-categories`, `GET/PUT/DELETE /service-categories/{id}` |
| 商品 | `GET/POST /products`, `GET/PUT/DELETE /products/{id}` |
| 商品操作 | `POST /products/{id}/on-sale\|off-shelf\|adjust-stock` |
| 票券 | `GET/POST /coupons`, `GET/PUT/DELETE /coupons/{id}` |
| 票券操作 | `POST /coupons/{id}/publish\|pause\|resume\|issue`, `POST /coupons/instances/{id}/redeem` |
| 行銷活動 | `GET/POST /campaigns`, `GET/PUT/DELETE /campaigns/{id}` |
| 活動操作 | `POST /campaigns/{id}/publish\|pause\|resume\|end` |
| 會員等級 | `GET/POST /membership-levels`, `GET/PUT/DELETE /membership-levels/{id}` |
| 報表 | `GET /reports/dashboard\|summary\|today\|weekly\|monthly\|daily\|top-services\|top-staff\|hourly\|advanced` |
| 設定 | `GET/PUT /settings`, `GET /settings/setup-status` |
| LINE 設定 | `GET/PUT /settings/line`, `POST /settings/line/activate\|deactivate\|test` |
| Rich Menu | `GET/POST/DELETE /settings/line/rich-menu`, `POST /settings/line/rich-menu/create\|upload-image` |
| 點數 | `GET /points/balance`, `POST /points/topup`, `GET /points/topups\|transactions` |
| 功能商店 | `GET /feature-store`, `GET /feature-store/{code}`, `POST /feature-store/{code}/apply\|cancel` |
| 行銷推播 | `GET/POST /marketing/pushes`, `POST /marketing/pushes/{id}/send`, `DELETE /marketing/pushes/{id}` |
| 報表匯出 | `GET /export/bookings/excel\|pdf`, `GET /export/reports/excel\|pdf`, `GET /export/customers/excel` |
| 員工行事曆 | `GET /staff/calendar` |
| 支付 | `GET/POST /payments`, `GET /payments/{id}`, `GET /payments/order/{merchantTradeNo}` |

### LINE Webhook

```
POST /api/line/webhook/{tenantCode}
```

### 即時通知 (SSE)

```
GET /api/notifications/stream   # SSE 訂閱（店家後台即時通知）
```

**SSE 事件類型：**
| 事件 | 觸發時機 | 說明 |
|------|----------|------|
| `connected` | 連線建立 | 確認 SSE 連線成功 |
| `new_booking` | 新預約建立 | 顯示通知、播放音效、刷新列表 |
| `booking_updated` | 預約編輯 | 刷新列表 |
| `booking_status_changed` | 狀態變更 | 確認/完成/爽約時觸發 |
| `booking_cancelled` | 預約取消 | 刷新列表 |
| `new_product_order` | 新商品訂單 | 顯示通知、播放音效、刷新訂單列表 |
| `product_order_status_changed` | 訂單狀態變更 | 確認/完成/取消時觸發，同時推送 LINE 通知 |
| `coupon_claimed` | 票券領取 | 顯示通知、播放音效、刷新票券列表 |
| `new_customer` | 新顧客建立 | 顯示通知、播放音效、刷新顧客列表 |

### 公開頁面

```
GET  /booking/cancel/{token}     # 顧客自助取消頁面
POST /booking/cancel/{token}     # 執行取消預約
POST /api/payments/callback      # ECPay 付款結果回調
```

---

## 排程任務

| 排程 | Cron | 說明 | 需訂閱功能 |
|------|------|------|-----------|
| 預約提醒 | `0 0 * * * *` | 每小時檢查並發送 LINE/SMS 提醒 | AUTO_REMINDER |
| 額度重置 | `0 5 0 1 * *` | 每月1日重置推送/SMS 額度 | - |
| 行銷推播 | `0 * * * * *` | 每分鐘檢查排程推播任務 | - |
| 生日祝福 | `0 0 9 * * *` | 每日 9:00 發送生日祝福 | AUTO_BIRTHDAY |
| 顧客喚回 | `0 0 14 * * *` | 每日 14:00 發送久未到訪顧客喚回通知 | AUTO_RECALL |

設定於 `application.yml`：
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

## 頁面路由

### 超級管理後台 (`/admin/`)

| 路徑 | 頁面 |
|------|------|
| /admin/login | 登入頁 |
| /admin/dashboard | 儀表板 |
| /admin/tenants | 店家列表 |
| /admin/tenants/{id} | 店家詳情 |
| /admin/point-topups | 儲值審核 |
| /admin/features | 功能管理 |

### 店家後台 (`/tenant/`)

| 路徑 | 頁面 |
|------|------|
| /tenant/login | 登入頁 |
| /tenant/register | 註冊頁 |
| /tenant/forgot-password | 忘記密碼 |
| /tenant/reset-password | 重設密碼 |
| /tenant/dashboard | 儀表板 |
| /tenant/bookings | 預約管理 |
| /tenant/calendar | 行事曆 |
| /tenant/reports | 營運報表 |
| /tenant/customers | 顧客列表 |
| /tenant/customers/{id} | 顧客詳情 |
| /tenant/staff | 員工管理 |
| /tenant/services | 服務管理 |
| /tenant/products | 商品管理 |
| /tenant/inventory | 庫存異動 |
| /tenant/product-orders | 商品訂單 |
| /tenant/coupons | 票券管理 |
| /tenant/campaigns | 行銷活動 |
| /tenant/marketing | 行銷推播 |
| /tenant/membership-levels | 會員等級 |
| /tenant/settings | 店家設定 |
| /tenant/line-settings | LINE 設定 |
| /tenant/feature-store | 功能商店 |
| /tenant/points | 點數管理 |

---

## 資料表

| 類別 | 表名 |
|------|------|
| 租戶 | `tenants`, `admin_users` |
| 員工 | `staffs`, `staff_schedules`, `staff_leaves` |
| 服務 | `service_categories`, `service_items` |
| 預約 | `bookings`, `booking_histories` |
| 顧客 | `customers`, `membership_levels`, `point_transactions` |
| 商品 | `products`, `product_orders`, `inventory_logs` |
| 行銷 | `coupons`, `coupon_instances`, `campaigns`, `marketing_pushes` |
| 系統 | `features`, `tenant_features`, `point_topups`, `payments`, `sms_logs` |
| LINE | `tenant_line_configs`, `line_users` |

---

## LINE Bot 功能

### Rich Menu（快捷選單）
底部固定選單，顧客開啟聊天室即可看到：

| 功能 | 說明 |
|------|------|
| 開始預約 | 啟動預約流程 |
| 我的預約 | 查看預約清單 |
| 瀏覽商品 | 瀏覽商品列表 |
| 領取票券 | 查看可領取票券 |
| 會員資訊 | 查看會員資料 |
| 聯絡店家 | 聯絡客服 |

**主題配色**：GREEN（LINE綠）、BLUE（海洋藍）、PURPLE（皇家紫）、ORANGE（日落橘）、DARK（暗黑）

**自訂圖片**：支援任意尺寸 PNG/JPG，系統自動縮放至 2500x843（cover 策略，置中裁切）

**跨平台字型**：Docker 環境安裝 font-wqy-zenhei（文泉驛正黑），確保中文正確顯示

**即時預覽功能**：在 LINE 設定頁面提供手機模擬預覽，可即時看到：
- 主題配色切換效果
- 自訂圖片上傳預覽
- 6 宮格選單佈局

### 主選單（Flex Message）
用戶隨時輸入任何文字都會顯示主選單（Flex Message），包含：
- 開始預約
- 我的預約
- 瀏覽商品
- 領取票券 / 我的票券（並排按鈕）
- 會員資訊

### 返回主選單功能
所有 Flex Message 皆包含「返回主選單」按鈕，方便用戶快速回到主選單：

| 訊息類型 | 按鈕位置 |
|---------|---------|
| 會員資訊 | Footer 底部（垂直排列） |
| 可領取票券列表 | Carousel 末端導航卡片 |
| 我的票券列表 | Carousel 末端導航卡片 |
| 商品列表 | Carousel 末端導航卡片 |
| 預約狀態通知 | Footer 底部 |
| 預約修改通知 | Footer 底部 |
| 預約提醒 | Footer 底部 |

### 預約流程

**有分類時（>= 2 個啟用分類且有服務歸屬）：**
```
IDLE（閒置）
  ↓ 點選「開始預約」
SELECTING_CATEGORY（選擇分類）- Carousel 顯示有服務的分類
  ↓ 選擇分類
SELECTING_SERVICE（選擇服務）- 只顯示該分類下的服務
  ↓ 選擇服務
SELECTING_DATE（選擇日期）- 支援 Carousel 顯示完整可預約天數
  ↓ 選擇日期
SELECTING_STAFF（選擇員工）- 根據日期篩選可服務員工
  ↓ 選擇員工（或不指定）
SELECTING_TIME（選擇時段）
  ↓ 選擇時段
INPUTTING_NOTE（輸入備註）- 可直接輸入文字或點選「跳過」
  ↓ 輸入備註或跳過
CONFIRMING_BOOKING（確認預約）
  ↓ 確認
IDLE（完成，回到閒置）
```

**無分類時（< 2 個分類或服務未歸屬分類）：**
```
IDLE（閒置）
  ↓ 點選「開始預約」
SELECTING_SERVICE（選擇服務）- 顯示全部服務
  ↓ 選擇服務
SELECTING_DATE → SELECTING_STAFF → SELECTING_TIME → INPUTTING_NOTE → CONFIRMING_BOOKING → IDLE
```

**分類流程啟動條件：**
- 至少 2 個啟用中的服務分類 **且**
- 至少 2 個分類底下有可預約的服務（ACTIVE + isVisible）
- 不符合條件時自動退回原有 4 步驟流程

**流程說明：**
- 先選日期再選員工，確保顧客只能看到當天有上班且未請假的員工
- 系統會自動過濾：非營業日、請假員工、過去的時段

**備註輸入說明：**
- 選擇時段後，系統會顯示備註輸入提示
- 用戶可以在聊天輸入框直接打字輸入備註內容
- 也可以點選「跳過」按鈕略過備註步驟
- 備註會顯示在預約確認頁面和店家後台

### 其他流程
- 取消預約：`IDLE → CONFIRMING_CANCEL_BOOKING → IDLE`
- 商品購買：`IDLE → BROWSING_PRODUCTS → VIEWING_PRODUCT_DETAIL → SELECTING_QUANTITY → CONFIRMING_PURCHASE → IDLE`
- 票券領取：`IDLE → BROWSING_COUPONS → IDLE`
- 會員資訊：`IDLE → VIEWING_MEMBER_INFO → IDLE`（單一 Bubble Flex Message）

Redis Key: `line:conversation:{tenantId}:{lineUserId}`，TTL: 30 分鐘

### 票券功能（顧客端）

**領取票券流程：**
1. 顧客點選「領取票券」查看可領取的票券列表
2. 點選要領取的票券
3. 系統檢查每人限領數量
4. 領取成功後顯示票券代碼和使用說明

**我的票券功能：**
- 第一張卡片：使用說明（3 步驟 + 注意事項）
- 後續卡片：每張已領取的票券
  - 可使用：大字顯示代碼 + 使用提示
  - 已使用/已過期：灰色顯示

**票券核銷流程（店家端）：**
1. 顧客消費時告知要使用票券
2. 顧客出示票券代碼（LINE 聊天室 → 我的票券）
3. 店家在後台「票券管理」點選「核銷票券」
4. 輸入代碼完成核銷

### 會員點數功能

**會員資訊顯示內容（Flex Message 單一 Bubble）：**
- 👤 會員名稱（Header 區塊）
- ⭐ 會員等級標籤
- 💰 點數餘額（醒目顯示）
- 📅 累計預約次數
- 💳 累計消費（如有）
- 操作按鈕：開始預約、我的票券

### 顧客點數累積設定

店家可在「店家設定 → 點數設定」自訂點數累積規則：

| 設定項目 | 說明 | 預設值 |
|---------|------|--------|
| `pointEarnEnabled` | 是否啟用點數累積 | true |
| `pointEarnRate` | 每消費多少元得 1 點 | 10 |
| `pointRoundMode` | 取整方式 (FLOOR/ROUND/CEIL) | FLOOR |

**計算範例**（比例 10，消費 NT$95）：
- FLOOR（無條件捨去）：9 點
- ROUND（四捨五入）：10 點
- CEIL（無條件進位）：10 點

**注意**：需訂閱 `POINT_SYSTEM` 功能才會自動集點

**點數獲得方式：**
- 完成預約自動累積
- 消費金額累積
- 參與活動獎勵
- 生日禮點數

**點數使用流程：**
1. 顧客消費時告知要折抵點數
2. 店家在後台「顧客詳情」頁面手動扣除點數
3. 點數交易會記錄在 point_transactions 表

**票券核銷通知：**
- 店家核銷票券後，顧客會收到 LINE 推播通知
- 通知內容包含票券名稱、折扣金額、核銷時間

---

## 員工管理功能

### 排班設定
- 每週 7 天的上班設定
- 每天可設：上班開關、開始/結束時間、休息時段

### 請假管理
- 支援特定日期請假（事假、病假、休假、特休、其他）
- 快速選擇：明天、下週一~五、本週末、下週末
- 請假原因備註

---

## 報表匯出功能

支援 Excel 和 PDF 格式匯出：

| 匯出類型 | API | 說明 |
|---------|-----|------|
| 預約記錄 Excel | `GET /api/export/bookings/excel` | 依日期範圍和狀態篩選 |
| 預約記錄 PDF | `GET /api/export/bookings/pdf` | 同上 |
| 營運報表 Excel | `GET /api/export/reports/excel` | 統計摘要報表 |
| 營運報表 PDF | `GET /api/export/reports/pdf` | 同上 |
| 顧客名單 Excel | `GET /api/export/customers/excel` | 全部顧客資料 |

---

## 郵件通知功能

使用 Resend HTTP API 發送郵件（Railway 封鎖 SMTP port 587/465，無法使用 Gmail SMTP）：

```yaml
app:
  email:
    from: ${MAIL_FROM:onboarding@resend.dev}
    from-name: 預約平台
    resend-api-key: ${RESEND_API_KEY}
```

**郵件類型**：
- 密碼重設（忘記密碼）
- 歡迎郵件（店家註冊）
- 密碼變更通知

**Resend 免費方案**：3000 封/月，預設寄件人 `onboarding@resend.dev`，可綁定自訂網域

---

## SMS 通知功能

支援三竹簡訊（Mitake）：

```yaml
sms:
  enabled: true
  provider: mitake
  mitake:
    username: ${SMS_USERNAME}
    password: ${SMS_PASSWORD}
```

**SMS 類型**：
- 預約確認（BOOKING_CONFIRMATION）
- 預約提醒（BOOKING_REMINDER）
- 預約取消（BOOKING_CANCELLED）
- 行銷推播（MARKETING）

---

## ECPay 金流整合

```yaml
ecpay:
  merchant-id: ${ECPAY_MERCHANT_ID}
  hash-key: ${ECPAY_HASH_KEY}
  hash-iv: ${ECPAY_HASH_IV}
```

**支援付款方式**：信用卡、ATM、超商代碼、超商條碼

---

## 多語系支援

支援語言：繁體中文（zh_TW）、簡體中文（zh_CN）、英文（en）

**切換方式**：URL 參數 `?lang=zh_TW`

**檔案位置**：
- `messages.properties` - 預設（繁中）
- `messages_zh_CN.properties` - 簡中
- `messages_en.properties` - 英文

---

## 預約衝突檢查

預約建立時自動檢查：
1. 員工全天請假
2. 員工半天假時段重疊
3. 既有預約時段衝突
4. 預約緩衝時間（`bookingBufferMinutes` 設定）

---

## 功能訂閱與側邊欄

側邊欄選單項目根據功能訂閱狀態動態顯示/隱藏。

### 機制說明

1. 側邊欄項目使用 `data-feature` 屬性標記所需功能
2. `tenant.js` 載入時呼叫 `/api/feature-store` 取得功能訂閱狀態
3. 未訂閱的功能對應的選單項目會被隱藏

### 功能與選單對應

| 功能代碼 | 選單項目 | 類型 | 每月點數 |
|---------|---------|------|---------|
| `BASIC_REPORT` | 營運報表 | 免費 | 0 |
| `PRODUCT_SALES` | 商品管理 | 付費 | 400 |
| `COUPON_SYSTEM` | 票券管理 | 付費 | 500 |

### 免費功能（預設顯示）

| 功能代碼 | 說明 |
|---------|------|
| `BASIC_BOOKING` | 基本預約功能 |
| `BASIC_CUSTOMER` | 基本顧客管理 |
| `BASIC_STAFF` | 基本員工管理（限3位） |
| `BASIC_SERVICE` | 基本服務項目管理 |
| `BASIC_REPORT` | 基本營運報表 |

### 付費功能

| 功能代碼 | 說明 | 每月點數 |
|---------|------|---------|
| `UNLIMITED_STAFF` | 無限員工數量 | 500 |
| `ADVANCED_REPORT` | 進階營運分析報表 | 300 |
| `COUPON_SYSTEM` | 票券系統 | 500 |
| `MEMBERSHIP_SYSTEM` | 會員等級系統 | 400 |
| `POINT_SYSTEM` | 顧客集點獎勵 | 300 |
| `PRODUCT_SALES` | 商品銷售功能 | 400 |
| `AUTO_REMINDER` | 自動預約提醒 | 200 |
| `AI_ASSISTANT` | AI 智慧客服 | 1000 |

### 測試方式

1. 登入店家後台
2. 進入「功能商店」頁面
3. 訂閱 `COUPON_SYSTEM` 或 `PRODUCT_SALES` 功能
4. 重新整理頁面，確認對應選單項目出現
5. 取消訂閱後，選單項目應該隱藏

---

## 啟動指令

```bash
# 開發環境
mvn spring-boot:run

# 生產環境 (Railway)
mvn spring-boot:run -Dspring.profiles.active=prod
```

**本機測試**：
- Admin: http://localhost:8080/admin/login (admin / admin123)
- Tenant: http://localhost:8080/tenant/login

---

## 環境變數

| 變數 | 說明 | 預設值 |
|------|------|--------|
| `DATABASE_URL` | PostgreSQL 連線 | - |
| `REDIS_URL` | Redis 連線 | - |
| `JWT_SECRET` | JWT 密鑰 | - |
| `ENCRYPTION_SECRET_KEY` | AES 加密金鑰 | - |
| `LINE_CHANNEL_TOKEN` | LINE Channel Token | - |
| `LINE_CHANNEL_SECRET` | LINE Channel Secret | - |
| `SMS_ENABLED` | 啟用 SMS | false |
| `SMS_PROVIDER` | SMS 供應商 | mitake |
| `SMS_USERNAME` | SMS 帳號 | - |
| `SMS_PASSWORD` | SMS 密碼 | - |
| `ECPAY_MERCHANT_ID` | ECPay 商店代號 | - |
| `ECPAY_HASH_KEY` | ECPay HashKey | - |
| `ECPAY_HASH_IV` | ECPay HashIV | - |
| `RESEND_API_KEY` | Resend 郵件 API Key | - |
| `MAIL_FROM` | 郵件寄件人地址 | onboarding@resend.dev |

---

## LINE 設定疑難排解

### 常見問題

| 問題 | 原因 | 解決方案 |
|------|------|----------|
| **Bot 完全沒有回應** | LINE OA 自動回應攔截訊息 | 必須關閉 LINE Official Account Manager 的自動回應（見下方詳細步驟） |
| Rich Menu 顯示亂碼 (□□□□) | Docker 環境缺少中文字型 | 已在 Dockerfile 安裝 font-wqy-zenhei |
| Rich Menu 電腦版沒顯示 | LINE 平台限制 | Rich Menu 僅支援手機版 LINE |
| 401 UNAUTHORIZED | Token 無效或過期 | 重新產生 Channel Access Token |
| Bot ID 顯示雙重 @ | HTML 多餘圖標 | 已修正 line-settings.html |
| Bot 頭像無法顯示 | LINE API 未設定頭像 | 在 LINE Official Account Manager 設定頭像 |

### LINE Bot 不回應訊息的完整檢查清單

**如果 LINE Bot 沒有回應訊息，請依序檢查以下項目：**

#### 1. LINE Developers Console 設定
- 前往 [LINE Developers Console](https://developers.line.biz/) → 您的 Channel → Messaging API
- 確認 **Webhook URL** 設定正確：`https://booking-platform-production-1e08.up.railway.app/api/line/webhook/{tenantCode}`
- 確認 **Use webhook** 為 **ON**（開啟）
- 點擊 **Verify** 按鈕測試連線，必須顯示 **Success**

#### 2. LINE Official Account Manager 設定（最常見問題）
- 前往 [LINE Official Account Manager](https://manager.line.biz/)
- 進入 **設定** → **回應設定**
- **回應方式** 必須設為 **「手動聊天」**（不要勾選「自動回應訊息」）
- **非回應時間** 也建議設為 **「手動聊天」**
- 確認 **Webhook** 區塊顯示為啟用狀態

#### 3. 店家後台 LINE 設定
- 登入店家後台 → LINE 設定
- 確認 Channel ID、Channel Secret、Channel Access Token 都已填入
- 點擊「連線測試」確認顯示成功
- 狀態應顯示為「運作中 (ACTIVE)」

### LINE 設定流程

1. 前往 [LINE Developers Console](https://developers.line.biz/)
2. 建立或選擇 Provider 和 Messaging API Channel
3. 複製 Channel ID、Channel Secret、Channel Access Token
4. 在店家後台 LINE 設定頁面填入並儲存
5. 設定 Webhook URL 到 LINE Developers Console
6. **重要**：關閉 LINE Official Account Manager 的自動回應功能（見下方）
7. 在 LINE Developers Console 點擊 Verify 確認連線成功

### 關閉 LINE Official Account 自動回應（必要步驟）

> ⚠️ **這是最常見的問題原因！** 如果不關閉自動回應，LINE 會攔截所有訊息，Webhook 完全收不到。

1. 前往 [LINE Official Account Manager](https://manager.line.biz/)
2. 進入您的官方帳號 → **設定** → **回應設定**
3. 在「**回應方式**」區塊：
   - 將「回應時間」改為只有 **「手動聊天」**
   - 將「非回應時間」也改為 **「手動聊天」**
   - **不要**勾選「自動回應訊息」
4. 確保「**Webhook**」顯示為啟用狀態

### 技術說明

- LINE 的 replyToken 只有約 **30 秒**有效期
- `LineMessageService.reply()` 方法**不使用 @Async**，確保在有效期內發送
- `LineWebhookService.processWebhook()` 使用 @Async 處理，但內部的 reply 是同步的
- 簽名驗證使用 Channel Secret，確保 Webhook 請求來自 LINE 平台

---

## E2E 測試

使用 Playwright 進行端對端測試：

```bash
# 執行所有測試
npx playwright test

# 執行特定測試
npx playwright test tests/06-sse-notifications.spec.ts

# 列出所有測試
npx playwright test --list
```

**測試套件 (960 tests)：**

| 檔案 | 說明 | 測試數 |
|------|------|--------|
| `00-setup.spec.ts` | 環境檢查 | 5 |
| `01-auth.spec.ts` | 認證功能 | 11 |
| `02-admin.spec.ts` | 超管後台基本測試 | 12 |
| `03-tenant-dashboard.spec.ts` | 店家後台基本測試 | 8 |
| `04-tenant-features.spec.ts` | API 測試 | 17 |
| `05-feature-store.spec.ts` | 功能商店 | 10 |
| `06-sse-notifications.spec.ts` | SSE 即時通知 | 15 |
| `07-admin-crud.spec.ts` | 超管 CRUD 完整測試 | 29 |
| `08-tenant-booking.spec.ts` | 預約管理完整測試 | 32 |
| `09-tenant-pages.spec.ts` | 店家後台所有頁面測試 | 33 |
| `09-tenant-customer.spec.ts` | 顧客管理測試 | 35 |
| `10-admin-pages.spec.ts` | 超管後台所有頁面測試 | 13 |
| `10-tenant-staff-service.spec.ts` | 員工&服務管理測試 | - |
| `11-public-pages.spec.ts` | 公開頁面測試 | 24 |
| `11-tenant-product-coupon.spec.ts` | 商品&票券管理測試 | - |
| `12-tenant-campaign-marketing.spec.ts` | 行銷活動&推播測試 | 26 |
| `13-tenant-settings.spec.ts` | 設定頁面測試 | 28 |
| `14-tenant-reports.spec.ts` | 報表&匯出測試 | 32 |
| `15-line-bot.spec.ts` | LINE Bot 測試 | 19 |
| `16-sidebar-feature-visibility.spec.ts` | 側邊欄功能訂閱測試 | 22 |
| `17-comprehensive-forms.spec.ts` | 表單驗證測試 | 25 |
| `18-feature-store-details.spec.ts` | 功能商店詳情測試 | - |
| `19-seo-pages.spec.ts` | SEO 頁面測試 | - |
| `19-ai-menu-logic.spec.ts` | AI 客服選單邏輯測試 | 2 |
| `20-f12-console-check.spec.ts` | F12 Console 全頁面錯誤檢測 | 40 |
| `21-notification-system.spec.ts` | 通知系統補齊+顧客刪除按鈕測試 | 25 |
| `22-full-coverage-ui.spec.ts` | 全覆蓋 UI 測試 | 94 |
| `23-deep-interaction-test.spec.ts` | 深度互動測試 | 33 |
| `24-onboarding-setup-status.spec.ts` | 新手引導系統&側邊欄設定狀態測試 | 50 |
| `25-page-health-validator.spec.ts` | 頁面健康驗證（載入完成、無卡住指標） | 22 |
| `26-api-contract-validator.spec.ts` | 前後端 API 契約驗證（欄位名匹配） | 22 |
| `27-line-category-selection.spec.ts` | LINE Bot 服務分類選擇功能測試 | 26 |
| `99-comprehensive-bug-hunt.spec.ts` | 全面 BUG 搜尋測試 | 33 |

**測試涵蓋範圍：**

- 所有超管頁面（儀表板、店家管理、功能管理、儲值審核）
- 所有店家頁面（18 頁面：儀表板、預約管理、行事曆、報表、顧客、員工、服務、商品、庫存異動、商品訂單、票券、行銷活動、行銷推播、設定、LINE設定、功能商店、點數管理、會員等級）
- 所有公開頁面（登入、註冊、忘記密碼、顧客自助取消預約）
- 所有 SEO 頁面（首頁、功能介紹、價格方案、FAQ、行業頁面、法律頁面）
- 所有 API 端點（19 個主要 API 完整驗證）
- 所有表單欄位和按鈕（9 個新增按鈕 Modal 測試）
- **F12 Console 自動監控**（所有 UI 測試自動檢測 JS 錯誤、HTTP 500、console.error）
- JavaScript 錯誤檢測（SyntaxError、ReferenceError、TypeError）
- HTTP 錯誤檢測（400/500 回應監控）
- 「載入失敗」文字檢測
- 功能訂閱與側邊欄顯示控制
- 新手引導卡片（顯示/步驟/進度條/關閉/導航/持久化）
- 側邊欄進度環（SVG/百分比/完成隱藏）
- 側邊欄注意圓點（脈動動畫/next-step）
- 側邊欄店家 footer（店家名稱/跨頁面載入）
- 設定完成狀態 API（欄位驗證/邊界值）
- LINE Bot 對話狀態和訊息格式
- LINE Bot 服務分類選擇流程（狀態機、Postback、goBack、邊界情況）
- Excel/PDF 匯出功能
- 靜態資源（CSS/JS）載入
- 顧客點數交易記錄 API
- 報表摘要統計（回頭客、服務營收）
- 超管儀表板金額計算
- SEO 資源驗證（robots.txt、sitemap.xml、OG 圖片、Meta Tags）
- **頁面健康驗證**（載入完成檢測、卡住的「載入中」、孤立 spinner、載入遮罩）
- **API 契約驗證**（前端欄位名 vs 後端 DTO 欄位名匹配，防止 400 錯誤）

**靜態分析腳本（scripts/audit-frontend-apis.js）：**

- 不需啟動伺服器，直接掃描 HTML 原始碼
- `STALE_LOADING`：檢查 HTML 有「載入中」文字的元素是否有對應 JS DOM 操作
- `ORPHAN_SPINNER`：檢查 spinner 容器是否有 JS 代碼去移除/替換
- `FIELD_MISMATCH`：比對前端 api.post/put 送出的欄位名與後端 Java DTO 欄位名，找出不匹配
- 執行方式：`node scripts/audit-frontend-apis.js`

**F12 Console 自動監控（fixtures.ts）：**

- 所有 UI 測試檔案透過 `import { test, expect } from './fixtures'` 自動啟用 F12 監控
- 監控三類錯誤：`pageerror`（JS 執行錯誤）、HTTP 500+（伺服器錯誤）、`console.error`（過濾瀏覽器雜訊）
- 任何未過濾的 F12 錯誤會讓測試直接失敗
- 過濾清單包含：瀏覽器內建訊息（favicon、net::ERR_、SSE 等）和應用程式預期的 API 錯誤處理（handleResponse、登入失敗、換頁中斷等）
- 共 21 個 UI 測試檔案已整合此 fixture

**測試基礎設施注意事項：**

- 使用 `domcontentloaded` 而非 `networkidle` 等待頁面載入
- 原因：SSE 連線會保持網路活躍，導致 `networkidle` 永遠無法觸發
- 所有測試檔案已更新使用正確的等待策略

**測試安全注意事項：**

- LINE 設定測試（`13-tenant-settings.spec.ts`）**不會覆蓋**真實的 LINE credentials
- 測試只會更新訊息設定（welcomeMessage、defaultReply），不動 channelId/channelSecret/channelAccessToken
- 啟用/停用測試會**確保最終保持啟用狀態**，避免影響生產環境

---

## 測試規範與最佳實踐（可跨專案複用）

> 本節整理適用於任何 Web 全端專案的測試策略與規範。

### 測試金字塔

```
        /  E2E  \          ← 少量，驗證關鍵使用者流程
       / 契約測試 \         ← 前後端介面對齊
      / 整合測試    \       ← API 層級，Service + DB
     / 單元測試       \     ← 大量，純邏輯函式
    ──────────────────
     靜態分析 (最底層)      ← 零成本，開發時即時回饋
```

### 第 0 層：靜態分析（開發時）

**原則**：能在編譯/掃描階段抓到的 bug，絕不留到執行時。

| 檢查類型 | 工具/方法 | 抓什麼 |
|---------|----------|--------|
| 型別檢查 | TypeScript / Java Compiler | 型別錯誤、未定義變數 |
| Lint | ESLint / Checkstyle | 程式碼風格、常見反模式 |
| 前後端欄位匹配 | `FIELD_MISMATCH` 靜態掃描 | 前端送的欄位名 vs 後端 DTO 欄位名不一致 |
| 孤立載入狀態 | `STALE_LOADING` / `ORPHAN_SPINNER` | HTML 有載入中 UI 但 JS 沒替換 |
| 安全掃描 | OWASP Dependency Check | 已知漏洞套件 |

**FIELD_MISMATCH 靜態分析實作模式**（適用於任何前後端分離專案）：

```
步驟：
1. 掃描前端原始碼，提取所有 API 呼叫（URL + HTTP Method + 送出的欄位名）
2. 建立「API URL → 後端 DTO 類別名」對應表（手動維護，~20-40 筆）
3. 解析後端 DTO 原始碼，提取欄位名和必填標記
4. 比對：
   - 前端送了但 DTO 沒有的欄位 → 資料被靜默丟棄（隱性 bug）
   - DTO 必填但前端沒送的欄位 → 必定 400 錯誤
5. 輸出報告，CI 中 exit code 1 阻擋合併
```

**關鍵技術細節**：
- 解析 ES6 shorthand：`{ name, phone }` 和 `{ name: val, phone: val }` 都要能提取 key
- 處理 JS 行內註解：`field: value, // 註解` 不能影響下一個欄位的解析
- 支援巢狀物件和陣列：追蹤 `{}[]()` 深度，只在深度 0 切割逗號
- 處理模板字串 URL：`` `/api/xxx/${id}` `` 清理為 `/api/xxx/`
- 子資源排除：`PUT /api/staff/{id}/schedule` 不匹配 `PUT /api/staff/`

### 第 1 層：API 契約測試

**原則**：前後端之間的「接口」是最容易出錯的地方，必須有專門的防護。

**契約測試設計模式**：

```typescript
// 對每個 POST/PUT API，用正確格式的 payload 打一次
// 只驗證「欄位名正確，不會 400」，不驗證業務邏輯
test('POST /api/bookings — 欄位名稱正確', async ({ request }) => {
    const res = await request.post('/api/bookings', {
        headers: { Authorization: `Bearer ${token}` },
        data: {
            customerId: existingId,
            serviceItemId: existingId,
            bookingDate: '2099-12-31',   // 用未來日期避免業務驗證
            startTime: '10:00',
            customerNote: '契約測試'
        }
    });
    // 400 = 欄位名或格式錯誤（測試失敗）
    // 200/201 = 成功
    // 409/422/403/404 = 業務錯誤（代表 DTO 解析成功，測試通過）
    expect(res.status()).not.toBe(400);
});
```

**測試資料策略**：

| 策略 | 說明 | 範例 |
|------|------|------|
| 用真實 ID | `beforeAll` 用 GET API 取得現有資料的 ID | `GET /api/customers?size=1` |
| 用未來日期 | 避免「日期已過」的業務驗證 | `bookingDate: '2099-12-31'` |
| 用錯誤密碼 | 驗證欄位名但不真的改密碼 | `currentPassword: 'wrong'` |
| 允許衝突 | 409（名稱重複）代表 DTO 解析成功 | 新增同名資料 |

### 第 2 層：E2E UI 測試

**原則**：模擬真實使用者操作，驗證完整的使用者流程。

**測試分類與優先級**：

| 優先級 | 類別 | 說明 | 範例 |
|--------|------|------|------|
| P0 | 核心流程 | 使用者不能完成 = 業務中斷 | 登入、建立預約、結帳 |
| P1 | CRUD 操作 | 每個資源的增刪改查 | 顧客新增/編輯/刪除 |
| P2 | 頁面載入 | 每個頁面能正常打開 | 所有後台頁面 |
| P3 | 邊界情況 | 錯誤處理、權限 | 未登入訪問、無效輸入 |
| P4 | 視覺回歸 | UI 元素位置和顯示 | 側邊欄、RWD |

**Playwright 最佳實踐**：

```typescript
// 1. 用 domcontentloaded 而非 networkidle（SSE/WebSocket 專案必備）
await page.waitForLoadState('domcontentloaded');

// 2. 用 request context 做 API 測試（不走 UI，快 10 倍）
test('API 測試', async ({ request }) => {
    const res = await request.post('/api/xxx', { data: {...} });
});

// 3. 用 fixture 自動注入橫切關注點（F12 Console 監控）
export const test = base.extend({
    page: async ({ page }, use) => {
        const errors = [];
        page.on('pageerror', e => errors.push(e.message));
        page.on('response', r => { if (r.status() >= 500) errors.push(r.url()); });
        await use(page);
        if (errors.length > 0) throw new Error(`F12 錯誤: ${errors.join(', ')}`);
    },
});

// 4. 避免硬等待，用明確條件等待
await page.waitForSelector('#data-table tbody tr'); // 好
await page.waitForTimeout(3000);                     // 差（偶爾才需要）

// 5. 測試命名：動詞 + 目標 + 預期
test('建立預約 — 選擇服務後顯示員工列表', ...);
test('刪除顧客 — 確認對話框顯示後執行刪除', ...);
```

**F12 Console 自動監控模式**（推薦所有 UI 測試都啟用）：

```
監控三類錯誤：
├── pageerror — JS 執行錯誤（永遠是 bug）
├── HTTP 500+ — 伺服器錯誤（永遠是 bug）
└── console.error — 過濾雜訊後的錯誤

過濾清單（這些不算 bug）：
├── 瀏覽器內建：favicon 404、net::ERR_、ResizeObserver、AbortError
├── SSE/WebSocket：stream 斷線、chunked encoding
└── 應用程式預期：API 錯誤處理器 log、登入失敗、Token 過期
```

### 第 3 層：頁面健康驗證

**原則**：頁面「能打開」不夠，還要「載入完成」。

**檢查項目**：

| 症狀 | 檢測方法 | 原因 |
|------|---------|------|
| 永遠顯示「載入中」 | 檢查是否有 `載入中` 文字在 5 秒後仍存在 | API 呼叫失敗但沒有錯誤處理 |
| Spinner 不消失 | 檢查 `.spinner-border` 是否 5 秒後仍可見 | 非同步操作沒有 finally 區塊 |
| 載入遮罩蓋住頁面 | 檢查 `.loading-overlay` opacity | 遮罩的 hide 邏輯有 bug |
| 空白表格 | 檢查 tbody 是否有 tr 或「無資料」提示 | API 回應格式錯但沒報錯 |

### 測試檔案命名規範

```
tests/
├── 00-setup.spec.ts            ← 環境前置檢查（health check）
├── 01-auth.spec.ts             ← 認證（最基本，其他測試依賴它）
├── 02~06-*.spec.ts             ← 基本功能測試
├── 07~15-*.spec.ts             ← 各模組 CRUD 測試
├── 16~19-*.spec.ts             ← 進階功能測試
├── 20-f12-console-check.spec.ts ← 全頁面 Console 錯誤檢測
├── 21~25-*.spec.ts             ← 專項驗證測試
├── 26-api-contract-validator.spec.ts ← API 契約驗證
├── 27-line-category-selection.spec.ts ← LINE Bot 分類選擇功能
├── 99-comprehensive-bug-hunt.spec.ts ← 全面掃描（壓軸）
├── fixtures.ts                 ← 共用 Fixture（F12 監控）
└── utils/test-helpers.ts       ← 共用輔助函式
```

**編號邏輯**：
- `00-09`：基礎設施和核心功能
- `10-19`：各模組深度測試
- `20-29`：品質驗證和防護網
- `99`：全面掃描（放最後跑）

### 防護網測試清單（適用於所有 Web 專案）

每個 Web 專案至少應有以下「防護網」測試：

| 防護網 | 抓什麼 | 實作方式 |
|--------|--------|---------|
| **頁面載入** | 每個路由都能 200 | 遍歷所有路由，`expect(status).toBe(200)` |
| **F12 無錯誤** | JS Error、HTTP 500 | Fixture 自動監控 |
| **API 契約** | 前後端欄位名不匹配 | 靜態分析 + E2E 契約測試 |
| **表單送出** | 必填欄位驗證、送出成功 | 填入有效資料，點擊送出，檢查結果 |
| **靜態資源** | CSS/JS 載入失敗 | `page.on('response')` 監控 404 |
| **權限保護** | 未登入不能訪問後台 | 直接訪問受保護頁面，檢查跳轉到登入 |
| **RWD 不崩版** | 小螢幕不出現橫向滾動 | `viewport` 設定 375px，檢查 `scrollWidth <= clientWidth` |
| **匯出功能** | Excel/PDF 下載不是 500 | 打 API 檢查 Content-Type 和 status |

### 測試中的生產環境安全守則

```
必須遵守：
├── 不覆蓋真實的第三方 credentials（API Key、Token、Secret）
├── 測試寫入的資料用明確前綴標記（如「契約測試」「E2E測試」）
├── 修改狀態的測試要確保最終狀態恢復（如停用後重新啟用）
├── 不觸發真實的外部通知（LINE 推播、SMS、Email）
└── 不刪除非測試建立的資料

建議做法：
├── 用獨立測試帳號，不用真實使用者帳號
├── 用環境變數區分測試環境和生產環境
├── 有副作用的操作（刪除、發送通知）用 mock 或跳過
└── beforeAll/afterAll 做清理，避免測試資料累積
```

### CI/CD 整合建議

```yaml
# GitHub Actions / GitLab CI 建議流程
stages:
  - lint          # ESLint + 靜態分析
  - build         # 編譯
  - unit-test     # 單元測試
  - contract-test # API 契約驗證
  - e2e-test      # E2E 測試（需要運行中的服務）

# 靜態分析（最快回饋）
lint:
  script:
    - node scripts/audit-frontend-apis.js  # FIELD_MISMATCH + STALE_LOADING

# API 契約測試（中等速度）
contract-test:
  script:
    - npx playwright test tests/26-api-contract-validator.spec.ts

# 完整 E2E（最慢，但最全面）
e2e-test:
  script:
    - npx playwright test
```

---

## 待開發功能 (Pending Features)

### AI 智慧客服方案

| 方案 | 提供者 | 免費額度 | 特點 | 整合難度 |
|------|--------|---------|------|---------|
| **Ollama** | 本地部署 | 無限（自架） | Llama 3.1、Mistral 等開源模型 | ⭐⭐ |
| **Groq** | groq.com | 14,400 req/day | Llama 3.1 70B、Mixtral，超快速度 | ⭐ |
| **Google AI** | Google | 60 req/min | Gemini 1.5 Flash，多模態 | ⭐ |
| **Cloudflare AI** | Cloudflare | 10,000 neurons/day | Workers AI，邊緣運算 | ⭐⭐ |
| **Hugging Face** | HF | 有限免費 | 多種開源模型 | ⭐⭐ |
| **OpenRouter** | openrouter.ai | 免費模型池 | 聚合多家免費 API | ⭐ |

**推薦方案：Groq + Llama 3.1**
- 免費額度足夠中小型店家使用
- 回應速度極快（<500ms）
- 支援繁體中文
- API 介面與 OpenAI 相容

### AI 智慧客服（已實作）

**技術架構**：Groq API + Llama 3.3 70B

**環境變數**（AI 功能預設關閉，需明確啟用）：
```
GROQ_ENABLED=true              # 啟用 AI（預設 false）
GROQ_API_KEY=your-groq-api-key # Groq API Key
GROQ_MODEL=llama-3.3-70b-versatile  # 模型（可選）
```

**功能說明**：
- 當顧客發送非關鍵字訊息時，AI 會自動回答
- AI 會根據店家資訊、服務項目、顧客資料生成回覆
- 回覆後自動附帶主選單，方便顧客繼續操作

**相關檔案**：
| 檔案 | 說明 |
|------|------|
| `GroqConfig.java` | Groq API 設定 |
| `AiAssistantService.java` | AI 服務邏輯 |
| `LineWebhookService.java` | 整合 AI 到 LINE Bot |

### AI 功能擴展規劃

| 功能 | 說明 | 狀態 |
|------|------|------|
| 智慧問答 | 回答常見問題（營業時間、服務價格等） | ✅ 已完成 |
| 預約助手 | 引導顧客完成預約流程 | ⏳ 規劃中 |
| 個人化推薦 | 根據歷史紀錄推薦服務/商品 | ⏳ 規劃中 |
| 自動回覆 | 非營業時間自動回覆 | ⏳ 規劃中 |
| 情緒分析 | 偵測顧客情緒並調整回應 | ⏳ 規劃中 |

### 功能實作狀態

| 功能代碼 | 說明 | 狀態 | 備註 |
|---------|------|------|------|
| `BASIC_BOOKING` | 基本預約功能 | ✅ 已完成 | 免費功能 |
| `BASIC_CUSTOMER` | 基本顧客管理 | ✅ 已完成 | 免費功能 |
| `BASIC_STAFF` | 基本員工管理 | ✅ 已完成 | 免費，限3位員工 |
| `BASIC_SERVICE` | 基本服務項目 | ✅ 已完成 | 免費功能 |
| `BASIC_REPORT` | 基本營運報表 | ✅ 已完成 | 免費功能 |
| `UNLIMITED_STAFF` | 無限員工數量 | ✅ 已完成 | 付費解除限制 |
| `ADVANCED_REPORT` | 進階報表分析 | ✅ 已完成 | 顧客分析、趨勢預測、前端 UI 已完成 |
| `COUPON_SYSTEM` | 票券系統 | ✅ 已完成 | 優惠券發放與核銷 |
| `MEMBERSHIP_SYSTEM` | 會員等級系統 | ✅ 已完成 | 等級設定與升降級 |
| `POINT_SYSTEM` | 顧客集點獎勵 | ✅ 已完成 | 自動集點與兌換 |
| `PRODUCT_SALES` | 商品銷售功能 | ✅ 已完成 | 商品管理與庫存 |
| `AUTO_REMINDER` | 自動預約提醒 | ✅ 已完成 | LINE/SMS 自動提醒 |
| `AUTO_BIRTHDAY` | 自動生日祝福 | ✅ 已完成 | 每日 9:00 發送祝福 |
| `AUTO_RECALL` | 顧客喚回通知 | ✅ 已完成 | 每日 14:00 發送喚回 |
| `EXTRA_PUSH` | 額外推送額度 | ✅ 已完成 | 突破每月推送限制 |
| `ADVANCED_CUSTOMER` | 進階顧客管理 | ✅ 已完成 | 顧客標籤與分群、前端 UI 已完成 |
| `AI_ASSISTANT` | AI 智慧客服 | ✅ 已完成 | Groq Llama 3.3（免費） |
| `MULTI_ACCOUNT` | 多帳號管理 | ❌ 未實作 | 複雜功能，不顯示在功能商店 |
| `MULTI_BRANCH` | 多分店管理 | ❌ 未實作 | 複雜功能，不顯示在功能商店 |

---

## RWD 響應式設計

支援手機、平板、電腦三種裝置。

### 斷點設定

| 裝置 | 斷點 | 側邊欄 | 表格欄位 |
|------|------|--------|----------|
| 手機 | < 576px | 滑出式選單 | 僅顯示關鍵欄位 |
| 平板 | 576-992px | 收合圖示模式 | 隱藏次要欄位 |
| 桌面 | >= 992px | 完整展開 | 顯示全部欄位 |

### 觸控優化

- 按鈕最小尺寸：44px x 44px
- 表單輸入框：最小高度 44px
- 下拉選單項目：適當間距

### CSS 檔案

| 檔案 | 說明 |
|------|------|
| `common.css` | 共用樣式、RWD 工具類 |
| `tenant.css` | 店家後台響應式佈局 |
| `admin.css` | 超管後台響應式佈局 |

### 響應式表格

在小螢幕隱藏次要欄位，使用 Bootstrap 的 `d-none d-md-table-cell` 類別：

```html
<!-- 手機隱藏，平板以上顯示 -->
<th class="d-none d-md-table-cell">服務</th>

<!-- 手機平板隱藏，桌面顯示 -->
<th class="d-none d-lg-table-cell">員工</th>
```

---

## 統計數據

| 項目 | 數量 |
|------|------|
| Controller | 33 |
| Service | 38 |
| Entity | 25 |
| Repository | 25 |
| DTO | 76+ |
| Enum | 28 |
| Scheduler | 5 |
| HTML 頁面 | 51 |
| CSS 檔案 | 3 |
| JS 檔案 | 4 |
| i18n 檔案 | 4 |
| E2E 測試 | 960 |

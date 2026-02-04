# Booking Platform - 多租戶預約平台系統

## 線上環境

| 環境 | URL | 短網址 |
|------|-----|--------|
| 店家後台 | https://booking-platform-production-1e08.up.railway.app/tenant/login | https://is.gd/MzrSX0 |
| 超管後台 | https://booking-platform-production-1e08.up.railway.app/admin/login | https://is.gd/uFDVuc |
| 健康檢查 | https://booking-platform-production-1e08.up.railway.app/health | https://is.gd/3Cm3RC |

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
├── controller/                # 控制器 (29 個)
│   ├── admin/                # 超管 API (4 個)
│   ├── auth/                 # 認證 API (1 個)
│   ├── line/                 # LINE Webhook (1 個)
│   ├── page/                 # 頁面路由 (2 個)
│   └── tenant/               # 店家 API (21 個)
├── service/                   # 服務層 (34 個)
│   ├── admin/                # 超管服務
│   ├── line/                 # LINE 相關
│   ├── notification/         # 通知服務 (Email, SSE, SMS)
│   ├── payment/              # 金流服務 (ECPay)
│   └── export/               # 匯出服務 (Excel, PDF)
├── scheduler/                 # 排程任務 (3 個)
├── repository/                # 資料存取層 (23 個)
├── entity/                    # 資料庫實體 (23 個)
│   ├── system/               # 系統實體 (含 Payment, SmsLog)
│   ├── staff/                # 員工實體
│   ├── marketing/            # 行銷實體 (含 MarketingPush)
│   └── tenant/               # 租戶實體
├── dto/                       # 資料傳輸物件 (70+ 個)
│   ├── request/              # 請求 DTO
│   └── response/             # 回應 DTO
├── enums/                     # 列舉 (26 個)
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
| 報表 | `GET /reports/dashboard\|summary\|today\|weekly\|monthly\|daily\|top-services\|top-staff` |
| 設定 | `GET/PUT /settings` |
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
| /tenant/coupons | 票券管理 |
| /tenant/campaigns | 行銷活動 |
| /tenant/marketing | 行銷推播 |
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
| 商品 | `products` |
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
```
IDLE（閒置）
  ↓ 點選「開始預約」
SELECTING_SERVICE（選擇服務）
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
| `MAIL_USERNAME` | 郵件帳號 | - |
| `MAIL_PASSWORD` | 郵件密碼 | - |

---

## LINE 設定疑難排解

### 常見問題

| 問題 | 原因 | 解決方案 |
|------|------|----------|
| Rich Menu 顯示亂碼 (□□□□) | Docker 環境缺少中文字型 | 已在 Dockerfile 安裝 font-wqy-zenhei |
| Rich Menu 電腦版沒顯示 | LINE 平台限制 | Rich Menu 僅支援手機版 LINE |
| 401 UNAUTHORIZED | Token 無效或過期 | 重新產生 Channel Access Token |
| Bot 無法回應訊息 | LINE OA 自動回應干擾 | 關閉 LINE Official Account Manager 的自動回應 |
| Bot ID 顯示雙重 @ | HTML 多餘圖標 | 已修正 line-settings.html |
| Bot 頭像無法顯示 | LINE API 未設定頭像 | 在 LINE Official Account Manager 設定頭像 |

### LINE 設定流程

1. 前往 [LINE Developers Console](https://developers.line.biz/)
2. 建立或選擇 Provider 和 Messaging API Channel
3. 複製 Channel ID、Channel Secret、Channel Access Token
4. 在店家後台 LINE 設定頁面填入
5. 設定 Webhook URL 到 LINE Developers Console
6. **重要**：關閉 LINE Official Account Manager 的自動回應功能

### 關閉 LINE Official Account 自動回應

1. 前往 [LINE Official Account Manager](https://manager.line.biz/)
2. 進入您的官方帳號 → 設定 → 回應設定
3. 將「自動回應訊息」設為關閉
4. 確保「Webhook」設為開啟

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

**測試套件 (436 tests)：**

| 檔案 | 說明 | 測試數 |
|------|------|--------|
| `00-setup.spec.ts` | 環境檢查 | 5 |
| `01-auth.spec.ts` | 認證功能 | 11 |
| `02-admin.spec.ts` | 超管後台基本測試 | 12 |
| `03-tenant-dashboard.spec.ts` | 店家後台基本測試 | 8 |
| `04-tenant-features.spec.ts` | API 測試 | 17 |
| `05-feature-store.spec.ts` | 功能商店 | 10 |
| `06-sse-notifications.spec.ts` | SSE 即時通知 | 15 |
| `07-admin-crud.spec.ts` | 超管 CRUD 完整測試 | 28 |
| `08-tenant-booking.spec.ts` | 預約管理完整測試 | 32 |
| `09-tenant-pages.spec.ts` | 店家後台所有頁面測試 | 33 |
| `10-admin-pages.spec.ts` | 超管後台所有頁面測試 | 11 |
| `11-public-pages.spec.ts` | 公開頁面測試 | 24 |
| `12-tenant-campaign-marketing.spec.ts` | 行銷活動&推播測試 | 25 |
| `13-tenant-settings.spec.ts` | 設定頁面測試 | 28 |
| `14-tenant-reports.spec.ts` | 報表&匯出測試 | 30 |
| `15-line-bot.spec.ts` | LINE Bot 測試 | 19 |
| `16-sidebar-feature-visibility.spec.ts` | 側邊欄功能訂閱測試 | 22 |
| `17-comprehensive-forms.spec.ts` | 表單驗證測試 | 25 |

**測試涵蓋範圍：**

- 所有超管頁面（儀表板、店家管理、功能管理、儲值審核）
- 所有店家頁面（16+ 頁面：儀表板、預約管理、行事曆、報表、顧客、員工、服務、商品、票券、行銷、設定等）
- 所有公開頁面（登入、註冊、忘記密碼、顧客自助取消預約）
- 所有 API 端點
- 所有表單欄位和按鈕
- JavaScript 錯誤檢測（SyntaxError、ReferenceError、TypeError）
- 功能訂閱與側邊欄顯示控制
- LINE Bot 對話狀態和訊息格式
- Excel/PDF 匯出功能
- 靜態資源（CSS/JS）載入

**測試基礎設施注意事項：**

- 使用 `domcontentloaded` 而非 `networkidle` 等待頁面載入
- 原因：SSE 連線會保持網路活躍，導致 `networkidle` 永遠無法觸發
- 所有測試檔案已更新使用正確的等待策略

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
| `ADVANCED_REPORT` | 進階報表分析 | ✅ 已完成 | 顧客分析、趨勢預測 |
| `COUPON_SYSTEM` | 票券系統 | ✅ 已完成 | 優惠券發放與核銷 |
| `MEMBERSHIP_SYSTEM` | 會員等級系統 | ✅ 已完成 | 等級設定與升降級 |
| `POINT_SYSTEM` | 顧客集點獎勵 | ✅ 已完成 | 自動集點與兌換 |
| `PRODUCT_SALES` | 商品銷售功能 | ✅ 已完成 | 商品管理與庫存 |
| `AUTO_REMINDER` | 自動預約提醒 | ✅ 已完成 | LINE/SMS 自動提醒 |
| `AUTO_BIRTHDAY` | 自動生日祝福 | ✅ 已完成 | 每日 9:00 發送祝福 |
| `AUTO_RECALL` | 顧客喚回通知 | ✅ 已完成 | 每日 14:00 發送喚回 |
| `EXTRA_PUSH` | 額外推送額度 | ✅ 已完成 | 突破每月推送限制 |
| `ADVANCED_CUSTOMER` | 進階顧客管理 | ✅ 已完成 | 顧客標籤與分群 |
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
| Controller | 29 |
| Service | 34 |
| Entity | 23 |
| Repository | 23 |
| DTO | 70+ |
| Enum | 26 |
| Scheduler | 3 |
| HTML 頁面 | 36 |
| CSS 檔案 | 3 |
| JS 檔案 | 4 |
| i18n 檔案 | 4 |
| E2E 測試 | 436 |

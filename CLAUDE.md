# Booking Platform - 多租戶預約平台系統

## 線上環境

| 環境 | URL |
|------|-----|
| 店家後台 | https://booking-platform-production-1e08.up.railway.app/tenant/login |
| 超管後台 | https://booking-platform-production-1e08.up.railway.app/admin/login |
| 健康檢查 | https://booking-platform-production-1e08.up.railway.app/health |

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
│   ├── config/               # 設定 (Security, Redis, Jackson, Async)
│   ├── exception/            # 例外 (BusinessException, ErrorCode)
│   ├── response/             # 統一回應 (ApiResponse, PageResponse)
│   ├── security/             # JWT (JwtTokenProvider, JwtAuthenticationFilter)
│   └── tenant/               # 多租戶 (TenantContext, TenantFilter)
├── controller/                # 控制器 (24 個)
│   ├── admin/                # 超管 API (4 個)
│   ├── auth/                 # 認證 API (1 個)
│   ├── line/                 # LINE Webhook (1 個)
│   ├── page/                 # 頁面路由 (2 個)
│   └── tenant/               # 店家 API (15 個)
├── service/                   # 服務層 (27 個)
│   ├── admin/                # 超管服務
│   ├── line/                 # LINE 相關
│   └── notification/         # 通知服務
├── repository/                # 資料存取層 (19 個)
├── entity/                    # 資料庫實體 (19 個)
│   ├── system/               # 系統實體
│   ├── staff/                # 員工實體
│   └── tenant/               # 租戶實體
├── dto/                       # 資料傳輸物件 (65+ 個)
│   ├── request/              # 請求 DTO
│   └── response/             # 回應 DTO
├── enums/                     # 列舉 (20 個)
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
| 服務分類 | `GET /service-categories` |
| 商品 | `GET/POST /products`, `GET/PUT/DELETE /products/{id}` |
| 商品操作 | `POST /products/{id}/on-sale\|off-shelf\|adjust-stock` |
| 票券 | `GET/POST /coupons`, `GET/PUT/DELETE /coupons/{id}` |
| 票券操作 | `POST /coupons/{id}/publish\|pause\|resume\|issue`, `POST /coupons/instances/{id}/redeem` |
| 行銷活動 | `GET/POST /campaigns`, `GET/PUT/DELETE /campaigns/{id}` |
| 活動操作 | `POST /campaigns/{id}/publish\|pause\|resume\|end` |
| 會員等級 | `GET/POST /membership-levels`, `GET/PUT/DELETE /membership-levels/{id}` |
| 報表 | `GET /reports/dashboard\|summary\|today\|weekly\|monthly\|daily\|top-services\|top-staff` |
| 設定 | `GET/PUT /settings` |
| LINE 設定 | `GET/PUT /settings/line`, `POST /settings/line/activate\|deactivate` |
| 點數 | `GET /points/balance`, `POST /points/topup`, `GET /points/topups\|transactions` |
| 功能商店 | `GET /feature-store`, `GET /feature-store/{code}`, `POST /feature-store/{code}/apply\|cancel` |

### LINE Webhook

```
POST /api/line/webhook/{tenantCode}
```

### 即時通知 (SSE)

```
GET /api/notifications/stream   # SSE 訂閱（店家後台即時通知）
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
| 行銷 | `coupons`, `coupon_instances`, `campaigns` |
| 系統 | `features`, `tenant_features`, `point_topups` |
| LINE | `tenant_line_configs`, `line_users` |

---

## LINE Bot 功能

### 主選單
用戶隨時輸入任何文字都會顯示主選單（Flex Message），包含：
- 開始預約
- 我的預約
- 瀏覽商品
- 領取票券
- 會員資訊

### 預約流程
```
IDLE（閒置）
  ↓ 點選「開始預約」
SELECTING_SERVICE（選擇服務）
  ↓ 選擇服務
SELECTING_STAFF（選擇員工）
  ↓ 選擇員工（或不指定）
SELECTING_DATE（選擇日期）- 支援 Carousel 顯示完整可預約天數
  ↓ 選擇日期
SELECTING_TIME（選擇時段）
  ↓ 選擇時段
CONFIRMING_BOOKING（確認預約）
  ↓ 確認
IDLE（完成，回到閒置）
```

### 其他流程
- 取消預約：`IDLE → CONFIRMING_CANCEL_BOOKING → IDLE`
- 商品購買：`IDLE → BROWSING_PRODUCTS → VIEWING_PRODUCT_DETAIL → SELECTING_QUANTITY → CONFIRMING_PURCHASE → IDLE`
- 票券領取：`IDLE → BROWSING_COUPONS → IDLE`
- 會員資訊：`IDLE → VIEWING_MEMBER_INFO → IDLE`

Redis Key: `line:conversation:{tenantId}:{lineUserId}`，TTL: 30 分鐘

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

## 統計數據

| 項目 | 數量 |
|------|------|
| Controller | 24 |
| Service | 27 |
| Entity | 19 |
| Repository | 19 |
| DTO | 65+ |
| Enum | 20 |
| HTML 頁面 | 34 |
| CSS 檔案 | 3 |
| JS 檔案 | 4 |

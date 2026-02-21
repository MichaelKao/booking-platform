# Booking Platform - 多租戶預約平台系統

## 線上環境

| 環境 | URL | 短網址 |
|------|-----|--------|
| 店家後台 | https://booking-platform-production-1e08.up.railway.app/tenant/login | https://is.gd/bkp_tenant |
| 超管後台 | https://booking-platform-production-1e08.up.railway.app/admin/login | https://is.gd/bkp_admin |
| 健康檢查 | https://booking-platform-production-1e08.up.railway.app/health | https://is.gd/bkp_health |

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

## 修改流程

1. 修改程式碼
2. `mvn compile -q` 確認編譯通過
3. 更新相關 CLAUDE.md 文件（如有結構/功能變更）
4. `git add` + `git commit` + `git push`（不需詢問，直接執行）
5. Railway 自動部署，等 60 秒後 health check 驗證

**注意**：不要每一步都問確認，修完直接 commit + push + deploy 驗證。

---

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
│   ├── config/               # 設定 (Security, Redis, Jackson, Async, Locale, Sms, Ecpay, Tracking)
│   ├── exception/            # 例外 (BusinessException, ErrorCode)
│   ├── response/             # 統一回應 (ApiResponse, PageResponse)
│   ├── security/             # JWT (JwtTokenProvider, JwtAuthenticationFilter)
│   └── tenant/               # 多租戶 (TenantContext, TenantFilter)
├── controller/                # 控制器 (34 個)
│   ├── admin/                # 超管 API (4 個)
│   ├── auth/                 # 認證 API (1 個)
│   ├── line/                 # LINE Webhook + 診斷 (2 個)
│   ├── page/                 # 頁面路由 (3 個)
│   └── tenant/               # 店家 API (23 個)
├── service/                   # 服務層 (39 個)
│   ├── admin/                # 超管服務
│   ├── line/                 # LINE 相關
│   ├── notification/         # 通知服務 (Email, SSE, SMS)
│   ├── payment/              # 金流服務 (ECPay)
│   └── export/               # 匯出服務 (Excel, PDF)
├── scheduler/                 # 排程任務 (5 個)
├── repository/                # 資料存取層 (26 個)
├── entity/                    # 資料庫實體 (26 個)
│   ├── system/               # 系統實體 (含 Payment, SmsLog)
│   ├── staff/                # 員工實體
│   ├── marketing/            # 行銷實體 (含 MarketingPush)
│   └── tenant/               # 租戶實體
├── dto/                       # 資料傳輸物件 (78+ 個)
│   ├── request/              # 請求 DTO
│   └── response/             # 回應 DTO
├── enums/                     # 列舉 (29 個)
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
| Rich Menu | `GET/POST/DELETE /settings/line/rich-menu`, `POST /settings/line/rich-menu/create\|upload-image\|create-custom` |
| 進階 Rich Menu | `POST /settings/line/rich-menu/create-advanced\|preview-advanced`, `GET/PUT /settings/line/rich-menu/advanced-config` |
| Flex Menu | `GET/PUT /settings/line/flex-menu`, `POST /settings/line/flex-menu/upload-card-image`, `DELETE /settings/line/flex-menu/card-image` |
| 點數 | `GET /points/balance`, `POST /points/topup`, `GET /points/topups\|transactions` |
| 功能商店 | `GET /feature-store`, `GET /feature-store/{code}`, `POST /feature-store/{code}/apply\|cancel` |
| 行銷推播 | `GET/POST /marketing/pushes`, `POST /marketing/pushes/{id}/send`, `DELETE /marketing/pushes/{id}` |
| 報表匯出 | `GET /export/bookings/excel\|pdf`, `GET /export/reports/excel\|pdf`, `GET /export/customers/excel` |
| 員工行事曆 | `GET /staff/calendar` |
| 支付 | `GET/POST /payments`, `GET /payments/{id}`, `GET /payments/order/{merchantTradeNo}` |
| 推薦 | `GET /referrals/dashboard`, `GET /referrals/code` |

### LINE Webhook

```
POST /api/line/webhook/{tenantCode}
```

### 公開圖片存取（不需認證）

```
GET /api/public/flex-card-image/{tenantId}/{cardIndex}   # Flex 卡片/步驟圖片（供 LINE Flex Message 使用）
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
| `coupon_redeemed` | 票券核銷 | 顯示通知、刷新票券列表 |
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
| /tenant/rich-menu-design | 選單設計（需訂閱 CUSTOM_RICH_MENU） |
| /tenant/feature-store | 功能商店 |
| /tenant/points | 點數管理 |
| /tenant/referrals | 推薦好友 |

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
| 行銷 | `coupons`, `coupon_instances`, `campaigns`, `marketing_pushes`, `tenant_referrals` |
| 系統 | `features`, `tenant_features`, `point_topups`, `payments`, `sms_logs` |
| LINE | `tenant_line_configs`, `line_users` |

---

## LINE Bot 功能

### Rich Menu（快捷選單）
底部固定選單，顧客開啟聊天室即可看到（7 格：上排 3 + 下排 4）：

```
┌──────────┬──────────┬──────────┐
│ 開始預約 │ 我的預約 │ 瀏覽商品 │  ← 上排 3 格
├───────┬───────┬───────┬───────┤
│領取票券│我的票券│會員資訊│聯絡店家│  ← 下排 4 格
└───────┴───────┴───────┴───────┘
```

| 功能 | 說明 |
|------|------|
| 開始預約 | 啟動預約流程 |
| 我的預約 | 查看預約清單 |
| 瀏覽商品 | 瀏覽商品列表 |
| 領取票券 | 查看可領取票券 |
| 我的票券 | 查看已領取票券 |
| 會員資訊 | 查看會員資料 |
| 聯絡店家 | 聯絡客服 |

**三種模式**：

| 模式 | 說明 |
|------|------|
| 預設選單 | 免費，系統生成 7 格選單，可選 5 種主題配色 |
| 自訂選單 | 付費（`CUSTOM_RICH_MENU`），店家上傳完整設計圖片，不疊加文字圖示，自行定義每格動作 |
| 進階自訂 | 付費（`CUSTOM_RICH_MENU`），上傳背景圖、每格獨立圓形圖示、自訂文字標籤、Flex 彈窗卡片、大尺寸選單 |

**功能訂閱控制**：自訂選單與進階自訂 Tab 在未訂閱 `CUSTOM_RICH_MENU` 時隱藏，顯示「前往功能商店」提示。後端 API 也會檢查訂閱狀態。

**自訂選單佈局範本**：

| 佈局代碼 | 說明 | 區域數 | 尺寸 |
|---------|------|--------|------|
| `3+4` | 上排 3 + 下排 4（預設） | 7 | Half |
| `2x3` | 經典 2 行 × 3 列 | 6 | Half |
| `2+3` | 上排 2 + 下排 3 | 5 | Half |
| `2x2` | 2 行 × 2 列 | 4 | Half |
| `1+2` | 上排 1（滿版）+ 下排 2 | 3 | Half |
| `3+4+4` | 上3+中4+下4 | 11 | Full |
| `3+4+4+1` | 上3+中4+下4+底1 | 12 | Full |
| `1+4+4` | 上1+中4+下4 | 9 | Full |
| `4+4` | 上4+下4 | 8 | Full |
| `custom_RxC` | 自訂格數（如 custom_2x3） | R*C | 自動 |

**自訂格數**：進階模式支援自訂行數(1-4) x 列數(1-5)，均等分割。3行以上自動使用 Full 尺寸。

**Rich Menu 尺寸**：Half = 2500×843、Full = 2500×1686（大尺寸，3行以上）

**主題配色**：GREEN（LINE綠）、BLUE（海洋藍）、PURPLE（皇家紫）、ORANGE（日落橘）、DARK（暗黑）、CUSTOM_BG（自訂背景+系統疊加）

**自訂圖片**：支援任意尺寸 PNG/JPG，系統自動縮放至 2500x843（cover 策略，置中裁切）。上傳背景圖時預設不疊加系統文字圖示。

**跨平台字型**：Docker 環境安裝 font-wqy-zenhei（文泉驛正黑），確保中文正確顯示

**即時預覽功能**：在 LINE 設定頁面提供手機模擬預覽，可即時看到：
- 預設模式：7 格 (3+4) + 主題配色切換效果
- 自訂模式：佈局選擇器 + 上傳圖片 + 區域數字標記
- 進階模式：背景+圖示+文字即時預覽 + 精確預覽（伺服器合成圖）
- 模式切換 Tab（色塊按鈕，非淡色文字）
- Flex Menu 主選單預覽：模擬 Flex Message Bubble 外觀，即時反映按鈕顏色/圖示/標題變更

### 主選單（Flex Message）
用戶隨時輸入任何文字都會顯示主選單（Flex Message），包含：
- 開始預約
- 我的預約
- 瀏覽商品
- 領取票券 / 我的票券（並排按鈕）
- 會員資訊

### 主選單自訂（Flex Menu Config）

店家可在「LINE 設定 → 主選單樣式」自訂 Flex Message 主選單外觀：

| 設定項目 | 說明 | 預設值 |
|---------|------|--------|
| `headerColor` | Header 背景色 | `#1DB446` |
| `headerTitle` | Header 標題（支援 `{shopName}` 變數） | `✨ {shopName}` |
| `headerSubtitle` | 歡迎語 | `歡迎光臨！請問需要什麼服務呢？` |
| `showTip` | 是否顯示使用提示 | `true` |
| `buttons[].color` | 按鈕背景色 | 各按鈕預設色 |
| `buttons[].icon` | 按鈕圖示 emoji | 📅📋🛍️🎁🎫👤📞 |
| `buttons[].title` | 按鈕標題 | 各按鈕預設標題 |
| `buttons[].subtitle` | 按鈕副標題 | 各按鈕預設副標題 |
| `buttons[].imageUrl` | 輪播卡片圖片 URL | 無（可上傳圖片） |
| `steps[].icon` | 步驟圖示 emoji | ✂️📅👤⏰📝✅ |
| `steps[].color` | 步驟 Header 背景色 | 各步驟預設色 |
| `steps[].title` | 步驟標題 | 各步驟預設標題 |
| `steps[].subtitle` | 步驟副標題 | 各步驟預設副標題 |
| `steps[].imageUrl` | 步驟 Hero 圖片 URL | 無（可上傳圖片） |

**步驟（Steps）說明**：預約流程中每個步驟的 Flex Message Header 可自訂外觀：

| 步驟 Key | 預設標題 | 預設圖示 | 預設色 |
|---------|---------|---------|--------|
| `service` | 選擇服務 | ✂️ | `#4A90D9` |
| `date` | 選擇日期 | 📅 | `#1DB446` |
| `staff` | 選擇服務人員 | 👤 | `#4A90D9` |
| `time` | 選擇時段 | ⏰ | `#4A90D9` |
| `note` | 是否需要備註？ | 📝 | `#5C6BC0` |
| `confirm` | 請確認預約資訊 | ✅ | `#1DB446` |

**圖片儲存**：`TenantLineConfig.flexMenuCardImages`（JSON TEXT 欄位，key=cardIndex 或 100+stepIndex，value=Base64 圖片）

**儲存位置**：`TenantLineConfig.flexMenuConfig`（JSON TEXT 欄位）

**API**：
```
GET  /api/settings/line/flex-menu                    # 取得配置
PUT  /api/settings/line/flex-menu                    # 更新配置
POST /api/settings/line/flex-menu/upload-card-image  # 上傳卡片/步驟圖片（回傳公開 URL）
DELETE /api/settings/line/flex-menu/card-image       # 刪除卡片/步驟圖片
```

**公開圖片端點**：
```
GET /api/public/flex-card-image/{tenantId}/{cardIndex}  # 公開存取卡片/步驟圖片（無需認證）
```

**注意**：LINE 聊天室背景是 LINE 平台功能，無法透過 API 控制。

### Carousel 導覽 Bubble

所有 Carousel 類型的 Flex Message（有資料時），第一張卡片為導覽 Bubble，包含：
- 使用 `applyFunctionHeader` 套用自訂 Header（標題、配色、圖示）
- 支援 Hero 圖片（從 `flexMenuConfig.functions[functionKey].imageUrl` 讀取）
- 操作提示文字

| 功能 | functionKey | 預設配色 | 說明 |
|------|------------|---------|------|
| 瀏覽商品 | `productMenu` | `#FF9800` | 商品列表導覽 |
| 領取票券 | `couponList` | `#E91E63` | 可領取票券導覽 |
| 我的票券 | `myCoupons` | `#9C27B0` | 已領取票券導覽（含使用說明） |
| 我的預約 | `bookingList` | `#0066CC` | 預約列表導覽 |

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

**統一流程（分類與服務合併為一步）：**
```
IDLE（閒置）
  ↓ 點選「開始預約」
SELECTING_SERVICE（選擇服務）- 有分類時按分類分組顯示（每分類一張卡片含服務列表）；無分類時顯示全部服務
  ↓ 選擇服務
SELECTING_DATE（選擇日期）- 只顯示有可預約時段的日期（過濾公休日、全員滿檔、無可用時段）
  ↓ 選擇日期
SELECTING_STAFF（選擇員工）- 根據日期篩選，顯示每位員工可預約時段數；無時段的員工灰色不可點
  ↓ 選擇員工（或不指定）     ※ requiresStaff=false 時自動跳過此步驟
SELECTING_TIME（選擇時段）- requiresStaff=false 時顯示剩餘名額
  ↓ 選擇時段
INPUTTING_NOTE（輸入備註）- 可直接輸入文字或點選「跳過」
  ↓ 輸入備註或跳過
CONFIRMING_BOOKING（確認預約）- requiresStaff=false 時不顯示「服務人員」行
  ↓ 確認
IDLE（完成，回到閒置）
```

**分類合併顯示條件：**
- 至少 2 個啟用中的服務分類 **且** 至少 2 個分類底下有可預約的服務（ACTIVE + isVisible）
- 符合條件時：每個分類為一張 Bubble 卡片，卡片內列出該分類下的服務（可點擊 box）
- 不符合條件時：直接顯示全部服務（無分類標籤）

**日期智慧過濾（真實時段檢查）：**
- `buildDateMenu` 接收服務時長（duration）參數
- 批次查詢：一次取得日期範圍內所有 CONFIRMED 預約 + 所有員工請假，建立記憶體快取
- 對每個日期實際檢查是否有任何員工的任何時段可用（含預約衝突比對，全在記憶體完成）
- 過濾條件：公休日、員工排班、請假、店家/員工休息時間、預約衝突、服務時長是否能放入時段
- 無可預約日期時顯示「目前沒有可預約的日期」提示
- 效能：僅需 3 次 DB 查詢（員工排班 + 預約 + 請假），所有比對在記憶體完成

**員工可用時段顯示：**
- `buildStaffMenuByDate` 接收服務時長（duration）參數
- 對每位員工呼叫 `getAvailableSlotCount` 計算可預約時段數
- 有時段：顯示「可預約 N 個時段」，可點擊
- 無時段：灰色顯示「今日無可預約時段」，不可點擊
- 「不指定」選項只在至少一位員工有時段時顯示

**流程說明：**
- 先選日期再選員工，確保顧客只能看到當天有上班且未請假的員工
- 系統會自動過濾：非營業日、請假員工、過去的時段

**備註輸入說明：**
- 選擇時段後，系統會顯示備註輸入提示
- 用戶可以在聊天輸入框直接打字輸入備註內容
- 也可以點選「跳過」按鈕略過備註步驟
- 備註會顯示在預約確認頁面和店家後台

**返回上一步（確定性狀態映射）：**
- `goBack()` 使用 switch 確定性映射，不依賴 `previousState` 欄位
- 每個狀態固定回到邏輯上的前一步，不受重複點擊或亂序操作影響
- IDLE 狀態時 goBack 直接顯示主選單

**下游資料自動清除（防止重複點擊殘留）：**
- 選擇服務時：清除日期、員工、時間、備註（`clearDownstreamFromDate`）
- 選擇日期時：清除員工、時間、備註（`clearDownstreamFromStaff`）
- 選擇員工時：清除時間、備註（`clearDownstreamFromTime`）
- 確保用戶從 LINE 聊天記錄中點擊舊按鈕時，不會殘留過時資料

**前置條件驗證（防呆機制）：**
- 所有 handler（handleSelectService/Date/Staff/Time）在執行前檢查必要資料是否存在
- 缺少前置資料時（如選員工但無日期），自動引導用戶重新開始預約流程
- `displayCurrentState` 包含 try-catch，異常時回覆錯誤提示避免無回應

**取消與返回按鈕：**
- 「↩ 返回上一步」（go_back）：回到前一個步驟
- 「✕ 取消預約」（cancel_flow）：顯示確認對話框，「繼續預約」留在當前步驟（resume_booking），「確定取消」重置回主選單

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
- **新員工預設排班**：週一至五 09:00-18:00、午休 12:00-13:00、週六日休息

### 請假管理
- 支援特定日期請假（事假、病假、休假、特休、其他）
- 快速選擇：明天、下週一~五、本週末、下週末
- 請假原因備註

---

## 報表統計規則

**重要**：所有報表統計查詢（預約數、營收、熱門服務/員工、回客數等）一律依 `bookingDate`（預約日期）篩選，不使用 `createdAt`（建立時間）。顧客統計和票券統計仍依 `createdAt`。

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

## 預約狀態與衝突檢查

### 預約狀態語意

| 狀態 | 佔用時段 | 說明 |
|------|---------|------|
| `PENDING` | **否** | 顧客申請，同時段可有多筆 PENDING，等待店家確認 |
| `CONFIRMED` | **是** | 店家確認，佔用員工時段，不可重疊 |
| `COMPLETED` | 否 | 已完成 |
| `CANCELLED` | 否 | 已取消 |
| `NO_SHOW` | 否 | 爽約 |

### 容量預約模式

支援三種預約模式，透過 `ServiceItem.maxCapacity` 和 `Staff.maxConcurrentBookings` 控制，**預設值皆為 1，確保現有行為完全不變**：

| 模式 | 條件 | 衝突判斷 | 適用場景 |
|------|------|---------|---------|
| 一對一（預設） | `requiresStaff=true`, `maxConcurrentBookings=1` | `exists` = 衝突 | 美容院、按摩 |
| 員工容量模式 | `requiresStaff=true`, `maxConcurrentBookings>1` | `count < maxConcurrentBookings` | 健身團課、教練 |
| 服務容量模式 | `requiresStaff=false` | `count < maxCapacity` | 餐廳、場地預約 |

**相關欄位**：
- `ServiceItem.maxCapacity`（Integer, default=1）：每時段最大預約數（`requiresStaff=false` 時使用）
- `ServiceItem.requiresStaff`（Boolean, default=true）：是否需要指定員工
- `Staff.maxConcurrentBookings`（Integer, default=1）：員工同一時段最大同時預約數

### 衝突檢查規則

**只有 `CONFIRMED` 狀態的預約才算衝突**，PENDING 不佔用時段。

**建立預約（create）**：
1. `requiresStaff=false`（服務容量模式）：`countByService < maxCapacity` → OK，staffId=null
2. `requiresStaff=true` + 指定員工：檢查請假 → `countByStaff < maxConcurrentBookings` → OK
3. `requiresStaff=true` + 不指定：自動分配 `countByStaff < maxConcurrentBookings` 的員工
4. 所有容量都滿 → 拒絕
5. 預約緩衝時間（`bookingBufferMinutes` 設定）

**確認預約（confirm）— 真正的驗證關卡**：
1. `requiresStaff=false`：`countByService < maxCapacity` → OK
2. 已指定員工 → `countByStaff < maxConcurrentBookings` → OK
3. 未指定員工 → 自動分配可用員工
4. 衝突或無可用容量 → 拒絕確認

### 「我的預約」（LINE Bot）

只顯示 `CONFIRMED` 狀態的預約，依日期時間 ASC 排序。

### 日期/員工/時段智慧過濾（LINE Bot）

**日期選單**：`buildDateMenu(tenantId, duration, serviceId)` 過濾無可預約時段的日期。
- `requiresStaff=true`：逐員工檢查是否有可用時段
- `requiresStaff=false`：檢查服務容量 `count < maxCapacity`（不檢查員工）

**員工選單**：`buildStaffMenuByDate(tenantId, serviceId, date, duration)` 對每位員工呼叫 `getAvailableSlotCount()` 計算可預約時段數，無時段的員工灰色不可點。（`requiresStaff=false` 時跳過此步驟）

**時段選單**：`buildTimeMenu(tenantId, staffId, date, duration, serviceId)` 產生可預約時段。
- `requiresStaff=false` + `maxCapacity > 1`：顯示剩餘名額「10:00 (剩3)」
- `requiresStaff=true`：使用 `count < maxConcurrentBookings` 檢查員工容量

### 時間/日期驗證規則

所有涉及開始/結束時間配對的 API 都在 Service 層驗證：**開始時間必須早於結束時間**。

| 功能 | 驗證項目 |
|------|---------|
| 員工排班 | startTime < endTime、breakStartTime < breakEndTime、休息在上班範圍內 |
| 員工請假（半天） | startTime < endTime |
| 店家營業設定 | businessStartTime < businessEndTime、breakStartTime < breakEndTime |
| 更新預約 | startTime < endTime（直接指定結束時間時） |
| 票券有效期 | validStartAt < validEndAt |
| 行銷活動期間 | startAt < endAt |

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
| `CUSTOM_RICH_MENU` | 進階自訂選單 | 400 |

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

> **注意**：LINE 已更改建立流程，現在無法直接在 Developers Console 建立 Messaging API Channel，必須先從 Official Account Manager 建立官方帳號。

| 步驟 | 操作 | 自動化程度 |
|------|------|-----------|
| 1 | 建立 LINE 官方帳號 + 啟用 Messaging API | 手動（LINE 平台限制） |
| 2 | 複製 Channel ID / Secret / Token 填入後台 | 手動 |
| 3 | 儲存設定 → Webhook URL 自動設定到 LINE | **自動**（透過 LINE API） |
| 4 | 連線測試 | 一鍵 |
| 5 | 關閉自動回應 → 頁面顯示直達連結 | **一鍵直達**（`manager.line.biz/account/@{basicId}/setting/response`） |

**詳細步驟：**

1. 前往 [LINE Developers Console](https://developers.line.biz/)
2. 點擊「**Create a LINE Official Account**」，跳轉到 LINE Official Account Manager
3. 建立 LINE 官方帳號（填寫帳號名稱、類別等基本資料）
4. 進入帳號管理後台 → **設定** → **Messaging API** → 點擊「**啟用 Messaging API**」
5. 選擇 Provider，完成後系統自動在 Developers Console 建立 Channel
6. 回到 Developers Console，進入 Channel，複製 **Channel ID**、**Channel Secret**、**Channel Access Token**
7. 在店家後台 LINE 設定頁面填入並儲存（系統**自動設定 Webhook URL** 到 LINE）
8. 點「連線測試」確認成功
9. 點頁面上的「**一鍵前往關閉自動回應**」按鈕，將回應方式改為「手動聊天」

### 自動化機制

- **Webhook URL 自動設定**：儲存 LINE 設定時，後端透過 `PUT /v2/bot/channel/webhook/endpoint` 自動將 Webhook URL 設定到 LINE 平台，並自動執行 webhook test
- **關閉自動回應直達連結**：連線測試成功後取得 `basicId`，頁面自動生成 `https://manager.line.biz/account/@{basicId}/setting/response` 直達連結，店家一鍵跳轉
- **無法自動化的部分**：關閉自動回應（LINE 無公開 API 可操作此設定，必須手動）

### 關閉 LINE Official Account 自動回應（必要步驟）

> ⚠️ **這是最常見的問題原因！** 如果不關閉自動回應，LINE 會攔截所有訊息，Webhook 完全收不到。

連線測試成功後，頁面會顯示「**一鍵前往關閉自動回應**」按鈕直達設定頁面。手動操作：

1. 在「**回應方式**」區塊：將「回應時間」改為只有 **「手動聊天」**
2. 將「非回應時間」也改為 **「手動聊天」**
3. **不要**勾選「自動回應訊息」
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
npx playwright test tests/02-booking-lifecycle.spec.ts

# 列出所有測試
npx playwright test --list
```

**測試套件（13 files，~346 tests，按業務邏輯分區）：**

| 檔案 | 測試數 | 說明 |
|------|--------|------|
| `00-infrastructure.spec.ts` | ~15 | 環境健康、認證流程、多租戶隔離、基本導航 |
| `01-admin-operations.spec.ts` | ~33 | 超管儀表板、店家 CRUD、功能管理、儲值審核、訂閱生命週期 |
| `02-booking-lifecycle.spec.ts` | ~37 | 預約全生命週期（PENDING→CONFIRMED→COMPLETED/CANCELLED/NO_SHOW）、時段衝突、PENDING不佔用 |
| `03-customer-management.spec.ts` | ~23 | 顧客 CRUD、點數加減餘額驗證、封鎖/解封、標籤管理 |
| `04-staff-service.spec.ts` | ~20 | 員工 CRUD、排班設定與持久化驗證、請假管理、服務 CRUD |
| `05-product-order.spec.ts` | ~22 | 商品 CRUD、庫存增減驗證、上下架狀態、商品訂單 |
| `06-coupon-campaign.spec.ts` | ~28 | 票券生命週期（DRAFT→ACTIVE→PAUSED→ACTIVE）、核銷流程、行銷活動狀態機（DRAFT→PUBLISHED→PAUSED→ENDED） |
| `07-settings-config.spec.ts` | ~28 | 店家設定、點數累積設定、LINE 設定（安全）、功能商店訂閱、會員等級、側邊欄功能控制 |
| `08-reports-export.spec.ts` | ~24 | 報表摘要/統計 API、報表一致性交叉驗證、Excel/PDF 匯出 |
| `09-line-bot.spec.ts` | ~20 | LINE Webhook API、LINE 設定 API、Flex Menu、Rich Menu UI、AI 智慧客服 |
| `10-public-pages.spec.ts` | ~30 | 公開頁面、SEO Meta、所有後台頁面健康驗證、RWD 響應式、新手引導 |
| `11-business-rules.spec.ts` | ~30 | 時間驗證規則、API 契約驗證、報表一致性、預約狀態機完整性、SSE 端點、密碼安全 |
| `12-data-integrity.spec.ts` | ~36 | API 回應格式、分頁一致性、軟刪除、空資料處理、並發安全、錯誤處理、特殊字元、Token 處理 |

**測試設計原則：**

- **使用者操作角度**：以業務流程（而非 API 端點）為測試單位
- **強斷言**：每個測試都有 `expect()` 斷言，杜絕 `console.log` 代替斷言、`if (!token) return` 靜默跳過
- **業務邏輯驗證**：狀態變更後重新 GET 確認持久化（不只看回應碼）
- **串行測試**：需要依序執行的測試使用 `test.describe.serial`
- **資料清理**：測試建立的資料在 `afterAll` 中清理，避免汙染環境

**F12 Console 自動監控（fixtures.ts）：**

- 所有測試檔案透過 `import { test, expect } from './fixtures'` 自動啟用 F12 監控
- 監控三類錯誤：`pageerror`（JS 執行錯誤）、HTTP 500+（伺服器錯誤）、`console.error`（過濾瀏覽器雜訊）
- 任何未過濾的 F12 錯誤會讓測試直接失敗

**測試基礎設施注意事項：**

- 使用 `domcontentloaded` 而非 `networkidle` 等待頁面載入（SSE 會保持網路活躍）
- LINE 設定測試**不會覆蓋**真實 LINE credentials
- 啟用/停用測試確保最終狀態恢復

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

### 測試檔案結構

```
tests/
├── 00-infrastructure.spec.ts     ← 環境健康、認證流程、多租戶隔離
├── 01-admin-operations.spec.ts   ← 超管儀表板、店家 CRUD、功能管理、儲值審核
├── 02-booking-lifecycle.spec.ts  ← 預約全生命週期、時段衝突、狀態機
├── 03-customer-management.spec.ts ← 顧客 CRUD、點數餘額、封鎖/解封、標籤
├── 04-staff-service.spec.ts      ← 員工排班/請假、服務 CRUD
├── 05-product-order.spec.ts      ← 商品 CRUD、庫存增減、上下架
├── 06-coupon-campaign.spec.ts    ← 票券/活動生命週期、票券核銷
├── 07-settings-config.spec.ts    ← 設定、LINE 設定、功能商店、會員等級
├── 08-reports-export.spec.ts     ← 報表 API、一致性驗證、Excel/PDF 匯出
├── 09-line-bot.spec.ts           ← LINE Webhook、設定 API、Flex Menu、Rich Menu
├── 10-public-pages.spec.ts       ← 公開頁面、SEO、頁面健康、RWD、引導
├── 11-business-rules.spec.ts     ← 時間驗證、API 契約、狀態機完整性、SSE
├── 12-data-integrity.spec.ts     ← 回應格式、分頁、軟刪除、並發、錯誤處理
├── fixtures.ts                   ← 共用 Fixture（F12 監控）
└── utils/test-helpers.ts         ← 共用輔助函式
```

**編號邏輯**：
- `00`：基礎設施與認證
- `01`：超管操作
- `02-08`：店家核心業務（預約→顧客→員工→商品→票券→設定→報表）
- `09`：LINE Bot
- `10-12`：品質閘門（頁面健康、業務規則、資料完整性）

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
| `CUSTOM_RICH_MENU` | 進階自訂選單 | ✅ 已完成 | 背景圖+圓形圖示+文字標籤+Flex 彈窗+大尺寸選單 |
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
| Controller | 34 |
| Service | 39 |
| Entity | 26 |
| Repository | 26 |
| DTO | 78+ |
| Enum | 29 |
| Scheduler | 5 |
| HTML 頁面 | 56 |
| CSS 檔案 | 3 |
| JS 檔案 | 4 |
| i18n 檔案 | 4 |
| E2E 測試檔案 | 13 |
| E2E 測試數量 | ~346 |

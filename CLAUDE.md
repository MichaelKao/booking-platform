# Booking Platform - 多租戶預約平台

## 快速參考

| 項目 | 值 |
|------|-----|
| Java | 17 |
| Spring Boot | 3.2 |
| 資料庫 | PostgreSQL (port 5433) |
| 快取 | Redis (port 6379) |
| 前端 | Thymeleaf + Bootstrap 5 |

## 角色

| 角色 | 說明 |
|------|------|
| ADMIN | 超級管理員，管理所有店家 |
| TENANT | 店家，管理自己的預約/顧客/員工 |
| 顧客 | 透過 LINE Bot 預約 |

## 專案結構

```
com.booking.platform
├── common/          # 共用：config, exception, security, tenant
├── controller/      # API：admin/, auth/, page/, line/
├── service/         # 業務邏輯
├── repository/      # 資料存取
├── entity/          # 資料庫實體
├── dto/             # request/, response/
└── enums/           # 列舉
```

## 命名規範

| 類型 | 格式 | 範例 |
|------|------|------|
| Entity | 單數 | `Booking` |
| DTO | XxxRequest/Response | `CreateBookingRequest` |
| Enum值 | 大寫底線 | `PENDING_CONFIRMATION` |

## 關鍵規則

1. **多租戶**：所有查詢必須包含 `tenant_id`，用 `TenantContext.getTenantId()`
2. **軟刪除**：用 `deleted_at`，查詢加 `DeletedAtIsNull`
3. **API回應**：統一用 `ApiResponse.ok(data)` 包裝
4. **註解**：繁體中文，寫在程式碼上方

## 認證

```
POST /api/auth/login              # 統一登入
POST /api/auth/tenant/register    # 店家註冊
POST /api/auth/forgot-password    # 忘記密碼
POST /api/auth/reset-password     # 重設密碼
POST /api/auth/change-password    # 更改密碼
POST /api/auth/refresh            # 刷新Token
```

預設管理員：`admin` / `admin123`

## API 路徑

### Admin API (`/api/admin/`)
- `GET/POST /tenants` - 店家列表/新增
- `GET/PUT /tenants/{id}` - 店家詳情/更新
- `PUT /tenants/{id}/status` - 更新狀態
- `POST /tenants/{id}/features/{code}/enable|disable` - 功能開關
- `GET/POST /point-topups` - 儲值審核
- `GET /dashboard` - 儀表板

### Tenant API (`/api/`)
- `bookings` - 預約 CRUD + `/confirm`, `/cancel`, `/complete`
- `customers` - 顧客 CRUD + `/block`, `/points/add`
- `staff` - 員工 CRUD
- `services` - 服務 CRUD
- `products` - 商品 CRUD
- `coupons` - 票券
- `campaigns` - 行銷活動
- `reports/dashboard` - 報表
- `settings` - 設定
- `points/balance` - 點數
- `feature-store` - 功能商店

### LINE
```
POST /api/line/webhook/{tenantCode}
```

## 頁面路由

| Admin | Tenant |
|-------|--------|
| /admin/login | /tenant/login |
| /admin/dashboard | /tenant/dashboard |
| /admin/tenants | /tenant/bookings |
| /admin/tenants/{id} | /tenant/customers |
| /admin/point-topups | /tenant/staff |
| /admin/features | /tenant/services |
| | /tenant/settings |
| | /tenant/register |
| | /tenant/forgot-password |

## 資料表

| 類別 | 表 |
|------|-----|
| 租戶 | tenants, admin_users |
| 員工 | staff, staff_schedules, staff_leaves |
| 服務 | service_categories, service_items |
| 預約 | bookings, booking_histories |
| 顧客 | customers, memberships, membership_levels |
| 商品 | product_categories, products |
| 行銷 | coupons, coupon_instances, campaigns |
| 系統 | features, tenant_features, point_top_ups |
| LINE | tenant_line_configs, line_users |

## LINE Bot

對話狀態：Redis 儲存，Key: `line:conversation:{tenantId}:{lineUserId}`，TTL: 30分鐘

```
IDLE → SELECTING_SERVICE → SELECTING_STAFF → SELECTING_DATE → SELECTING_TIME → CONFIRMING → IDLE
```

## 啟動

```bash
mvn spring-boot:run
# Admin: http://localhost:8080/admin/login (admin/admin123)
# Tenant: http://localhost:8080/tenant/login
```

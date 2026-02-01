# .claude 目錄說明

此目錄包含 Claude Code 的輔助資源。

## 線上環境

- **店家後台**: https://booking-platform-production-1e08.up.railway.app/tenant/login
- **超管後台**: https://booking-platform-production-1e08.up.railway.app/admin/login
- **健康檢查**: https://booking-platform-production-1e08.up.railway.app/health

## 專案狀態

**Phase 1-8 已完成，已部署至 Railway**

| 階段 | 功能 | 狀態 |
|-----|------|------|
| Phase 1 | 核心骨架 (Tenant) | ✅ 完成 |
| Phase 2 | 預約核心 (Staff, Service, Booking) | ✅ 完成 |
| Phase 3 | 顧客管理 (Customer, Membership) | ✅ 完成 |
| Phase 4 | 商業化 (Feature, Points) | ✅ 完成 |
| Phase 5 | 加值功能 (Coupon, Campaign, Product, Report) | ✅ 完成 |
| Phase 6 | LINE Bot 整合 | ✅ 完成 |
| Phase 7 | 超級管理後台頁面 | ✅ 完成 |
| Phase 8 | 店家後台頁面 | ✅ 完成 |

## 已完成功能

### LINE Bot 功能 ✅
- [x] 主選單（預約、商品、票券、會員資訊）
- [x] 預約流程（服務 → 員工 → 日期 → 時段 → 確認）
- [x] 日期選擇支援 Carousel（完整可預約天數）
- [x] 商品瀏覽與購買意向
- [x] 票券領取與查看
- [x] 會員資訊查詢
- [x] 取消預約功能

### 員工管理功能 ✅
- [x] 每週排班設定（7 天工作時間）
- [x] 特定日期請假（事假、病假、休假、特休）
- [x] 快速選擇（明天、下週、週末）
- [x] 請假列表管理

### 後台功能 ✅
- [x] 營運報表頁面（統計、趨勢圖、排名）
- [x] 預約管理（編輯、狀態變更）
- [x] 即時通知（SSE 新預約推送）
- [x] 功能商店

## 目錄結構

- `templates/` - 開發模板，建立新功能時使用
  - `crud-module.md` - CRUD 模組建立模板
  - `line-flex-message.md` - LINE Flex Message 模板
  - `page-template.md` - 前端頁面模板

## 使用方式

### 建立新模組
```
請參考 .claude/templates/crud-module.md 的模板，建立 [模組名稱] 的完整 CRUD
```

### 使用 LINE 模板
```
請參考 .claude/templates/line-flex-message.md 建立新的 Flex Message
```

## 資料表清單

### 租戶與系統
- `tenants` - 租戶/店家
- `admin_users` - 超級管理員
- `features` - 功能定義
- `tenant_features` - 功能訂閱
- `point_topups` - 點數儲值申請

### 員工與排班
- `staffs` - 員工
- `staff_schedules` - 每週排班
- `staff_leaves` - 特定日期請假

### 服務與預約
- `service_categories` - 服務分類
- `service_items` - 服務項目
- `bookings` - 預約

### 顧客
- `customers` - 顧客
- `membership_levels` - 會員等級
- `point_transactions` - 點數交易

### 商品與行銷
- `products` - 商品
- `coupons` - 票券定義
- `coupon_instances` - 票券實例
- `campaigns` - 行銷活動

### LINE
- `tenant_line_configs` - 店家 LINE 設定
- `line_users` - LINE 用戶
- 對話狀態使用 Redis 儲存（Key: `line:conversation:{tenantId}:{lineUserId}`）

## 前端頁面清單

### Admin 後台
| 路徑 | 檔案 | 說明 |
|------|------|------|
| /admin/login | admin/login.html | 登入頁 |
| /admin/dashboard | admin/dashboard.html | 儀表板 |
| /admin/tenants | admin/tenants.html | 店家列表 |
| /admin/tenants/{id} | admin/tenant-detail.html | 店家詳情 |
| /admin/point-topups | admin/point-topups.html | 儲值審核 |
| /admin/features | admin/features.html | 功能管理 |

### Tenant 後台
| 路徑 | 檔案 | 說明 |
|------|------|------|
| /tenant/login | tenant/login.html | 登入頁 |
| /tenant/dashboard | tenant/dashboard.html | 儀表板 |
| /tenant/bookings | tenant/bookings.html | 預約管理 |
| /tenant/calendar | tenant/calendar.html | 行事曆 |
| /tenant/reports | tenant/reports.html | 營運報表 |
| /tenant/customers | tenant/customers.html | 顧客列表 |
| /tenant/staff | tenant/staff.html | 員工管理 |
| /tenant/services | tenant/services.html | 服務項目 |
| /tenant/products | tenant/products.html | 商品管理 |
| /tenant/coupons | tenant/coupons.html | 票券管理 |
| /tenant/campaigns | tenant/campaigns.html | 行銷活動 |
| /tenant/settings | tenant/settings.html | 店家設定 |
| /tenant/line-settings | tenant/line-settings.html | LINE 設定 |
| /tenant/feature-store | tenant/feature-store.html | 功能商店 |
| /tenant/points | tenant/points.html | 點數管理 |

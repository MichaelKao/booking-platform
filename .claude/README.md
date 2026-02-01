# .claude 目錄說明

此目錄包含 Claude Code 的輔助資源。

## 線上環境

- **店家後台**: https://booking-platform-production-1e08.up.railway.app/tenant/login
- **超管後台**: https://booking-platform-production-1e08.up.railway.app/admin/login
- **健康檢查**: https://booking-platform-production-1e08.up.railway.app/health

## 專案狀態

**Phase 1-8 已完成，已部署至 Railway，僅剩 Phase 9 整合測試待開發**

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
| Phase 9 | 整合測試 | ⬜ 待開發 |

## 已完成功能

### Phase 6 - LINE Bot 整合 ✅
- [x] TenantLineConfig、LineUser Entity
- [x] LINE Webhook Controller（簽章驗證）
- [x] EncryptionService（AES-256-GCM 加密）
- [x] LineConfigService、LineUserService
- [x] LineConversationService（Redis 對話狀態）
- [x] LineMessageService、LineWebhookService
- [x] LineFlexMessageBuilder（7 種 Flex Message）
- [x] ConversationState 對話狀態機

### Phase 7 - 超級管理後台頁面 ✅
- [x] 共用 CSS/JS（common.css、common.js）
- [x] Admin 專用 CSS/JS（admin.css、admin.js）
- [x] 登入頁（login.html）
- [x] 儀表板（dashboard.html）
- [x] 店家管理（tenants.html、tenant-detail.html）
- [x] 儲值審核（point-topups.html）
- [x] 功能管理（features.html）
- [x] 側邊欄片段（sidebar-admin.html）
- [x] 錯誤頁面（404.html、500.html）
- [x] AdminPageController

### Phase 8 - 店家後台頁面 ✅
- [x] Tenant 專用 CSS/JS（tenant.css、tenant.js）
- [x] 登入頁（login.html）
- [x] 儀表板（dashboard.html）
- [x] 預約管理（bookings.html）
- [x] 行事曆（calendar.html）
- [x] 顧客管理（customers.html）
- [x] 員工管理（staff.html）
- [x] 服務項目（services.html）
- [x] 商品管理（products.html）
- [x] 票券管理（coupons.html）
- [x] 行銷活動（campaigns.html）
- [x] 店家設定（settings.html）
- [x] LINE 設定（line-settings.html）
- [x] 功能商店（feature-store.html）
- [x] 點數管理（points.html）
- [x] 側邊欄片段（sidebar-tenant.html）
- [x] TenantPageController

## 待實作功能

### Phase 9 - 整合測試
- [ ] 端對端測試
- [ ] 效能測試
- [ ] LINE Bot 流程測試

## 目錄結構

- `examples/` - 範例程式碼，Claude 會參考這些風格
- `templates/` - 開發模板，建立新功能時使用
  - `crud-module.md` - CRUD 模組建立模板
  - `line-flex-message.md` - LINE Flex Message 模板
  - `page-template.md` - 前端頁面模板
- `snippets/` - 常用程式碼片段，可直接複用

## 使用方式

### 建立新模組
```
請參考 .claude/examples/ 的範例風格，建立 [模組名稱] 模組
```

### 使用模板
```
請依照 .claude/templates/crud-module.md 的模板，建立 [模組名稱] 的完整 CRUD
```

### 使用程式碼片段
```
請參考 .claude/snippets/cache-pattern.java 的快取模式，加入快取功能
```

## 資料表清單

### Phase 1
- `tenants` - 租戶/店家
- `tenant_configs` - 店家設定

### Phase 2
- `staff` - 員工
- `staff_schedules` - 員工班表
- `staff_leaves` - 員工請假
- `service_categories` - 服務分類
- `service_items` - 服務項目
- `bookings` - 預約
- `booking_histories` - 預約歷史

### Phase 3
- `customers` - 顧客
- `memberships` - 會員資格
- `membership_levels` - 會員等級

### Phase 4
- `features` - 功能定義
- `feature_subscriptions` - 功能訂閱
- `point_accounts` - 點數帳戶
- `point_transactions` - 點數異動
- `point_top_ups` - 點數儲值申請

### Phase 5
- `coupons` - 票券定義
- `coupon_instances` - 票券實例
- `campaigns` - 行銷活動
- `product_categories` - 商品分類
- `products` - 商品

### Phase 6
- `tenant_line_configs` - 店家 LINE 設定
- `line_users` - LINE 用戶
- 對話狀態使用 Redis 儲存（Key: `line:conversation:{tenantId}:{lineUserId}`）

### 系統相關
- `audit_logs` - 稽核日誌

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

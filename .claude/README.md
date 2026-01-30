# .claude 目錄說明

此目錄包含 Claude Code 的輔助資源。

## 專案狀態

**所有 5 個開發階段已完成！**

| 階段 | 功能 | 狀態 |
|-----|------|------|
| Phase 1 | 核心骨架 (Tenant) | ✅ 完成 |
| Phase 2 | 預約核心 (Staff, Service, Booking) | ✅ 完成 |
| Phase 3 | 顧客管理 (Customer, Membership) | ✅ 完成 |
| Phase 4 | 商業化 (Feature, Points) | ✅ 完成 |
| Phase 5 | 加值功能 (Coupon, Campaign, Product, Report) | ✅ 完成 |

## 待實作功能

- [ ] LINE Webhook 處理
- [ ] 前端頁面 (Thymeleaf)
- [ ] 預約提醒通知排程
- [ ] AI 智慧客服

## 目錄結構

- `examples/` - 範例程式碼，Claude 會參考這些風格
- `templates/` - 開發模板，建立新功能時使用
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

### Phase 2
- `staffs` - 員工
- `service_categories` - 服務分類
- `service_items` - 服務項目
- `bookings` - 預約

### Phase 3
- `customers` - 顧客
- `membership_levels` - 會員等級

### Phase 4
- `features` - 功能定義
- `tenant_features` - 租戶功能啟用狀態
- `point_top_ups` - 點數儲值申請

### Phase 5
- `coupons` - 票券定義
- `coupon_instances` - 票券實例
- `campaigns` - 行銷活動
- `products` - 商品

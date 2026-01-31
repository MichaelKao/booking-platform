# 前端頁面規範

## 技術

Thymeleaf + Bootstrap 5 + 原生 JS + FullCalendar

## 結構

| 目錄 | 內容 |
|------|------|
| admin/ | login, dashboard, tenants, tenant-detail, point-topups, features |
| tenant/ | login, register, forgot-password, dashboard, bookings, calendar, customers, staff, services, products, coupons, campaigns, settings, line-settings, feature-store, points |
| fragments/ | sidebar-admin, sidebar-tenant |
| error/ | 404, 500 |

## 版面

```html
<head th:replace="~{tenant/layout :: head}"></head>
<nav th:replace="~{fragments/sidebar-tenant :: sidebar}"></nav>
<th:block th:replace="~{tenant/layout :: scripts}"></th:block>
```

## common.js API

```javascript
// Token
getToken(), setToken(token), isLoggedIn()

// API（自動處理 Token、錯誤）
api.get(url, params)
api.post(url, data)
api.put(url, data)
api.delete(url)

// 通知
showSuccess(msg), showError(msg), showConfirm(msg)

// 格式化
formatDate(), formatDateTime(), formatMoney(), formatNumber()

// 分頁
renderPagination(pageData, onPageClick)
```

## tenant.js 公開頁面

checkAuth() 會跳過：`/login`, `/register`, `/forgot-password`, `/reset-password`

# 前端頁面規範

## 技術棧

| 技術 | 用途 |
|------|------|
| Thymeleaf | 伺服器端模板引擎 |
| Bootstrap 5 | UI 框架 |
| Bootstrap Icons | 圖示 |
| 原生 JavaScript | 前端邏輯 (Fetch API) |
| FullCalendar | 行事曆元件 |
| Chart.js | 圖表元件 |

---

## 頁面結構 (36 個 HTML 檔案)

### 超級管理後台 (`admin/`)

| 檔案 | 功能 | 路由 |
|------|------|------|
| login.html | 登入頁 | /admin/login |
| layout.html | 主佈局樣板 | - |
| dashboard.html | 儀表板 | /admin/dashboard |
| tenants.html | 店家列表 | /admin/tenants |
| tenant-detail.html | 店家詳情 | /admin/tenants/{id} |
| point-topups.html | 儲值審核 | /admin/point-topups |
| features.html | 功能管理 | /admin/features |

### 店家後台 (`tenant/`)

| 檔案 | 功能 | 路由 |
|------|------|------|
| login.html | 登入頁 | /tenant/login |
| register.html | 註冊頁 | /tenant/register |
| forgot-password.html | 忘記密碼 | /tenant/forgot-password |
| reset-password.html | 重設密碼 | /tenant/reset-password |
| layout.html | 主佈局樣板 | - |
| dashboard.html | 儀表板 | /tenant/dashboard |
| bookings.html | 預約管理 | /tenant/bookings |
| calendar.html | 行事曆 | /tenant/calendar |
| reports.html | 營運報表 | /tenant/reports |
| customers.html | 顧客列表 | /tenant/customers |
| customer-detail.html | 顧客詳情 | /tenant/customers/{id} |
| staff.html | 員工管理 | /tenant/staff |
| services.html | 服務管理 | /tenant/services |
| products.html | 商品管理 | /tenant/products |
| coupons.html | 票券管理 | /tenant/coupons |
| campaigns.html | 行銷活動 | /tenant/campaigns |
| marketing.html | 行銷推播 | /tenant/marketing |
| settings.html | 店家設定 | /tenant/settings |
| line-settings.html | LINE 設定 | /tenant/line-settings |
| feature-store.html | 功能商店 | /tenant/feature-store |
| points.html | 點數管理 | /tenant/points |

### 共用片段 (`fragments/`)

| 檔案 | 功能 |
|------|------|
| sidebar-admin.html | 超管側邊欄 |
| sidebar-tenant.html | 店家側邊欄 |
| header.html | 共用頁頭 |
| pagination.html | 分頁元件 |
| modal.html | 模態框元件 |

### Email 樣板 (`email/`)

| 檔案 | 功能 |
|------|------|
| welcome.html | 歡迎郵件 |
| password-reset.html | 密碼重設郵件 |
| password-changed.html | 密碼已變更郵件 |

### 錯誤頁面 (`error/`)

| 檔案 | 功能 |
|------|------|
| 404.html | 404 Not Found |
| 500.html | 500 Server Error |

### 公開頁面 (`public/`)

| 檔案 | 功能 | 路由 |
|------|------|------|
| cancel-booking.html | 顧客自助取消預約 | /booking/cancel/{token} |

---

## 版面結構

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="~{tenant/layout :: head}">
    <title>頁面標題</title>
</head>
<body>
    <!-- 側邊欄 -->
    <nav th:replace="~{fragments/sidebar-tenant :: sidebar}"></nav>

    <!-- 主內容 -->
    <main class="main-content">
        <!-- 頁面內容 -->
    </main>

    <!-- Scripts -->
    <th:block th:replace="~{tenant/layout :: scripts}"></th:block>

    <script>
        // 頁面專屬 JS
    </script>
</body>
</html>
```

---

## common.js API

### Token 管理

```javascript
getToken()           // 取得 Token
setToken(token)      // 設定 Token
removeToken()        // 移除 Token
isLoggedIn()         // 檢查是否登入
```

### API 呼叫

```javascript
// 自動處理 Token、錯誤、Loading
api.get(url, params)     // GET 請求
api.post(url, data)      // POST 請求
api.put(url, data)       // PUT 請求
api.delete(url)          // DELETE 請求

// 範例
const result = await api.get('/api/bookings', { page: 0, size: 20 });
if (result.success) {
    renderTable(result.data.content);
}
```

### 通知

```javascript
showSuccess(message)     // 成功通知 (綠色)
showError(message)       // 錯誤通知 (紅色)
showWarning(message)     // 警告通知 (黃色)
showConfirm(message)     // 確認對話框 (回傳 Promise)
```

### 格式化

```javascript
formatDate(date)         // 格式化日期 (YYYY-MM-DD)
formatDateTime(datetime) // 格式化日期時間 (YYYY-MM-DD HH:mm)
formatMoney(amount)      // 格式化金額 (NT$ 1,000)
formatNumber(number)     // 格式化數字 (1,000)
```

### 分頁

```javascript
renderPagination(pageData, onPageClick)

// 範例
renderPagination(result.data, (page) => {
    loadData(page);
});
```

---

## tenant.js 公開頁面

`checkAuth()` 會跳過以下頁面（不需登入）：

- `/tenant/login`
- `/tenant/register`
- `/tenant/forgot-password`
- `/tenant/reset-password`

---

## CSS 結構

| 檔案 | 用途 |
|------|------|
| common.css | 共用樣式 (字體、顏色、佈局) |
| admin.css | 超管後台專用樣式 |
| tenant.css | 店家後台專用樣式 |

---

## 員工管理頁面功能

`staff.html` 包含：
- 員工 CRUD Modal
- 排班設定 Modal（每週 7 天工作時間）
- 請假管理 Modal（特定日期請假、快速選擇）

---

## 報表頁面

`reports.html` 使用 Chart.js：
- 統計卡片（總預約、總營收、完成率、新客戶）
- 每日趨勢折線圖（雙 Y 軸：預約數 + 營收）
- 服務分布圓餅圖
- 熱門服務 TOP 5 表格
- 員工業績 TOP 5 表格

---

## 快取清除

更新 CSS/JS 後，在引用時加上版本號：

```html
<link rel="stylesheet" th:href="@{/css/common.css?v=2}">
<script th:src="@{/js/common.js?v=2}"></script>
```

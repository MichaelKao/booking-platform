# Static 靜態資源

## 目錄結構

```
static/
├── css/
│   ├── common.css   # 共用樣式、CSS 變數
│   ├── admin.css    # 超管後台樣式
│   └── tenant.css   # 店家後台樣式
└── js/
    ├── common.js    # 共用函式（API、Token、通知）
    ├── admin.js     # 超管後台 JS
    └── tenant.js    # 店家後台 JS
```

---

## common.js 函式

### Token 管理

| 函式 | 說明 |
|------|------|
| getToken() | 取得 JWT Token |
| setToken(token) | 設定 JWT Token |
| removeToken() | 移除 Token |
| isLoggedIn() | 檢查是否登入 |

### API 呼叫

| 函式 | 說明 |
|------|------|
| api.get(url, params) | GET 請求 |
| api.post(url, data) | POST 請求 |
| api.put(url, data) | PUT 請求 |
| api.delete(url) | DELETE 請求 |

```javascript
// 範例
const result = await api.get('/api/bookings', { page: 0, size: 20 });
if (result.success) {
    renderTable(result.data.content);
}
```

### 通知

| 函式 | 說明 |
|------|------|
| showSuccess(message) | 成功通知 (綠色) |
| showError(message) | 錯誤通知 (紅色) |
| showWarning(message) | 警告通知 (黃色) |
| showConfirm(message) | 確認對話框 |

### 格式化

| 函式 | 說明 | 輸出 |
|------|------|------|
| formatDate(date) | 格式化日期 | 2024-01-15 |
| formatDateTime(datetime) | 格式化日期時間 | 2024-01-15 14:30 |
| formatMoney(amount) | 格式化金額 | NT$ 1,000 |
| formatNumber(number) | 格式化數字 | 1,000 |

### 其他

| 函式 | 說明 |
|------|------|
| renderPagination(pageData, onPageClick) | 分頁元件 |
| getStatusBadge(status, type) | 狀態徽章 |
| debounce(func, wait) | 防抖動 |

---

## tenant.js 公開頁面

`checkAuth()` 會跳過以下頁面（不需登入）：

- `/tenant/login`
- `/tenant/register`
- `/tenant/forgot-password`
- `/tenant/reset-password`

---

## admin.js 初始化

```javascript
document.addEventListener('DOMContentLoaded', function() {
    checkAdminAuth();  // 檢查管理員登入
    loadDashboardData();
});
```

---

## CSS 變數 (common.css)

```css
:root {
    --primary-color: #4361ee;
    --secondary-color: #3f37c9;
    --success-color: #10b981;
    --danger-color: #ef4444;
    --warning-color: #f59e0b;
    --info-color: #3b82f6;

    --sidebar-width: 250px;
    --header-height: 60px;
}
```

---

## 快取破壞

更新 CSS/JS 後，在 HTML 引用加版本號：

```html
<link rel="stylesheet" th:href="@{/css/common.css?v=2}">
<script th:src="@{/js/common.js?v=2}"></script>
```

---

## 使用範例

### 載入資料

```javascript
async function loadBookings(page = 0) {
    try {
        const result = await api.get('/api/bookings', { page, size: 20 });
        if (result.success) {
            renderTable(result.data.content);
            renderPagination(result.data, loadBookings);
        }
    } catch (error) {
        showError('載入資料失敗');
    }
}
```

### 提交表單

```javascript
async function submitForm() {
    const data = {
        customerName: document.getElementById('customerName').value,
        phone: document.getElementById('phone').value
    };

    try {
        const result = await api.post('/api/customers', data);
        if (result.success) {
            showSuccess('新增成功');
            closeModal();
            loadCustomers();
        }
    } catch (error) {
        showError(error.message || '操作失敗');
    }
}
```

### 刪除確認

```javascript
async function deleteCustomer(id) {
    const confirmed = await showConfirm('確定要刪除此顧客嗎？');
    if (confirmed) {
        const result = await api.delete(`/api/customers/${id}`);
        if (result.success) {
            showSuccess('刪除成功');
            loadCustomers();
        }
    }
}
```

# Static 靜態資源

## 結構

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

## common.js 主要函式

| 函式 | 用途 |
|------|------|
| getToken() / setToken() | Token 管理 |
| isLoggedIn() | 檢查登入狀態 |
| api.get/post/put/delete() | API 呼叫（自動加 Token） |
| showSuccess/Error/Confirm() | 通知訊息 |
| formatDate/DateTime/Money() | 格式化 |
| renderPagination() | 分頁元件 |
| getStatusBadge() | 狀態徽章 |

## tenant.js 注意事項

`checkAuth()` 會跳過的公開頁面：
- /login
- /register
- /forgot-password
- /reset-password

## 快取破壞

更新 JS/CSS 後，在 HTML 引用加 `?v=n`：
```html
<script th:src="@{/js/common.js?v=2}"></script>
```

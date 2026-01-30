# 前端頁面規範

## 技術選擇

- Thymeleaf 模板引擎
- Bootstrap 5 UI 框架
- 原生 JavaScript（Fetch API）

## 目錄結構
```
templates/
├── admin/           # 超級管理後台
├── tenant/          # 店家後台
├── fragments/       # 共用片段
└── error/           # 錯誤頁面
```

## 頁面命名

- 列表頁：`xxx-list.html` 或 `xxxs.html`
- 詳情頁：`xxx-detail.html`
- 表單頁：`xxx-form.html`

## API 呼叫
```javascript
async function loadData() {
    const response = await fetch('/api/xxx', {
        headers: {
            'Authorization': 'Bearer ' + getToken()
        }
    });
    const result = await response.json();
    if (result.success) {
        renderData(result.data);
    } else {
        showError(result.message);
    }
}
```

## 共用函式（common.js）

- `getToken()` - 取得 JWT Token
- `showSuccess(message)` - 顯示成功訊息
- `showError(message)` - 顯示錯誤訊息
- `formatDate(date)` - 格式化日期
- `formatMoney(amount)` - 格式化金額
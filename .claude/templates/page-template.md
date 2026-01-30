# 頁面模板

建立 Thymeleaf 頁面時的模板。

## Layout 模板
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${pageTitle} + ' - Booking Platform'">Booking Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link rel="stylesheet" th:href="@{/css/common.css}">
</head>
<body>
    <!-- 側邊欄 -->
    <div th:replace="~{fragments/sidebar :: sidebar}"></div>
    
    <!-- 主內容區 -->
    <main class="main-content">
        <!-- 頁面標題 -->
        <div class="page-header">
            <h1 th:text="${pageTitle}">頁面標題</h1>
        </div>
        
        <!-- 頁面內容（由子頁面填充） -->
        <div th:replace="${content}"></div>
    </main>
    
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script th:src="@{/js/common.js}"></script>
</body>
</html>
```

## 列表頁面模板
```html
<div th:fragment="content">
    <!-- 搜尋區 -->
    <div class="card mb-3">
        <div class="card-body">
            <form id="searchForm" class="row g-3">
                <div class="col-md-3">
                    <input type="text" class="form-control" name="keyword" placeholder="搜尋...">
                </div>
                <div class="col-md-2">
                    <select class="form-select" name="status">
                        <option value="">全部狀態</option>
                        <option value="ACTIVE">啟用</option>
                        <option value="INACTIVE">停用</option>
                    </select>
                </div>
                <div class="col-md-2">
                    <button type="submit" class="btn btn-primary">搜尋</button>
                </div>
                <div class="col-md-5 text-end">
                    <button type="button" class="btn btn-success" onclick="openCreateModal()">
                        新增
                    </button>
                </div>
            </form>
        </div>
    </div>
    
    <!-- 資料表格 -->
    <div class="card">
        <div class="card-body">
            <table class="table table-hover">
                <thead>
                    <tr>
                        <th>名稱</th>
                        <th>狀態</th>
                        <th>建立時間</th>
                        <th>操作</th>
                    </tr>
                </thead>
                <tbody id="dataTable">
                    <!-- JavaScript 動態填充 -->
                </tbody>
            </table>
            
            <!-- 分頁 -->
            <nav id="pagination"></nav>
        </div>
    </div>
</div>

<script>
// 載入資料
async function loadData(page = 0) {
    const params = new URLSearchParams(new FormData(document.getElementById('searchForm')));
    params.set('page', page);
    
    const response = await fetch('/api/examples?' + params, {
        headers: { 'Authorization': 'Bearer ' + getToken() }
    });
    const result = await response.json();
    
    if (result.success) {
        renderTable(result.data.content);
        renderPagination(result.data);
    }
}

// 渲染表格
function renderTable(data) {
    const tbody = document.getElementById('dataTable');
    tbody.innerHTML = data.map(item => `
        <tr>
            <td>${item.name}</td>
            <td><span class="badge bg-${item.isActive ? 'success' : 'secondary'}">${item.isActive ? '啟用' : '停用'}</span></td>
            <td>${item.createdAt}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary" onclick="edit('${item.id}')">編輯</button>
                <button class="btn btn-sm btn-outline-danger" onclick="remove('${item.id}')">刪除</button>
            </td>
        </tr>
    `).join('');
}

// 頁面載入
document.addEventListener('DOMContentLoaded', () => loadData());
</script>
```
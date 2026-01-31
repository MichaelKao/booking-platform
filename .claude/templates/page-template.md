# 頁面模板

建立 Thymeleaf + Bootstrap 5 頁面時的模板。

---

## 1. 共用 Layout 模板

### admin/layout.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" th:fragment="layout(content)">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理後台</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <link href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.10.0/font/bootstrap-icons.css" rel="stylesheet">
    <link th:href="@{/css/common.css}" rel="stylesheet">
    <link th:href="@{/css/admin.css}" rel="stylesheet">
</head>
<body>
    <div class="d-flex">
        <!-- 側邊欄 -->
        <nav id="sidebar" class="sidebar bg-dark text-white">
            <div class="sidebar-header p-3 border-bottom border-secondary">
                <h5 class="mb-0">Booking Platform</h5>
                <small class="text-muted">管理後台</small>
            </div>
            <ul class="nav flex-column p-2">
                <li class="nav-item">
                    <a class="nav-link text-white" th:href="@{/admin/dashboard}">
                        <i class="bi bi-speedometer2 me-2"></i> 儀表板
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-white" th:href="@{/admin/tenants}">
                        <i class="bi bi-shop me-2"></i> 店家管理
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-white" th:href="@{/admin/point-topups}">
                        <i class="bi bi-cash-coin me-2"></i> 儲值審核
                    </a>
                </li>
                <li class="nav-item">
                    <a class="nav-link text-white" th:href="@{/admin/features}">
                        <i class="bi bi-puzzle me-2"></i> 功能管理
                    </a>
                </li>
            </ul>
            <div class="mt-auto p-3 border-top border-secondary">
                <a class="nav-link text-white" href="javascript:logout()">
                    <i class="bi bi-box-arrow-right me-2"></i> 登出
                </a>
            </div>
        </nav>

        <!-- 主內容區 -->
        <main class="main-content flex-grow-1">
            <!-- 頂部導航 -->
            <nav class="navbar navbar-expand navbar-light bg-white border-bottom px-4">
                <button class="btn btn-link text-dark" onclick="toggleSidebar()">
                    <i class="bi bi-list fs-4"></i>
                </button>
                <span class="navbar-text ms-3 fw-bold">頁面標題</span>
                <div class="ms-auto">
                    <span class="text-muted">管理員</span>
                </div>
            </nav>

            <!-- 頁面內容 -->
            <div class="container-fluid p-4">
                <th:block th:replace="${content}" />
            </div>
        </main>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script th:src="@{/js/common.js}"></script>
    <script th:src="@{/js/admin.js}"></script>
</body>
</html>
```

---

## 2. 登入頁模板

### admin/login.html
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>管理員登入 - Booking Platform</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/css/bootstrap.min.css" rel="stylesheet">
    <style>
        body {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
        }
        .login-card {
            width: 100%;
            max-width: 400px;
            border-radius: 1rem;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
        }
    </style>
</head>
<body>
    <div class="card login-card">
        <div class="card-body p-5">
            <h3 class="text-center mb-4">管理員登入</h3>

            <div id="errorAlert" class="alert alert-danger d-none"></div>

            <form onsubmit="handleLogin(event)">
                <div class="mb-3">
                    <label class="form-label">帳號</label>
                    <input type="text" id="username" class="form-control" required>
                </div>
                <div class="mb-3">
                    <label class="form-label">密碼</label>
                    <input type="password" id="password" class="form-control" required>
                </div>
                <button type="submit" id="submitBtn" class="btn btn-primary w-100">
                    登入
                </button>
            </form>
        </div>
    </div>

    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.0/dist/js/bootstrap.bundle.min.js"></script>
    <script>
        async function handleLogin(e) {
            e.preventDefault();

            const submitBtn = document.getElementById('submitBtn');
            const errorAlert = document.getElementById('errorAlert');

            submitBtn.disabled = true;
            submitBtn.innerHTML = '登入中...';
            errorAlert.classList.add('d-none');

            try {
                const response = await fetch('/api/auth/admin/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        username: document.getElementById('username').value,
                        password: document.getElementById('password').value
                    })
                });

                const result = await response.json();

                if (result.success) {
                    localStorage.setItem('token', result.data.token);
                    localStorage.setItem('userType', 'admin');
                    window.location.href = '/admin/dashboard';
                } else {
                    errorAlert.textContent = result.message || '登入失敗';
                    errorAlert.classList.remove('d-none');
                }
            } catch (error) {
                errorAlert.textContent = '系統錯誤，請稍後再試';
                errorAlert.classList.remove('d-none');
            } finally {
                submitBtn.disabled = false;
                submitBtn.innerHTML = '登入';
            }
        }
    </script>
</body>
</html>
```

---

## 3. 列表頁模板

### admin/tenants.html
```html
<th:block th:replace="~{admin/layout :: layout(~{::content})}">
    <th:block th:fragment="content">
        <!-- 搜尋區 -->
        <div class="card mb-4">
            <div class="card-body">
                <form id="searchForm" class="row g-3">
                    <div class="col-md-3">
                        <label class="form-label">搜尋</label>
                        <input type="text" name="keyword" class="form-control" placeholder="店家名稱、代碼">
                    </div>
                    <div class="col-md-2">
                        <label class="form-label">狀態</label>
                        <select name="status" class="form-select">
                            <option value="">全部</option>
                            <option value="ACTIVE">啟用</option>
                            <option value="SUSPENDED">停權</option>
                            <option value="PENDING">待審核</option>
                        </select>
                    </div>
                    <div class="col-md-2 d-flex align-items-end">
                        <button type="submit" class="btn btn-primary">
                            <i class="bi bi-search"></i> 搜尋
                        </button>
                    </div>
                    <div class="col-md-5 d-flex align-items-end justify-content-end">
                        <button type="button" class="btn btn-success" onclick="openCreateModal()">
                            <i class="bi bi-plus-lg"></i> 新增店家
                        </button>
                    </div>
                </form>
            </div>
        </div>

        <!-- 資料表 -->
        <div class="card">
            <div class="card-body">
                <div class="table-responsive">
                    <table class="table table-hover">
                        <thead>
                            <tr>
                                <th>店家名稱</th>
                                <th>代碼</th>
                                <th>聯絡人</th>
                                <th>狀態</th>
                                <th>點數餘額</th>
                                <th>建立時間</th>
                                <th>操作</th>
                            </tr>
                        </thead>
                        <tbody id="dataTable">
                            <!-- 動態填入 -->
                        </tbody>
                    </table>
                </div>

                <!-- 分頁 -->
                <nav id="pagination"></nav>

                <!-- Loading -->
                <div id="loading" class="text-center py-5 d-none">
                    <div class="spinner-border text-primary"></div>
                    <p class="mt-2">載入中...</p>
                </div>

                <!-- 無資料 -->
                <div id="noData" class="text-center py-5 text-muted d-none">
                    <i class="bi bi-inbox fs-1"></i>
                    <p>沒有資料</p>
                </div>
            </div>
        </div>

        <!-- 新增/編輯 Modal -->
        <div class="modal fade" id="formModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="formModalTitle">新增店家</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <form id="dataForm">
                            <input type="hidden" id="formId">
                            <div class="mb-3">
                                <label class="form-label">店家名稱 *</label>
                                <input type="text" id="formName" class="form-control" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">店家代碼 *</label>
                                <input type="text" id="formCode" class="form-control" required>
                                <div class="form-text">用於 LINE Webhook URL，只能使用英文、數字、底線</div>
                            </div>
                            <div class="mb-3">
                                <label class="form-label">聯絡人</label>
                                <input type="text" id="formContactName" class="form-control">
                            </div>
                            <div class="mb-3">
                                <label class="form-label">聯絡電話</label>
                                <input type="text" id="formContactPhone" class="form-control">
                            </div>
                            <div class="mb-3">
                                <label class="form-label">Email</label>
                                <input type="email" id="formEmail" class="form-control">
                            </div>
                        </form>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary" onclick="saveData()">儲存</button>
                    </div>
                </div>
            </div>
        </div>
    </th:block>
</th:block>

<script>
    // 頁面載入
    document.addEventListener('DOMContentLoaded', function() {
        loadData();

        // 搜尋表單提交
        document.getElementById('searchForm').addEventListener('submit', function(e) {
            e.preventDefault();
            loadData();
        });
    });

    // 載入資料
    async function loadData(page = 0) {
        showLoading(true);

        try {
            const formData = new FormData(document.getElementById('searchForm'));
            const params = new URLSearchParams(formData);
            params.set('page', page);
            params.set('size', 20);

            const result = await api.get('/api/admin/tenants?' + params);

            if (result.success) {
                renderTable(result.data.content);
                renderPagination(result.data, loadData);
            }
        } catch (error) {
            showError(error.message);
        } finally {
            showLoading(false);
        }
    }

    // 渲染表格
    function renderTable(data) {
        const tbody = document.getElementById('dataTable');
        const noData = document.getElementById('noData');

        if (data.length === 0) {
            tbody.innerHTML = '';
            noData.classList.remove('d-none');
            return;
        }

        noData.classList.add('d-none');

        tbody.innerHTML = data.map(item => `
            <tr>
                <td>
                    <a href="/admin/tenants/${item.id}">${item.name}</a>
                </td>
                <td>${item.code}</td>
                <td>${item.contactName || '-'}</td>
                <td>${getStatusBadge(item.status)}</td>
                <td>${formatNumber(item.pointBalance)} 點</td>
                <td>${formatDate(item.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-outline-primary" onclick="editData('${item.id}')">
                        <i class="bi bi-pencil"></i>
                    </button>
                    <a href="/admin/tenants/${item.id}" class="btn btn-sm btn-outline-info">
                        <i class="bi bi-eye"></i>
                    </a>
                </td>
            </tr>
        `).join('');
    }

    // 狀態標籤
    function getStatusBadge(status) {
        const badges = {
            'ACTIVE': '<span class="badge bg-success">啟用</span>',
            'SUSPENDED': '<span class="badge bg-danger">停權</span>',
            'PENDING': '<span class="badge bg-warning">待審核</span>',
            'TERMINATED': '<span class="badge bg-secondary">已終止</span>'
        };
        return badges[status] || status;
    }

    // 顯示 Loading
    function showLoading(show) {
        document.getElementById('loading').classList.toggle('d-none', !show);
        document.getElementById('dataTable').classList.toggle('d-none', show);
    }

    // 開啟新增 Modal
    function openCreateModal() {
        document.getElementById('formModalTitle').textContent = '新增店家';
        document.getElementById('dataForm').reset();
        document.getElementById('formId').value = '';
        new bootstrap.Modal(document.getElementById('formModal')).show();
    }

    // 編輯
    async function editData(id) {
        try {
            const result = await api.get(`/api/admin/tenants/${id}`);
            if (result.success) {
                const data = result.data;
                document.getElementById('formModalTitle').textContent = '編輯店家';
                document.getElementById('formId').value = data.id;
                document.getElementById('formName').value = data.name;
                document.getElementById('formCode').value = data.code;
                document.getElementById('formContactName').value = data.contactName || '';
                document.getElementById('formContactPhone').value = data.contactPhone || '';
                document.getElementById('formEmail').value = data.email || '';
                new bootstrap.Modal(document.getElementById('formModal')).show();
            }
        } catch (error) {
            showError(error.message);
        }
    }

    // 儲存
    async function saveData() {
        const id = document.getElementById('formId').value;
        const data = {
            name: document.getElementById('formName').value,
            code: document.getElementById('formCode').value,
            contactName: document.getElementById('formContactName').value,
            contactPhone: document.getElementById('formContactPhone').value,
            email: document.getElementById('formEmail').value
        };

        try {
            let result;
            if (id) {
                result = await api.put(`/api/admin/tenants/${id}`, data);
            } else {
                result = await api.post('/api/admin/tenants', data);
            }

            if (result.success) {
                showSuccess(id ? '更新成功' : '新增成功');
                bootstrap.Modal.getInstance(document.getElementById('formModal')).hide();
                loadData();
            }
        } catch (error) {
            showError(error.message);
        }
    }
</script>
```

---

## 4. 詳情頁模板

### admin/tenant-detail.html
```html
<th:block th:replace="~{admin/layout :: layout(~{::content})}">
    <th:block th:fragment="content">
        <!-- 頁面標題 -->
        <div class="d-flex justify-content-between align-items-center mb-4">
            <div>
                <a th:href="@{/admin/tenants}" class="text-muted text-decoration-none">
                    <i class="bi bi-arrow-left"></i> 返回列表
                </a>
                <h4 class="mt-2" id="tenantName">店家詳情</h4>
            </div>
            <div>
                <button class="btn btn-outline-primary" onclick="editData()">
                    <i class="bi bi-pencil"></i> 編輯
                </button>
            </div>
        </div>

        <div class="row">
            <!-- 左側資訊 -->
            <div class="col-md-8">
                <!-- 基本資訊卡片 -->
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="mb-0">基本資訊</h5>
                    </div>
                    <div class="card-body">
                        <dl class="row mb-0" id="basicInfo">
                            <!-- 動態填入 -->
                        </dl>
                    </div>
                </div>

                <!-- 功能列表卡片 -->
                <div class="card">
                    <div class="card-header d-flex justify-content-between align-items-center">
                        <h5 class="mb-0">功能開關</h5>
                        <button class="btn btn-sm btn-success">
                            <i class="bi bi-plus"></i> 開通功能
                        </button>
                    </div>
                    <div class="card-body">
                        <div class="table-responsive">
                            <table class="table">
                                <thead>
                                    <tr>
                                        <th>功能</th>
                                        <th>狀態</th>
                                        <th>到期日</th>
                                        <th>操作</th>
                                    </tr>
                                </thead>
                                <tbody id="featureTable">
                                    <!-- 動態填入 -->
                                </tbody>
                            </table>
                        </div>
                    </div>
                </div>
            </div>

            <!-- 右側資訊 -->
            <div class="col-md-4">
                <!-- 點數卡片 -->
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="mb-0">點數餘額</h5>
                        <h2 class="text-primary mt-2" id="pointBalance">0</h2>
                        <button class="btn btn-sm btn-outline-primary">
                            <i class="bi bi-plus-circle"></i> 調整點數
                        </button>
                    </div>
                </div>

                <!-- 統計卡片 -->
                <div class="card mb-4">
                    <div class="card-header">
                        <h5 class="mb-0">統計資訊</h5>
                    </div>
                    <div class="card-body">
                        <div class="d-flex justify-content-between mb-2">
                            <span>總預約數</span>
                            <strong id="totalBookings">0</strong>
                        </div>
                        <div class="d-flex justify-content-between mb-2">
                            <span>總顧客數</span>
                            <strong id="totalCustomers">0</strong>
                        </div>
                        <div class="d-flex justify-content-between">
                            <span>總營收</span>
                            <strong id="totalRevenue">$0</strong>
                        </div>
                    </div>
                </div>

                <!-- 操作卡片 -->
                <div class="card">
                    <div class="card-header">
                        <h5 class="mb-0">狀態操作</h5>
                    </div>
                    <div class="card-body">
                        <div class="d-grid gap-2">
                            <button id="btnSuspend" class="btn btn-warning" onclick="changeTenantStatus('SUSPENDED')">
                                <i class="bi bi-pause-circle"></i> 停權
                            </button>
                            <button id="btnActivate" class="btn btn-success" onclick="changeTenantStatus('ACTIVE')">
                                <i class="bi bi-play-circle"></i> 啟用
                            </button>
                            <button id="btnTerminate" class="btn btn-danger" onclick="changeTenantStatus('TERMINATED')">
                                <i class="bi bi-x-circle"></i> 終止
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </th:block>
</th:block>

<script>
    const tenantId = window.location.pathname.split('/').pop();

    document.addEventListener('DOMContentLoaded', function() {
        loadTenantDetail();
    });

    async function loadTenantDetail() {
        try {
            const result = await api.get(`/api/admin/tenants/${tenantId}`);
            if (result.success) {
                renderTenantDetail(result.data);
            }
        } catch (error) {
            showError(error.message);
        }
    }

    function renderTenantDetail(data) {
        document.getElementById('tenantName').textContent = data.name;
        document.getElementById('pointBalance').textContent = formatNumber(data.pointBalance);

        // 基本資訊
        document.getElementById('basicInfo').innerHTML = `
            <dt class="col-sm-4">店家代碼</dt>
            <dd class="col-sm-8">${data.code}</dd>
            <dt class="col-sm-4">狀態</dt>
            <dd class="col-sm-8">${getStatusBadge(data.status)}</dd>
            <dt class="col-sm-4">聯絡人</dt>
            <dd class="col-sm-8">${data.contactName || '-'}</dd>
            <dt class="col-sm-4">聯絡電話</dt>
            <dd class="col-sm-8">${data.contactPhone || '-'}</dd>
            <dt class="col-sm-4">Email</dt>
            <dd class="col-sm-8">${data.email || '-'}</dd>
            <dt class="col-sm-4">建立時間</dt>
            <dd class="col-sm-8">${formatDateTime(data.createdAt)}</dd>
        `;

        // 功能列表
        renderFeatureTable(data.features || []);

        // 更新按鈕狀態
        document.getElementById('btnSuspend').style.display = data.status === 'ACTIVE' ? 'block' : 'none';
        document.getElementById('btnActivate').style.display = data.status !== 'ACTIVE' ? 'block' : 'none';
    }

    function renderFeatureTable(features) {
        const tbody = document.getElementById('featureTable');
        if (features.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center text-muted">尚未開通任何功能</td></tr>';
            return;
        }

        tbody.innerHTML = features.map(f => `
            <tr>
                <td>${f.featureName}</td>
                <td>${getFeatureStatusBadge(f.status)}</td>
                <td>${f.expireDate ? formatDate(f.expireDate) : '永久'}</td>
                <td>
                    <button class="btn btn-sm btn-outline-danger" onclick="disableFeature('${f.featureCode}')">
                        關閉
                    </button>
                </td>
            </tr>
        `).join('');
    }

    async function changeTenantStatus(status) {
        if (!confirm(`確定要將店家狀態改為「${status}」嗎？`)) return;

        try {
            const result = await api.put(`/api/admin/tenants/${tenantId}/status`, { status });
            if (result.success) {
                showSuccess('狀態更新成功');
                loadTenantDetail();
            }
        } catch (error) {
            showError(error.message);
        }
    }
</script>
```

---

## 5. common.js 共用函式

### static/js/common.js
```javascript
/**
 * 共用 JavaScript 函式
 */

// ========================================
// Token 管理
// ========================================

function getToken() {
    return localStorage.getItem('token');
}

function setToken(token) {
    localStorage.setItem('token', token);
}

function removeToken() {
    localStorage.removeItem('token');
    localStorage.removeItem('userType');
}

// ========================================
// API 呼叫
// ========================================

const api = {
    /**
     * GET 請求
     */
    async get(url) {
        return this.request(url, { method: 'GET' });
    },

    /**
     * POST 請求
     */
    async post(url, data) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    /**
     * PUT 請求
     */
    async put(url, data) {
        return this.request(url, {
            method: 'PUT',
            body: JSON.stringify(data)
        });
    },

    /**
     * DELETE 請求
     */
    async delete(url) {
        return this.request(url, { method: 'DELETE' });
    },

    /**
     * 通用請求方法
     */
    async request(url, options = {}) {
        const token = getToken();

        const config = {
            headers: {
                'Content-Type': 'application/json',
                ...(token && { 'Authorization': `Bearer ${token}` })
            },
            ...options
        };

        try {
            const response = await fetch(url, config);

            // 401 未授權，導向登入頁
            if (response.status === 401) {
                removeToken();
                const userType = localStorage.getItem('userType');
                window.location.href = userType === 'admin' ? '/admin/login' : '/tenant/login';
                throw new Error('登入已過期，請重新登入');
            }

            const result = await response.json();

            if (!result.success) {
                throw new Error(result.message || '請求失敗');
            }

            return result;
        } catch (error) {
            if (error.name === 'TypeError') {
                throw new Error('網路連線失敗，請檢查網路');
            }
            throw error;
        }
    }
};

// ========================================
// 通知
// ========================================

/**
 * 顯示成功訊息
 */
function showSuccess(message) {
    showToast(message, 'success');
}

/**
 * 顯示錯誤訊息
 */
function showError(message) {
    showToast(message, 'danger');
}

/**
 * 顯示警告訊息
 */
function showWarning(message) {
    showToast(message, 'warning');
}

/**
 * 顯示 Toast 通知
 */
function showToast(message, type = 'info') {
    // 建立 Toast 容器（如果不存在）
    let container = document.getElementById('toastContainer');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
    }

    // 建立 Toast
    const toastId = 'toast-' + Date.now();
    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white bg-${type} border-0" role="alert">
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', toastHtml);

    const toastEl = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastEl, { delay: 3000 });
    toast.show();

    // 自動移除
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
}

/**
 * 確認對話框
 */
function showConfirm(message) {
    return confirm(message);
}

// ========================================
// 格式化
// ========================================

/**
 * 格式化日期
 */
function formatDate(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleDateString('zh-TW');
}

/**
 * 格式化日期時間
 */
function formatDateTime(dateStr) {
    if (!dateStr) return '-';
    const date = new Date(dateStr);
    return date.toLocaleString('zh-TW');
}

/**
 * 格式化金額
 */
function formatMoney(amount) {
    if (amount == null) return '$0';
    return '$' + Number(amount).toLocaleString('zh-TW');
}

/**
 * 格式化數字
 */
function formatNumber(num) {
    if (num == null) return '0';
    return Number(num).toLocaleString('zh-TW');
}

// ========================================
// 分頁
// ========================================

/**
 * 渲染分頁
 */
function renderPagination(pageData, onPageClick) {
    const container = document.getElementById('pagination');
    if (!container) return;

    const { page, totalPages, first, last } = pageData;

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = '<ul class="pagination justify-content-center">';

    // 上一頁
    html += `
        <li class="page-item ${first ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="${!first ? `onPageClick(${page - 1})` : ''}">
                <i class="bi bi-chevron-left"></i>
            </a>
        </li>
    `;

    // 頁碼
    const startPage = Math.max(0, page - 2);
    const endPage = Math.min(totalPages - 1, page + 2);

    for (let i = startPage; i <= endPage; i++) {
        html += `
            <li class="page-item ${i === page ? 'active' : ''}">
                <a class="page-link" href="javascript:void(0)" onclick="onPageClick(${i})">${i + 1}</a>
            </li>
        `;
    }

    // 下一頁
    html += `
        <li class="page-item ${last ? 'disabled' : ''}">
            <a class="page-link" href="javascript:void(0)" onclick="${!last ? `onPageClick(${page + 1})` : ''}">
                <i class="bi bi-chevron-right"></i>
            </a>
        </li>
    `;

    html += '</ul>';
    container.innerHTML = html;
}

// ========================================
// 工具函式
// ========================================

/**
 * 登出
 */
function logout() {
    removeToken();
    const userType = localStorage.getItem('userType');
    window.location.href = userType === 'admin' ? '/admin/login' : '/tenant/login';
}

/**
 * 切換側邊欄
 */
function toggleSidebar() {
    document.getElementById('sidebar').classList.toggle('collapsed');
}

/**
 * 狀態標籤
 */
function getStatusBadge(status) {
    const badges = {
        'ACTIVE': '<span class="badge bg-success">啟用</span>',
        'SUSPENDED': '<span class="badge bg-danger">停權</span>',
        'PENDING': '<span class="badge bg-warning">待審核</span>',
        'TERMINATED': '<span class="badge bg-secondary">已終止</span>',
        'PENDING_CONFIRMATION': '<span class="badge bg-warning">待確認</span>',
        'CONFIRMED': '<span class="badge bg-info">已確認</span>',
        'COMPLETED': '<span class="badge bg-success">已完成</span>',
        'CANCELLED': '<span class="badge bg-secondary">已取消</span>',
        'NO_SHOW': '<span class="badge bg-danger">爽約</span>'
    };
    return badges[status] || `<span class="badge bg-secondary">${status}</span>`;
}

/**
 * 功能狀態標籤
 */
function getFeatureStatusBadge(status) {
    const badges = {
        'ENABLED': '<span class="badge bg-success">已啟用</span>',
        'DISABLED': '<span class="badge bg-secondary">已停用</span>',
        'EXPIRED': '<span class="badge bg-warning">已過期</span>'
    };
    return badges[status] || status;
}
```

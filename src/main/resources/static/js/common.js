/**
 * 共用 JavaScript
 *
 * 預約平台 - 共用函式庫
 */

// ========================================
// 常數
// ========================================

const API_BASE_URL = '';
const TOKEN_KEY = 'booking_platform_token';
const REFRESH_TOKEN_KEY = 'booking_platform_refresh_token';
const USER_KEY = 'booking_platform_user';

// ========================================
// Token 管理
// ========================================

/**
 * 取得 Token
 */
function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

/**
 * 設定 Token
 */
function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

/**
 * 取得 Refresh Token
 */
function getRefreshToken() {
    return localStorage.getItem(REFRESH_TOKEN_KEY);
}

/**
 * 設定 Refresh Token
 */
function setRefreshToken(token) {
    localStorage.setItem(REFRESH_TOKEN_KEY, token);
}

/**
 * 移除所有 Token
 */
function removeTokens() {
    localStorage.removeItem(TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(USER_KEY);
}

/**
 * 取得當前用戶
 */
function getCurrentUser() {
    const userJson = localStorage.getItem(USER_KEY);
    return userJson ? JSON.parse(userJson) : null;
}

/**
 * 設定當前用戶
 */
function setCurrentUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
}

/**
 * 檢查是否已登入
 */
function isLoggedIn() {
    return !!getToken();
}

/**
 * 登出
 */
function logout(redirectUrl = '/admin/login') {
    removeTokens();
    window.location.href = redirectUrl;
}

// ========================================
// API 呼叫
// ========================================

/**
 * API 呼叫物件
 */
const api = {
    /**
     * GET 請求
     */
    async get(url, params = {}) {
        const queryString = new URLSearchParams(params).toString();
        const fullUrl = queryString ? `${url}?${queryString}` : url;
        return this.request(fullUrl, { method: 'GET' });
    },

    /**
     * POST 請求
     */
    async post(url, data = {}) {
        return this.request(url, {
            method: 'POST',
            body: JSON.stringify(data)
        });
    },

    /**
     * PUT 請求
     */
    async put(url, data = {}) {
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
        const headers = {
            'Content-Type': 'application/json',
            ...options.headers
        };

        // 加入 Token
        const token = getToken();
        if (token) {
            headers['Authorization'] = `Bearer ${token}`;
        }

        try {
            showLoading();

            const response = await fetch(API_BASE_URL + url, {
                ...options,
                headers
            });

            hideLoading();

            // 處理 401 未授權
            if (response.status === 401) {
                // 嘗試刷新 Token
                const refreshed = await this.refreshToken();
                if (refreshed) {
                    // 重試請求
                    headers['Authorization'] = `Bearer ${getToken()}`;
                    const retryResponse = await fetch(API_BASE_URL + url, {
                        ...options,
                        headers
                    });
                    return this.handleResponse(retryResponse);
                } else {
                    // 登出
                    logout();
                    return null;
                }
            }

            return this.handleResponse(response);

        } catch (error) {
            hideLoading();
            console.error('API 請求錯誤:', error);
            showError('網路連線錯誤，請稍後再試');
            throw error;
        }
    },

    /**
     * 處理回應
     */
    async handleResponse(response) {
        const data = await response.json();

        if (!response.ok) {
            const message = data.message || '操作失敗';
            showError(message);
            throw new Error(message);
        }

        if (data.success === false) {
            const message = data.message || '操作失敗';
            showError(message);
            throw new Error(message);
        }

        return data;
    },

    /**
     * 刷新 Token
     */
    async refreshToken() {
        const refreshToken = getRefreshToken();
        if (!refreshToken) {
            return false;
        }

        try {
            const response = await fetch(API_BASE_URL + '/api/auth/refresh', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ refreshToken })
            });

            if (response.ok) {
                const data = await response.json();
                if (data.success && data.data) {
                    setToken(data.data.accessToken);
                    if (data.data.refreshToken) {
                        setRefreshToken(data.data.refreshToken);
                    }
                    return true;
                }
            }
            return false;
        } catch (error) {
            console.error('刷新 Token 失敗:', error);
            return false;
        }
    }
};

// ========================================
// 通知
// ========================================

/**
 * 顯示成功通知
 */
function showSuccess(message, duration = 3000) {
    showToast(message, 'success', duration);
}

/**
 * 顯示錯誤通知
 */
function showError(message, duration = 5000) {
    showToast(message, 'error', duration);
}

/**
 * 顯示警告通知
 */
function showWarning(message, duration = 4000) {
    showToast(message, 'warning', duration);
}

/**
 * 顯示資訊通知
 */
function showInfo(message, duration = 3000) {
    showToast(message, 'info', duration);
}

/**
 * 顯示 Toast 通知
 */
function showToast(message, type = 'info', duration = 3000) {
    // 確保容器存在
    let container = document.querySelector('.toast-container');
    if (!container) {
        container = document.createElement('div');
        container.className = 'toast-container';
        document.body.appendChild(container);
    }

    // 建立 Toast
    const toast = document.createElement('div');
    toast.className = `toast toast-${type} show`;
    toast.setAttribute('role', 'alert');
    toast.innerHTML = `
        <div class="toast-header">
            <i class="bi ${getToastIcon(type)} me-2"></i>
            <strong class="me-auto">${getToastTitle(type)}</strong>
            <button type="button" class="btn-close" onclick="this.closest('.toast').remove()"></button>
        </div>
        <div class="toast-body">${message}</div>
    `;

    container.appendChild(toast);

    // 自動關閉
    if (duration > 0) {
        setTimeout(() => {
            toast.remove();
        }, duration);
    }
}

/**
 * 取得 Toast 圖示
 */
function getToastIcon(type) {
    const icons = {
        success: 'bi-check-circle-fill text-success',
        error: 'bi-x-circle-fill text-danger',
        warning: 'bi-exclamation-triangle-fill text-warning',
        info: 'bi-info-circle-fill text-info'
    };
    return icons[type] || icons.info;
}

/**
 * 取得 Toast 標題
 */
function getToastTitle(type) {
    const titles = {
        success: '成功',
        error: '錯誤',
        warning: '警告',
        info: '提示'
    };
    return titles[type] || titles.info;
}

/**
 * 顯示確認對話框
 */
function showConfirm(message, title = '確認') {
    return new Promise((resolve) => {
        // 使用 Bootstrap Modal
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.tabIndex = -1;
        modal.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">${title}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p>${message}</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">取消</button>
                        <button type="button" class="btn btn-primary" id="confirmBtn">確定</button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        const bsModal = new bootstrap.Modal(modal);

        modal.querySelector('#confirmBtn').addEventListener('click', () => {
            bsModal.hide();
            resolve(true);
        });

        modal.addEventListener('hidden.bs.modal', () => {
            modal.remove();
            resolve(false);
        });

        bsModal.show();
    });
}

// ========================================
// Loading
// ========================================

let loadingCount = 0;
let loadingOverlay = null;

/**
 * 顯示 Loading
 */
function showLoading() {
    loadingCount++;
    if (loadingCount === 1) {
        if (!loadingOverlay) {
            loadingOverlay = document.createElement('div');
            loadingOverlay.className = 'loading-overlay';
            loadingOverlay.innerHTML = '<div class="loading-spinner"></div>';
        }
        document.body.appendChild(loadingOverlay);
    }
}

/**
 * 隱藏 Loading
 */
function hideLoading() {
    loadingCount = Math.max(0, loadingCount - 1);
    if (loadingCount === 0 && loadingOverlay && loadingOverlay.parentNode) {
        loadingOverlay.remove();
    }
}

// ========================================
// 格式化
// ========================================

/**
 * 格式化日期
 */
function formatDate(date) {
    if (!date) return '-';
    const d = new Date(date);
    return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

/**
 * 格式化日期時間
 */
function formatDateTime(datetime) {
    if (!datetime) return '-';
    const d = new Date(datetime);
    return `${formatDate(d)} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/**
 * 格式化時間
 */
function formatTime(time) {
    if (!time) return '-';
    if (typeof time === 'string' && time.includes(':')) {
        return time.substring(0, 5);
    }
    const d = new Date(time);
    return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
}

/**
 * 格式化金額
 */
function formatMoney(amount) {
    if (amount === null || amount === undefined) return '-';
    return new Intl.NumberFormat('zh-TW', {
        style: 'currency',
        currency: 'TWD',
        minimumFractionDigits: 0
    }).format(amount);
}

/**
 * 格式化數字（含千分位）
 */
function formatNumber(num) {
    if (num === null || num === undefined) return '-';
    return new Intl.NumberFormat('zh-TW').format(num);
}

/**
 * 格式化百分比
 */
function formatPercent(value, decimals = 1) {
    if (value === null || value === undefined) return '-';
    return `${(value * 100).toFixed(decimals)}%`;
}

// ========================================
// 分頁
// ========================================

/**
 * 渲染分頁
 */
function renderPagination(pageData, onPageClick, containerId = 'pagination') {
    const container = document.getElementById(containerId);
    if (!container) return;

    const { page, totalPages, first, last } = pageData;

    if (totalPages <= 1) {
        container.innerHTML = '';
        return;
    }

    let html = '<nav><ul class="pagination justify-content-center mb-0">';

    // 上一頁
    html += `
        <li class="page-item ${first ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${page - 1}" ${first ? 'tabindex="-1"' : ''}>
                <i class="bi bi-chevron-left"></i>
            </a>
        </li>
    `;

    // 頁碼
    const startPage = Math.max(0, page - 2);
    const endPage = Math.min(totalPages - 1, page + 2);

    if (startPage > 0) {
        html += `<li class="page-item"><a class="page-link" href="#" data-page="0">1</a></li>`;
        if (startPage > 1) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
    }

    for (let i = startPage; i <= endPage; i++) {
        html += `
            <li class="page-item ${i === page ? 'active' : ''}">
                <a class="page-link" href="#" data-page="${i}">${i + 1}</a>
            </li>
        `;
    }

    if (endPage < totalPages - 1) {
        if (endPage < totalPages - 2) {
            html += `<li class="page-item disabled"><span class="page-link">...</span></li>`;
        }
        html += `<li class="page-item"><a class="page-link" href="#" data-page="${totalPages - 1}">${totalPages}</a></li>`;
    }

    // 下一頁
    html += `
        <li class="page-item ${last ? 'disabled' : ''}">
            <a class="page-link" href="#" data-page="${page + 1}" ${last ? 'tabindex="-1"' : ''}>
                <i class="bi bi-chevron-right"></i>
            </a>
        </li>
    `;

    html += '</ul></nav>';

    container.innerHTML = html;

    // 綁定事件
    container.querySelectorAll('.page-link[data-page]').forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            const pageNum = parseInt(link.dataset.page);
            if (!isNaN(pageNum) && onPageClick) {
                onPageClick(pageNum);
            }
        });
    });
}

// ========================================
// 狀態徽章
// ========================================

/**
 * 取得狀態徽章 HTML
 */
function getStatusBadge(status, statusMap = {}) {
    const defaultMap = {
        // 通用狀態
        ACTIVE: { label: '啟用', class: 'bg-success' },
        INACTIVE: { label: '停用', class: 'bg-secondary' },
        PENDING: { label: '待處理', class: 'bg-warning text-dark' },
        APPROVED: { label: '已核准', class: 'bg-success' },
        REJECTED: { label: '已拒絕', class: 'bg-danger' },
        CANCELLED: { label: '已取消', class: 'bg-secondary' },
        // 租戶狀態
        SUSPENDED: { label: '已停權', class: 'bg-warning text-dark' },
        FROZEN: { label: '已凍結', class: 'bg-danger' },
        // 預約狀態
        CONFIRMED: { label: '已確認', class: 'bg-primary' },
        IN_PROGRESS: { label: '進行中', class: 'bg-info' },
        COMPLETED: { label: '已完成', class: 'bg-success' },
        NO_SHOW: { label: '爽約', class: 'bg-danger' }
    };

    const map = { ...defaultMap, ...statusMap };
    const config = map[status] || { label: status, class: 'bg-secondary' };

    return `<span class="badge ${config.class}">${config.label}</span>`;
}

// ========================================
// 表單處理
// ========================================

/**
 * 取得表單資料
 */
function getFormData(formId) {
    const form = document.getElementById(formId);
    if (!form) return {};

    const formData = new FormData(form);
    const data = {};

    for (const [key, value] of formData.entries()) {
        // 處理數字
        if (form.elements[key]?.type === 'number') {
            data[key] = value ? parseFloat(value) : null;
        }
        // 處理 checkbox
        else if (form.elements[key]?.type === 'checkbox') {
            data[key] = form.elements[key].checked;
        }
        // 處理空字串
        else {
            data[key] = value || null;
        }
    }

    return data;
}

/**
 * 設定表單資料
 */
function setFormData(formId, data) {
    const form = document.getElementById(formId);
    if (!form || !data) return;

    for (const [key, value] of Object.entries(data)) {
        const element = form.elements[key];
        if (!element) continue;

        if (element.type === 'checkbox') {
            element.checked = !!value;
        } else if (element.type === 'radio') {
            const radio = form.querySelector(`input[name="${key}"][value="${value}"]`);
            if (radio) radio.checked = true;
        } else {
            element.value = value ?? '';
        }
    }
}

/**
 * 重置表單
 */
function resetForm(formId) {
    const form = document.getElementById(formId);
    if (form) form.reset();
}

/**
 * 驗證表單
 */
function validateForm(formId) {
    const form = document.getElementById(formId);
    if (!form) return false;

    if (!form.checkValidity()) {
        form.classList.add('was-validated');
        return false;
    }

    return true;
}

// ========================================
// 工具函式
// ========================================

/**
 * 防抖
 */
function debounce(func, wait = 300) {
    let timeout;
    return function (...args) {
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(this, args), wait);
    };
}

/**
 * 節流
 */
function throttle(func, limit = 300) {
    let inThrottle;
    return function (...args) {
        if (!inThrottle) {
            func.apply(this, args);
            inThrottle = true;
            setTimeout(() => inThrottle = false, limit);
        }
    };
}

/**
 * 深拷貝
 */
function deepClone(obj) {
    return JSON.parse(JSON.stringify(obj));
}

/**
 * 取得 URL 參數
 */
function getUrlParam(name) {
    const params = new URLSearchParams(window.location.search);
    return params.get(name);
}

/**
 * 更新 URL 參數
 */
function updateUrlParams(params) {
    const url = new URL(window.location);
    for (const [key, value] of Object.entries(params)) {
        if (value === null || value === undefined || value === '') {
            url.searchParams.delete(key);
        } else {
            url.searchParams.set(key, value);
        }
    }
    window.history.replaceState({}, '', url);
}

/**
 * 複製到剪貼簿
 */
async function copyToClipboard(text) {
    try {
        await navigator.clipboard.writeText(text);
        showSuccess('已複製到剪貼簿');
        return true;
    } catch (err) {
        showError('複製失敗');
        return false;
    }
}

/**
 * 下載檔案
 */
function downloadFile(url, filename) {
    const link = document.createElement('a');
    link.href = url;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
}

/**
 * 匯出 CSV
 */
function exportToCsv(data, filename, headers) {
    if (!data || !data.length) {
        showWarning('沒有資料可匯出');
        return;
    }

    const keys = headers ? Object.keys(headers) : Object.keys(data[0]);
    const headerRow = headers ? Object.values(headers) : keys;

    const csvContent = [
        headerRow.join(','),
        ...data.map(row => keys.map(key => {
            let value = row[key] ?? '';
            // 處理包含逗號或換行的值
            if (typeof value === 'string' && (value.includes(',') || value.includes('\n'))) {
                value = `"${value.replace(/"/g, '""')}"`;
            }
            return value;
        }).join(','))
    ].join('\n');

    const blob = new Blob(['\ufeff' + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    downloadFile(url, filename);
    URL.revokeObjectURL(url);
}

// ========================================
// 初始化
// ========================================

document.addEventListener('DOMContentLoaded', () => {
    // 檢查 Bootstrap 是否已載入
    if (typeof bootstrap !== 'undefined') {
        // 初始化 Bootstrap Tooltips
        const tooltipTriggerList = document.querySelectorAll('[data-bs-toggle="tooltip"]');
        tooltipTriggerList.forEach(el => new bootstrap.Tooltip(el));

        // 初始化 Bootstrap Popovers
        const popoverTriggerList = document.querySelectorAll('[data-bs-toggle="popover"]');
        popoverTriggerList.forEach(el => new bootstrap.Popover(el));
    }
});

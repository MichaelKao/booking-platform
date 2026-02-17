/**
 * Admin 後台 JavaScript
 *
 * 預約平台 - 超級管理後台專用 JS
 */

// ========================================
// 初始化
// ========================================

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    initUserDropdown();
    checkAuth();
});

/**
 * 檢查認證狀態
 */
function checkAuth() {
    // 如果在登入頁面，不檢查
    if (window.location.pathname.includes('/login')) {
        return;
    }

    // 檢查是否有 Token
    if (!isLoggedIn()) {
        window.location.href = '/admin/login';
    }
}

// ========================================
// 側邊欄
// ========================================

/**
 * 初始化側邊欄
 */
function initSidebar() {
    const sidebar = document.getElementById('sidebar');
    const toggleBtn = document.getElementById('sidebarToggle');
    const toggleBtnMobile = document.getElementById('sidebarToggleMobile');
    const overlay = document.querySelector('.sidebar-overlay');

    if (!sidebar) return;

    // 從 localStorage 讀取狀態
    const isCollapsed = localStorage.getItem('admin_sidebar_collapsed') === 'true';
    if (isCollapsed && window.innerWidth > 768) {
        sidebar.classList.add('collapsed');
    }

    // 桌面版切換
    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
            localStorage.setItem('admin_sidebar_collapsed', sidebar.classList.contains('collapsed'));
        });
    }

    // 手機版切換
    if (toggleBtnMobile) {
        toggleBtnMobile.addEventListener('click', () => {
            sidebar.classList.toggle('show');
            if (overlay) overlay.classList.toggle('show');
        });
    }

    // 點擊遮罩關閉
    if (overlay) {
        overlay.addEventListener('click', () => {
            sidebar.classList.remove('show');
            overlay.classList.remove('show');
        });
    }

    // 響應式處理
    window.addEventListener('resize', () => {
        if (window.innerWidth > 768) {
            sidebar.classList.remove('show');
            if (overlay) overlay.classList.remove('show');
        }
    });
}

// ========================================
// 用戶下拉選單
// ========================================

/**
 * 初始化用戶下拉選單
 */
function initUserDropdown() {
    const dropdown = document.getElementById('userDropdown');
    if (!dropdown) return;

    // 載入用戶資訊
    const user = getCurrentUser();
    if (user) {
        const avatarEl = dropdown.querySelector('.user-avatar');
        const nameEl = dropdown.querySelector('.user-name');

        if (avatarEl && user.name) {
            avatarEl.textContent = user.name.charAt(0).toUpperCase();
        }
        if (nameEl && user.name) {
            nameEl.textContent = user.name;
        }
    }
}

/**
 * 登出
 */
function adminLogout() {
    showConfirm('確定要登出嗎？').then(confirmed => {
        if (confirmed) {
            logout('/admin/login');
        }
    });
}

// ========================================
// 登入
// ========================================

/**
 * Admin 登入
 */
async function adminLogin(event) {
    if (event) event.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    if (!username || !password) {
        showError('請輸入帳號和密碼');
        return;
    }

    try {
        const result = await api.post('/api/auth/admin/login', {
            username,
            password
        });

        if (result.success && result.data) {
            if (!result.data.accessToken) {
                showError('登入回應異常，請稍後再試');
                return;
            }
            setToken(result.data.accessToken);
            if (result.data.refreshToken) {
                setRefreshToken(result.data.refreshToken);
            }
            // 用戶資訊直接在 data 中
            setCurrentUser({
                id: result.data.userId,
                name: result.data.displayName || result.data.username,
                username: result.data.username,
                role: result.data.role
            });

            showSuccess('登入成功');
            setTimeout(() => {
                window.location.href = '/admin/dashboard';
            }, 500);
        }
    } catch (error) {
        console.error('登入失敗:', error);
        showError(error.message || '登入失敗，請檢查帳號密碼');
    }
}

// ========================================
// 租戶管理
// ========================================

/**
 * 租戶狀態對應
 */
const TENANT_STATUS_MAP = {
    PENDING: { label: '待審核', class: 'bg-warning text-dark' },
    ACTIVE: { label: '啟用中', class: 'bg-success' },
    SUSPENDED: { label: '已停權', class: 'bg-secondary' },
    FROZEN: { label: '已凍結', class: 'bg-danger' },
    CANCELLED: { label: '已取消', class: 'bg-dark' }
};

/**
 * 取得租戶狀態徽章
 */
function getTenantStatusBadge(status) {
    return getStatusBadge(status, TENANT_STATUS_MAP);
}

/**
 * 啟用租戶
 */
async function activateTenant(tenantId) {
    const confirmed = await showConfirm('確定要啟用此店家嗎？');
    if (!confirmed) return;

    try {
        await api.put(`/api/admin/tenants/${tenantId}/status`, { status: 'ACTIVE' });
        showSuccess('店家已啟用');
        if (typeof loadTenants === 'function') loadTenants();
    } catch (error) {
        console.error('啟用失敗:', error);
        showError(error.message || '啟用失敗');
    }
}

/**
 * 停權租戶
 */
async function suspendTenant(tenantId) {
    const confirmed = await showConfirm('確定要停權此店家嗎？停權後店家將無法使用系統。');
    if (!confirmed) return;

    try {
        await api.put(`/api/admin/tenants/${tenantId}/status`, { status: 'SUSPENDED' });
        showSuccess('店家已停權');
        if (typeof loadTenants === 'function') loadTenants();
    } catch (error) {
        console.error('停權失敗:', error);
        showError(error.message || '停權失敗');
    }
}

// ========================================
// 儲值審核
// ========================================

/**
 * 儲值狀態對應
 */
const TOPUP_STATUS_MAP = {
    PENDING: { label: '待審核', class: 'bg-warning text-dark' },
    APPROVED: { label: '已核准', class: 'bg-success' },
    REJECTED: { label: '已拒絕', class: 'bg-danger' },
    CANCELLED: { label: '已取消', class: 'bg-secondary' }
};

/**
 * 取得儲值狀態徽章
 */
function getTopupStatusBadge(status) {
    return getStatusBadge(status, TOPUP_STATUS_MAP);
}

/**
 * 核准儲值
 */
async function approveTopup(topupId) {
    const confirmed = await showConfirm('確定要核准此儲值申請嗎？');
    if (!confirmed) return;

    try {
        await api.post(`/api/admin/point-topups/${topupId}/approve`);
        showSuccess('儲值申請已核准');
        if (typeof loadTopups === 'function') loadTopups();
    } catch (error) {
        console.error('核准失敗:', error);
        showError(error.message || '核准失敗');
    }
}

/**
 * 拒絕儲值
 */
async function rejectTopup(topupId) {
    const reason = prompt('請輸入拒絕原因：');
    if (reason === null) return;

    try {
        await api.post(`/api/admin/point-topups/${topupId}/reject`, { reason });
        showSuccess('儲值申請已拒絕');
        if (typeof loadTopups === 'function') loadTopups();
    } catch (error) {
        console.error('拒絕失敗:', error);
        showError(error.message || '拒絕失敗');
    }
}

// ========================================
// 功能管理
// ========================================

/**
 * 功能狀態對應
 */
const FEATURE_STATUS_MAP = {
    ENABLED: { label: '已啟用', class: 'bg-success' },
    DISABLED: { label: '已停用', class: 'bg-secondary' },
    PENDING: { label: '待處理', class: 'bg-warning text-dark' }
};

/**
 * 啟用店家功能
 */
async function enableTenantFeature(tenantId, featureCode) {
    const confirmed = await showConfirm('確定要為此店家啟用此功能嗎？');
    if (!confirmed) return;

    try {
        await api.post(`/api/admin/tenants/${tenantId}/features/${featureCode}/enable`);
        showSuccess('功能已啟用');
        location.reload();
    } catch (error) {
        console.error('啟用失敗:', error);
    }
}

/**
 * 停用店家功能
 */
async function disableTenantFeature(tenantId, featureCode) {
    const confirmed = await showConfirm('確定要為此店家停用此功能嗎？');
    if (!confirmed) return;

    try {
        await api.post(`/api/admin/tenants/${tenantId}/features/${featureCode}/disable`);
        showSuccess('功能已停用');
        location.reload();
    } catch (error) {
        console.error('停用失敗:', error);
    }
}

// ========================================
// 儀表板
// ========================================

/**
 * 載入儀表板資料
 */
async function loadDashboardData() {
    try {
        const result = await api.get('/api/admin/dashboard');
        if (result.success && result.data) {
            renderDashboardStats(result.data);
        }
    } catch (error) {
        console.error('載入儀表板資料失敗:', error);
    }
}

/**
 * 渲染儀表板統計
 */
function renderDashboardStats(data) {
    // 租戶數
    const tenantCountEl = document.getElementById('tenantCount');
    if (tenantCountEl) {
        tenantCountEl.textContent = formatNumber(data.totalTenants || 0);
    }

    // 活躍租戶數
    const activeTenantEl = document.getElementById('activeTenantCount');
    if (activeTenantEl) {
        activeTenantEl.textContent = formatNumber(data.activeTenants || 0);
    }

    // 待審核儲值
    const pendingTopupEl = document.getElementById('pendingTopupCount');
    if (pendingTopupEl) {
        pendingTopupEl.textContent = formatNumber(data.pendingTopUps || 0);
    }

    // 本月儲值金額
    const revenueEl = document.getElementById('monthlyRevenue');
    if (revenueEl) {
        revenueEl.textContent = formatMoney(data.monthlyApprovedAmount || 0);
    }
}

// ========================================
// 匯出
// ========================================

/**
 * 匯出租戶列表
 */
async function exportTenants() {
    try {
        const result = await api.get('/api/admin/tenants', { size: 9999 });
        if (result.success && result.data && result.data.content) {
            const headers = {
                code: '店家代碼',
                name: '店家名稱',
                phone: '聯絡電話',
                email: '電子郵件',
                status: '狀態',
                pointBalance: '點數餘額',
                createdAt: '建立時間'
            };

            const data = result.data.content.map(tenant => ({
                ...tenant,
                status: TENANT_STATUS_MAP[tenant.status]?.label || tenant.status,
                createdAt: formatDateTime(tenant.createdAt)
            }));

            exportToCsv(data, `店家列表_${formatDate(new Date())}.csv`, headers);
        }
    } catch (error) {
        console.error('匯出失敗:', error);
    }
}

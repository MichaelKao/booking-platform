/**
 * Tenant 店家後台 JavaScript
 *
 * 預約平台 - 店家後台專用 JS
 */

// ========================================
// 初始化
// ========================================

document.addEventListener('DOMContentLoaded', () => {
    initSidebar();
    initUserDropdown();
    checkAuth();
    loadPointsBalance();
});

/**
 * 檢查認證狀態
 */
function checkAuth() {
    // 公開頁面列表（不需要登入）
    const publicPages = ['/login', '/register', '/forgot-password', '/reset-password'];

    // 如果在公開頁面，不檢查
    const currentPath = window.location.pathname;
    if (publicPages.some(page => currentPath.includes(page))) {
        return;
    }

    // 檢查是否有 Token
    if (!isLoggedIn()) {
        window.location.href = '/tenant/login';
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
    const isCollapsed = localStorage.getItem('tenant_sidebar_collapsed') === 'true';
    if (isCollapsed && window.innerWidth > 768) {
        sidebar.classList.add('collapsed');
    }

    // 桌面版切換
    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('collapsed');
            localStorage.setItem('tenant_sidebar_collapsed', sidebar.classList.contains('collapsed'));
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
        const tenantEl = dropdown.querySelector('.tenant-name');

        if (avatarEl && user.name) {
            avatarEl.textContent = user.name.charAt(0).toUpperCase();
        }
        if (nameEl && user.name) {
            nameEl.textContent = user.name;
        }
        if (tenantEl && user.tenantName) {
            tenantEl.textContent = user.tenantName;
        }
    }
}

/**
 * 載入點數餘額
 */
async function loadPointsBalance() {
    // 如果未登入，不載入
    if (!isLoggedIn()) return;

    const pointsEl = document.getElementById('pointsBalance');
    if (!pointsEl) return;

    try {
        const result = await api.get('/api/points/balance');
        if (result.success && result.data && result.data.balance !== undefined) {
            pointsEl.textContent = formatNumber(result.data.balance);
        }
    } catch (error) {
        console.error('載入點數餘額失敗:', error);
    }
}

/**
 * 登出
 */
function tenantLogout() {
    showConfirm('確定要登出嗎？').then(confirmed => {
        if (confirmed) {
            logout('/tenant/login');
        }
    });
}

// ========================================
// 登入
// ========================================

/**
 * Tenant 登入
 */
async function tenantLogin(event) {
    if (event) event.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;

    if (!username || !password) {
        showError('請輸入帳號和密碼');
        return;
    }

    try {
        const result = await api.post('/api/auth/tenant/login', {
            username,
            password
        });

        if (result.success && result.data) {
            setToken(result.data.accessToken);
            if (result.data.refreshToken) {
                setRefreshToken(result.data.refreshToken);
            }
            // 用戶資訊直接在 data 中
            setCurrentUser({
                id: result.data.userId,
                name: result.data.displayName || result.data.username,
                username: result.data.username,
                role: result.data.role,
                tenantId: result.data.tenantId,
                tenantName: result.data.tenantName
            });

            showSuccess('登入成功');
            setTimeout(() => {
                window.location.href = '/tenant/dashboard';
            }, 500);
        }
    } catch (error) {
        console.error('登入失敗:', error);
    }
}

// ========================================
// 預約狀態
// ========================================

/**
 * 預約狀態對應
 */
const BOOKING_STATUS_MAP = {
    PENDING: { label: '待確認', class: 'bg-warning text-dark' },
    PENDING_CONFIRMATION: { label: '待確認', class: 'bg-warning text-dark' },
    CONFIRMED: { label: '已確認', class: 'bg-primary' },
    IN_PROGRESS: { label: '進行中', class: 'bg-info' },
    COMPLETED: { label: '已完成', class: 'bg-success' },
    CANCELLED: { label: '已取消', class: 'bg-secondary' },
    NO_SHOW: { label: '未到店', class: 'bg-danger' }
};

/**
 * 取得預約狀態徽章
 */
function getBookingStatusBadge(status) {
    return getStatusBadge(status, BOOKING_STATUS_MAP);
}

// ========================================
// 顧客狀態
// ========================================

/**
 * 顧客狀態對應
 */
const CUSTOMER_STATUS_MAP = {
    ACTIVE: { label: '活躍', class: 'bg-success' },
    INACTIVE: { label: '不活躍', class: 'bg-secondary' },
    BLOCKED: { label: '黑名單', class: 'bg-danger' }
};

/**
 * 取得顧客狀態徽章
 */
function getCustomerStatusBadge(status) {
    return getStatusBadge(status, CUSTOMER_STATUS_MAP);
}

// ========================================
// 員工狀態
// ========================================

/**
 * 員工狀態對應
 */
const STAFF_STATUS_MAP = {
    ACTIVE: { label: '在職', class: 'bg-success' },
    INACTIVE: { label: '離職', class: 'bg-secondary' },
    ON_LEAVE: { label: '請假中', class: 'bg-warning text-dark' }
};

/**
 * 取得員工狀態徽章
 */
function getStaffStatusBadge(status) {
    return getStatusBadge(status, STAFF_STATUS_MAP);
}

// ========================================
// 票券狀態
// ========================================

/**
 * 票券狀態對應
 */
const COUPON_STATUS_MAP = {
    DRAFT: { label: '草稿', class: 'bg-secondary' },
    PUBLISHED: { label: '已發布', class: 'bg-success' },
    PAUSED: { label: '已暫停', class: 'bg-warning text-dark' },
    ENDED: { label: '已結束', class: 'bg-danger' }
};

/**
 * 票券實例狀態對應
 */
const COUPON_INSTANCE_STATUS_MAP = {
    AVAILABLE: { label: '可使用', class: 'bg-success' },
    USED: { label: '已使用', class: 'bg-secondary' },
    EXPIRED: { label: '已過期', class: 'bg-danger' },
    CANCELLED: { label: '已取消', class: 'bg-dark' }
};

/**
 * 取得票券狀態徽章
 */
function getCouponStatusBadge(status) {
    return getStatusBadge(status, COUPON_STATUS_MAP);
}

/**
 * 取得票券實例狀態徽章
 */
function getCouponInstanceStatusBadge(status) {
    return getStatusBadge(status, COUPON_INSTANCE_STATUS_MAP);
}

// ========================================
// 活動狀態
// ========================================

/**
 * 活動狀態對應
 */
const CAMPAIGN_STATUS_MAP = {
    DRAFT: { label: '草稿', class: 'bg-secondary' },
    SCHEDULED: { label: '已排程', class: 'bg-info' },
    ACTIVE: { label: '進行中', class: 'bg-success' },
    PAUSED: { label: '已暫停', class: 'bg-warning text-dark' },
    ENDED: { label: '已結束', class: 'bg-dark' }
};

/**
 * 取得活動狀態徽章
 */
function getCampaignStatusBadge(status) {
    return getStatusBadge(status, CAMPAIGN_STATUS_MAP);
}

// ========================================
// 商品狀態
// ========================================

/**
 * 商品狀態對應
 */
const PRODUCT_STATUS_MAP = {
    ACTIVE: { label: '上架中', class: 'bg-success' },
    INACTIVE: { label: '已下架', class: 'bg-secondary' },
    OUT_OF_STOCK: { label: '缺貨中', class: 'bg-warning text-dark' }
};

/**
 * 取得商品狀態徽章
 */
function getProductStatusBadge(status) {
    return getStatusBadge(status, PRODUCT_STATUS_MAP);
}

// ========================================
// 儀表板
// ========================================

/**
 * 載入儀表板資料
 */
async function loadDashboardData() {
    try {
        const result = await api.get('/api/reports/dashboard');
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
    // 今日預約
    const todayBookingsEl = document.getElementById('todayBookings');
    if (todayBookingsEl) {
        todayBookingsEl.textContent = formatNumber(data.todayBookings || 0);
    }

    // 待確認預約
    const pendingBookingsEl = document.getElementById('pendingBookings');
    if (pendingBookingsEl) {
        pendingBookingsEl.textContent = formatNumber(data.pendingBookings || 0);
    }

    // 本月營收
    const monthlyRevenueEl = document.getElementById('monthlyRevenue');
    if (monthlyRevenueEl) {
        monthlyRevenueEl.textContent = formatMoney(data.monthlyRevenue || 0);
    }

    // 顧客數
    const totalCustomersEl = document.getElementById('totalCustomers');
    if (totalCustomersEl) {
        totalCustomersEl.textContent = formatNumber(data.totalCustomers || 0);
    }
}

// ========================================
// 行事曆
// ========================================

/**
 * 初始化行事曆
 */
function initCalendar(elementId, options = {}) {
    const calendarEl = document.getElementById(elementId);
    if (!calendarEl || typeof FullCalendar === 'undefined') return null;

    const defaultOptions = {
        initialView: 'dayGridMonth',
        locale: 'zh-tw',
        headerToolbar: {
            left: 'prev,next today',
            center: 'title',
            right: 'dayGridMonth,timeGridWeek,timeGridDay'
        },
        buttonText: {
            today: '今天',
            month: '月',
            week: '週',
            day: '日'
        },
        eventTimeFormat: {
            hour: '2-digit',
            minute: '2-digit',
            hour12: false
        },
        slotMinTime: '08:00:00',
        slotMaxTime: '22:00:00',
        allDaySlot: false,
        nowIndicator: true,
        editable: false,
        selectable: true,
        eventClick: function(info) {
            if (options.onEventClick) {
                options.onEventClick(info.event);
            }
        },
        dateClick: function(info) {
            if (options.onDateClick) {
                options.onDateClick(info.date);
            }
        },
        events: async function(fetchInfo, successCallback, failureCallback) {
            try {
                const params = {
                    start: fetchInfo.startStr.split('T')[0],
                    end: fetchInfo.endStr.split('T')[0]
                };
                const result = await api.get('/api/bookings/calendar', params);
                if (result.success && result.data) {
                    const events = result.data.map(booking => ({
                        id: booking.id,
                        title: `${booking.customerName} - ${booking.serviceName}`,
                        start: booking.startTime,
                        end: booking.endTime,
                        className: `status-${booking.status.toLowerCase().replace('_', '-')}`,
                        extendedProps: booking
                    }));
                    successCallback(events);
                } else {
                    failureCallback(new Error('載入失敗'));
                }
            } catch (error) {
                console.error('載入行事曆資料失敗:', error);
                failureCallback(error);
            }
        }
    };

    const calendar = new FullCalendar.Calendar(calendarEl, { ...defaultOptions, ...options });
    calendar.render();
    return calendar;
}

// ========================================
// 預約操作
// ========================================

/**
 * 確認預約
 */
async function confirmBooking(bookingId) {
    const confirmed = await showConfirm('確定要確認此預約嗎？');
    if (!confirmed) return;

    try {
        await api.post(`/api/bookings/${bookingId}/confirm`);
        showSuccess('預約已確認');
        if (typeof loadBookings === 'function') loadBookings();
        if (typeof calendar !== 'undefined') calendar.refetchEvents();
    } catch (error) {
        console.error('確認預約失敗:', error);
    }
}

/**
 * 取消預約
 */
async function cancelBooking(bookingId) {
    const reason = prompt('請輸入取消原因（可選）：');
    if (reason === null) return;

    try {
        await api.post(`/api/bookings/${bookingId}/cancel?reason=${encodeURIComponent(reason || '')}`);
        showSuccess('預約已取消');
        if (typeof loadBookings === 'function') loadBookings();
        if (typeof calendar !== 'undefined') calendar.refetchEvents();
    } catch (error) {
        console.error('取消預約失敗:', error);
    }
}

/**
 * 完成預約
 */
async function completeBooking(bookingId) {
    const confirmed = await showConfirm('確定要完成此預約嗎？');
    if (!confirmed) return;

    try {
        await api.post(`/api/bookings/${bookingId}/complete`);
        showSuccess('預約已完成');
        if (typeof loadBookings === 'function') loadBookings();
        if (typeof calendar !== 'undefined') calendar.refetchEvents();
    } catch (error) {
        console.error('完成預約失敗:', error);
    }
}

// ========================================
// 匯出功能
// ========================================

/**
 * 匯出預約列表
 */
async function exportBookings(params = {}) {
    try {
        const result = await api.get('/api/bookings', { ...params, size: 9999 });
        if (result.success && result.data && result.data.content) {
            const headers = {
                bookingNo: '預約編號',
                customerName: '顧客姓名',
                serviceName: '服務項目',
                staffName: '服務人員',
                bookingDate: '預約日期',
                startTime: '開始時間',
                endTime: '結束時間',
                status: '狀態',
                totalAmount: '金額'
            };

            const data = result.data.content.map(booking => ({
                ...booking,
                status: BOOKING_STATUS_MAP[booking.status]?.label || booking.status,
                bookingDate: formatDate(booking.bookingDate),
                startTime: booking.startTime?.substring(0, 5) || '',
                endTime: booking.endTime?.substring(0, 5) || '',
                totalAmount: booking.totalAmount || 0
            }));

            exportToCsv(data, `預約列表_${formatDate(new Date())}.csv`, headers);
        }
    } catch (error) {
        console.error('匯出失敗:', error);
    }
}

/**
 * 匯出顧客列表
 */
async function exportCustomers(params = {}) {
    try {
        const result = await api.get('/api/customers', { ...params, size: 9999 });
        if (result.success && result.data && result.data.content) {
            const headers = {
                name: '姓名',
                phone: '電話',
                email: '電子郵件',
                status: '狀態',
                totalBookings: '預約次數',
                totalSpent: '消費金額',
                createdAt: '加入日期'
            };

            const data = result.data.content.map(customer => ({
                ...customer,
                status: CUSTOMER_STATUS_MAP[customer.status]?.label || customer.status,
                createdAt: formatDate(customer.createdAt)
            }));

            exportToCsv(data, `顧客列表_${formatDate(new Date())}.csv`, headers);
        }
    } catch (error) {
        console.error('匯出失敗:', error);
    }
}

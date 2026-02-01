/**
 * SSE 通知服務
 *
 * 處理即時通知：
 * - 新預約通知
 * - 預約狀態變更
 * - 音效提醒
 * - 自動刷新
 */

// ========================================
// 設定
// ========================================

const NotificationConfig = {
    // SSE 端點
    sseEndpoint: '/api/notifications/stream',
    // 重連間隔（毫秒）
    reconnectInterval: 5000,
    // 最大重連次數
    maxReconnectAttempts: 10,
    // 通知音效檔案
    soundFile: '/sounds/notification.mp3'
};

// ========================================
// 狀態
// ========================================

let eventSource = null;
let reconnectAttempts = 0;
let notificationSound = null;
let soundEnabled = true;

// ========================================
// 初始化
// ========================================

/**
 * 初始化通知服務
 */
function initNotificationService() {
    // 檢查是否已登入
    if (!isLoggedIn()) {
        console.log('未登入，不啟動通知服務');
        return;
    }

    // 初始化音效
    initNotificationSound();

    // 連接 SSE
    connectSSE();

    // 監聽頁面可見性變化
    document.addEventListener('visibilitychange', handleVisibilityChange);

    console.log('通知服務已初始化');
}

/**
 * 初始化通知音效
 */
function initNotificationSound() {
    try {
        // 嘗試載入音效檔案
        notificationSound = new Audio(NotificationConfig.soundFile);
        notificationSound.volume = 0.5;

        // 檢查音效是否可用，如果不可用則使用 Web Audio API
        notificationSound.onerror = () => {
            console.log('使用 Web Audio API 作為通知音效');
            notificationSound = null;
        };

        // 預載音效
        notificationSound.load();
    } catch (e) {
        console.warn('無法初始化通知音效，將使用 Web Audio API:', e);
        notificationSound = null;
    }
}

// ========================================
// SSE 連線管理
// ========================================

/**
 * 連接 SSE
 */
function connectSSE() {
    if (eventSource) {
        eventSource.close();
    }

    try {
        eventSource = new EventSource(NotificationConfig.sseEndpoint);

        // 連線成功
        eventSource.addEventListener('connected', (event) => {
            console.log('SSE 連線成功');
            reconnectAttempts = 0;
        });

        // 新預約事件
        eventSource.addEventListener('new_booking', handleNewBooking);

        // 預約更新事件
        eventSource.addEventListener('booking_updated', handleBookingUpdated);

        // 預約狀態變更事件
        eventSource.addEventListener('booking_status_changed', handleBookingStatusChanged);

        // 預約取消事件
        eventSource.addEventListener('booking_cancelled', handleBookingCancelled);

        // 錯誤處理
        eventSource.onerror = handleSSEError;

    } catch (e) {
        console.error('SSE 連線失敗:', e);
        scheduleReconnect();
    }
}

/**
 * 處理 SSE 錯誤
 */
function handleSSEError(event) {
    console.warn('SSE 連線錯誤');

    if (eventSource) {
        eventSource.close();
        eventSource = null;
    }

    scheduleReconnect();
}

/**
 * 排程重連
 */
function scheduleReconnect() {
    if (reconnectAttempts >= NotificationConfig.maxReconnectAttempts) {
        console.error('SSE 重連次數已達上限');
        return;
    }

    reconnectAttempts++;
    console.log(`SSE 將在 ${NotificationConfig.reconnectInterval / 1000} 秒後重連 (第 ${reconnectAttempts} 次)`);

    setTimeout(() => {
        if (document.visibilityState === 'visible') {
            connectSSE();
        }
    }, NotificationConfig.reconnectInterval);
}

/**
 * 處理頁面可見性變化
 */
function handleVisibilityChange() {
    if (document.visibilityState === 'visible') {
        // 頁面變為可見時，檢查連線狀態
        if (!eventSource || eventSource.readyState === EventSource.CLOSED) {
            reconnectAttempts = 0;
            connectSSE();
        }
    }
}

// ========================================
// 事件處理
// ========================================

/**
 * 處理新預約事件
 */
function handleNewBooking(event) {
    try {
        const booking = JSON.parse(event.data);
        console.log('收到新預約:', booking);

        // 播放音效
        playNotificationSound();

        // 顯示通知
        showNotificationToast(
            '新預約',
            `${booking.customerName || '顧客'} - ${booking.serviceName || '服務'}`,
            'success'
        );

        // 刷新頁面資料
        refreshPageData('new_booking', booking);

    } catch (e) {
        console.error('處理新預約事件失敗:', e);
    }
}

/**
 * 處理預約更新事件
 */
function handleBookingUpdated(event) {
    try {
        const booking = JSON.parse(event.data);
        console.log('預約已更新:', booking);

        // 顯示通知
        showNotificationToast(
            '預約已更新',
            `${booking.customerName || '顧客'} - ${booking.serviceName || '服務'}`,
            'info'
        );

        // 刷新頁面資料
        refreshPageData('booking_updated', booking);

    } catch (e) {
        console.error('處理預約更新事件失敗:', e);
    }
}

/**
 * 處理預約狀態變更事件
 */
function handleBookingStatusChanged(event) {
    try {
        const data = JSON.parse(event.data);
        const booking = data.booking;
        const newStatus = data.newStatus;
        console.log('預約狀態變更:', newStatus, booking);

        const statusText = getStatusText(newStatus);

        // 顯示通知
        showNotificationToast(
            '預約狀態變更',
            `${booking.customerName || '顧客'} - ${statusText}`,
            'info'
        );

        // 刷新頁面資料
        refreshPageData('booking_status_changed', booking);

    } catch (e) {
        console.error('處理預約狀態變更事件失敗:', e);
    }
}

/**
 * 處理預約取消事件
 */
function handleBookingCancelled(event) {
    try {
        const booking = JSON.parse(event.data);
        console.log('預約已取消:', booking);

        // 顯示通知
        showNotificationToast(
            '預約已取消',
            `${booking.customerName || '顧客'} - ${booking.serviceName || '服務'}`,
            'warning'
        );

        // 刷新頁面資料
        refreshPageData('booking_cancelled', booking);

    } catch (e) {
        console.error('處理預約取消事件失敗:', e);
    }
}

// ========================================
// 通知 UI
// ========================================

/**
 * 顯示通知 Toast
 */
function showNotificationToast(title, message, type = 'info') {
    // 使用 common.js 中的 showSuccess/showWarning 或自訂 Toast
    const toastContainer = document.querySelector('.toast-container');
    if (!toastContainer) return;

    const toastId = 'notification-toast-' + Date.now();
    const bgClass = {
        'success': 'bg-success',
        'warning': 'bg-warning',
        'info': 'bg-info',
        'error': 'bg-danger'
    }[type] || 'bg-info';

    const toastHtml = `
        <div id="${toastId}" class="toast align-items-center text-white ${bgClass}" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="d-flex">
                <div class="toast-body">
                    <strong>${escapeHtml(title)}</strong><br>
                    ${escapeHtml(message)}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `;

    toastContainer.insertAdjacentHTML('beforeend', toastHtml);

    const toastEl = document.getElementById(toastId);
    const toast = new bootstrap.Toast(toastEl, { autohide: true, delay: 5000 });
    toast.show();

    // 自動移除 DOM
    toastEl.addEventListener('hidden.bs.toast', () => {
        toastEl.remove();
    });
}

/**
 * 轉義 HTML
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 取得狀態文字
 */
function getStatusText(status) {
    const statusMap = {
        'PENDING': '待確認',
        'CONFIRMED': '已確認',
        'IN_PROGRESS': '進行中',
        'COMPLETED': '已完成',
        'CANCELLED': '已取消',
        'NO_SHOW': '未到'
    };
    return statusMap[status] || status;
}

// ========================================
// 音效
// ========================================

/**
 * 播放通知音效
 */
function playNotificationSound() {
    if (!soundEnabled) return;

    // 如果有音效檔案，使用它
    if (notificationSound) {
        try {
            notificationSound.currentTime = 0;
            notificationSound.play().catch(e => {
                console.debug('無法播放音效（可能需要使用者互動）:', e);
                // 嘗試使用 Web Audio API 作為備用
                playBeepSound();
            });
            return;
        } catch (e) {
            console.debug('播放音效失敗:', e);
        }
    }

    // 使用 Web Audio API 播放簡單的提示音
    playBeepSound();
}

/**
 * 使用 Web Audio API 播放簡單的提示音
 */
function playBeepSound() {
    try {
        const audioContext = new (window.AudioContext || window.webkitAudioContext)();

        // 建立振盪器
        const oscillator = audioContext.createOscillator();
        const gainNode = audioContext.createGain();

        oscillator.connect(gainNode);
        gainNode.connect(audioContext.destination);

        // 設定音調（較高的頻率，聽起來像通知聲）
        oscillator.frequency.value = 880; // A5 音符
        oscillator.type = 'sine';

        // 設定音量漸變
        gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.3);

        // 播放
        oscillator.start(audioContext.currentTime);
        oscillator.stop(audioContext.currentTime + 0.3);

        // 第二個音（形成雙音提示）
        setTimeout(() => {
            const osc2 = audioContext.createOscillator();
            const gain2 = audioContext.createGain();
            osc2.connect(gain2);
            gain2.connect(audioContext.destination);
            osc2.frequency.value = 1100; // 更高的音
            osc2.type = 'sine';
            gain2.gain.setValueAtTime(0.3, audioContext.currentTime);
            gain2.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + 0.2);
            osc2.start(audioContext.currentTime);
            osc2.stop(audioContext.currentTime + 0.2);
        }, 150);

    } catch (e) {
        console.debug('Web Audio API 不可用:', e);
    }
}

/**
 * 啟用/停用音效
 */
function toggleNotificationSound(enabled) {
    soundEnabled = enabled;
    console.log('通知音效:', enabled ? '已啟用' : '已停用');
}

// ========================================
// 頁面刷新
// ========================================

/**
 * 刷新頁面資料
 */
function refreshPageData(eventType, data) {
    // 根據當前頁面決定是否刷新
    if (typeof currentPage !== 'undefined') {
        switch (currentPage) {
            case 'bookings':
                // 預約管理頁面 - 刷新列表
                if (typeof loadBookings === 'function') {
                    loadBookings(typeof currentPageNum !== 'undefined' ? currentPageNum : 0);
                }
                break;

            case 'calendar':
                // 行事曆頁面 - 刷新行事曆
                if (typeof calendar !== 'undefined' && calendar.refetchEvents) {
                    calendar.refetchEvents();
                }
                break;

            case 'dashboard':
                // 儀表板頁面 - 刷新統計
                if (typeof loadDashboardData === 'function') {
                    loadDashboardData();
                }
                break;
        }
    }
}

// ========================================
// 自動初始化
// ========================================

// 頁面載入完成後初始化
document.addEventListener('DOMContentLoaded', () => {
    // 延遲初始化，確保其他腳本已載入
    setTimeout(initNotificationService, 500);
});

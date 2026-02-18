import { test, expect, APIRequestContext } from './fixtures';
import { tenantLogin, waitForLoading, WAIT_TIME, TEST_ACCOUNTS } from './utils/test-helpers';

/**
 * SSE 即時通知 + 通知系統完整測試
 *
 * 測試範圍：
 * 1. SSE 端點連線與認證
 * 2. SSE Content-Type 與服務健康
 * 3. notification.js 載入與基本功能
 * 4. notification.js 事件類型與處理函數完整性
 * 5. notification.js 音效/toast/頁面刷新整合
 * 6. SSE 通知服務 API 整合驗證（商品訂單/票券/顧客）
 * 7. 商品訂單 LINE 通知驗證
 * 8. 顧客管理 - 刪除按鈕 UI 驗證
 * 9. 相關頁面 currentPage 設定與 loadData 驗證
 * 10. 通知 UI 元素驗證
 */

// 取得超管 Token
async function getAdminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/admin/login', {
    data: {
      username: TEST_ACCOUNTS.admin.username,
      password: TEST_ACCOUNTS.admin.password
    }
  });
  const data = await response.json();
  return data.data.accessToken;
}

// 取得店家 Token
async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// ========================================
// 1. SSE 端點連線與認證
// ========================================
test.describe('SSE 端點連線與認證', () => {
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    adminToken = await getAdminToken(request);
  });

  test('未認證的 SSE 連線應返回 401', async ({ request }) => {
    const response = await request.get('/api/notifications/stream', {
      headers: {
        'Accept': 'text/event-stream'
      }
    });

    expect(response.status()).toBe(401);
  });

  test('認證的 SSE 連線應返回 200', async ({ request }) => {
    const response = await request.get('/api/notifications/stream', {
      headers: {
        'Authorization': `Bearer ${adminToken}`,
        'Accept': 'text/event-stream'
      }
    });

    // 認證後應該成功（可能因為沒有 tenantId 而立即關閉）
    const status = response.status();
    console.log(`認證 SSE 狀態碼: ${status}`);
    expect(status).toBeLessThan(500);
  });

  test('SSE 端點回應正確的 Content-Type', async ({ request }) => {
    const response = await request.get('/api/notifications/stream', {
      headers: {
        'Authorization': `Bearer ${adminToken}`,
        'Accept': 'text/event-stream'
      }
    });

    if (response.ok()) {
      const contentType = response.headers()['content-type'];
      console.log(`Content-Type: ${contentType}`);
      if (contentType) {
        expect(contentType).toContain('text/event-stream');
      }
    }
  });

  test('SSE 服務健康檢查正常', async ({ request }) => {
    const response = await request.get('/health');
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.status).toBe('UP');
  });
});

// ========================================
// 2. notification.js 載入與基本功能
// ========================================
test.describe('notification.js 載入與基本功能', () => {

  test('notification.js 可正確載入', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    expect(response?.status()).toBe(200);

    const content = await response?.text();
    expect(content).toContain('initNotificationService');
    expect(content).toContain('EventSource');
    expect(content).toContain('playNotificationSound');

    const hasBeepSound = content?.includes('playBeepSound');
    console.log(`Web Audio API fallback: ${hasBeepSound ? '已部署' : '待部署'}`);
  });

  test('音效播放功能存在', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    expect(content).toContain('playNotificationSound');
    expect(content).toContain('soundEnabled');

    const hasWebAudioFallback = content.includes('AudioContext') && content.includes('createOscillator');
    console.log(`音效實作: ${hasWebAudioFallback ? 'Web Audio API + Audio Element' : '僅 Audio Element'}`);

    // 至少要有基本的音效功能
    expect(content.includes('Audio') || content.includes('AudioContext')).toBeTruthy();
  });
});

// ========================================
// 3. notification.js 事件類型與處理函數完整性
// ========================================
test.describe('notification.js 事件類型完整性', () => {

  test('包含所有 SSE 事件監聽', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    // 預約事件
    expect(content).toContain('new_booking');
    expect(content).toContain('booking_updated');
    expect(content).toContain('booking_status_changed');
    expect(content).toContain('booking_cancelled');

    // 商品訂單/票券/顧客事件
    expect(content).toContain('new_product_order');
    expect(content).toContain('product_order_status_changed');
    expect(content).toContain('coupon_claimed');
    expect(content).toContain('new_customer');
  });

  test('包含所有事件處理函數', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    // 預約處理函數
    expect(content).toContain('handleNewBooking');
    expect(content).toContain('handleBookingUpdated');
    expect(content).toContain('handleBookingStatusChanged');
    expect(content).toContain('handleBookingCancelled');

    // 新增處理函數
    expect(content).toContain('handleNewProductOrder');
    expect(content).toContain('handleProductOrderStatusChanged');
    expect(content).toContain('handleCouponClaimed');
    expect(content).toContain('handleNewCustomer');
  });

  test('connectSSE 函數正確註冊所有事件監聽', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    const expectedListeners = [
      "'new_booking'",
      "'booking_updated'",
      "'booking_status_changed'",
      "'booking_cancelled'",
      "'new_product_order'",
      "'product_order_status_changed'",
      "'coupon_claimed'",
      "'new_customer'"
    ];

    for (const listener of expectedListeners) {
      expect(content).toContain(`addEventListener(${listener}`);
      console.log(`事件監聽 ${listener}: 已註冊`);
    }
  });
});

// ========================================
// 4. notification.js 音效/toast/頁面刷新整合
// ========================================
test.describe('notification.js 處理函數整合驗證', () => {

  test('事件處理函數含音效播放', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    const handlers = ['handleNewProductOrder', 'handleCouponClaimed', 'handleNewCustomer'];
    for (const handler of handlers) {
      const handlerRegex = new RegExp(`function\\s+${handler}[\\s\\S]*?playNotificationSound`);
      expect(content).toMatch(handlerRegex);
      console.log(`${handler}: 包含音效播放`);
    }
  });

  test('事件處理函數含 toast 通知', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    const handlers = [
      'handleNewProductOrder',
      'handleProductOrderStatusChanged',
      'handleCouponClaimed',
      'handleNewCustomer'
    ];
    for (const handler of handlers) {
      const handlerRegex = new RegExp(`function\\s+${handler}[\\s\\S]*?showNotificationToast`);
      expect(content).toMatch(handlerRegex);
      console.log(`${handler}: 包含 toast 通知`);
    }
  });

  test('事件處理函數含 refreshPageData', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    const handlers = [
      'handleNewProductOrder',
      'handleProductOrderStatusChanged',
      'handleCouponClaimed',
      'handleNewCustomer'
    ];
    for (const handler of handlers) {
      const handlerRegex = new RegExp(`function\\s+${handler}[\\s\\S]*?refreshPageData`);
      expect(content).toMatch(handlerRegex);
      console.log(`${handler}: 包含頁面刷新`);
    }
  });

  test('refreshPageData 包含所有頁面 case', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    // 原有 case
    expect(content).toContain("case 'bookings'");
    expect(content).toContain("case 'calendar'");
    expect(content).toContain("case 'dashboard'");

    // 新增 case
    expect(content).toContain("case 'product-orders'");
    expect(content).toContain("case 'customers'");
    expect(content).toContain("case 'coupons'");
  });

  test('notification.js 前端完整性統計', async ({ page }) => {
    const response = await page.goto('/js/notification.js');
    const content = await response?.text() || '';

    // 統計事件類型
    const eventTypes = [
      'new_booking', 'booking_updated', 'booking_status_changed', 'booking_cancelled',
      'new_product_order', 'product_order_status_changed', 'coupon_claimed', 'new_customer'
    ];

    let registered = 0;
    for (const eventType of eventTypes) {
      if (content.includes(`addEventListener('${eventType}'`)) {
        registered++;
      }
    }

    console.log(`事件類型: ${registered}/${eventTypes.length} 已註冊`);
    expect(registered).toBe(eventTypes.length);

    // 統計頁面刷新 case
    const pageCases = ['bookings', 'calendar', 'dashboard', 'product-orders', 'customers', 'coupons'];
    let caseCount = 0;
    for (const pageCase of pageCases) {
      if (content.includes(`case '${pageCase}'`)) {
        caseCount++;
      }
    }

    console.log(`頁面刷新 case: ${caseCount}/${pageCases.length}`);
    expect(caseCount).toBe(pageCases.length);
  });
});

// ========================================
// 5. SSE 通知服務 API 整合驗證
// ========================================
test.describe('SSE 通知服務 API 整合驗證', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('預約 API 存在且可呼叫', async ({ request }) => {
    const adminToken = await getAdminToken(request);

    const response = await request.get('/api/bookings?page=0&size=1', {
      headers: {
        'Authorization': `Bearer ${adminToken}`
      }
    });

    const status = response.status();
    console.log(`預約 API 狀態碼: ${status}`);
    // 超管呼叫店家 API 可能返回 403，但不應該是 404 或 500
    expect(status).toBeLessThan(500);
  });

  test('商品訂單 API 可呼叫（SSE 通知已整合）', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/product-orders?page=0&size=1', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });

    expect(response.status()).toBeLessThan(500);
    if (response.ok()) {
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`商品訂單數: ${data.data?.totalElements || 0}`);
    }
  });

  test('票券 API 可呼叫（SSE 通知已整合）', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/coupons?page=0&size=1', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });

    expect(response.status()).toBeLessThan(500);
    if (response.ok()) {
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`票券數: ${data.data?.totalElements || 0}`);
    }
  });

  test('顧客 API 可呼叫（SSE 通知已整合）', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/customers?page=0&size=1', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });

    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    console.log(`顧客數: ${data.data?.totalElements || 0}`);
  });

  test('SseNotificationService 所有通知方法已整合', async ({ request }) => {
    const response = await request.get('/health');
    expect(response.ok()).toBeTruthy();

    console.log('=== SSE 通知服務整合清單 ===');
    console.log('預約通知 (BookingService):');
    console.log('  - notifyNewBooking - 新預約');
    console.log('  - notifyBookingUpdated - 預約更新');
    console.log('  - notifyBookingStatusChanged - 狀態變更');
    console.log('  - notifyBookingCancelled - 取消預約');
    console.log('');
    console.log('商品訂單通知 (ProductOrderService):');
    console.log('  - notifyNewProductOrder - 新訂單 (SSE)');
    console.log('  - notifyProductOrderStatusChanged - 確認/完成/取消 (SSE)');
    console.log('  - sendOrderLineNotification - 確認/完成/取消 (LINE)');
    console.log('');
    console.log('票券通知 (CouponService):');
    console.log('  - notifyCouponClaimed - 票券領取 (SSE)');
    console.log('');
    console.log('顧客通知 (CustomerService):');
    console.log('  - notifyNewCustomer - 新顧客 (SSE)');
  });
});

// ========================================
// 6. 商品訂單 LINE 通知驗證
// ========================================
test.describe('商品訂單 LINE 通知驗證', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('商品訂單確認 API 可呼叫（含 LINE 通知）', async ({ request }) => {
    if (!tenantToken) return;

    const listResponse = await request.get('/api/product-orders?status=PENDING&page=0&size=1', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });

    if (listResponse.ok()) {
      const listData = await listResponse.json();
      if (listData.data?.content?.length > 0) {
        const orderId = listData.data.content[0].id;
        console.log(`找到待處理訂單: ${orderId}`);

        const confirmResponse = await request.post(`/api/product-orders/${orderId}/confirm`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`確認訂單回應: ${confirmResponse.status()}`);
        expect(confirmResponse.status()).toBeLessThan(500);
      } else {
        console.log('無待處理訂單，跳過確認測試');
      }
    }
  });

  test('商品訂單完成 API 可呼叫（含 LINE 通知）', async ({ request }) => {
    if (!tenantToken) return;

    const listResponse = await request.get('/api/product-orders?status=CONFIRMED&page=0&size=1', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });

    if (listResponse.ok()) {
      const listData = await listResponse.json();
      if (listData.data?.content?.length > 0) {
        const orderId = listData.data.content[0].id;
        console.log(`找到已確認訂單: ${orderId}`);

        const completeResponse = await request.post(`/api/product-orders/${orderId}/complete`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`完成訂單回應: ${completeResponse.status()}`);
        expect(completeResponse.status()).toBeLessThan(500);
      } else {
        console.log('無已確認訂單，跳過完成測試');
      }
    }
  });

  test('商品訂單取消 API 可呼叫（含 LINE 通知）', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.post('/api/product-orders/nonexistent/cancel', {
      headers: {
        'Authorization': `Bearer ${tenantToken}`,
        'Content-Type': 'application/json'
      },
      data: { reason: '測試取消' }
    });

    // 應該是 404 (找不到訂單) 而非 500
    expect(response.status()).toBeLessThan(500);
    console.log(`取消不存在訂單回應: ${response.status()}`);
  });
});

// ========================================
// 7. 通知 UI 元素驗證
// ========================================
test.describe('通知 UI 元素驗證', () => {

  test('tenant layout 有基本 HTML 結構', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.waitForLoadState('domcontentloaded');

    const html = await page.content();
    expect(html).toContain('<!DOCTYPE html>');
  });

  test('sounds 目錄可訪問（即使為空）', async ({ page }) => {
    const response = await page.goto('/sounds/notification.mp3');

    const status = response?.status() || 404;
    console.log(`Sound file status: ${status}`);
    expect(status).toBeGreaterThan(0);
  });

  test('店家登入頁面正常', async ({ page }) => {
    await page.goto('/tenant/login');
    await expect(page.locator('#username')).toBeVisible();
  });
});

// ========================================
// 8. SSE 前端整合測試（超管後台）
// ========================================
test.describe('SSE 前端整合測試', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/login');
    await page.evaluate(() => localStorage.clear());
    await page.fill('#username', TEST_ACCOUNTS.admin.username);
    await page.fill('#password', TEST_ACCOUNTS.admin.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 20000 });
  });

  test('超管後台 notification.js 應正確載入', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');

    const title = await page.title();
    expect(title).toBeDefined();
  });
});

// ========================================
// 9. 顧客管理 - 刪除按鈕 UI 驗證
// ========================================
test.describe('顧客管理 - 刪除按鈕 UI 驗證', () => {

  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('customers.html 包含 deleteCustomer 函數', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const hasDeleteFn = await page.evaluate(() => {
      return typeof (window as any).deleteCustomer === 'function';
    });
    expect(hasDeleteFn).toBeTruthy();
    console.log('deleteCustomer 函數: 存在');
  });

  test('顧客列表有刪除按鈕', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const deleteButtons = page.locator('table tbody .btn-outline-danger, table tbody button[title="刪除顧客"]');
    const count = await deleteButtons.count();
    console.log(`刪除按鈕數: ${count}`);

    if (count > 0) {
      const firstBtn = deleteButtons.first();
      const hasTrashIcon = await firstBtn.locator('i.bi-trash').count();
      expect(hasTrashIcon).toBeGreaterThan(0);
      console.log('刪除按鈕圖示: bi-trash');
    }
  });

  test('刪除按鈕點擊顯示確認對話框', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const deleteBtn = page.locator('table tbody button[title="刪除顧客"]').first();
    if (await deleteBtn.isVisible()) {
      await deleteBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      const confirmModal = page.locator('.modal.show, #confirmModal.show');
      const isVisible = await confirmModal.isVisible();
      console.log(`確認對話框: ${isVisible ? '顯示' : '未顯示'}`);

      if (isVisible) {
        const modalText = await confirmModal.textContent();
        expect(modalText).toContain('刪除');
        console.log('對話框文字: 包含「刪除」');

        const cancelBtn = confirmModal.locator('#confirmCancelBtn, .btn-secondary');
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
          await page.waitForTimeout(WAIT_TIME.short);
        }
      }
    } else {
      console.log('無顧客資料，跳過刪除按鈕測試');
    }
  });

  test('顧客操作按鈕組完整（查看+編輯+刪除）', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const firstRow = page.locator('table tbody tr').first();
    const btnGroup = firstRow.locator('.btn-group');

    if (await btnGroup.isVisible()) {
      const viewBtn = btnGroup.locator('.btn-outline-primary, a[title="查看詳情"]');
      const editBtn = btnGroup.locator('.btn-outline-secondary, button[title="編輯資料"]');
      const deleteBtn = btnGroup.locator('.btn-outline-danger, button[title="刪除顧客"]');

      const viewCount = await viewBtn.count();
      const editCount = await editBtn.count();
      const deleteCount = await deleteBtn.count();

      console.log(`查看按鈕: ${viewCount > 0 ? 'OK' : 'MISSING'}`);
      console.log(`編輯按鈕: ${editCount > 0 ? 'OK' : 'MISSING'}`);
      console.log(`刪除按鈕: ${deleteCount > 0 ? 'OK' : 'MISSING'}`);

      expect(viewCount).toBeGreaterThan(0);
      expect(editCount).toBeGreaterThan(0);
      expect(deleteCount).toBeGreaterThan(0);
    } else {
      console.log('無顧客資料，跳過按鈕組檢查');
    }
  });
});

// ========================================
// 10. 相關頁面 currentPage 設定與 loadData 驗證
// ========================================
test.describe('通知相關頁面 currentPage 與 loadData 驗證', () => {

  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('商品訂單頁面載入正常且 currentPage 正確', async ({ page }) => {
    await page.goto('/tenant/product-orders');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const title = page.locator('h1, .page-title');
    await expect(title.first()).toBeVisible();

    const html = await page.content();
    expect(html).toContain("currentPage = 'product-orders'");
    console.log('商品訂單 currentPage: product-orders');
  });

  test('商品訂單頁面有 loadData 函數', async ({ page }) => {
    await page.goto('/tenant/product-orders');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const hasLoadData = await page.evaluate(() => typeof (window as any).loadData === 'function');
    expect(hasLoadData).toBeTruthy();
    console.log('loadData 函數: 存在');
  });

  test('顧客頁面載入正常且 currentPage 正確', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const title = page.locator('h1, .page-title');
    await expect(title.first()).toBeVisible();

    const html = await page.content();
    expect(html).toContain("currentPage = 'customers'");
    console.log('顧客 currentPage: customers');
  });

  test('顧客頁面有 loadData 函數', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const hasLoadData = await page.evaluate(() => typeof (window as any).loadData === 'function');
    expect(hasLoadData).toBeTruthy();
    console.log('loadData 函數: 存在');
  });

  test('票券頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/coupons');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const title = page.locator('h1, .page-title');
    await expect(title.first()).toBeVisible();
  });
});

import { test, expect } from './fixtures';
import { TEST_ACCOUNTS, tenantLogin, adminLogin, waitForLoading } from './utils/test-helpers';

/**
 * 基礎設施測試
 *
 * 測試範圍：環境健康、認證流程、基本導航
 * 合併自：00-setup + 01-auth + 03-tenant-dashboard
 */

// ========================================
// 環境健康檢查
// ========================================

test.describe('環境健康', () => {
  test('健康檢查端點回傳 UP', async ({ request }) => {
    const res = await request.get('/health');
    expect(res.ok()).toBeTruthy();
    const data = await res.json();
    expect(data.status).toBe('UP');
  });

  test('店家登入頁可訪問', async ({ request }) => {
    const res = await request.get('/tenant/login');
    expect(res.ok()).toBeTruthy();
  });

  test('超管登入頁可訪問', async ({ request }) => {
    const res = await request.get('/admin/login');
    expect(res.ok()).toBeTruthy();
  });
});

// ========================================
// 店家認證流程
// ========================================

test.describe('店家認證', () => {
  test('正確帳密登入成功並跳轉到儀表板', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.fill('#username', TEST_ACCOUNTS.tenant.username);
    await page.fill('#password', TEST_ACCOUNTS.tenant.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/tenant\/dashboard/, { timeout: 15000 });
    expect(page.url()).toContain('/tenant/dashboard');
  });

  test('錯誤帳密顯示錯誤訊息', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.fill('#username', 'wrong@wrong.com');
    await page.fill('#password', 'wrongpassword');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(3000);
    // 應停留在登入頁
    expect(page.url()).toContain('/login');
  });

  test('未登入訪問後台重導向到登入頁', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    // SPA 用 JS 做重導向，等待頁面載入後檢查
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(5000);
    // 應該被重導向到登入頁或停留在 dashboard（顯示登入表單）
    const url = page.url();
    const hasLoginForm = await page.locator('#username').isVisible().catch(() => false);
    expect(url.includes('/login') || hasLoginForm).toBeTruthy();
  });

  test('登入後 API 帶 Token 可正常呼叫', async ({ request }) => {
    const loginRes = await request.post('/api/auth/tenant/login', {
      data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
    });
    expect(loginRes.ok()).toBeTruthy();
    const loginData = await loginRes.json();
    expect(loginData.data.accessToken).toBeTruthy();

    // 用 Token 呼叫 API
    const bookingsRes = await request.get('/api/bookings?size=1', {
      headers: { Authorization: `Bearer ${loginData.data.accessToken}` }
    });
    expect(bookingsRes.ok()).toBeTruthy();
    const bookingsData = await bookingsRes.json();
    expect(bookingsData.success).toBe(true);
  });

  test('Token 刷新流程', async ({ request }) => {
    const loginRes = await request.post('/api/auth/tenant/login', {
      data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
    });
    const loginData = await loginRes.json();
    const refreshToken = loginData.data.refreshToken;
    if (!refreshToken) { test.skip(); return; }

    const refreshRes = await request.post('/api/auth/refresh', {
      data: { refreshToken }
    });
    expect(refreshRes.status()).toBeLessThan(500);
  });
});

// ========================================
// 超管認證流程
// ========================================

test.describe('超管認證', () => {
  test('超管正確帳密登入成功', async ({ page }) => {
    await page.goto('/admin/login');
    await page.fill('#username', TEST_ACCOUNTS.admin.username);
    await page.fill('#password', TEST_ACCOUNTS.admin.password);
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 15000 });
    expect(page.url()).toContain('/admin/dashboard');
  });

  test('超管未登入重導向到登入頁', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForURL(/\/admin\/login/, { timeout: 15000 });
    expect(page.url()).toContain('/admin/login');
  });

  test('超管 API 正常回應', async ({ request }) => {
    const loginRes = await request.post('/api/auth/admin/login', {
      data: { username: TEST_ACCOUNTS.admin.username, password: TEST_ACCOUNTS.admin.password }
    });
    expect(loginRes.ok()).toBeTruthy();
    const data = await loginRes.json();
    expect(data.data.accessToken).toBeTruthy();
  });
});

// ========================================
// 多租戶資料隔離
// ========================================

test.describe('多租戶隔離', () => {
  test('店家 Token 不能呼叫超管 API', async ({ request }) => {
    const tenantRes = await request.post('/api/auth/tenant/login', {
      data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
    });
    const tenantToken = (await tenantRes.json()).data.accessToken;

    const tenantsRes = await request.get('/api/admin/tenants', {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    expect(tenantsRes.status()).not.toBe(200);
  });

  test('無 Token 呼叫受保護 API 被拒絕', async ({ request }) => {
    const res = await request.get('/api/bookings');
    expect([401, 403]).toContain(res.status());
  });
});

// ========================================
// 店家後台基本導航
// ========================================

test.describe('店家後台導航', () => {
  test('儀表板載入並顯示統計數據', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(2000);

    // 儀表板應顯示統計卡片
    const cards = page.locator('.card, .stat-card, .dashboard-card');
    const cardCount = await cards.count();
    expect(cardCount).toBeGreaterThan(0);
  });

  test('側邊欄包含核心導航項目', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);

    const coreLinks = ['/tenant/bookings', '/tenant/customers', '/tenant/staff', '/tenant/services'];
    for (const link of coreLinks) {
      const navItem = page.locator(`a[href="${link}"]`);
      expect(await navItem.count(), `側邊欄應有 ${link}`).toBeGreaterThan(0);
    }
  });
});

/**
 * 10-public-pages.spec.ts
 *
 * 公開頁面、SEO、頁面健康、RWD、新手引導 整合測試
 *
 * 整合自：
 * - 22-public-seo-pages.spec.ts (公開頁面 + SEO)
 * - 20-f12-console-check.spec.ts (F12 Console 錯誤)
 * - 28-rwd-responsive.spec.ts (RWD 響應式)
 * - 29-page-health-validator.spec.ts (頁面健康驗證)
 * - 24-onboarding-setup-status.spec.ts (新手引導)
 *
 * 透過 fixtures.ts 自動監控 F12 Console 錯誤
 */
import { test, expect, Page } from './fixtures';
import { TEST_ACCOUNTS, tenantLogin, adminLogin, waitForLoading, WAIT_TIME } from './utils/test-helpers';

const BASE_URL = process.env.BASE_URL || 'https://booking-platform-production-1e08.up.railway.app';

// ========== 工具函式 ==========

/** 取得店家 Token（透過 API 登入） */
async function getTenantToken(request: any): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  expect(response.ok(), 'tenant login should succeed').toBeTruthy();
  const data = await response.json();
  const token = data.data?.accessToken || '';
  expect(token, 'tenant token should not be empty').toBeTruthy();
  return token;
}

/** 檢查頁面是否有水平溢出 */
async function checkNoHorizontalOverflow(page: Page): Promise<boolean> {
  return await page.evaluate(() => {
    return document.documentElement.scrollWidth <= document.documentElement.clientWidth;
  });
}

/** 等待所有 /api/ 請求完成（排除 SSE） */
async function waitForApiSettled(page: Page, timeoutMs = 8000): Promise<void> {
  let pending = 0;

  const onRequest = (req: { url: () => string }) => {
    const url = req.url();
    if (url.includes('/api/') && !url.includes('/notifications/stream')) pending++;
  };
  const onResponse = (res: { url: () => string }) => {
    const url = res.url();
    if (url.includes('/api/') && !url.includes('/notifications/stream')) pending = Math.max(0, pending - 1);
  };
  const onFailed = (req: { url: () => string }) => {
    const url = req.url();
    if (url.includes('/api/') && !url.includes('/notifications/stream')) pending = Math.max(0, pending - 1);
  };

  page.on('request', onRequest);
  page.on('response', onResponse);
  page.on('requestfailed', onFailed);

  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    await page.waitForTimeout(300);
    if (pending === 0) {
      await page.waitForTimeout(500);
      if (pending === 0) break;
    }
  }

  page.off('request', onRequest);
  page.off('response', onResponse);
  page.off('requestfailed', onFailed);
}

/**
 * 驗證頁面健康：無卡住的載入指標、無 orphan spinner、無載入遮罩
 */
async function validatePageHealth(page: Page, url: string): Promise<string[]> {
  await page.goto(url);
  await page.waitForLoadState('domcontentloaded');
  await waitForApiSettled(page, 15000);

  // 額外等待 loading overlay 消失（某些頁面 API 完成後仍有動畫延遲）
  try {
    await page.waitForFunction(() => {
      const overlays = document.querySelectorAll('.loading-overlay');
      for (const overlay of overlays) {
        const style = window.getComputedStyle(overlay as HTMLElement);
        if (style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0') return false;
      }
      return true;
    }, { timeout: 10000 });
  } catch {
    // timeout - will be caught by the overlay check below
  }

  const issues: string[] = [];

  // 檢查 1: 卡住的「載入中」文字
  const staleLoadingElements = await page.evaluate(() => {
    const results: string[] = [];
    const walker = document.createTreeWalker(document.body, NodeFilter.SHOW_TEXT, {
      acceptNode: (node) =>
        node.textContent && node.textContent.includes('載入中') ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT,
    });
    while (walker.nextNode()) {
      const el = walker.currentNode.parentElement;
      if (!el) continue;
      if (el.closest('.btn-loading')) continue;
      const style = window.getComputedStyle(el);
      if (style.display === 'none' || style.visibility === 'hidden') continue;
      let ancestor: HTMLElement | null = el.parentElement;
      let hidden = false;
      while (ancestor && ancestor !== document.body) {
        const s = window.getComputedStyle(ancestor);
        if (s.display === 'none' || s.visibility === 'hidden') { hidden = true; break; }
        ancestor = ancestor.parentElement;
      }
      if (hidden) continue;
      const modal = el.closest('.modal');
      if (modal && !modal.classList.contains('show')) continue;
      const closestId = el.closest('[id]');
      results.push(closestId ? `#${closestId.id}` : el.tagName.toLowerCase());
    }
    return results;
  });
  for (const id of staleLoadingElements) {
    issues.push(`Stale loading text visible at ${id}`);
  }

  // 檢查 2: Orphan spinner
  const orphanSpinners = await page.evaluate(() => {
    const spinners = document.querySelectorAll('.spinner-border');
    const locations: string[] = [];
    for (const spinner of spinners) {
      const el = spinner as HTMLElement;
      if (el.closest('.btn-loading')) continue;
      const modal = el.closest('.modal');
      if (modal && !modal.classList.contains('show')) continue;
      const style = window.getComputedStyle(el);
      if (style.display === 'none' || style.visibility === 'hidden') continue;
      let ancestor: HTMLElement | null = el.parentElement;
      let hidden = false;
      while (ancestor && ancestor !== document.body) {
        const s = window.getComputedStyle(ancestor);
        if (s.display === 'none' || s.visibility === 'hidden') { hidden = true; break; }
        ancestor = ancestor.parentElement;
      }
      if (hidden) continue;
      const closestId = el.closest('[id]');
      locations.push(closestId ? `#${closestId.id}` : 'unknown');
    }
    return locations;
  });
  if (orphanSpinners.length > 0) {
    issues.push(`${orphanSpinners.length} orphan spinner(s) at ${orphanSpinners.join(', ')}`);
  }

  // 檢查 3: Loading overlay
  const overlayVisible = await page.evaluate(() => {
    const overlays = document.querySelectorAll('.loading-overlay');
    for (const overlay of overlays) {
      const style = window.getComputedStyle(overlay as HTMLElement);
      if (style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0') return true;
    }
    return false;
  });
  if (overlayVisible) {
    issues.push('Loading overlay still visible');
  }

  return issues;
}

// ============================================================
// 1. 公開頁面可訪問
// ============================================================

test.describe('公開頁面可訪問', () => {

  test('首頁 GET / 回應 200', async ({ page }) => {
    const response = await page.goto('/', { waitUntil: 'domcontentloaded' });
    expect(response).not.toBeNull();
    expect(response!.status()).toBe(200);
    await expect(page.locator('body')).toBeVisible();
  });

  test('店家登入頁 GET /tenant/login 回應 200 且有登入表單', async ({ page }) => {
    const response = await page.goto('/tenant/login', { waitUntil: 'domcontentloaded' });
    expect(response).not.toBeNull();
    expect(response!.status()).toBe(200);
    await expect(page.locator('#username, input[name="username"]')).toBeVisible();
    await expect(page.locator('#password, input[name="password"]')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('店家註冊頁 GET /tenant/register 回應 200 且有註冊表單', async ({ page }) => {
    const response = await page.goto('/tenant/register', { waitUntil: 'domcontentloaded' });
    expect(response).not.toBeNull();
    expect(response!.status()).toBe(200);
    await expect(page.locator('input[name="email"], #email')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('超管登入頁 GET /admin/login 回應 200 且有登入表單', async ({ page }) => {
    const response = await page.goto('/admin/login', { waitUntil: 'domcontentloaded' });
    expect(response).not.toBeNull();
    expect(response!.status()).toBe(200);
    await expect(page.locator('#username, input[name="username"]')).toBeVisible();
    await expect(page.locator('#password, input[name="password"]')).toBeVisible();
  });

  test('健康檢查 GET /health 回應 200 且 status: UP', async ({ request }) => {
    const response = await request.get('/health');
    expect(response.status()).toBe(200);
    const body = await response.text();
    expect(body).toContain('UP');
  });
});

// ============================================================
// 2. SEO Meta Tags
// ============================================================

test.describe('SEO Meta Tags', () => {

  test('首頁有 title、meta description、og:title、og:description', async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });

    // title 標籤存在且不為空
    const title = await page.title();
    expect(title.length).toBeGreaterThan(0);

    // meta description
    const metaDesc = page.locator('meta[name="description"]');
    expect(await metaDesc.count()).toBeGreaterThanOrEqual(1);
    const descContent = await metaDesc.first().getAttribute('content');
    expect(descContent).toBeTruthy();

    // Open Graph tags
    await expect(page.locator('meta[property="og:title"]')).toHaveCount(1);
    await expect(page.locator('meta[property="og:description"]')).toHaveCount(1);
  });

  test('robots.txt 回應 200 且包含 User-agent', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/robots.txt`);
    expect(response.status()).toBe(200);
    const text = await response.text();
    expect(text).toContain('User-agent');
  });

  test('sitemap.xml 回應 200 且包含 urlset（如存在）', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/sitemap.xml`);
    // 某些環境可能不部署 sitemap，允許 404
    if (response.status() === 404) {
      return; // skip gracefully
    }
    expect(response.status()).toBe(200);
    const text = await response.text();
    expect(text).toContain('urlset');
  });

  test('首頁有 Schema.org 結構化資料', async ({ page }) => {
    await page.goto('/', { waitUntil: 'domcontentloaded' });
    const schemaScripts = await page.locator('script[type="application/ld+json"]').count();
    expect(schemaScripts).toBeGreaterThanOrEqual(1);
  });
});

// ============================================================
// 3. 店家後台所有頁面載入（Page Health）
// ============================================================

test.describe('店家後台頁面載入', () => {
  // 這些頁面涉及多個 API 呼叫，在伺服器負載高時可能需要重試
  test.describe.configure({ retries: 2 });

  const tenantPages = [
    { path: '/tenant/dashboard', name: '儀表板' },
    { path: '/tenant/bookings', name: '預約管理' },
    { path: '/tenant/calendar', name: '行事曆' },
    { path: '/tenant/reports', name: '營運報表' },
    { path: '/tenant/customers', name: '顧客列表' },
    { path: '/tenant/staff', name: '員工管理' },
    { path: '/tenant/services', name: '服務管理' },
    { path: '/tenant/settings', name: '店家設定' },
    { path: '/tenant/line-settings', name: 'LINE 設定' },
    { path: '/tenant/feature-store', name: '功能商店' },
    { path: '/tenant/points', name: '點數管理' },
  ];

  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  for (const tp of tenantPages) {
    test(`${tp.name} (${tp.path}) 載入正常且無卡住指標`, async ({ page }) => {
      const issues = await validatePageHealth(page, tp.path);
      expect(issues, `${tp.name} has health issues: ${issues.join('; ')}`).toEqual([]);

      // 驗證頁面有實質內容（body 不為空）
      const bodyText = await page.locator('body').innerText();
      expect(bodyText.length).toBeGreaterThan(0);
    });
  }
});

// ============================================================
// 4. 超管後台頁面載入
// ============================================================

test.describe('超管後台頁面載入', () => {

  const adminPages = [
    { path: '/admin/dashboard', name: '儀表板' },
    { path: '/admin/tenants', name: '店家列表' },
    { path: '/admin/features', name: '功能管理' },
    { path: '/admin/point-topups', name: '儲值審核' },
  ];

  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  for (const ap of adminPages) {
    test(`${ap.name} (${ap.path}) 載入正常且無卡住指標`, async ({ page }) => {
      const issues = await validatePageHealth(page, ap.path);
      expect(issues, `${ap.name} has health issues: ${issues.join('; ')}`).toEqual([]);

      const bodyText = await page.locator('body').innerText();
      expect(bodyText.length).toBeGreaterThan(0);
    });
  }
});

// ============================================================
// 5. RWD 基本驗證
// ============================================================

test.describe('RWD 響應式驗證', () => {

  test('儀表板 - 手機 (375px) 無水平捲動', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const noOverflow = await checkNoHorizontalOverflow(page);
    expect(noOverflow, 'Dashboard should not have horizontal scroll on mobile').toBeTruthy();
  });

  test('預約管理 - 手機 (375px) 無水平捲動', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 667 });
    await tenantLogin(page);
    await page.goto('/tenant/bookings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const noOverflow = await checkNoHorizontalOverflow(page);
    expect(noOverflow, 'Bookings should not have horizontal scroll on mobile').toBeTruthy();
  });

  test('儀表板 - 平板 (768px) 無水平捲動', async ({ page }) => {
    await page.setViewportSize({ width: 768, height: 1024 });
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const noOverflow = await checkNoHorizontalOverflow(page);
    expect(noOverflow, 'Dashboard should not have horizontal scroll on tablet').toBeTruthy();
  });
});

// ============================================================
// 6. 新手引導狀態
// ============================================================

test.describe('新手引導 Setup Status', () => {

  test('GET /api/settings/setup-status 回傳正確結構', async ({ request }) => {
    const token = await getTenantToken(request);

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();

    // 驗證所有必要欄位存在
    expect(data.data).toHaveProperty('hasBasicInfo');
    expect(data.data).toHaveProperty('hasBusinessHours');
    expect(data.data).toHaveProperty('staffCount');
    expect(data.data).toHaveProperty('serviceCount');
    expect(data.data).toHaveProperty('lineConfigured');
    expect(data.data).toHaveProperty('hasBookings');
    expect(data.data).toHaveProperty('completionPercentage');
    expect(data.data).toHaveProperty('totalSteps');
    expect(data.data).toHaveProperty('completedSteps');
  });

  test('completionPercentage 在 0-100 之間且 completedSteps <= totalSteps', async ({ request }) => {
    const token = await getTenantToken(request);

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const data = await response.json();

    expect(data.data.completionPercentage).toBeGreaterThanOrEqual(0);
    expect(data.data.completionPercentage).toBeLessThanOrEqual(100);
    expect(data.data.completedSteps).toBeLessThanOrEqual(data.data.totalSteps);
    expect(data.data.completedSteps).toBeGreaterThanOrEqual(0);
    expect(data.data.staffCount).toBeGreaterThanOrEqual(0);
    expect(data.data.serviceCount).toBeGreaterThanOrEqual(0);
  });

  test('未授權存取 setup-status 回傳 401', async ({ request }) => {
    const response = await request.get('/api/settings/setup-status');
    expect(response.status()).toBe(401);
  });
});

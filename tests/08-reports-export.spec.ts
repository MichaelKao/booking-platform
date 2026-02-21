import { test, expect, APIRequestContext } from './fixtures';
import { TEST_ACCOUNTS, tenantLogin, waitForLoading, WAIT_TIME } from './utils/test-helpers';

/**
 * 報表與匯出功能完整測試
 *
 * 整合來源：12-reports-export.spec.ts + 31-business-logic-correctness.spec.ts（報表交叉驗證部分）
 *
 * 測試範圍：
 * 1. 報表摘要 API（today / weekly / monthly / summary）
 * 2. 報表統計 API（dashboard / daily / top-services / top-staff / hourly）
 * 3. 報表統計一致性驗證（cross-validation between summary and dashboard）
 * 4. 匯出功能（Excel / PDF for bookings, reports, customers）
 * 5. 報表頁面 UI（page load, dashboard cards, charts）
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  expect(response.ok()).toBeTruthy();
  const data = await response.json();
  expect(data.data?.accessToken).toBeTruthy();
  return data.data.accessToken;
}

// ============================================================
// 1. 報表摘要 API
// ============================================================

test.describe('報表摘要 API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  const authHeaders = () => ({ 'Authorization': `Bearer ${token}` });

  test('GET /api/reports/today - 回傳結構正確且數值非負', async ({ request }) => {
    const response = await request.get('/api/reports/today', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();

    const data = body.data;
    expect(data).toBeTruthy();
    expect(data).toHaveProperty('totalBookings');
    expect(data).toHaveProperty('serviceRevenue');
    expect(Number(data.totalBookings)).toBeGreaterThanOrEqual(0);
    expect(Number(data.serviceRevenue)).toBeGreaterThanOrEqual(0);
  });

  test('GET /api/reports/weekly - 回傳結構正確且數值非負', async ({ request }) => {
    const response = await request.get('/api/reports/weekly', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();

    const data = body.data;
    expect(data).toBeTruthy();
    expect(data).toHaveProperty('totalBookings');
    expect(data).toHaveProperty('serviceRevenue');
    expect(Number(data.totalBookings)).toBeGreaterThanOrEqual(0);
    expect(Number(data.serviceRevenue)).toBeGreaterThanOrEqual(0);
  });

  test('GET /api/reports/monthly - 回傳結構正確且數值非負', async ({ request }) => {
    const response = await request.get('/api/reports/monthly', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();

    const data = body.data;
    expect(data).toBeTruthy();
    expect(data).toHaveProperty('totalBookings');
    expect(data).toHaveProperty('serviceRevenue');
    expect(data).toHaveProperty('totalRevenue');
    expect(Number(data.totalBookings)).toBeGreaterThanOrEqual(0);
    expect(Number(data.serviceRevenue)).toBeGreaterThanOrEqual(0);
    expect(Number(data.totalRevenue)).toBeGreaterThanOrEqual(0);
  });

  test('GET /api/reports/summary - 支援 week/month/quarter 範圍且結構正確', async ({ request }) => {
    const ranges = ['week', 'month', 'quarter'];
    for (const range of ranges) {
      const response = await request.get(`/api/reports/summary?range=${range}`, { headers: authHeaders() });
      expect(response.ok()).toBeTruthy();

      const body = await response.json();
      expect(body.success).toBeTruthy();

      const data = body.data;
      expect(data).toHaveProperty('totalBookings');
      expect(data).toHaveProperty('totalRevenue');
      expect(data).toHaveProperty('serviceRevenue');
      expect(data).toHaveProperty('productRevenue');
      expect(data).toHaveProperty('completedBookings');
      expect(data).toHaveProperty('returningCustomers');

      expect(Number(data.totalBookings)).toBeGreaterThanOrEqual(0);
      expect(Number(data.totalRevenue)).toBeGreaterThanOrEqual(0);
      expect(Number(data.serviceRevenue)).toBeGreaterThanOrEqual(0);
      expect(Number(data.productRevenue)).toBeGreaterThanOrEqual(0);
      expect(Number(data.completedBookings)).toBeGreaterThanOrEqual(0);
      expect(Number(data.returningCustomers)).toBeGreaterThanOrEqual(0);
    }
  });
});

// ============================================================
// 2. 報表統計 API
// ============================================================

test.describe('報表統計 API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  const authHeaders = () => ({ 'Authorization': `Bearer ${token}` });

  test('GET /api/reports/dashboard - 回傳結構正確且數值非負', async ({ request }) => {
    const response = await request.get('/api/reports/dashboard', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();

    const data = body.data;
    expect(data).toBeTruthy();

    const numericFields = ['totalBookings', 'totalCustomers', 'todayBookings', 'pendingBookings', 'monthlyRevenue'];
    for (const field of numericFields) {
      if (data[field] !== undefined) {
        expect(typeof data[field]).toBe('number');
        expect(data[field]).toBeGreaterThanOrEqual(0);
      }
    }
  });

  test('GET /api/reports/daily - 回傳陣列且每日數值非負', async ({ request }) => {
    const response = await request.get('/api/reports/daily?range=week', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();
    expect(Array.isArray(body.data)).toBeTruthy();

    for (const day of body.data) {
      expect(Number(day.bookingCount)).toBeGreaterThanOrEqual(0);
      expect(Number(day.revenue)).toBeGreaterThanOrEqual(0);
    }
  });

  test('GET /api/reports/top-services - 回傳陣列且欄位結構正確', async ({ request }) => {
    const response = await request.get('/api/reports/top-services?range=month&limit=10', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();
    expect(Array.isArray(body.data)).toBeTruthy();

    for (const service of body.data) {
      expect(service).toHaveProperty('id');
      expect(service).toHaveProperty('name');
      expect(service).toHaveProperty('count');
      expect(service).toHaveProperty('amount');
      expect(service.id).not.toBeNull();
      expect(service.name).not.toBeNull();
      expect(typeof service.count).toBe('number');
      expect(typeof service.amount).toBe('number');
      expect(service.count).toBeGreaterThanOrEqual(0);
      expect(service.amount).toBeGreaterThanOrEqual(0);
    }
  });

  test('GET /api/reports/top-staff - 回傳陣列且欄位結構正確', async ({ request }) => {
    const response = await request.get('/api/reports/top-staff?range=month&limit=10', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();
    expect(Array.isArray(body.data)).toBeTruthy();

    for (const staff of body.data) {
      expect(staff).toHaveProperty('id');
      expect(staff).toHaveProperty('name');
      expect(staff).toHaveProperty('count');
      expect(staff).toHaveProperty('amount');
      expect(staff.id).not.toBeNull();
      expect(staff.name).not.toBeNull();
      expect(staff.name).not.toBe('');
      expect(typeof staff.count).toBe('number');
      expect(typeof staff.amount).toBe('number');
      expect(staff.count).toBeGreaterThanOrEqual(0);
      expect(staff.amount).toBeGreaterThanOrEqual(0);
    }
  });

  test('GET /api/reports/hourly - 回傳時段陣列且結構正確', async ({ request }) => {
    const response = await request.get('/api/reports/hourly?range=month', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBeTruthy();
    expect(Array.isArray(body.data)).toBeTruthy();

    for (const item of body.data) {
      expect(item).toHaveProperty('hour');
      expect(item).toHaveProperty('hourLabel');
      expect(item).toHaveProperty('bookingCount');
      expect(typeof item.hour).toBe('number');
      expect(typeof item.hourLabel).toBe('string');
      expect(typeof item.bookingCount).toBe('number');
      expect(item.bookingCount).toBeGreaterThanOrEqual(0);
    }
  });
});

// ============================================================
// 3. 報表統計一致性驗證（Business Logic Cross-validation）
// ============================================================

test.describe('報表統計一致性驗證', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  const authHeaders = () => ({ 'Authorization': `Bearer ${token}` });

  test('dashboard 與 summary(month) 的預約數一致', async ({ request }) => {
    const [dashRes, summaryRes] = await Promise.all([
      request.get('/api/reports/dashboard', { headers: authHeaders() }),
      request.get('/api/reports/summary?range=month', { headers: authHeaders() })
    ]);
    expect(dashRes.ok()).toBeTruthy();
    expect(summaryRes.ok()).toBeTruthy();

    const dashboard = (await dashRes.json()).data;
    const summary = (await summaryRes.json()).data;

    // Both should report non-negative values
    expect(Number(dashboard.totalBookings)).toBeGreaterThanOrEqual(0);
    expect(Number(summary.totalBookings)).toBeGreaterThanOrEqual(0);

    // Dashboard totalBookings covers all time; summary covers the month range.
    // Dashboard total should be >= monthly summary total.
    expect(Number(dashboard.totalBookings)).toBeGreaterThanOrEqual(Number(summary.totalBookings));
  });

  test('today <= weekly <= monthly 報表範圍遞增', async ({ request }) => {
    const [todayRes, weeklyRes, monthlyRes] = await Promise.all([
      request.get('/api/reports/today', { headers: authHeaders() }),
      request.get('/api/reports/weekly', { headers: authHeaders() }),
      request.get('/api/reports/monthly', { headers: authHeaders() })
    ]);
    expect(todayRes.ok()).toBeTruthy();
    expect(weeklyRes.ok()).toBeTruthy();
    expect(monthlyRes.ok()).toBeTruthy();

    const today = (await todayRes.json()).data;
    const weekly = (await weeklyRes.json()).data;
    const monthly = (await monthlyRes.json()).data;

    expect(Number(today.totalBookings)).toBeLessThanOrEqual(Number(weekly.totalBookings));
    expect(Number(weekly.totalBookings)).toBeLessThanOrEqual(Number(monthly.totalBookings));
    expect(Number(today.serviceRevenue)).toBeLessThanOrEqual(Number(weekly.serviceRevenue));
    expect(Number(weekly.serviceRevenue)).toBeLessThanOrEqual(Number(monthly.serviceRevenue));
  });

  test('totalRevenue 等於 serviceRevenue + productRevenue', async ({ request }) => {
    const response = await request.get('/api/reports/summary?range=quarter', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const data = (await response.json()).data;
    const totalRevenue = Number(data.totalRevenue);
    const serviceRevenue = Number(data.serviceRevenue);
    const productRevenue = Number(data.productRevenue);

    expect(totalRevenue).toBe(serviceRevenue + productRevenue);
  });

  test('completionRate 與 completedBookings/totalBookings 一致', async ({ request }) => {
    const response = await request.get('/api/reports/summary?range=quarter', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const data = (await response.json()).data;
    const total = Number(data.totalBookings);
    const completed = Number(data.completedBookings);
    const rate = Number(data.completionRate);

    if (total > 0) {
      const expectedRate = (completed / total) * 100;
      expect(Math.abs(rate - expectedRate)).toBeLessThan(0.1);
    } else {
      expect(rate).toBe(0);
    }
  });

  test('各狀態預約數加總不超過總預約數', async ({ request }) => {
    const response = await request.get('/api/reports/summary?range=quarter', { headers: authHeaders() });
    expect(response.ok()).toBeTruthy();

    const data = (await response.json()).data;
    const total = Number(data.totalBookings);
    const completed = Number(data.completedBookings);
    const cancelled = Number(data.cancelledBookings);
    const noShow = Number(data.noShowBookings);

    expect(completed + cancelled + noShow).toBeLessThanOrEqual(total);
  });

  test('每日報表營收合計應接近摘要服務營收', async ({ request }) => {
    const [summaryRes, dailyRes] = await Promise.all([
      request.get('/api/reports/summary?range=month', { headers: authHeaders() }),
      request.get('/api/reports/daily?range=month', { headers: authHeaders() })
    ]);
    expect(summaryRes.ok()).toBeTruthy();
    expect(dailyRes.ok()).toBeTruthy();

    const serviceRevenue = Number((await summaryRes.json()).data.serviceRevenue);
    const dailyData = (await dailyRes.json()).data as Array<{ revenue: number }>;
    const dailyRevenueSum = dailyData.reduce((sum, d) => sum + Number(d.revenue), 0);

    // Allow $1 rounding difference
    expect(Math.abs(dailyRevenueSum - serviceRevenue)).toBeLessThan(1);
  });
});

// ============================================================
// 4. 匯出功能
// ============================================================

test.describe('匯出功能', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  const authHeaders = () => ({ 'Authorization': `Bearer ${token}` });

  test('GET /api/export/bookings/excel - 回傳 Excel 檔案', async ({ request }) => {
    const response = await request.get('/api/export/bookings/excel?startDate=2024-01-01&endDate=2026-12-31', { headers: authHeaders() });
    expect(response.status()).toBe(200);

    const contentType = response.headers()['content-type'] || '';
    expect(contentType.includes('spreadsheet') || contentType.includes('octet-stream')).toBeTruthy();

    const body = await response.body();
    expect(body.length).toBeGreaterThan(0);
  });

  test('GET /api/export/bookings/pdf - 回傳 PDF 檔案', async ({ request }) => {
    const response = await request.get('/api/export/bookings/pdf?startDate=2024-01-01&endDate=2026-12-31', { headers: authHeaders() });
    expect(response.status()).toBe(200);

    const contentType = response.headers()['content-type'] || '';
    expect(contentType.includes('pdf')).toBeTruthy();

    const body = await response.body();
    expect(body.length).toBeGreaterThan(0);
  });

  test('GET /api/export/reports/excel - 回傳 Excel 檔案', async ({ request }) => {
    const response = await request.get('/api/export/reports/excel?range=month', { headers: authHeaders() });
    expect(response.status()).toBe(200);

    const contentType = response.headers()['content-type'] || '';
    expect(contentType.includes('spreadsheet') || contentType.includes('octet-stream')).toBeTruthy();

    const body = await response.body();
    expect(body.length).toBeGreaterThan(0);
  });

  test('GET /api/export/reports/pdf - 回傳 PDF 檔案', async ({ request }) => {
    const response = await request.get('/api/export/reports/pdf?range=month', { headers: authHeaders() });
    expect(response.status()).toBe(200);

    const contentType = response.headers()['content-type'] || '';
    expect(contentType.includes('pdf')).toBeTruthy();

    const body = await response.body();
    expect(body.length).toBeGreaterThan(0);
  });

  test('GET /api/export/customers/excel - 回傳 Excel 檔案', async ({ request }) => {
    const response = await request.get('/api/export/customers/excel', { headers: authHeaders() });
    expect(response.status()).toBe(200);

    const contentType = response.headers()['content-type'] || '';
    expect(contentType.includes('spreadsheet') || contentType.includes('octet-stream')).toBeTruthy();

    const body = await response.body();
    expect(body.length).toBeGreaterThan(0);
  });
});

// ============================================================
// 5. 報表頁面 UI
// ============================================================

test.describe('報表頁面 UI', () => {
  test.describe.configure({ retries: 1 });

  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('報表頁面載入成功', async ({ page }) => {
    await page.goto('/tenant/reports');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);

    const title = page.locator('h1, h2, .page-title');
    await expect(title.first()).toBeVisible();
  });

  test('統計卡片顯示數字', async ({ page }) => {
    await page.goto('/tenant/reports');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // Report page should have stat cards with numeric values
    const cards = page.locator('.card, .stat-card');
    const cardCount = await cards.count();
    expect(cardCount).toBeGreaterThan(0);
  });

  test('Chart.js canvas 元素存在', async ({ page }) => {
    await page.goto('/tenant/reports');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const canvases = page.locator('canvas');
    const canvasCount = await canvases.count();
    expect(canvasCount).toBeGreaterThan(0);
  });

  test('時段分布區塊載入完成且不顯示載入中', async ({ page }) => {
    await page.goto('/tenant/reports');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.long);

    const hourlySection = page.locator('#hourlyDistribution');
    await expect(hourlySection).toBeVisible();

    // 等待載入中文字消失（報表資料可能較慢載入）
    try {
      await page.waitForFunction(
        (selector) => {
          const el = document.querySelector(selector);
          return el && !el.textContent?.includes('載入中');
        },
        '#hourlyDistribution',
        { timeout: 10000 }
      );
    } catch {
      // timeout fallback
    }

    const loadingText = hourlySection.locator('text=載入中');
    const hasLoading = await loadingText.count();
    expect(hasLoading).toBe(0);
  });
});

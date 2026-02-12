import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  getToday,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 店家後台 - 報表頁面 & 匯出功能完整測試
 *
 * 測試範圍：
 * 1. 報表儀表板
 * 2. 各種報表 API
 * 3. Excel/PDF 匯出
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('報表 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('報表摘要 API', () => {
    test('取得今日報表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/today', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`今日報表: ${JSON.stringify(data.data)}`);
    });

    test('取得週報表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/weekly', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`週報表: ${JSON.stringify(data.data)}`);
    });

    test('取得月報表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/monthly', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`月報表: ${JSON.stringify(data.data)}`);
    });

    test('取得報表摘要 - 不同時間範圍', async ({ request }) => {
      if (!tenantToken) return;

      const ranges = ['week', 'month', 'quarter', 'year'];
      for (const range of ranges) {
        const response = await request.get(`/api/reports/summary?range=${range}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`報表摘要 ${range}: ${response.ok() ? '成功' : response.status()}`);
      }
    });

    test('驗證報表摘要統計欄位', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/summary?range=month', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();

      const summary = data.data;
      // 驗證回頭客統計欄位
      expect(summary).toHaveProperty('returningCustomers');
      expect(typeof summary.returningCustomers).toBe('number');
      expect(summary.returningCustomers).toBeGreaterThanOrEqual(0);

      // 驗證服務營收統計欄位
      expect(summary).toHaveProperty('serviceRevenue');
      expect(typeof summary.serviceRevenue).toBe('number');
      expect(summary.serviceRevenue).toBeGreaterThanOrEqual(0);

      // 驗證總營收計算合理
      expect(summary).toHaveProperty('totalRevenue');
      expect(summary).toHaveProperty('productRevenue');

      console.log(`回頭客: ${summary.returningCustomers}`);
      console.log(`服務營收: ${summary.serviceRevenue}`);
      console.log(`商品營收: ${summary.productRevenue}`);
      console.log(`總營收: ${summary.totalRevenue}`);
    });
  });

  test.describe('每日報表 API', () => {
    test('取得每日報表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/daily?range=week', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`每日報表數據點: ${data.data?.length || 0}`);
    });

    test('每日報表 - 不同時間範圍', async ({ request }) => {
      if (!tenantToken) return;

      const ranges = ['week', 'month', 'quarter'];
      for (const range of ranges) {
        const response = await request.get(`/api/reports/daily?range=${range}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`每日報表 ${range}: ${response.ok() ? '成功' : response.status()}`);
      }
    });
  });

  test.describe('排行榜 API', () => {
    test('熱門服務 TOP 10', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/top-services?range=month&limit=10', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`熱門服務數: ${data.data?.length || 0}`);
    });

    test('熱門員工 TOP 10', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/top-staff?range=month&limit=10', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`熱門員工數: ${data.data?.length || 0}`);
    });

    test('驗證熱門服務營收計算', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/top-services?range=month&limit=10', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();

      if (data.data && data.data.length > 0) {
        // 驗證每個服務都有必要欄位
        for (const service of data.data) {
          expect(service).toHaveProperty('id');
          expect(service).toHaveProperty('name');
          expect(service).toHaveProperty('count');
          expect(service).toHaveProperty('amount');
          expect(typeof service.count).toBe('number');
          expect(typeof service.amount).toBe('number');
          console.log(`服務: ${service.name}, 次數: ${service.count}, 營收: ${service.amount}`);
        }
      }
    });

    test('排行榜 - 不同限制', async ({ request }) => {
      if (!tenantToken) return;

      const limits = [5, 10, 20];
      for (const limit of limits) {
        const servicesRes = await request.get(`/api/reports/top-services?range=month&limit=${limit}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        const staffRes = await request.get(`/api/reports/top-staff?range=month&limit=${limit}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });

        console.log(`TOP ${limit} 服務: ${servicesRes.ok() ? '成功' : servicesRes.status()}`);
        console.log(`TOP ${limit} 員工: ${staffRes.ok() ? '成功' : staffRes.status()}`);
      }
    });
  });

  test.describe('儀表板 API', () => {
    test('取得儀表板資料', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/reports/dashboard', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`儀表板資料: ${JSON.stringify(data.data)}`);
    });
  });
});

test.describe('匯出 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('預約匯出', () => {
    test('匯出預約 Excel', async ({ request }) => {
      if (!tenantToken) return;

      const today = getToday();
      const response = await request.get(`/api/export/bookings/excel?startDate=${today}&endDate=${today}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`預約 Excel 匯出: ${response.ok() ? '成功' : response.status()}`);
    });

    test('匯出預約 PDF', async ({ request }) => {
      if (!tenantToken) return;

      const today = getToday();
      const response = await request.get(`/api/export/bookings/pdf?startDate=${today}&endDate=${today}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`預約 PDF 匯出: ${response.ok() ? '成功' : response.status()}`);
    });

    test('匯出預約 - 帶狀態篩選', async ({ request }) => {
      if (!tenantToken) return;

      const statuses = ['CONFIRMED', 'COMPLETED'];
      for (const status of statuses) {
        const response = await request.get(`/api/export/bookings/excel?status=${status}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`預約匯出 ${status}: ${response.status()}`);
      }
    });
  });

  test.describe('報表匯出', () => {
    test('匯出報表 Excel', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/export/reports/excel?range=month', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`報表 Excel 匯出: ${response.ok() ? '成功' : response.status()}`);
    });

    test('匯出報表 PDF', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/export/reports/pdf?range=month', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`報表 PDF 匯出: ${response.ok() ? '成功' : response.status()}`);
    });
  });

  test.describe('顧客匯出', () => {
    test('匯出顧客 Excel', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/export/customers/excel', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`顧客 Excel 匯出: ${response.ok() ? '成功' : response.status()}`);
    });
  });
});

test.describe('報表 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('報表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('統計卡片顯示', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查統計卡片
      const statsLabels = ['總預約', '總營收', '完成率', '新客戶'];
      for (const label of statsLabels) {
        const element = page.locator(`:has-text("${label}")`);
        console.log(`統計 "${label}": ${await element.count() > 0 ? '存在' : '不存在'}`);
      }
    });

    test('圖表顯示', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查 Chart.js 畫布
      const charts = page.locator('canvas');
      const chartCount = await charts.count();
      console.log(`圖表數量: ${chartCount}`);
    });

    test('日期範圍選擇', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);

      const rangeSelect = page.locator('select[name="range"], #dateRange');
      if (await rangeSelect.isVisible()) {
        const options = await rangeSelect.locator('option').all();
        console.log(`日期範圍選項數: ${options.length}`);

        for (const option of options) {
          const value = await option.getAttribute('value');
          const text = await option.textContent();
          console.log(`- ${value}: ${text?.trim()}`);
        }
      }
    });

    test('日期範圍切換', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);

      const rangeSelect = page.locator('select[name="range"], #dateRange');
      if (await rangeSelect.isVisible()) {
        await rangeSelect.selectOption('week').catch(() => {});
        await page.waitForTimeout(WAIT_TIME.api);

        await rangeSelect.selectOption('month').catch(() => {});
        await page.waitForTimeout(WAIT_TIME.api);

        await rangeSelect.selectOption('quarter').catch(() => {});
        await page.waitForTimeout(WAIT_TIME.api);
      }
    });

    test('熱門服務表格', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const servicesSection = page.locator(':has-text("熱門服務"), :has-text("TOP 服務")');
      console.log(`熱門服務區塊: ${await servicesSection.count() > 0 ? '存在' : '不存在'}`);
    });

    test('員工業績表格', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const staffSection = page.locator(':has-text("員工業績"), :has-text("TOP 員工")');
      console.log(`員工業績區塊: ${await staffSection.count() > 0 ? '存在' : '不存在'}`);
    });

    test('匯出按鈕', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);

      const excelBtn = page.locator('button:has-text("Excel"), button:has-text("匯出 Excel")');
      const pdfBtn = page.locator('button:has-text("PDF"), button:has-text("匯出 PDF")');

      console.log(`Excel 按鈕: ${await excelBtn.isVisible()}`);
      console.log(`PDF 按鈕: ${await pdfBtn.isVisible()}`);
    });

    test('匯出 Excel 功能', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);

      const excelBtn = page.locator('button:has-text("Excel")').first();
      if (await excelBtn.isVisible()) {
        // 監聽下載事件
        const downloadPromise = page.waitForEvent('download', { timeout: 10000 }).catch(() => null);
        await excelBtn.click();
        const download = await downloadPromise;

        if (download) {
          console.log(`下載檔案: ${download.suggestedFilename()}`);
        }
      }
    });

    test('匯出 PDF 功能', async ({ page }) => {
      await page.goto('/tenant/reports');
      await waitForLoading(page);

      const pdfBtn = page.locator('button:has-text("PDF")').first();
      if (await pdfBtn.isVisible()) {
        const downloadPromise = page.waitForEvent('download', { timeout: 10000 }).catch(() => null);
        await pdfBtn.click();
        const download = await downloadPromise;

        if (download) {
          console.log(`下載檔案: ${download.suggestedFilename()}`);
        }
      }
    });
  });

  test.describe('儀表板頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('今日統計卡片', async ({ page }) => {
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const cards = page.locator('.card, .stat-card');
      const cardCount = await cards.count();
      expect(cardCount).toBeGreaterThan(0);
      console.log(`儀表板卡片數: ${cardCount}`);
    });

    test('待處理預約', async ({ page }) => {
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const pendingSection = page.locator(':has-text("待確認"), :has-text("待處理")');
      console.log(`待處理區塊: ${await pendingSection.count() > 0 ? '存在' : '不存在'}`);
    });

    test('最近預約列表', async ({ page }) => {
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const recentBookings = page.locator(':has-text("最近預約"), :has-text("近期預約")');
      console.log(`最近預約區塊: ${await recentBookings.count() > 0 ? '存在' : '不存在'}`);
    });

    test('快速操作按鈕', async ({ page }) => {
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const quickActions = [
        '新增預約',
        '新增顧客',
        '查看報表'
      ];

      for (const action of quickActions) {
        const btn = page.locator(`button:has-text("${action}"), a:has-text("${action}")`);
        console.log(`快速操作 "${action}": ${await btn.count() > 0 ? '存在' : '不存在'}`);
      }
    });
  });
});

test.describe('時段分布 & 進階報表 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('/api/reports/hourly API 回傳格式正確', async ({ request }) => {
    const response = await request.get('/api/reports/hourly?range=month', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.status()).toBe(200);
    const data = await response.json();
    expect(data.success).toBe(true);
    expect(Array.isArray(data.data)).toBe(true);

    // 檢查 PeakHour 結構
    if (data.data.length > 0) {
      const item = data.data[0];
      expect(item).toHaveProperty('hour');
      expect(item).toHaveProperty('hourLabel');
      expect(item).toHaveProperty('bookingCount');
      expect(typeof item.hour).toBe('number');
      expect(typeof item.hourLabel).toBe('string');
      expect(typeof item.bookingCount).toBe('number');
    }
  });

  test('/api/reports/hourly 支援不同 range 參數', async ({ request }) => {
    for (const range of ['week', 'month', 'quarter']) {
      const response = await request.get(`/api/reports/hourly?range=${range}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBe(200);
      const data = await response.json();
      expect(data.success).toBe(true);
      console.log(`hourly range=${range}: ${data.data?.length || 0} 筆時段資料`);
    }
  });

  test('/api/reports/advanced API 回傳 hasAccess 欄位', async ({ request }) => {
    const response = await request.get('/api/reports/advanced?range=month', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.status()).toBe(200);
    const data = await response.json();
    expect(data.success).toBe(true);
    expect(data.data).toHaveProperty('hasAccess');
    expect(typeof data.data.hasAccess).toBe('boolean');

    if (data.data.hasAccess) {
      expect(data.data).toHaveProperty('retentionRate');
      expect(data.data).toHaveProperty('activeCustomers');
      expect(data.data).toHaveProperty('serviceTrends');
      expect(data.data).toHaveProperty('peakHours');
      console.log('進階報表：已訂閱，有完整資料');
    } else {
      expect(data.data).toHaveProperty('message');
      console.log('進階報表：未訂閱，顯示提示訊息');
    }
  });

  test('報表頁面時段分布不再顯示「載入中」', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/reports');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.long);

    const hourlySection = page.locator('#hourlyDistribution');
    await expect(hourlySection).toBeVisible();

    // 確認不再顯示「載入中」
    const loadingText = hourlySection.locator('text=載入中');
    const hasLoading = await loadingText.count();
    expect(hasLoading).toBe(0);
    console.log('時段分布區塊已正確載入（非「載入中」狀態）');
  });

  test('報表頁面顯示進階報表區塊或推廣卡片', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/reports');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.long);

    const advSection = page.locator('#advancedReportSection');
    const promoSection = page.locator('#advancedReportPromo');

    // 其中一個應該可見
    const advVisible = await advSection.isVisible();
    const promoVisible = await promoSection.isVisible();

    expect(advVisible || promoVisible).toBe(true);
    console.log(`進階報表區塊: ${advVisible ? '已訂閱顯示' : '未訂閱推廣'}`);
  });
});

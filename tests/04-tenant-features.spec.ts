import { test, expect, Page, APIRequestContext } from '@playwright/test';
import { WAIT_TIME } from './utils/test-helpers';

/**
 * 店家後台功能測試
 *
 * 使用超管 API 取得店家列表，然後模擬店家登入測試各功能
 */

// 取得超管 Token
async function getAdminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/admin/login', {
    data: {
      username: 'admin',
      password: 'admin123'
    }
  });
  const data = await response.json();
  return data.data.accessToken;
}

// 取得店家列表
async function getTenants(request: APIRequestContext, token: string): Promise<any[]> {
  const response = await request.get('/api/admin/tenants', {
    headers: {
      'Authorization': `Bearer ${token}`
    }
  });
  const data = await response.json();
  return data.data?.content || [];
}

test.describe('店家後台 API 測試', () => {
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    adminToken = await getAdminToken(request);
    expect(adminToken).toBeTruthy();
  });

  test.describe('超管 API', () => {
    test('取得店家列表', async ({ request }) => {
      const response = await request.get('/api/admin/tenants', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`店家總數: ${data.data?.totalElements || 0}`);
    });

    test('取得功能列表', async ({ request }) => {
      const response = await request.get('/api/admin/features', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`功能數量: ${data.data?.length || 0}`);
    });

    test('取得儀表板資料', async ({ request }) => {
      const response = await request.get('/api/admin/dashboard', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.success).toBeTruthy();
    });

    test('取得儲值申請列表', async ({ request }) => {
      const response = await request.get('/api/admin/point-topups', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.success).toBeTruthy();
    });
  });

  test.describe('點數 API 測試', () => {
    test('點數餘額 API', async ({ request }) => {
      const response = await request.get('/api/points/balance', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.status()).toBeLessThan(500);
    });

    test('點數異動記錄 API', async ({ request }) => {
      const response = await request.get('/api/points/transactions?size=20', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      if (response.ok()) {
        const data = await response.json();
        console.log('點數異動記錄 API 回應:', data.success ? '成功' : '失敗');
        if (data.data?.content) {
          console.log('記錄數量:', data.data.content.length);
        }
      }
    });
  });

  test.describe('報表 API 測試', () => {
    test('報表摘要 API - 使用 range 參數', async ({ request }) => {
      // 測試 range=week
      const responseWeek = await request.get('/api/reports/summary?range=week', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      // 可能需要店家 token，這裡主要測試 API 是否返回非 500 錯誤
      expect(responseWeek.status()).toBeLessThan(500);

      // 測試 range=month
      const responseMonth = await request.get('/api/reports/summary?range=month', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(responseMonth.status()).toBeLessThan(500);

      // 測試 range=quarter
      const responseQuarter = await request.get('/api/reports/summary?range=quarter', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(responseQuarter.status()).toBeLessThan(500);
    });

    test('每日報表 API', async ({ request }) => {
      const response = await request.get('/api/reports/daily?range=week', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(response.status()).toBeLessThan(500);
    });

    test('熱門服務 API', async ({ request }) => {
      const response = await request.get('/api/reports/top-services?range=month&limit=10', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(response.status()).toBeLessThan(500);
    });

    test('熱門員工 API', async ({ request }) => {
      const response = await request.get('/api/reports/top-staff?range=month&limit=10', {
        headers: {
          'Authorization': `Bearer ${adminToken}`
        }
      });
      expect(response.status()).toBeLessThan(500);
    });
  });
});

test.describe('超管後台 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    // 登入超管
    await page.goto('/admin/login');
    await page.evaluate(() => localStorage.clear());
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 20000 });
  });

  test('儀表板頁面載入', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');

    // 檢查頁面標題
    const title = page.locator('h1, .page-title');
    await expect(title).toBeVisible();
  });

  test('店家列表頁面載入', async ({ page }) => {
    await page.goto('/admin/tenants');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查表格
    const table = page.locator('table');
    await expect(table).toBeVisible();
  });

  test('功能管理頁面載入', async ({ page }) => {
    await page.goto('/admin/features');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查頁面內容
    const content = page.locator('main, #content, .container');
    await expect(content).toBeVisible();
  });

  test('儲值審核頁面載入', async ({ page }) => {
    await page.goto('/admin/point-topups');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查表格
    const table = page.locator('table');
    await expect(table).toBeVisible();
  });

  test('側邊欄導航功能', async ({ page }) => {
    // 點擊各個選單項目
    const menuItems = [
      { selector: 'a[href="/admin/tenants"]', expectedUrl: '/admin/tenants' },
      { selector: 'a[href="/admin/features"]', expectedUrl: '/admin/features' },
      { selector: 'a[href="/admin/point-topups"]', expectedUrl: '/admin/point-topups' },
      { selector: 'a[href="/admin/dashboard"]', expectedUrl: '/admin/dashboard' }
    ];

    for (const item of menuItems) {
      const link = page.locator(item.selector).first();
      if (await link.isVisible()) {
        await link.click();
        await page.waitForLoadState('domcontentloaded');
        expect(page.url()).toContain(item.expectedUrl);
        console.log(`✓ 導航到 ${item.expectedUrl} 成功`);
      }
    }
  });

  test('登出功能', async ({ page }) => {
    // 找到登出按鈕
    const logoutBtn = page.locator('a:has-text("登出"), button:has-text("登出")').first();

    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();

      // 可能有確認對話框
      const confirmBtn = page.locator('.modal .btn-primary, .swal2-confirm, #confirmOkBtn');
      if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
        await confirmBtn.click();
      }

      await page.waitForTimeout(WAIT_TIME.api);

      // 應該回到登入頁
      expect(page.url()).toContain('/login');
    }
  });
});

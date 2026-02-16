import { test, expect, Page } from '@playwright/test';
import { TEST_ACCOUNTS, tenantLogin, adminLogin, waitForLoading, WAIT_TIME } from './utils/test-helpers';

// 收集所有錯誤
const errors: string[] = [];

test.describe('全面 BUG 搜尋測試', () => {

  test.describe('店家後台 - 每個頁面點擊測試', () => {
    let page: Page;

    test.beforeAll(async ({ browser }) => {
      page = await browser.newPage();

      // 監聽所有 console 錯誤
      page.on('console', msg => {
        if (msg.type() === 'error') {
          const text = msg.text();
          if (!text.includes('favicon') && !text.includes('ERR_CONNECTION')) {
            errors.push(`Console Error: ${text}`);
          }
        }
      });

      // 監聽所有網路錯誤
      page.on('requestfailed', request => {
        const url = request.url();
        if (!url.includes('favicon')) {
          errors.push(`Network Failed: ${url} - ${request.failure()?.errorText}`);
        }
      });

      // 監聯所有 400/500 回應
      page.on('response', response => {
        const status = response.status();
        const url = response.url();
        if (status >= 400 && !url.includes('favicon')) {
          errors.push(`HTTP ${status}: ${url}`);
        }
      });

      await tenantLogin(page);
    });

    test.afterAll(async () => {
      await page.close();
      if (errors.length > 0) {
        console.log('\n========== 發現的錯誤 ==========');
        errors.forEach(e => console.log(e));
        console.log('================================\n');
      }
    });

    const pages = [
      { name: '儀表板', url: '/tenant/dashboard' },
      { name: '預約管理', url: '/tenant/bookings' },
      { name: '行事曆', url: '/tenant/calendar' },
      { name: '顧客管理', url: '/tenant/customers' },
      { name: '員工管理', url: '/tenant/staff' },
      { name: '服務管理', url: '/tenant/services' },
      { name: '商品管理', url: '/tenant/products' },
      { name: '庫存異動', url: '/tenant/inventory' },
      { name: '商品訂單', url: '/tenant/product-orders' },
      { name: '票券管理', url: '/tenant/coupons' },
      { name: '行銷活動', url: '/tenant/campaigns' },
      { name: '行銷推播', url: '/tenant/marketing' },
      { name: '營運報表', url: '/tenant/reports' },
      { name: '店家設定', url: '/tenant/settings' },
      { name: 'LINE 設定', url: '/tenant/line-settings' },
      { name: '功能商店', url: '/tenant/feature-store' },
      { name: '點數管理', url: '/tenant/points' },
      { name: '會員等級', url: '/tenant/membership-levels' },
      { name: '推薦好友', url: '/tenant/referrals' },
    ];

    for (const p of pages) {
      test(`${p.name} 頁面載入無錯誤`, async () => {
        const pageErrors: string[] = [];

        // 監聽此頁面的錯誤
        const errorHandler = (msg: any) => {
          if (msg.type() === 'error') {
            pageErrors.push(msg.text());
          }
        };
        page.on('console', errorHandler);

        const responseHandler = (response: any) => {
          if (response.status() >= 400 && !response.url().includes('favicon')) {
            pageErrors.push(`HTTP ${response.status()}: ${response.url()}`);
          }
        };
        page.on('response', responseHandler);

        await page.goto(p.url);
        await waitForLoading(page);
        await page.waitForTimeout(2000); // 等待 API 完成

        page.off('console', errorHandler);
        page.off('response', responseHandler);

        if (pageErrors.length > 0) {
          console.log(`\n${p.name} 頁面錯誤:`);
          pageErrors.forEach(e => console.log(`  - ${e}`));
        }

        // 檢查是否有「載入失敗」文字
        const failText = await page.locator('text=載入失敗').count();
        expect(failText, `${p.name} 顯示載入失敗`).toBe(0);

        // 檢查沒有 HTTP 錯誤
        const httpErrors = pageErrors.filter(e => e.startsWith('HTTP'));
        expect(httpErrors.length, `${p.name} 有 HTTP 錯誤: ${httpErrors.join(', ')}`).toBe(0);
      });
    }
  });

  test.describe('店家後台 - 所有按鈕點擊測試', () => {
    test('預約管理 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/bookings');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增預約"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 預約新增 Modal 正常開啟');
      }
    });

    test('顧客管理 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增顧客"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 顧客新增 Modal 正常開啟');
      }
    });

    test('員工管理 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/staff');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增員工"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 員工新增 Modal 正常開啟');
      }
    });

    test('服務管理 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/services');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增服務"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 服務新增 Modal 正常開啟');
      }
    });

    test('商品管理 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/products');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增商品"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 商品新增 Modal 正常開啟');
      }
    });

    test('票券管理 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/coupons');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增票券"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 票券新增 Modal 正常開啟');
      }
    });

    test('行銷活動 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/campaigns');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增活動"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 活動新增 Modal 正常開啟');
      }
    });

    test('行銷推播 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/marketing');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("建立推播"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 推播新增 Modal 正常開啟');
      }
    });

    test('會員等級 - 新增按鈕', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/membership-levels');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      const addBtn = page.locator('button:has-text("新增等級"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
        console.log('✓ 等級新增 Modal 正常開啟');
      }
    });
  });

  test.describe('店家後台 - 顧客詳情頁測試', () => {
    test('顧客詳情頁所有區塊載入', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(1000);

      // 點擊第一個顧客
      const viewBtn = page.locator('a[href*="/tenant/customers/"]').first();
      if (await viewBtn.isVisible()) {
        await viewBtn.click();
        await waitForLoading(page);
        await page.waitForTimeout(2000);

        // 檢查是否有載入失敗
        const failCount = await page.locator('text=載入失敗').count();
        expect(failCount).toBe(0);

        // 檢查各區塊
        const sections = ['顧客資訊', '預約記錄', '點數記錄'];
        for (const section of sections) {
          const exists = await page.locator(`text=${section}`).count() > 0;
          console.log(`${section}: ${exists ? '✓' : '✗'}`);
        }
      }
    });
  });

  test.describe('API 直接測試', () => {
    test('所有 API 端點回應正常', async ({ request }) => {
      // 登入取得 Token
      const loginRes = await request.post('/api/auth/tenant/login', {
        data: {
          username: TEST_ACCOUNTS.tenant.username,
          password: TEST_ACCOUNTS.tenant.password
        }
      });
      const loginData = await loginRes.json();
      const token = loginData.data?.accessToken;

      if (!token) {
        console.log('登入失敗，跳過 API 測試');
        return;
      }

      const endpoints = [
        '/api/bookings?page=0&size=20',
        '/api/customers?page=0&size=20',
        '/api/staff',
        '/api/services',
        '/api/products?page=0&size=20',
        '/api/coupons',
        '/api/campaigns',
        '/api/marketing/pushes?page=0&size=20',
        '/api/reports/summary?range=month',
        '/api/reports/dashboard',
        '/api/reports/top-services?range=month&limit=10',
        '/api/reports/top-staff?range=month&limit=10',
        '/api/settings',
        '/api/settings/line',
        '/api/feature-store',
        '/api/points/balance',
        '/api/membership-levels',
        '/api/inventory/logs?page=0&size=30',
        '/api/product-orders?page=0&size=20',
      ];

      const failedEndpoints: string[] = [];

      for (const endpoint of endpoints) {
        const res = await request.get(endpoint, {
          headers: { 'Authorization': `Bearer ${token}` }
        });

        if (!res.ok()) {
          failedEndpoints.push(`${endpoint} => ${res.status()}`);
        }
        console.log(`${endpoint}: ${res.ok() ? '✓' : `✗ ${res.status()}`}`);
      }

      expect(failedEndpoints.length, `失敗的 API: ${failedEndpoints.join(', ')}`).toBe(0);
    });
  });

  test.describe('超管後台測試', () => {
    const adminPages = [
      { name: '儀表板', url: '/admin/dashboard' },
      { name: '店家管理', url: '/admin/tenants' },
      { name: '功能管理', url: '/admin/features' },
      { name: '儲值審核', url: '/admin/point-topups' },
    ];

    for (const p of adminPages) {
      test(`超管 ${p.name} 頁面載入無錯誤`, async ({ page }) => {
        const pageErrors: string[] = [];

        page.on('response', response => {
          if (response.status() >= 400 && !response.url().includes('favicon')) {
            pageErrors.push(`HTTP ${response.status()}: ${response.url()}`);
          }
        });

        await adminLogin(page);
        await page.goto(p.url);
        await waitForLoading(page);
        await page.waitForTimeout(2000);

        const failText = await page.locator('text=載入失敗').count();
        expect(failText, `${p.name} 顯示載入失敗`).toBe(0);

        const httpErrors = pageErrors.filter(e => e.startsWith('HTTP'));
        expect(httpErrors.length, `${p.name} 有 HTTP 錯誤: ${httpErrors.join(', ')}`).toBe(0);
      });
    }
  });
});

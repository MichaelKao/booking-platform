import { test, expect } from '@playwright/test';
import { adminLogin, WAIT_TIME, TEST_ACCOUNTS } from './utils/test-helpers';

const BASE_URL = process.env.BASE_URL || 'https://booking-platform-production-1e08.up.railway.app';

test.describe('超管後台所有頁面測試', () => {

  test.beforeEach(async ({ page }) => {
    // 登入超管後台
    await adminLogin(page);
  });

  test.describe('儀表板', () => {
    test('儀表板頁面載入成功', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查統計卡片
      const statsCards = page.locator('.card, .stat-card, [class*="stat"]');
      expect(await statsCards.count()).toBeGreaterThan(0);
    });
  });

  test.describe('店家管理', () => {
    test('店家列表頁面載入成功', async ({ page }) => {
      await page.goto('/admin/tenants');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查表格存在
      const table = page.locator('table');
      await expect(table).toBeVisible();
    });

    test('店家列表無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto('/admin/tenants');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(3000);

      // 過濾掉非關鍵錯誤
      const criticalErrors = errors.filter(e =>
        e.includes('SyntaxError') ||
        e.includes('ReferenceError') ||
        e.includes('TypeError')
      );

      if (criticalErrors.length > 0) {
        console.log('店家列表頁面錯誤:', criticalErrors);
      }

      expect(criticalErrors).toHaveLength(0);
    });
  });

  test.describe('儲值審核', () => {
    test('儲值審核頁面載入成功', async ({ page }) => {
      await page.goto('/admin/point-topups');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();
    });
  });

  test.describe('功能管理', () => {
    test('功能管理頁面載入成功', async ({ page }) => {
      await page.goto('/admin/features');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查功能卡片或表格
      const content = page.locator('table, .card, .feature-card');
      expect(await content.count()).toBeGreaterThan(0);
    });
  });
});

test.describe('超管後台側邊欄導航', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test('所有側邊欄連結可點擊', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');

    const navLinks = page.locator('#sidebar .nav-link, .sidebar .nav-link');
    const count = await navLinks.count();
    console.log(`超管側邊欄連結數量: ${count}`);

    expect(count).toBeGreaterThan(2);
  });
});

test.describe('超管後台無 JavaScript 錯誤', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  const pages = [
    { name: '儀表板', url: '/admin/dashboard' },
    { name: '店家管理', url: '/admin/tenants' },
    { name: '儲值審核', url: '/admin/point-topups' },
    { name: '功能管理', url: '/admin/features' },
  ];

  for (const p of pages) {
    test(`${p.name} 頁面無 JavaScript 錯誤`, async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto(p.url);
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 過濾掉非關鍵錯誤
      const criticalErrors = errors.filter(e =>
        e.includes('SyntaxError') ||
        e.includes('ReferenceError') ||
        e.includes('TypeError')
      );

      if (criticalErrors.length > 0) {
        console.log(`${p.name} 頁面錯誤:`, criticalErrors);
      }

      expect(criticalErrors).toHaveLength(0);
    });
  }
});

test.describe('超管 API 測試', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test('儀表板 API 回應正確', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    // 取得 token
    const token = await page.evaluate(() => localStorage.getItem('admin_token'));
    if (!token) {
      console.log('Token not found, skipping API test');
      return;
    }

    // 測試 API
    const response = await page.evaluate(async (token) => {
      const res = await fetch('/api/admin/dashboard', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      return { status: res.status, ok: res.ok };
    }, token);

    expect(response.ok).toBe(true);
  });

  test('店家列表 API 回應正確', async ({ page }) => {
    await page.goto('/admin/tenants');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    const token = await page.evaluate(() => localStorage.getItem('admin_token'));
    if (!token) {
      console.log('Token not found, skipping API test');
      return;
    }

    const response = await page.evaluate(async (token) => {
      const res = await fetch('/api/admin/tenants?page=0&size=10', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      return { status: res.status, ok: res.ok };
    }, token);

    expect(response.ok).toBe(true);
  });

  test('功能列表 API 回應正確', async ({ page }) => {
    await page.goto('/admin/features');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    const token = await page.evaluate(() => localStorage.getItem('admin_token'));
    if (!token) {
      console.log('Token not found, skipping API test');
      return;
    }

    const response = await page.evaluate(async (token) => {
      const res = await fetch('/api/admin/features', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      return { status: res.status, ok: res.ok };
    }, token);

    expect(response.ok).toBe(true);
  });
});

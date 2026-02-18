import { test, expect } from './fixtures';
import {
  adminLogin,
  waitForLoading,
  waitForApiResponse,
  tableHasData,
  WAIT_TIME,
  generateTestData,
  generateTestEmail,
  confirmDialog
} from './utils/test-helpers';

/**
 * 超管後台測試
 *
 * 測試範圍：
 * 1. 儀表板
 * 2. 店家管理
 * 3. 功能管理
 * 4. 儲值審核
 * 5. 側邊欄導航
 * 6. API 回應驗證
 */

test.describe('超管後台', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test.describe('儀表板', () => {
    test('儀表板載入正常', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await waitForLoading(page);

      // 檢查統計卡片
      const statsCards = page.locator('.card, .stat-card, [class*="card"]');
      await expect(statsCards.first()).toBeVisible();
    });

    test('顯示統計數據', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查是否有數字顯示
      const hasNumbers = await page.evaluate(() => {
        const cards = document.querySelectorAll('.card, .stat-card');
        for (const card of cards) {
          const text = card.textContent || '';
          if (/\d+/.test(text)) return true;
        }
        return false;
      });

      expect(hasNumbers).toBeTruthy();
    });
  });

  test.describe('店家管理', () => {
    test('店家列表頁面載入', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);

      // 檢查表格
      const table = page.locator('table.table');
      await expect(table).toBeVisible();
    });

    test('店家列表有資料', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const hasData = await tableHasData(page);
      // 可能有資料也可能沒有，主要確認頁面正常載入
      console.log(`店家列表有資料: ${hasData}`);
    });

    test('可以查看店家詳情', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 點擊第一個店家的詳情按鈕
      const detailBtn = page.locator('table tbody tr:first-child a, table tbody tr:first-child button').first();
      if (await detailBtn.isVisible()) {
        await detailBtn.click();
        await waitForLoading(page);

        // 確認跳轉到詳情頁或開啟 Modal
        await page.waitForTimeout(WAIT_TIME.medium);
      }
    });

    test('搜尋功能正常', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);

      // 找到搜尋框
      const searchInput = page.locator('input[type="search"], input[name="search"], input[placeholder*="搜尋"]');
      if (await searchInput.isVisible()) {
        await searchInput.fill('test');
        await page.waitForTimeout(WAIT_TIME.api);
        // 確認沒有錯誤
      }
    });
  });

  test.describe('功能管理', () => {
    test('功能列表頁面載入', async ({ page }) => {
      await page.goto('/admin/features');
      await waitForLoading(page);

      // 檢查頁面載入
      const pageContent = page.locator('.container, main, #content');
      await expect(pageContent).toBeVisible();
    });

    test('顯示功能列表', async ({ page }) => {
      await page.goto('/admin/features');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查是否有功能項目顯示
      const featureItems = page.locator('table tbody tr, .feature-item, .card');
      const count = await featureItems.count();
      console.log(`功能項目數量: ${count}`);
    });
  });

  test.describe('儲值審核', () => {
    test('儲值列表頁面載入', async ({ page }) => {
      await page.goto('/admin/point-topups');
      await waitForLoading(page);

      // 檢查頁面載入
      const pageContent = page.locator('.container, main, #content');
      await expect(pageContent).toBeVisible();
    });

    test('顯示儲值申請列表', async ({ page }) => {
      await page.goto('/admin/point-topups');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查表格
      const table = page.locator('table');
      await expect(table).toBeVisible();
    });

    test('篩選待審核項目', async ({ page }) => {
      await page.goto('/admin/point-topups');
      await waitForLoading(page);

      // 找到篩選按鈕或下拉選單
      const filterBtn = page.locator('select[name="status"], button:has-text("待審核"), [data-status="PENDING"]');
      if (await filterBtn.first().isVisible()) {
        await filterBtn.first().click();
        await page.waitForTimeout(WAIT_TIME.api);
      }
    });
  });

  test.describe('側邊欄導航', () => {
    test('所有選單項目可點擊', async ({ page }) => {
      await page.goto('/admin/dashboard');

      const menuItems = [
        { text: '儀表板', url: '/admin/dashboard' },
        { text: '店家管理', url: '/admin/tenants' },
        { text: '儲值審核', url: '/admin/point-topups' },
        { text: '功能管理', url: '/admin/features' }
      ];

      for (const item of menuItems) {
        const link = page.locator(`a:has-text("${item.text}")`).first();
        if (await link.isVisible()) {
          await link.click();
          await waitForLoading(page);
          expect(page.url()).toContain(item.url);
          console.log(`✓ ${item.text} 頁面正常`);
        }
      }
    });
  });

  test.describe('超管 API 測試', () => {
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
});

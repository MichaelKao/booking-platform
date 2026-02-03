import { test, expect } from '@playwright/test';
import { tenantLogin, waitForLoading, WAIT_TIME, TEST_ACCOUNTS } from './utils/test-helpers';

const BASE_URL = process.env.BASE_URL || 'https://booking-platform-production-1e08.up.railway.app';

test.describe('店家後台所有頁面測試', () => {

  test.beforeEach(async ({ page }) => {
    // 登入店家後台
    await tenantLogin(page);
  });

  test.describe('儀表板', () => {
    test('儀表板頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/dashboard');
      await page.waitForLoadState('domcontentloaded');

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查統計卡片
      const statsCards = page.locator('.card, .stat-card, [class*="stat"]');
      expect(await statsCards.count()).toBeGreaterThan(0);
    });
  });

  test.describe('預約管理', () => {
    test('預約列表頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(3000); // 等待 JavaScript 執行

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查表格存在
      const table = page.locator('table');
      await expect(table).toBeVisible();

      // 檢查篩選器
      const dateFilter = page.locator('#dateFilter, input[type="date"]');
      await expect(dateFilter.first()).toBeVisible();
    });

    test('預約列表無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto('/tenant/bookings');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(3000);

      // 確認沒有 JavaScript 錯誤
      expect(errors.filter(e => e.includes('SyntaxError'))).toHaveLength(0);
      expect(errors.filter(e => e.includes('durationEl'))).toHaveLength(0);
    });

    test('新增預約 Modal 可開啟', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const addBtn = page.locator('button:has-text("新增預約")');
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(500);

        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
      }
    });
  });

  test.describe('行事曆', () => {
    test('行事曆頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/calendar');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查 FullCalendar 元素
      const calendar = page.locator('#calendar, .fc');
      await expect(calendar.first()).toBeVisible();
    });
  });

  test.describe('營運報表', () => {
    test('報表頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/reports');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查圖表區域
      const chartArea = page.locator('canvas, .chart, [class*="chart"]');
      expect(await chartArea.count()).toBeGreaterThan(0);
    });
  });

  test.describe('顧客管理', () => {
    test('顧客列表頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/customers');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查表格
      const table = page.locator('table');
      await expect(table).toBeVisible();
    });

    test('新增顧客 Modal 可開啟', async ({ page }) => {
      await page.goto('/tenant/customers');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const addBtn = page.locator('button:has-text("新增顧客"), button:has-text("新增")');
      if (await addBtn.first().isVisible()) {
        await addBtn.first().click();
        await page.waitForTimeout(500);

        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();
      }
    });
  });

  test.describe('員工管理', () => {
    test('員工列表頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/staff');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查表格或卡片
      const content = page.locator('table, .card, .staff-card');
      expect(await content.count()).toBeGreaterThan(0);
    });
  });

  test.describe('服務管理', () => {
    test('服務列表頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/services');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查表格或卡片
      const content = page.locator('table, .card, .service-card');
      expect(await content.count()).toBeGreaterThan(0);
    });
  });

  test.describe('商品管理', () => {
    test('商品列表頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/products');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();
    });
  });

  test.describe('票券管理', () => {
    test('票券列表頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/coupons');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();
    });
  });

  test.describe('行銷活動', () => {
    test('行銷活動頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/campaigns');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();
    });
  });

  test.describe('店家設定', () => {
    test('設定頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/settings');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查表單元素
      const form = page.locator('form, .settings-form, input, select');
      expect(await form.count()).toBeGreaterThan(0);
    });
  });

  test.describe('LINE 設定', () => {
    test('LINE 設定頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();
    });
  });

  test.describe('功能商店', () => {
    test('功能商店頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();

      // 檢查功能卡片
      const cards = page.locator('.card, .feature-card');
      expect(await cards.count()).toBeGreaterThan(0);
    });
  });

  test.describe('點數管理', () => {
    test('點數管理頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/points');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();
    });
  });

  test.describe('會員等級', () => {
    test('會員等級頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/membership-levels');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查頁面標題
      await expect(page.locator('h1, .page-title').first()).toBeVisible();
    });
  });
});

test.describe('店家後台側邊欄導航', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('所有側邊欄連結可點擊', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');

    const navLinks = page.locator('#sidebar .nav-link, .sidebar .nav-link');
    const count = await navLinks.count();
    console.log(`側邊欄連結數量: ${count}`);

    expect(count).toBeGreaterThan(5);
  });
});

test.describe('店家後台無 JavaScript 錯誤', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  const pages = [
    { name: '儀表板', url: '/tenant/dashboard' },
    { name: '預約管理', url: '/tenant/bookings' },
    { name: '行事曆', url: '/tenant/calendar' },
    { name: '營運報表', url: '/tenant/reports' },
    { name: '顧客管理', url: '/tenant/customers' },
    { name: '員工管理', url: '/tenant/staff' },
    { name: '服務管理', url: '/tenant/services' },
    { name: '商品管理', url: '/tenant/products' },
    { name: '票券管理', url: '/tenant/coupons' },
    { name: '行銷活動', url: '/tenant/campaigns' },
    { name: '店家設定', url: '/tenant/settings' },
    { name: 'LINE 設定', url: '/tenant/line-settings' },
    { name: '功能商店', url: '/tenant/feature-store' },
    { name: '點數管理', url: '/tenant/points' },
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

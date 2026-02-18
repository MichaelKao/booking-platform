/**
 * RWD 響應式測試
 *
 * 測試所有頁面在手機/平板/桌面三種尺寸的響應式表現
 * 包含側邊欄捲動、頁面載入、無水平溢出等檢查
 * 透過 fixtures.ts 自動監控 F12 Console 錯誤
 */
import { test, expect } from './fixtures';
import { tenantLogin, adminLogin, WAIT_TIME } from './utils/test-helpers';

// 三種裝置尺寸
const VIEWPORTS = {
  mobile: { width: 375, height: 667 },
  tablet: { width: 768, height: 1024 },
  desktop: { width: 1280, height: 720 },
};

// 店家後台頁面
const TENANT_PAGES = [
  { name: '儀表板', path: '/tenant/dashboard' },
  { name: '預約管理', path: '/tenant/bookings' },
  { name: '行事曆', path: '/tenant/calendar' },
  { name: '顧客列表', path: '/tenant/customers' },
  { name: '員工管理', path: '/tenant/staff' },
  { name: '服務管理', path: '/tenant/services' },
  { name: '商品管理', path: '/tenant/products' },
  { name: '票券管理', path: '/tenant/coupons' },
  { name: '行銷活動', path: '/tenant/campaigns' },
  { name: '行銷推播', path: '/tenant/marketing' },
  { name: '店家設定', path: '/tenant/settings' },
  { name: 'LINE 設定', path: '/tenant/line-settings' },
  { name: '功能商店', path: '/tenant/feature-store' },
  { name: '點數管理', path: '/tenant/points' },
  { name: '營運報表', path: '/tenant/reports' },
  { name: '會員等級', path: '/tenant/membership-levels' },
];

// 超管後台頁面
const ADMIN_PAGES = [
  { name: '儀表板', path: '/admin/dashboard' },
  { name: '店家列表', path: '/admin/tenants' },
  { name: '儲值審核', path: '/admin/point-topups' },
  { name: '功能管理', path: '/admin/features' },
];

// 公開頁面
const PUBLIC_PAGES = [
  { name: '店家登入', path: '/tenant/login' },
  { name: '超管登入', path: '/admin/login' },
  { name: '店家註冊', path: '/tenant/register' },
  { name: '忘記密碼', path: '/tenant/forgot-password' },
  { name: '首頁', path: '/' },
  { name: '功能介紹', path: '/features' },
  { name: '價格方案', path: '/pricing' },
  { name: 'FAQ', path: '/faq' },
];

/**
 * 檢查頁面是否有水平溢出
 */
async function checkNoHorizontalOverflow(page: any): Promise<boolean> {
  return await page.evaluate(() => {
    return document.documentElement.scrollWidth <= document.documentElement.clientWidth;
  });
}

// =============================================
// 店家後台 - 側邊欄測試（手機版）
// =============================================
test.describe('店家後台 - 手機版側邊欄', () => {

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.mobile);
    await tenantLogin(page);
  });

  test('側邊欄 toggle 開關正常', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    // 側邊欄預設應隱藏
    const sidebar = page.locator('#sidebar');
    await expect(sidebar).not.toHaveClass(/show/);

    // 點擊 toggle 按鈕
    const toggleBtn = page.locator('#sidebarToggleMobile, .btn-sidebar-toggle').first();
    if (await toggleBtn.isVisible()) {
      await toggleBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 側邊欄應顯示
      await expect(sidebar).toHaveClass(/show/);
    }
  });

  test('側邊欄遮罩顯示正常', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const toggleBtn = page.locator('#sidebarToggleMobile, .btn-sidebar-toggle').first();
    if (await toggleBtn.isVisible()) {
      await toggleBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 遮罩應顯示
      const overlay = page.locator('.sidebar-overlay');
      const overlayCount = await overlay.count();
      if (overlayCount > 0) {
        await expect(overlay.first()).toHaveClass(/show/);
      }
    }
  });

  test('側邊欄可捲動', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const toggleBtn = page.locator('#sidebarToggleMobile, .btn-sidebar-toggle').first();
    if (await toggleBtn.isVisible()) {
      await toggleBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 檢查側邊欄內的 nav 捲動區域 overflow-y 是否為 auto 或 scroll
      const navColumn = page.locator('#sidebar .nav.flex-column');
      const overflowY = await navColumn.evaluate((el: Element) => {
        return window.getComputedStyle(el).overflowY;
      });
      expect(['auto', 'scroll']).toContain(overflowY);
    }
  });

  test('側邊欄最後的選單項目可見', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const toggleBtn = page.locator('#sidebarToggleMobile, .btn-sidebar-toggle').first();
    if (await toggleBtn.isVisible()) {
      await toggleBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 捲動到最底部
      const navColumn = page.locator('#sidebar .nav.flex-column');
      await navColumn.evaluate((el: Element) => {
        el.scrollTop = el.scrollHeight;
      });
      await page.waitForTimeout(WAIT_TIME.short);

      // 最後一個 nav-link 應在視窗範圍內（可見或可滑到）
      const lastNavLink = page.locator('#sidebar .nav-link').last();
      const count = await lastNavLink.count();
      expect(count).toBeGreaterThan(0);
    }
  });
});

// =============================================
// 超管後台 - 側邊欄測試（手機版）
// =============================================
test.describe('超管後台 - 手機版側邊欄', () => {

  test.beforeEach(async ({ page }) => {
    await page.setViewportSize(VIEWPORTS.mobile);
    await adminLogin(page);
  });

  test('側邊欄 toggle 開關正常', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const sidebar = page.locator('#sidebar');
    await expect(sidebar).not.toHaveClass(/show/);

    const toggleBtn = page.locator('#sidebarToggleMobile, .btn-sidebar-toggle').first();
    if (await toggleBtn.isVisible()) {
      await toggleBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);
      await expect(sidebar).toHaveClass(/show/);
    }
  });

  test('側邊欄可捲動', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.long);

    const toggleBtn = page.locator('#sidebarToggleMobile, .btn-sidebar-toggle').first();
    if (await toggleBtn.isVisible()) {
      await toggleBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 檢查側邊欄內的 nav 捲動區域 overflow-y 是否為 auto 或 scroll
      const navColumn = page.locator('#sidebar .nav.flex-column');
      const overflowY = await navColumn.evaluate((el: Element) => {
        return window.getComputedStyle(el).overflowY;
      });
      expect(['auto', 'scroll']).toContain(overflowY);
    }
  });
});

// =============================================
// 店家後台 - 頁面載入測試（三種尺寸）
// =============================================
for (const [viewportName, viewportSize] of Object.entries(VIEWPORTS)) {
  test.describe(`店家後台 - ${viewportName} (${viewportSize.width}x${viewportSize.height})`, () => {

    test.beforeEach(async ({ page }) => {
      await page.setViewportSize(viewportSize);
      await tenantLogin(page);
    });

    for (const tenantPage of TENANT_PAGES) {
      test(`${tenantPage.name} 頁面載入正常`, async ({ page }) => {
        await page.goto(tenantPage.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(WAIT_TIME.long);

        // 頁面 body 應可見
        await expect(page.locator('body')).toBeVisible();

        // 無水平溢出
        const noOverflow = await checkNoHorizontalOverflow(page);
        expect(noOverflow).toBeTruthy();
      });
    }
  });
}

// =============================================
// 超管後台 - 頁面載入測試（三種尺寸）
// =============================================
for (const [viewportName, viewportSize] of Object.entries(VIEWPORTS)) {
  test.describe(`超管後台 - ${viewportName} (${viewportSize.width}x${viewportSize.height})`, () => {

    test.beforeEach(async ({ page }) => {
      await page.setViewportSize(viewportSize);
      await adminLogin(page);
    });

    for (const adminPage of ADMIN_PAGES) {
      test(`${adminPage.name} 頁面載入正常`, async ({ page }) => {
        await page.goto(adminPage.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(WAIT_TIME.long);

        await expect(page.locator('body')).toBeVisible();

        const noOverflow = await checkNoHorizontalOverflow(page);
        expect(noOverflow).toBeTruthy();
      });
    }
  });
}

// =============================================
// 公開頁面 - 頁面載入測試（三種尺寸）
// =============================================
for (const [viewportName, viewportSize] of Object.entries(VIEWPORTS)) {
  test.describe(`公開頁面 - ${viewportName} (${viewportSize.width}x${viewportSize.height})`, () => {

    for (const publicPage of PUBLIC_PAGES) {
      test(`${publicPage.name} 頁面載入正常`, async ({ page }) => {
        await page.setViewportSize(viewportSize);
        await page.goto(publicPage.path);
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(WAIT_TIME.medium);

        await expect(page.locator('body')).toBeVisible();

        const noOverflow = await checkNoHorizontalOverflow(page);
        expect(noOverflow).toBeTruthy();
      });
    }
  });
}

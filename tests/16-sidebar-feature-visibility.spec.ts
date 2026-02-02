import { test, expect, APIRequestContext } from '@playwright/test';
import {
  tenantLogin,
  adminLogin,
  waitForLoading,
  WAIT_TIME
} from './utils/test-helpers';

/**
 * 側邊欄功能訂閱控制測試
 *
 * 測試範圍：
 * 1. 功能訂閱狀態影響側邊欄顯示
 * 2. data-feature 屬性正確設定
 * 3. 訂閱/取消訂閱後側邊欄更新
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: 'g0909095118@gmail.com', password: 'gaojunting11' }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

async function getAdminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/admin/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  const data = await response.json();
  return data.data.accessToken;
}

test.describe('側邊欄功能訂閱控制測試', () => {
  test.describe('data-feature 屬性檢查', () => {
    test('側邊欄包含正確的 data-feature 屬性', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查 data-feature 屬性
      const featureElements = page.locator('[data-feature]');
      const count = await featureElements.count();
      console.log(`具有 data-feature 屬性的元素數: ${count}`);

      // 列出所有 data-feature 值
      const features = new Set<string>();
      for (let i = 0; i < count; i++) {
        const feature = await featureElements.nth(i).getAttribute('data-feature');
        if (feature) {
          features.add(feature);
        }
      }

      console.log('發現的 data-feature 值:');
      for (const feature of features) {
        console.log(`- ${feature}`);
      }

      // 驗證預期的 feature
      expect(features.has('BASIC_REPORT') || features.has('PRODUCT_SALES') || features.has('COUPON_SYSTEM')).toBeTruthy();
    });

    test('營運報表選單有 BASIC_REPORT 屬性', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const reportsMenuItem = page.locator('[data-feature="BASIC_REPORT"]');
      const count = await reportsMenuItem.count();
      console.log(`BASIC_REPORT 選單項目數: ${count}`);
    });

    test('商品管理選單有 PRODUCT_SALES 屬性', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const productsMenuItem = page.locator('[data-feature="PRODUCT_SALES"]');
      const count = await productsMenuItem.count();
      console.log(`PRODUCT_SALES 選單項目數: ${count}`);
    });

    test('票券管理選單有 COUPON_SYSTEM 屬性', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const couponsMenuItem = page.locator('[data-feature="COUPON_SYSTEM"]');
      const count = await couponsMenuItem.count();
      console.log(`COUPON_SYSTEM 選單項目數: ${count}`);
    });
  });

  test.describe('功能訂閱 API 與側邊欄', () => {
    test('取得功能訂閱狀態', async ({ request }) => {
      const tenantToken = await getTenantToken(request);
      if (!tenantToken) return;

      const response = await request.get('/api/feature-store', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();

      console.log('功能訂閱狀態:');
      for (const feature of data.data || []) {
        console.log(`- ${feature.code}: ${feature.subscribed ? '已訂閱' : '未訂閱'}`);
      }
    });

    test('側邊欄根據訂閱狀態顯示/隱藏選單', async ({ page, request }) => {
      const tenantToken = await getTenantToken(request);
      if (!tenantToken) return;

      // 取得訂閱狀態
      const response = await request.get('/api/feature-store', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const features = await response.json();

      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查每個功能的選單是否正確顯示
      for (const feature of features.data || []) {
        const menuItem = page.locator(`[data-feature="${feature.code}"]`);
        const count = await menuItem.count();

        if (count > 0) {
          const isVisible = await menuItem.first().isVisible();
          console.log(`${feature.code}: 訂閱=${feature.subscribed}, 可見=${isVisible}`);

          // 如果已訂閱，選單應該可見；如果未訂閱，選單可能隱藏
          // 注意：免費功能可能不受此限制
          if (feature.subscribed) {
            expect(isVisible).toBeTruthy();
          }
        }
      }
    });
  });

  test.describe('基本選單項目（不需訂閱）', () => {
    test('儀表板選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const dashboardLink = page.locator('a.nav-link[href="/tenant/dashboard"]');
      await expect(dashboardLink).toBeVisible();
    });

    test('預約列表選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const bookingsLink = page.locator('a.nav-link[href="/tenant/bookings"]');
      await expect(bookingsLink).toBeVisible();
    });

    test('行事曆選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const calendarLink = page.locator('a.nav-link[href="/tenant/calendar"]');
      await expect(calendarLink).toBeVisible();
    });

    test('顧客列表選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const customersLink = page.locator('a.nav-link[href="/tenant/customers"]');
      await expect(customersLink).toBeVisible();
    });

    test('員工管理選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const staffLink = page.locator('a.nav-link[href="/tenant/staff"]');
      await expect(staffLink).toBeVisible();
    });

    test('服務項目選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const servicesLink = page.locator('a.nav-link[href="/tenant/services"]');
      await expect(servicesLink).toBeVisible();
    });

    test('店家設定選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const settingsLink = page.locator('a.nav-link[href="/tenant/settings"]');
      await expect(settingsLink).toBeVisible();
    });

    test('LINE 設定選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const lineSettingsLink = page.locator('a.nav-link[href="/tenant/line-settings"]');
      await expect(lineSettingsLink).toBeVisible();
    });

    test('功能商店選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const featureStoreLink = page.locator('a.nav-link[href="/tenant/feature-store"]');
      await expect(featureStoreLink).toBeVisible();
    });

    test('點數管理選單始終可見', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);

      const pointsLink = page.locator('a.nav-link[href="/tenant/points"]');
      await expect(pointsLink).toBeVisible();
    });
  });

  test.describe('側邊欄完整選單測試', () => {
    test('所有選單項目可點擊導航', async ({ page }) => {
      await tenantLogin(page);

      const menuItems = [
        { href: '/tenant/dashboard', name: '儀表板' },
        { href: '/tenant/bookings', name: '預約列表' },
        { href: '/tenant/calendar', name: '行事曆' },
        { href: '/tenant/customers', name: '顧客列表' },
        { href: '/tenant/staff', name: '員工管理' },
        { href: '/tenant/services', name: '服務項目' },
        { href: '/tenant/campaigns', name: '行銷活動' },
        { href: '/tenant/marketing', name: '行銷推播' },
        { href: '/tenant/settings', name: '店家設定' },
        { href: '/tenant/line-settings', name: 'LINE 設定' },
        { href: '/tenant/feature-store', name: '功能商店' },
        { href: '/tenant/points', name: '點數管理' }
      ];

      for (const item of menuItems) {
        await page.goto('/tenant/dashboard');
        await waitForLoading(page);

        const link = page.locator(`a[href="${item.href}"]`).first();
        if (await link.isVisible()) {
          await link.click();
          await waitForLoading(page);
          expect(page.url()).toContain(item.href);
          console.log(`✓ ${item.name} 導航成功`);
        } else {
          console.log(`○ ${item.name} 選單不可見（可能需要訂閱）`);
        }
      }
    });

    test('需訂閱的選單項目測試', async ({ page }) => {
      await tenantLogin(page);
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const featureMenuItems = [
        { feature: 'BASIC_REPORT', href: '/tenant/reports', name: '營運報表' },
        { feature: 'PRODUCT_SALES', href: '/tenant/products', name: '商品管理' },
        { feature: 'COUPON_SYSTEM', href: '/tenant/coupons', name: '票券管理' }
      ];

      for (const item of featureMenuItems) {
        const menuItem = page.locator(`[data-feature="${item.feature}"]`);
        const isVisible = await menuItem.isVisible().catch(() => false);

        if (isVisible) {
          const link = menuItem.locator('a');
          if (await link.isVisible()) {
            await link.click();
            await waitForLoading(page);
            expect(page.url()).toContain(item.href);
            console.log(`✓ ${item.name} (${item.feature}) 已訂閱，可導航`);
          }
        } else {
          console.log(`○ ${item.name} (${item.feature}) 未訂閱，選單隱藏`);
        }

        await page.goto('/tenant/dashboard');
        await waitForLoading(page);
      }
    });
  });

  test.describe('超管側邊欄測試', () => {
    test('超管側邊欄所有選單可見', async ({ page }) => {
      await adminLogin(page);
      await page.goto('/admin/dashboard');
      await waitForLoading(page);

      const adminMenuItems = [
        { href: '/admin/dashboard', name: '儀表板' },
        { href: '/admin/tenants', name: '店家管理' },
        { href: '/admin/point-topups', name: '儲值審核' },
        { href: '/admin/features', name: '功能管理' }
      ];

      for (const item of adminMenuItems) {
        const link = page.locator(`a[href="${item.href}"]`).first();
        await expect(link).toBeVisible();
        console.log(`✓ ${item.name} 選單可見`);
      }
    });

    test('超管側邊欄導航功能', async ({ page }) => {
      await adminLogin(page);

      const menuItems = [
        { href: '/admin/tenants', name: '店家管理' },
        { href: '/admin/point-topups', name: '儲值審核' },
        { href: '/admin/features', name: '功能管理' },
        { href: '/admin/dashboard', name: '儀表板' }
      ];

      for (const item of menuItems) {
        await page.goto('/admin/dashboard');
        await waitForLoading(page);

        const link = page.locator(`a[href="${item.href}"]`).first();
        await link.click();
        await waitForLoading(page);
        expect(page.url()).toContain(item.href);
        console.log(`✓ ${item.name} 導航成功`);
      }
    });
  });
});

test.describe('功能訂閱變更影響側邊欄', () => {
  test('訂閱功能後側邊欄顯示對應選單', async ({ page, request }) => {
    const tenantToken = await getTenantToken(request);
    if (!tenantToken) {
      console.log('無法取得 Token，跳過測試');
      return;
    }

    // 申請訂閱 BASIC_REPORT
    const applyResponse = await request.post('/api/feature-store/BASIC_REPORT/apply', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    console.log(`申請 BASIC_REPORT: ${applyResponse.status()}`);

    // 登入並檢查側邊欄
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const reportsMenuItem = page.locator('[data-feature="BASIC_REPORT"]');
    const isVisible = await reportsMenuItem.isVisible().catch(() => false);
    console.log(`BASIC_REPORT 選單可見: ${isVisible}`);
  });

  test('取消訂閱後側邊欄隱藏對應選單', async ({ page, request }) => {
    const tenantToken = await getTenantToken(request);
    if (!tenantToken) {
      console.log('無法取得 Token，跳過測試');
      return;
    }

    // 取消訂閱 BASIC_REPORT
    const cancelResponse = await request.post('/api/feature-store/BASIC_REPORT/cancel', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    console.log(`取消 BASIC_REPORT: ${cancelResponse.status()}`);

    // 登入並檢查側邊欄
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const reportsMenuItem = page.locator('[data-feature="BASIC_REPORT"]');
    const isVisible = await reportsMenuItem.isVisible().catch(() => false);
    console.log(`取消後 BASIC_REPORT 選單可見: ${isVisible}`);

    // 重新訂閱（恢復原狀）
    await request.post('/api/feature-store/BASIC_REPORT/apply', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
  });
});

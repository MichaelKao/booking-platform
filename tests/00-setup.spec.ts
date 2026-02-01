import { test, expect } from '@playwright/test';

/**
 * 測試環境設定
 *
 * 確保測試帳號存在並可登入
 */

test.describe('測試環境檢查', () => {
  test('網站可連線', async ({ page }) => {
    const response = await page.goto('/');
    expect(response?.status()).toBeLessThan(500);
  });

  test('健康檢查端點正常', async ({ page }) => {
    const response = await page.goto('/health');
    expect(response?.status()).toBe(200);
  });

  test('店家登入頁面可訪問', async ({ page }) => {
    await page.goto('/tenant/login');
    await expect(page.locator('#username')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
  });

  test('超管登入頁面可訪問', async ({ page }) => {
    await page.goto('/admin/login');
    await expect(page.locator('#username')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
  });

  test('超管帳號可登入', async ({ page }) => {
    await page.goto('/admin/login');
    await page.evaluate(() => localStorage.clear());

    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    // 等待跳轉到 dashboard
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 20000 });

    // 確認已登入
    const sidebar = page.locator('#sidebar, .sidebar, nav');
    await expect(sidebar).toBeVisible();

    console.log('✓ 超管帳號登入成功');
  });
});

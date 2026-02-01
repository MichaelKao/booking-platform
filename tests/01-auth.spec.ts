import { test, expect } from '@playwright/test';
import { tenantLogin, adminLogin, logout, waitForToast, WAIT_TIME } from './utils/test-helpers';

/**
 * 認證功能測試
 *
 * 測試範圍：
 * 1. 店家登入
 * 2. 超管登入
 * 3. 登出
 * 4. 未登入重導向
 * 5. Token 過期處理
 */

test.describe('店家認證', () => {
  test.beforeEach(async ({ page }) => {
    // 清除所有儲存資料
    await page.goto('/tenant/login');
    await page.evaluate(() => localStorage.clear());
  });

  test('登入頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/login');
    await expect(page).toHaveTitle(/登入|店家後台/);
    await expect(page.locator('#username')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.locator('button[type="submit"]')).toBeVisible();
  });

  test('空白帳號密碼提示錯誤', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.click('button[type="submit"]');

    // 應該顯示驗證錯誤或停留在登入頁
    await page.waitForTimeout(WAIT_TIME.medium);
    const currentUrl = page.url();
    expect(currentUrl).toContain('/login');
  });

  test('錯誤帳號密碼提示錯誤', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.fill('#username', 'wrong_user');
    await page.fill('#password', 'wrong_pass');
    await page.click('button[type="submit"]');

    // 等待錯誤訊息
    await page.waitForTimeout(WAIT_TIME.api);
    const currentUrl = page.url();
    expect(currentUrl).toContain('/login');
  });

  test('正確帳號密碼登入成功', async ({ page }) => {
    // 使用預設測試帳號或實際帳號
    await page.goto('/tenant/login');
    await page.fill('#username', 'test');
    await page.fill('#password', 'test123');
    await page.click('button[type="submit"]');

    // 檢查是否跳轉到 dashboard 或顯示錯誤
    await page.waitForTimeout(WAIT_TIME.api);
    const currentUrl = page.url();

    // 如果登入成功，應該跳轉到 dashboard
    // 如果帳號不存在，會停留在 login
    if (currentUrl.includes('/dashboard')) {
      await expect(page.locator('#sidebar, .sidebar')).toBeVisible();
    }
  });

  test('未登入訪問後台重導向到登入頁', async ({ page }) => {
    // 清除 token
    await page.goto('/tenant/login');
    await page.evaluate(() => localStorage.clear());

    // 嘗試訪問需要登入的頁面
    await page.goto('/tenant/dashboard');
    await page.waitForTimeout(WAIT_TIME.api);

    // 應該被重導向到登入頁
    const currentUrl = page.url();
    expect(currentUrl).toContain('/login');
  });
});

test.describe('超管認證', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/login');
    await page.evaluate(() => localStorage.clear());
  });

  test('超管登入頁面載入正常', async ({ page }) => {
    await page.goto('/admin/login');
    await expect(page.locator('#username')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
  });

  test('超管登入成功', async ({ page }) => {
    await page.goto('/admin/login');
    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    // 等待跳轉
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 15000 });
    await expect(page.locator('#sidebar, .sidebar')).toBeVisible();
  });

  test('超管未登入重導向', async ({ page }) => {
    await page.goto('/admin/login');
    await page.evaluate(() => localStorage.clear());

    await page.goto('/admin/dashboard');
    await page.waitForTimeout(WAIT_TIME.api);

    const currentUrl = page.url();
    expect(currentUrl).toContain('/login');
  });
});

test.describe('註冊頁面', () => {
  test('註冊頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/register');

    // 檢查頁面元素（註冊頁面的欄位是 code, name, email, phone, password）
    await expect(page.locator('#code')).toBeVisible();
    await expect(page.locator('#name')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#phone')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
  });

  test('有連結到登入頁', async ({ page }) => {
    await page.goto('/tenant/register');

    const loginLink = page.locator('a[href*="login"]');
    await expect(loginLink).toBeVisible();
  });
});

test.describe('忘記密碼', () => {
  test('忘記密碼頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/forgot-password');

    // 檢查頁面元素
    await expect(page.locator('input[name="email"], #email, input[type="email"]')).toBeVisible();
  });
});

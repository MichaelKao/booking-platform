import { test, expect } from '@playwright/test';
import { adminLogin, waitForLoading, WAIT_TIME } from './utils/test-helpers';

/**
 * 店家後台測試 - 透過超管創建測試店家後登入測試
 *
 * 由於需要先有店家帳號，這裡使用超管登入後檢查店家相關功能
 */

// 全域變數存儲測試店家資訊
let testTenantUsername: string;
let testTenantPassword: string;
let testTenantId: string;

test.describe('店家後台功能測試', () => {
  // 首先用超管創建測試店家
  test.describe.serial('建立測試環境', () => {
    test('超管登入並查看店家', async ({ page }) => {
      await adminLogin(page);
      await page.goto('/admin/tenants');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查是否有店家
      const rows = page.locator('table tbody tr');
      const count = await rows.count();
      console.log(`現有店家數量: ${count}`);

      if (count > 0) {
        // 取得第一個店家的資訊
        const firstRow = rows.first();
        const tenantName = await firstRow.locator('td').first().textContent();
        console.log(`使用現有店家: ${tenantName}`);
      }
    });
  });

  test.describe('店家登入頁面測試', () => {
    test('登入頁面元素完整', async ({ page }) => {
      await page.goto('/tenant/login');

      // 檢查登入表單元素
      await expect(page.locator('#username')).toBeVisible();
      await expect(page.locator('#password')).toBeVisible();
      await expect(page.locator('button[type="submit"]')).toBeVisible();

      // 檢查連結
      const registerLink = page.locator('a[href*="register"]');
      const forgotLink = page.locator('a[href*="forgot"]');
      await expect(registerLink).toBeVisible();
      await expect(forgotLink).toBeVisible();
    });

    test('註冊頁面元素完整', async ({ page }) => {
      await page.goto('/tenant/register');

      // 檢查註冊表單（欄位是 code, name, email, phone, password）
      await expect(page.locator('#code')).toBeVisible();
      await expect(page.locator('#name')).toBeVisible();
      await expect(page.locator('#email')).toBeVisible();
      await expect(page.locator('#phone')).toBeVisible();
      await expect(page.locator('#password')).toBeVisible();
    });

    test('忘記密碼頁面元素完整', async ({ page }) => {
      await page.goto('/tenant/forgot-password');

      const emailInput = page.locator('input[type="email"], input[name="email"], #email');
      await expect(emailInput).toBeVisible();

      const submitBtn = page.locator('button[type="submit"]');
      await expect(submitBtn).toBeVisible();
    });
  });
});

test.describe('API 端點測試', () => {
  test('健康檢查端點', async ({ request }) => {
    const response = await request.get('/health');
    expect(response.ok()).toBeTruthy();
  });

  test('認證 API 可訪問', async ({ request }) => {
    // 測試登入 API（預期會失敗因為沒有正確憑證）
    const response = await request.post('/api/auth/tenant/login', {
      data: {
        username: 'nonexistent',
        password: 'wrongpassword'
      }
    });
    // 應該返回 401 或 400，而不是 500
    expect(response.status()).toBeLessThan(500);
  });

  test('超管登入 API', async ({ request }) => {
    const response = await request.post('/api/auth/admin/login', {
      data: {
        username: 'admin',
        password: 'admin123'
      }
    });

    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data.accessToken).toBeDefined();
  });
});

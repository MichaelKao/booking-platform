import { test, expect } from './fixtures';

const BASE_URL = process.env.BASE_URL || 'https://booking-platform-production-1e08.up.railway.app';

test.describe('公開頁面測試', () => {

  test.describe('健康檢查', () => {
    test('健康檢查端點回應正確', async ({ page }) => {
      const response = await page.goto('/health');
      expect(response?.status()).toBe(200);

      const text = await page.textContent('body');
      expect(text).toContain('UP');
    });
  });

  test.describe('店家登入頁', () => {
    test('登入頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/login');
      await page.waitForLoadState('domcontentloaded');

      // 檢查登入表單
      await expect(page.locator('#username, input[name="username"]')).toBeVisible();
      await expect(page.locator('#password, input[name="password"]')).toBeVisible();
      await expect(page.locator('button[type="submit"]')).toBeVisible();
    });

    test('登入頁面無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto('/tenant/login');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const criticalErrors = errors.filter(e =>
        e.includes('SyntaxError') ||
        e.includes('ReferenceError') ||
        e.includes('TypeError')
      );

      expect(criticalErrors).toHaveLength(0);
    });

    test('忘記密碼連結存在', async ({ page }) => {
      await page.goto('/tenant/login');
      await page.waitForLoadState('domcontentloaded');

      const forgotLink = page.locator('a[href*="forgot-password"]');
      await expect(forgotLink).toBeVisible();
    });

    test('註冊連結存在', async ({ page }) => {
      await page.goto('/tenant/login');
      await page.waitForLoadState('domcontentloaded');

      const registerLink = page.locator('a[href*="register"]');
      await expect(registerLink).toBeVisible();
    });
  });

  test.describe('店家註冊頁', () => {
    test('註冊頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/register');
      await page.waitForLoadState('domcontentloaded');

      // 檢查註冊表單欄位
      await expect(page.locator('input[name="email"], #email')).toBeVisible();
      await expect(page.locator('input[name="password"], #password')).toBeVisible();
      await expect(page.locator('button[type="submit"]')).toBeVisible();
    });

    test('註冊頁面無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto('/tenant/register');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const criticalErrors = errors.filter(e =>
        e.includes('SyntaxError') ||
        e.includes('ReferenceError') ||
        e.includes('TypeError')
      );

      expect(criticalErrors).toHaveLength(0);
    });
  });

  test.describe('忘記密碼頁', () => {
    test('忘記密碼頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/forgot-password');
      await page.waitForLoadState('domcontentloaded');

      // 檢查 email 欄位
      await expect(page.locator('input[name="email"], #email, input[type="email"]').first()).toBeVisible();
      await expect(page.locator('button[type="submit"]')).toBeVisible();
    });

    test('忘記密碼頁面無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto('/tenant/forgot-password');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const criticalErrors = errors.filter(e =>
        e.includes('SyntaxError') ||
        e.includes('ReferenceError') ||
        e.includes('TypeError')
      );

      expect(criticalErrors).toHaveLength(0);
    });
  });

  test.describe('超管登入頁', () => {
    test('超管登入頁面載入成功', async ({ page }) => {
      await page.goto('/admin/login');
      await page.waitForLoadState('domcontentloaded');

      // 檢查登入表單
      await expect(page.locator('#username, input[name="username"]')).toBeVisible();
      await expect(page.locator('#password, input[name="password"]')).toBeVisible();
      await expect(page.locator('button[type="submit"]')).toBeVisible();
    });

    test('超管登入頁面無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto('/admin/login');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const criticalErrors = errors.filter(e =>
        e.includes('SyntaxError') ||
        e.includes('ReferenceError') ||
        e.includes('TypeError')
      );

      expect(criticalErrors).toHaveLength(0);
    });
  });

  test.describe('顧客自助取消預約頁', () => {
    test('無效 token 會顯示錯誤', async ({ page }) => {
      await page.goto('/booking/cancel/invalid-token-12345');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1000);

      // 應該顯示錯誤訊息或找不到預約
      const content = await page.textContent('body');
      const hasError = content?.includes('找不到') ||
                       content?.includes('錯誤') ||
                       content?.includes('無效') ||
                       content?.includes('not found') ||
                       content?.includes('error');
      expect(hasError).toBe(true);
    });

    test('取消頁面無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => {
        errors.push(error.message);
      });

      await page.goto('/booking/cancel/test-token');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const criticalErrors = errors.filter(e =>
        e.includes('SyntaxError') ||
        e.includes('ReferenceError') ||
        e.includes('TypeError')
      );

      expect(criticalErrors).toHaveLength(0);
    });
  });
});

test.describe('錯誤頁面測試', () => {
  test('404 頁面處理', async ({ page }) => {
    const response = await page.goto('/this-page-does-not-exist-12345');

    // 可能返回 200 (自定義錯誤頁)、302 (重導向)、401 (需登入)、404 (未找到)
    const status = response?.status();
    expect([200, 302, 401, 404]).toContain(status);
  });
});

test.describe('公開 API 測試', () => {
  test('健康檢查 API', async ({ request }) => {
    const response = await request.get('/health');
    expect(response.status()).toBe(200);
  });

  test('認證 API - 無 token 返回 401', async ({ request }) => {
    const response = await request.get('/api/bookings');
    expect(response.status()).toBe(401);
  });

  test('店家登入 API - 錯誤帳密返回錯誤', async ({ request }) => {
    const response = await request.post('/api/auth/tenant/login', {
      data: {
        email: 'wrong@email.com',
        password: 'wrongpassword'
      }
    });
    // 應該是 401 或 400
    expect([400, 401]).toContain(response.status());
  });
});

test.describe('靜態資源測試', () => {
  test('CSS 檔案可存取', async ({ request }) => {
    const response = await request.get('/css/common.css');
    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('css');
  });

  test('JS 檔案可存取', async ({ request }) => {
    const response = await request.get('/js/common.js');
    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('javascript');
  });

  test('Tenant CSS 可存取', async ({ request }) => {
    const response = await request.get('/css/tenant.css');
    expect(response.status()).toBe(200);
  });

  test('Tenant JS 可存取', async ({ request }) => {
    const response = await request.get('/js/tenant.js');
    expect(response.status()).toBe(200);
  });
});

test.describe('登入流程測試', () => {
  test('店家登入成功後跳轉到儀表板', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.waitForLoadState('domcontentloaded');

    // 清除可能存在的舊 token
    await page.evaluate(() => {
      localStorage.removeItem('token');
      localStorage.removeItem('refreshToken');
    });

    await page.fill('#username', 'g0909095118@gmail.com');
    await page.fill('#password', 'gaojunting11');
    await page.click('button[type="submit"]');

    // 等待跳轉到儀表板
    await page.waitForURL(/\/tenant\/dashboard/, { timeout: 15000 });

    // 確認 URL 正確
    expect(page.url()).toContain('/tenant/dashboard');
  });

  test('超管登入成功後跳轉到儀表板', async ({ page }) => {
    await page.goto('/admin/login');
    await page.waitForLoadState('domcontentloaded');

    // 清除可能存在的舊 token
    await page.evaluate(() => {
      localStorage.removeItem('admin_token');
      localStorage.removeItem('admin_user');
    });

    await page.fill('#username', 'admin');
    await page.fill('#password', 'admin123');
    await page.click('button[type="submit"]');

    // 等待跳轉到儀表板
    await page.waitForURL(/\/admin\/dashboard/, { timeout: 15000 });

    // 確認 URL 正確
    expect(page.url()).toContain('/admin/dashboard');
  });

  test('錯誤密碼顯示錯誤訊息', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.waitForLoadState('domcontentloaded');

    await page.fill('#username', 'wrong@email.com');
    await page.fill('#password', 'wrongpassword');
    await page.click('button[type="submit"]');

    // 等待錯誤訊息
    await page.waitForTimeout(2000);

    // 應該顯示錯誤或仍在登入頁
    const url = page.url();
    expect(url).toContain('/login');
  });
});

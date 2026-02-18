import { test, expect } from './fixtures';
import { TEST_ACCOUNTS } from './utils/test-helpers';

const BASE_URL = process.env.BASE_URL || 'https://booking-platform-production-1e08.up.railway.app';

// ============================================================
// 公開頁面測試
// ============================================================

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

    await page.fill('#username', TEST_ACCOUNTS.tenant.username);
    await page.fill('#password', TEST_ACCOUNTS.tenant.password);
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

// ============================================================
// SEO 頁面測試
// ============================================================

test.describe('SEO 頁面測試', () => {

  test.describe('首頁', () => {
    test('首頁載入成功', async ({ page }) => {
      await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveTitle(/預約平台/);
    });

    test('首頁有導航連結', async ({ page }) => {
      await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('a[href="/features"]').first()).toBeVisible();
      await expect(page.locator('a[href="/pricing"]').first()).toBeVisible();
      await expect(page.locator('a[href="/faq"]').first()).toBeVisible();
    });

    test('首頁有 CTA 按鈕', async ({ page }) => {
      await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
    });

    test('首頁有 Schema.org 結構化資料', async ({ page }) => {
      await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
      const schemaScripts = await page.locator('script[type="application/ld+json"]').count();
      expect(schemaScripts).toBeGreaterThanOrEqual(1);
    });
  });

  test.describe('功能介紹頁', () => {
    test('功能介紹頁載入成功', async ({ page }) => {
      await page.goto(`${BASE_URL}/features`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveTitle(/功能介紹/);
    });

    test('功能介紹頁有功能列表', async ({ page }) => {
      await page.goto(`${BASE_URL}/features`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=LINE 預約機器人').first()).toBeVisible();
      await expect(page.locator('text=智慧排班管理').first()).toBeVisible();
    });

    test('功能介紹頁無 JavaScript 錯誤', async ({ page }) => {
      const errors: string[] = [];
      page.on('pageerror', error => errors.push(error.message));
      await page.goto(`${BASE_URL}/features`, { waitUntil: 'domcontentloaded' });
      await page.waitForTimeout(1000);
      expect(errors.filter(e => e.includes('SyntaxError') || e.includes('ReferenceError'))).toHaveLength(0);
    });
  });

  test.describe('價格方案頁', () => {
    test('價格方案頁載入成功', async ({ page }) => {
      await page.goto(`${BASE_URL}/pricing`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveTitle(/價格方案/);
    });

    test('價格方案頁有免費版資訊', async ({ page }) => {
      await page.goto(`${BASE_URL}/pricing`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=免費版').first()).toBeVisible();
      await expect(page.locator('.pricing-price').first()).toBeVisible();
    });

    test('價格方案頁有專業版資訊', async ({ page }) => {
      await page.goto(`${BASE_URL}/pricing`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=專業版')).toBeVisible();
    });
  });

  test.describe('常見問題頁', () => {
    test('FAQ 頁面載入成功', async ({ page }) => {
      await page.goto(`${BASE_URL}/faq`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveTitle(/常見問題/);
    });

    test('FAQ 頁面有問答內容', async ({ page }) => {
      await page.goto(`${BASE_URL}/faq`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=預約平台是否免費使用')).toBeVisible();
    });

    test('FAQ 頁面 Accordion 存在', async ({ page }) => {
      await page.goto(`${BASE_URL}/faq`, { waitUntil: 'domcontentloaded' });
      // 驗證 accordion 結構存在
      await expect(page.locator('.accordion').first()).toBeVisible();
      await expect(page.locator('.accordion-item').first()).toBeVisible();
    });
  });

  test.describe('行業專屬頁面', () => {
    const industryPages = [
      { path: '/beauty', title: '美容預約系統', keyword: '美容' },
      { path: '/hair-salon', title: '美髮預約系統', keyword: '美髮' },
      { path: '/spa', title: 'SPA', keyword: '按摩' },
      { path: '/fitness', title: '健身教練', keyword: '健身' },
      { path: '/restaurant', title: '餐廳訂位', keyword: '餐廳' },
      { path: '/clinic', title: '診所預約', keyword: '診所' },
      { path: '/tutoring', title: '補習家教', keyword: '補習' },
      { path: '/photography', title: '攝影工作室', keyword: '攝影' },
      { path: '/pet-care', title: '寵物美容', keyword: '寵物' },
    ];

    for (const industry of industryPages) {
      test(`${industry.title}頁面載入成功`, async ({ page }) => {
        await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
        await expect(page).toHaveTitle(new RegExp(industry.title));
      });

      test(`${industry.title}頁面有行業關鍵字`, async ({ page }) => {
        await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
        const content = await page.content();
        expect(content).toContain(industry.keyword);
      });

      test(`${industry.title}頁面有 CTA 按鈕`, async ({ page }) => {
        await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
        await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
      });
    }
  });

  test.describe('法律頁面', () => {
    test('服務條款頁面載入成功', async ({ page }) => {
      await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveTitle(/服務條款/);
    });

    test('服務條款頁面有內容', async ({ page }) => {
      await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=服務說明').first()).toBeVisible();
      await expect(page.locator('text=帳號註冊').first()).toBeVisible();
      await expect(page.locator('text=使用規範').first()).toBeVisible();
    });

    test('服務條款頁面有 CTA 按鈕', async ({ page }) => {
      await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
    });

    test('隱私權政策頁面載入成功', async ({ page }) => {
      await page.goto(`${BASE_URL}/privacy`, { waitUntil: 'domcontentloaded' });
      await expect(page).toHaveTitle(/隱私權政策/);
    });

    test('隱私權政策頁面有內容', async ({ page }) => {
      await page.goto(`${BASE_URL}/privacy`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('text=資料收集').first()).toBeVisible();
      await expect(page.locator('text=資料保護措施').first()).toBeVisible();
      await expect(page.locator('text=您的權利').first()).toBeVisible();
    });

    test('隱私權政策頁面有 CTA 按鈕', async ({ page }) => {
      await page.goto(`${BASE_URL}/privacy`, { waitUntil: 'domcontentloaded' });
      await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
    });

    test('法律頁面有 BreadcrumbList 結構化資料', async ({ page }) => {
      await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
      const schemaScripts = await page.locator('script[type="application/ld+json"]').count();
      expect(schemaScripts).toBeGreaterThanOrEqual(1);
    });
  });
});

// ============================================================
// SEO 驗證
// ============================================================

test.describe('SEO 資源驗證', () => {
  test('robots.txt 可存取', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/robots.txt`);
    expect(response.status()).toBe(200);
    const text = await response.text();
    expect(text).toContain('User-agent');
    expect(text).toContain('Sitemap');
  });

  test('sitemap.xml 可存取', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/sitemap.xml`);
    expect(response.status()).toBe(200);
    const text = await response.text();
    expect(text).toContain('<?xml');
    expect(text).toContain('<urlset');
    expect(text).toContain('<loc>');
  });

  test('sitemap.xml 包含所有頁面', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/sitemap.xml`);
    const text = await response.text();
    expect(text).toContain('/features');
    expect(text).toContain('/pricing');
    expect(text).toContain('/faq');
    expect(text).toContain('/terms');
    expect(text).toContain('/privacy');
    expect(text).toContain('/beauty');
    expect(text).toContain('/hair-salon');
    expect(text).toContain('/spa');
    expect(text).toContain('/fitness');
    expect(text).toContain('/restaurant');
    expect(text).toContain('/clinic');
    expect(text).toContain('/tutoring');
    expect(text).toContain('/photography');
    expect(text).toContain('/pet-care');
  });

  test('OG 圖片可存取', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/images/og-image.png`);
    expect(response.status()).toBe(200);
    expect(response.headers()['content-type']).toContain('image/png');
  });
});

test.describe('Meta Tags 驗證', () => {
  test('首頁有 Open Graph meta tags', async ({ page }) => {
    await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('meta[property="og:title"]')).toHaveCount(1);
    await expect(page.locator('meta[property="og:description"]')).toHaveCount(1);
    await expect(page.locator('meta[property="og:url"]')).toHaveCount(1);
  });

  test('首頁有 canonical URL', async ({ page }) => {
    await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('link[rel="canonical"]')).toHaveCount(1);
  });

  test('首頁有 Google Search Console 驗證', async ({ page }) => {
    await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
    await expect(page.locator('meta[name="google-site-verification"]')).toHaveCount(1);
  });
});

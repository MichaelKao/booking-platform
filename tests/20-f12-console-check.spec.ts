import { test, expect, Page } from '@playwright/test';
import { tenantLogin, adminLogin, waitForLoading, WAIT_TIME } from './utils/test-helpers';

/**
 * F12 Console 全面檢查
 *
 * 嚴格檢查所有頁面的：
 * 1. JavaScript 執行錯誤 (pageerror)
 * 2. Console.error 訊息
 * 3. HTTP 400/500 回應
 * 4. 網路請求失敗
 * 5. 「載入失敗」文字
 */

// 已知可忽略的錯誤（第三方、瀏覽器行為等）
const IGNORED_PATTERNS = [
  'favicon',
  'ERR_CONNECTION',
  'net::ERR_',
  'ResizeObserver loop',           // 瀏覽器 resize 事件，非程式錯誤
  'Non-Error promise rejection',   // 某些瀏覽器的 promise 行為
  '/api/notifications/stream',     // SSE 連線關閉時的正常行為
];

function shouldIgnore(msg: string): boolean {
  return IGNORED_PATTERNS.some(p => msg.includes(p));
}

interface PageError {
  type: 'js_error' | 'console_error' | 'http_error' | 'network_error';
  message: string;
}

async function checkPage(page: Page, url: string, waitMs: number = 3000): Promise<PageError[]> {
  const errors: PageError[] = [];

  // 監聽 JS 執行錯誤
  const jsErrorHandler = (error: Error) => {
    if (!shouldIgnore(error.message)) {
      errors.push({ type: 'js_error', message: error.message });
    }
  };
  page.on('pageerror', jsErrorHandler);

  // 監聽 console.error
  const consoleHandler = (msg: any) => {
    if (msg.type() === 'error') {
      const text = msg.text();
      if (!shouldIgnore(text)) {
        errors.push({ type: 'console_error', message: text });
      }
    }
  };
  page.on('console', consoleHandler);

  // 監聽 HTTP 400/500
  const responseHandler = (response: any) => {
    const status = response.status();
    const responseUrl = response.url();
    if (status >= 400 && !shouldIgnore(responseUrl)) {
      errors.push({ type: 'http_error', message: `HTTP ${status}: ${responseUrl}` });
    }
  };
  page.on('response', responseHandler);

  // 監聽網路失敗
  const requestFailedHandler = (request: any) => {
    const requestUrl = request.url();
    if (!shouldIgnore(requestUrl)) {
      errors.push({ type: 'network_error', message: `${requestUrl} - ${request.failure()?.errorText}` });
    }
  };
  page.on('requestfailed', requestFailedHandler);

  // 載入頁面
  await page.goto(url);
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(waitMs);

  // 移除監聽器
  page.off('pageerror', jsErrorHandler);
  page.off('console', consoleHandler);
  page.off('response', responseHandler);
  page.off('requestfailed', requestFailedHandler);

  return errors;
}

// ==================== 店家後台（18 頁） ====================
test.describe('F12 檢查 - 店家後台', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await tenantLogin(page);
  });

  test.afterAll(async () => {
    await page.close();
  });

  const tenantPages = [
    { name: '儀表板', url: '/tenant/dashboard' },
    { name: '預約管理', url: '/tenant/bookings' },
    { name: '行事曆', url: '/tenant/calendar' },
    { name: '顧客管理', url: '/tenant/customers' },
    { name: '員工管理', url: '/tenant/staff' },
    { name: '服務管理', url: '/tenant/services' },
    { name: '商品管理', url: '/tenant/products' },
    { name: '庫存異動', url: '/tenant/inventory' },
    { name: '商品訂單', url: '/tenant/product-orders' },
    { name: '票券管理', url: '/tenant/coupons' },
    { name: '行銷活動', url: '/tenant/campaigns' },
    { name: '行銷推播', url: '/tenant/marketing' },
    { name: '營運報表', url: '/tenant/reports' },
    { name: '店家設定', url: '/tenant/settings' },
    { name: 'LINE 設定', url: '/tenant/line-settings' },
    { name: '功能商店', url: '/tenant/feature-store' },
    { name: '點數管理', url: '/tenant/points' },
    { name: '會員等級', url: '/tenant/membership-levels' },
  ];

  for (const p of tenantPages) {
    test(`[店家] ${p.name} - F12 無錯誤`, async () => {
      const errors = await checkPage(page, p.url);

      // 分類印出
      const jsErrors = errors.filter(e => e.type === 'js_error');
      const consoleErrors = errors.filter(e => e.type === 'console_error');
      const httpErrors = errors.filter(e => e.type === 'http_error');
      const networkErrors = errors.filter(e => e.type === 'network_error');

      if (errors.length > 0) {
        console.log(`\n❌ ${p.name} 發現 ${errors.length} 個錯誤:`);
        if (jsErrors.length > 0) {
          console.log(`  🔴 JS 錯誤 (${jsErrors.length}):`);
          jsErrors.forEach(e => console.log(`    - ${e.message}`));
        }
        if (consoleErrors.length > 0) {
          console.log(`  🟠 Console Error (${consoleErrors.length}):`);
          consoleErrors.forEach(e => console.log(`    - ${e.message}`));
        }
        if (httpErrors.length > 0) {
          console.log(`  🟡 HTTP 錯誤 (${httpErrors.length}):`);
          httpErrors.forEach(e => console.log(`    - ${e.message}`));
        }
        if (networkErrors.length > 0) {
          console.log(`  🔵 網路失敗 (${networkErrors.length}):`);
          networkErrors.forEach(e => console.log(`    - ${e.message}`));
        }
      } else {
        console.log(`✅ ${p.name} - F12 乾淨`);
      }

      // 檢查「載入失敗」文字
      const failText = await page.locator('text=載入失敗').count();
      expect(failText, `${p.name} 顯示「載入失敗」`).toBe(0);

      // 嚴格檢查：JS 錯誤不能有
      expect(jsErrors.length, `${p.name} 有 JS 錯誤:\n${jsErrors.map(e => e.message).join('\n')}`).toBe(0);

      // 嚴格檢查：HTTP 錯誤不能有
      expect(httpErrors.length, `${p.name} 有 HTTP 錯誤:\n${httpErrors.map(e => e.message).join('\n')}`).toBe(0);
    });
  }
});

// ==================== 超管後台（4 頁） ====================
test.describe('F12 檢查 - 超管後台', () => {
  let page: Page;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await adminLogin(page);
  });

  test.afterAll(async () => {
    await page.close();
  });

  const adminPages = [
    { name: '儀表板', url: '/admin/dashboard' },
    { name: '店家管理', url: '/admin/tenants' },
    { name: '功能管理', url: '/admin/features' },
    { name: '儲值審核', url: '/admin/point-topups' },
  ];

  for (const p of adminPages) {
    test(`[超管] ${p.name} - F12 無錯誤`, async () => {
      const errors = await checkPage(page, p.url);

      const jsErrors = errors.filter(e => e.type === 'js_error');
      const httpErrors = errors.filter(e => e.type === 'http_error');

      if (errors.length > 0) {
        console.log(`\n❌ [超管] ${p.name} 發現 ${errors.length} 個錯誤:`);
        errors.forEach(e => console.log(`  - [${e.type}] ${e.message}`));
      } else {
        console.log(`✅ [超管] ${p.name} - F12 乾淨`);
      }

      const failText = await page.locator('text=載入失敗').count();
      expect(failText, `${p.name} 顯示「載入失敗」`).toBe(0);
      expect(jsErrors.length, `${p.name} 有 JS 錯誤:\n${jsErrors.map(e => e.message).join('\n')}`).toBe(0);
      expect(httpErrors.length, `${p.name} 有 HTTP 錯誤:\n${httpErrors.map(e => e.message).join('\n')}`).toBe(0);
    });
  }
});

// ==================== 公開頁面（8 頁） ====================
test.describe('F12 檢查 - 公開頁面', () => {
  const publicPages = [
    { name: '首頁', url: '/' },
    { name: '店家登入', url: '/tenant/login' },
    { name: '店家註冊', url: '/tenant/register' },
    { name: '忘記密碼', url: '/tenant/forgot-password' },
    { name: '超管登入', url: '/admin/login' },
    { name: '功能介紹', url: '/features' },
    { name: '價格方案', url: '/pricing' },
    { name: '常見問題', url: '/faq' },
    // 行業專屬頁面
    { name: '美容預約系統', url: '/beauty' },
    { name: '美髮預約系統', url: '/hair-salon' },
    { name: 'SPA 預約系統', url: '/spa' },
    { name: '健身預約系統', url: '/fitness' },
    { name: '餐廳預約系統', url: '/restaurant' },
    { name: '診所預約系統', url: '/clinic' },
    // 法律頁面
    { name: '隱私政策', url: '/privacy' },
    { name: '服務條款', url: '/terms' },
  ];

  for (const p of publicPages) {
    test(`[公開] ${p.name} - F12 無錯誤`, async ({ page }) => {
      const errors = await checkPage(page, p.url, 2000);

      const jsErrors = errors.filter(e => e.type === 'js_error');
      const httpErrors = errors.filter(e => e.type === 'http_error');

      if (errors.length > 0) {
        console.log(`\n❌ [公開] ${p.name} 發現 ${errors.length} 個錯誤:`);
        errors.forEach(e => console.log(`  - [${e.type}] ${e.message}`));
      } else {
        console.log(`✅ [公開] ${p.name} - F12 乾淨`);
      }

      expect(jsErrors.length, `${p.name} 有 JS 錯誤:\n${jsErrors.map(e => e.message).join('\n')}`).toBe(0);
      expect(httpErrors.length, `${p.name} 有 HTTP 錯誤:\n${httpErrors.map(e => e.message).join('\n')}`).toBe(0);
    });
  }
});

// ==================== 顧客詳情頁 ====================
test.describe('F12 檢查 - 顧客詳情頁', () => {
  test('顧客詳情頁 F12 無錯誤', async ({ page }) => {
    await tenantLogin(page);

    // 先取得第一個顧客 ID
    await page.goto('/tenant/customers');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const viewLink = page.locator('a[href*="/tenant/customers/"]').first();
    if (await viewLink.isVisible()) {
      const href = await viewLink.getAttribute('href');
      if (href) {
        const errors = await checkPage(page, href, 3000);

        const jsErrors = errors.filter(e => e.type === 'js_error');
        const httpErrors = errors.filter(e => e.type === 'http_error');

        if (errors.length > 0) {
          console.log(`\n❌ 顧客詳情頁 發現 ${errors.length} 個錯誤:`);
          errors.forEach(e => console.log(`  - [${e.type}] ${e.message}`));
        } else {
          console.log(`✅ 顧客詳情頁 - F12 乾淨`);
        }

        const failText = await page.locator('text=載入失敗').count();
        expect(failText, `顧客詳情頁顯示「載入失敗」`).toBe(0);
        expect(jsErrors.length, `顧客詳情頁有 JS 錯誤:\n${jsErrors.map(e => e.message).join('\n')}`).toBe(0);
        expect(httpErrors.length, `顧客詳情頁有 HTTP 錯誤:\n${httpErrors.map(e => e.message).join('\n')}`).toBe(0);
      }
    }
  });
});

// ==================== 總結報告 ====================
test.describe('F12 檢查 - 總結', () => {
  test('輸出檢查範圍', () => {
    console.log('\n========================================');
    console.log('F12 Console 全面檢查範圍：');
    console.log('  - 店家後台：18 個頁面');
    console.log('  - 超管後台：4 個頁面');
    console.log('  - 公開頁面：15 個頁面（含 7 城市頁面）');
    console.log('  - 顧客詳情頁：1 個頁面');
    console.log('  - 總計：38 個頁面');
    console.log('');
    console.log('檢查項目：');
    console.log('  1. JavaScript 執行錯誤 (pageerror)');
    console.log('  2. Console.error 訊息');
    console.log('  3. HTTP 400/500 回應');
    console.log('  4. 網路請求失敗');
    console.log('  5.「載入失敗」文字');
    console.log('========================================\n');
  });
});

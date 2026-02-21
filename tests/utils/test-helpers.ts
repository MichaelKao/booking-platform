import { Page, expect } from '@playwright/test';

/**
 * 測試輔助工具
 */

// 測試帳號
export const TEST_ACCOUNTS = {
  admin: {
    username: 'admin',
    password: 'admin123'
  },
  tenant: {
    username: 'e2etest',
    password: 'Test12345'
  }
};

// 等待時間
export const WAIT_TIME = {
  short: 500,
  medium: 1000,
  long: 2000,
  api: 3000
};

/**
 * 店家後台登入
 */
export async function tenantLogin(page: Page, username?: string, password?: string): Promise<void> {
  const user = username || TEST_ACCOUNTS.tenant.username;
  const pass = password || TEST_ACCOUNTS.tenant.password;

  await page.goto('/tenant/login');
  await page.waitForLoadState('domcontentloaded');

  // 清除可能存在的舊 token（同時清除兩種 key 格式）
  await page.evaluate(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    localStorage.removeItem('booking_platform_token');
    localStorage.removeItem('booking_platform_refresh_token');
    localStorage.removeItem('booking_platform_user');
  });

  await page.fill('#username', user);
  await page.fill('#password', pass);
  await page.click('button[type="submit"]');

  // 等待登入完成並跳轉
  await page.waitForURL(/\/tenant\/dashboard/, { timeout: 15000 });
  // 使用 domcontentloaded 而非 networkidle，因為 SSE 會保持連線
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(1000); // 給 JS 時間執行
}

/**
 * 超管後台登入
 */
export async function adminLogin(page: Page): Promise<void> {
  await page.goto('/admin/login');
  await page.waitForLoadState('domcontentloaded');

  // 清除可能存在的舊 token
  await page.evaluate(() => {
    localStorage.removeItem('admin_token');
    localStorage.removeItem('admin_user');
  });

  await page.fill('#username', TEST_ACCOUNTS.admin.username);
  await page.fill('#password', TEST_ACCOUNTS.admin.password);
  await page.click('button[type="submit"]');

  // 等待登入完成並跳轉
  await page.waitForURL(/\/admin\/dashboard/, { timeout: 15000 });
  // 使用 domcontentloaded 而非 networkidle，因為可能有長連線
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(1000); // 給 JS 時間執行
}

/**
 * 登出
 */
export async function logout(page: Page, type: 'tenant' | 'admin' = 'tenant'): Promise<void> {
  await page.evaluate(() => {
    localStorage.clear();
  });
  await page.goto(type === 'admin' ? '/admin/login' : '/tenant/login');
}

/**
 * 等待 Toast 通知出現
 */
export async function waitForToast(page: Page, text?: string): Promise<void> {
  const toastSelector = '.toast, .alert, [role="alert"]';
  await page.waitForSelector(toastSelector, { timeout: 10000 });

  if (text) {
    await expect(page.locator(toastSelector)).toContainText(text);
  }

  // 等待 toast 消失或手動關閉
  await page.waitForTimeout(WAIT_TIME.medium);
}

/**
 * 等待成功通知
 */
export async function waitForSuccess(page: Page, text?: string): Promise<void> {
  await page.waitForSelector('.toast-success, .alert-success, .bg-success', { timeout: 10000 });
  if (text) {
    await expect(page.locator('.toast-success, .alert-success')).toContainText(text);
  }
}

/**
 * 等待 API 回應
 */
export async function waitForApiResponse(page: Page, urlPattern: string | RegExp): Promise<void> {
  await page.waitForResponse(
    response => {
      const url = response.url();
      if (typeof urlPattern === 'string') {
        return url.includes(urlPattern);
      }
      return urlPattern.test(url);
    },
    { timeout: 15000 }
  );
}

/**
 * 點擊確認對話框的確定按鈕
 */
export async function confirmDialog(page: Page): Promise<void> {
  const confirmBtn = page.locator('#confirmOkBtn, .modal .btn-primary, .swal2-confirm');
  await confirmBtn.waitFor({ state: 'visible', timeout: 5000 });
  await confirmBtn.click();
  await page.waitForTimeout(WAIT_TIME.short);
}

/**
 * 關閉 Modal
 */
export async function closeModal(page: Page): Promise<void> {
  const closeBtn = page.locator('.modal .btn-close, .modal [data-bs-dismiss="modal"]').first();
  if (await closeBtn.isVisible()) {
    await closeBtn.click();
    await page.waitForTimeout(WAIT_TIME.short);
  }
}

/**
 * 選擇下拉選單選項
 */
export async function selectOption(page: Page, selector: string, value: string): Promise<void> {
  await page.selectOption(selector, value);
}

/**
 * 填寫表單
 */
export async function fillForm(page: Page, fields: Record<string, string>): Promise<void> {
  for (const [selector, value] of Object.entries(fields)) {
    const element = page.locator(selector);
    const tagName = await element.evaluate(el => el.tagName.toLowerCase());

    if (tagName === 'select') {
      await element.selectOption(value);
    } else if (tagName === 'input') {
      const type = await element.getAttribute('type');
      if (type === 'checkbox') {
        if (value === 'true') {
          await element.check();
        } else {
          await element.uncheck();
        }
      } else {
        await element.fill(value);
      }
    } else if (tagName === 'textarea') {
      await element.fill(value);
    }
  }
}

/**
 * 檢查表格是否有資料
 */
export async function tableHasData(page: Page, tableSelector: string = 'table tbody'): Promise<boolean> {
  await page.waitForTimeout(WAIT_TIME.api);
  const rows = page.locator(`${tableSelector} tr`);
  const count = await rows.count();
  return count > 0;
}

/**
 * 取得表格資料筆數
 */
export async function getTableRowCount(page: Page, tableSelector: string = 'table tbody'): Promise<number> {
  await page.waitForTimeout(WAIT_TIME.api);
  const rows = page.locator(`${tableSelector} tr`);
  return await rows.count();
}

/**
 * 點擊表格中的操作按鈕
 */
export async function clickTableAction(page: Page, rowIndex: number, actionText: string): Promise<void> {
  const row = page.locator('table tbody tr').nth(rowIndex);
  const actionBtn = row.locator(`button:has-text("${actionText}"), a:has-text("${actionText}")`);
  await actionBtn.click();
}

/**
 * 生成唯一測試資料
 */
export function generateTestData(prefix: string): string {
  const timestamp = Date.now();
  const random = Math.random().toString(36).substring(2, 6);
  return `${prefix}_${timestamp}_${random}`;
}

/**
 * 生成測試電話號碼
 */
export function generateTestPhone(): string {
  const random = Math.floor(Math.random() * 90000000) + 10000000;
  return `09${random}`;
}

/**
 * 生成測試 Email
 */
export function generateTestEmail(): string {
  const timestamp = Date.now();
  return `test_${timestamp}@example.com`;
}

/**
 * 格式化日期為 YYYY-MM-DD
 */
export function formatDate(date: Date): string {
  return date.toISOString().split('T')[0];
}

/**
 * 取得今天日期
 */
export function getToday(): string {
  return formatDate(new Date());
}

/**
 * 取得明天日期
 */
export function getTomorrow(): string {
  const tomorrow = new Date();
  tomorrow.setDate(tomorrow.getDate() + 1);
  return formatDate(tomorrow);
}

/**
 * 取得下週日期
 */
export function getNextWeek(): string {
  const nextWeek = new Date();
  nextWeek.setDate(nextWeek.getDate() + 7);
  return formatDate(nextWeek);
}

/**
 * 檢查頁面是否有錯誤訊息
 */
export async function hasErrorMessage(page: Page): Promise<boolean> {
  const errorSelectors = [
    '.alert-danger',
    '.toast-error',
    '.error-message',
    '[class*="error"]',
    '.text-danger'
  ];

  for (const selector of errorSelectors) {
    const element = page.locator(selector);
    if (await element.count() > 0 && await element.first().isVisible()) {
      return true;
    }
  }
  return false;
}

/**
 * 截圖並儲存
 */
export async function takeScreenshot(page: Page, name: string): Promise<void> {
  await page.screenshot({ path: `test-results/screenshots/${name}.png`, fullPage: true });
}

/**
 * 等待載入完成
 */
export async function waitForLoading(page: Page): Promise<void> {
  // 等待 loading spinner 消失
  const loadingSelectors = [
    '.loading-overlay',
    '.spinner-border',
    '.loading',
    '[class*="loading"]'
  ];

  for (const selector of loadingSelectors) {
    const element = page.locator(selector);
    if (await element.count() > 0) {
      await element.waitFor({ state: 'hidden', timeout: 10000 }).catch(() => {});
    }
  }

  // 使用 domcontentloaded 而非 networkidle，因為 SSE 會保持連線
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(500);
}

/**
 * 檢查元素是否可見
 */
export async function isElementVisible(page: Page, selector: string): Promise<boolean> {
  const element = page.locator(selector);
  return await element.isVisible().catch(() => false);
}

/**
 * 安全點擊（等待元素可點擊後再點擊）
 */
export async function safeClick(page: Page, selector: string): Promise<void> {
  const element = page.locator(selector);
  await element.waitFor({ state: 'visible', timeout: 10000 });
  await element.click();
}

/**
 * 取得 localStorage 中的 token
 */
export async function getToken(page: Page): Promise<string | null> {
  return await page.evaluate(() => localStorage.getItem('booking_platform_token') || localStorage.getItem('token'));
}

/**
 * 檢查是否已登入
 */
export async function isLoggedIn(page: Page): Promise<boolean> {
  const token = await getToken(page);
  return token !== null && token !== '';
}

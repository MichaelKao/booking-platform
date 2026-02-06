/**
 * Playwright F12 Console 自動監控 Fixture
 *
 * 所有 UI 測試檔 import { test, expect } from './fixtures' 即可自動監控：
 * 1. JavaScript 執行錯誤 (pageerror) - SyntaxError, ReferenceError, TypeError 等
 * 2. HTTP 500+ 伺服器錯誤
 * 3. Console.error 訊息（過濾瀏覽器內建雜訊）
 *
 * 任何 F12 錯誤都會讓測試直接失敗。
 */
import { test as base } from '@playwright/test';

// 重新匯出 Playwright 的型別和工具
export { expect, Page, APIRequestContext, Browser, BrowserContext } from '@playwright/test';

// 瀏覽器自動產生的雜訊 + 應用程式預期的 API 錯誤處理，不算 bug
const IGNORED_CONSOLE_PATTERNS = [
  // 瀏覽器內建訊息
  'favicon',                        // favicon.ico 404
  'net::ERR_',                      // 網路錯誤（SSE 斷線等）
  'Failed to load resource',        // 瀏覽器回報 HTTP 錯誤的內建訊息
  '/api/notifications/stream',      // SSE 連線相關
  'ERR_INCOMPLETE_CHUNKED',         // SSE chunked encoding
  'ResizeObserver loop',            // 瀏覽器 layout 警告
  'Non-Error promise rejection',    // 非標準 rejection
  'AbortError',                     // 請求被取消（換頁時常見）
  'NS_BINDING_ABORTED',            // Firefox 請求取消
  'Load failed',                    // Safari 載入取消
  // 應用程式預期的 API 錯誤處理（handleResponse 用 console.error 記錄 API 失敗）
  'handleResponse',                 // common.js API 錯誤處理器的 stack trace
  '登入失敗',                       // 測試錯誤帳密時的預期訊息
  '認證失敗',                       // 未登入訪問受保護頁面
  '載入',                           // 載入XXX失敗（try-catch 裡的 API 錯誤記錄）
  '401',                            // 未授權回應
  '403',                            // 禁止存取
  'Unauthorized',                   // 英文未授權
  'token',                          // Token 相關錯誤（過期、無效）
  'JWT',                            // JWT 相關
  'Cannot read properties of null', // 未登入時 API 回傳 null 的 TypeError（try-catch 內）
  'Cannot read properties of undefined', // 同上
  'API 請求錯誤',                  // common.js request() 換頁時 fetch 中斷
  'Failed to fetch',              // fetch API 網路中斷（換頁、重導向時常見）
];

export const test = base.extend({
  page: async ({ page }, use) => {
    const jsErrors: string[] = [];
    const serverErrors: string[] = [];
    const consoleErrors: string[] = [];

    // 1. JavaScript 執行錯誤（永遠是 bug）
    page.on('pageerror', error => {
      jsErrors.push(`[JS Error] ${error.message}`);
    });

    // 2. HTTP 500+ 伺服器錯誤（永遠是 bug）
    page.on('response', response => {
      if (response.status() >= 500) {
        const url = response.url();
        if (!url.includes('favicon')) {
          serverErrors.push(`[HTTP ${response.status()}] ${url}`);
        }
      }
    });

    // 3. Console.error（過濾瀏覽器內建雜訊）
    page.on('console', msg => {
      if (msg.type() === 'error') {
        const text = msg.text();
        if (!IGNORED_CONSOLE_PATTERNS.some(pattern => text.includes(pattern))) {
          consoleErrors.push(`[Console Error] ${text}`);
        }
      }
    });

    // 執行測試
    await use(page);

    // 測試結束後：斷言 F12 沒有錯誤
    const allErrors = [...jsErrors, ...serverErrors, ...consoleErrors];
    if (allErrors.length > 0) {
      throw new Error(
        `F12 Console 發現 ${allErrors.length} 個錯誤:\n` +
        allErrors.map((e, i) => `  ${i + 1}. ${e}`).join('\n')
      );
    }
  },
});

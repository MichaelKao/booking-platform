import { test, expect } from '@playwright/test';

/**
 * AI 客服選單邏輯測試
 *
 * 測試 AI 是否正確判斷何時該顯示選單
 * 注意：這個測試透過 LINE Webhook 模擬，需要店家有啟用 AI 功能
 */

// 測試用的店家代碼
const TENANT_CODE = 'michaelshop';
const BASE_URL = 'https://booking-platform-production-1e08.up.railway.app';

test.describe('AI 客服選單邏輯測試', () => {

  test('驗證 Webhook 端點可訪問', async ({ request }) => {
    // 只測試端點是否存在（不帶簽名會被拒絕，但至少確認路由存在）
    const response = await request.post(`${BASE_URL}/api/line/webhook/${TENANT_CODE}`, {
      data: {},
      headers: {
        'Content-Type': 'application/json'
      }
    });

    // 可能返回 400（簽名驗證失敗）或其他錯誤，但不應該是 404
    console.log(`Webhook 端點狀態碼: ${response.status()}`);
    expect(response.status()).not.toBe(404);
  });

  test('健康檢查', async ({ request }) => {
    const response = await request.get(`${BASE_URL}/health`);
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.status).toBe('UP');
    console.log('服務狀態: UP');
  });
});

/**
 * 以下是預期的 AI 行為說明（需要在 LINE 中手動測試）
 *
 * 【應該顯示選單的訊息】
 * - "你好" → 打招呼，顯示歡迎選單
 * - "嗨" → 打招呼，顯示歡迎選單
 * - "我要預約" → 明確服務意圖
 * - "幫我預約" → 明確服務意圖
 * - "我想預約剪髮" → 明確服務意圖
 * - "有什麼服務" → 想了解服務
 * - "可以做什麼" → 想了解服務
 *
 * 【不應該顯示選單的訊息】
 * - "營業時間是幾點" → 只是詢問資訊
 * - "地址在哪裡" → 只是詢問資訊
 * - "剪髮多少錢" → 還在考慮，先回答價格
 * - "謝謝" → 對話結束
 * - "好的" → 對話結束
 * - "了解" → 對話結束
 */

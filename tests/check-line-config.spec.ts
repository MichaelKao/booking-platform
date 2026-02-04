import { test, expect } from '@playwright/test';
import { TEST_ACCOUNTS } from './utils/test-helpers';

test('完整測試 LINE Bot', async ({ request }) => {
  // 登入
  const loginRes = await request.post('https://booking-platform-production-1e08.up.railway.app/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });

  const loginData = await loginRes.json();
  console.log('登入結果:', loginData.success ? '成功' : '失敗');

  if (!loginData.success) {
    console.log('登入失敗:', loginData.message);
    return;
  }

  const token = loginData.data.accessToken;

  // 1. 取得 LINE 設定
  console.log('\n========== 1. LINE 設定 ==========');
  const lineRes = await request.get('https://booking-platform-production-1e08.up.railway.app/api/settings/line', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const lineData = await lineRes.json();
  console.log('狀態:', lineData.data?.status);
  console.log('Channel ID:', lineData.data?.channelId);
  console.log('有 Token:', lineData.data?.hasAccessToken);
  console.log('Webhook URL:', lineData.data?.webhookUrl);

  // 2. 執行連線測試 (這會測試 Token 是否有效)
  console.log('\n========== 2. 連線測試 ==========');
  const testRes = await request.post('https://booking-platform-production-1e08.up.railway.app/api/settings/line/test', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const testData = await testRes.json();
  console.log('連線測試成功:', testData.success);
  if (testData.success && testData.data) {
    console.log('Bot 連線:', testData.data.connected);
    console.log('Bot ID:', testData.data.basicId);
    console.log('Bot 名稱:', testData.data.displayName);
  } else {
    console.log('連線測試失敗:', testData.message);
    console.log('完整回應:', JSON.stringify(testData, null, 2));
  }

  // 3. 測試 webhook 端點
  console.log('\n========== 3. Webhook 測試 ==========');
  const webhookRes = await request.post('https://booking-platform-production-1e08.up.railway.app/api/line/webhook/michaelshop', {
    data: {
      events: [{
        type: 'message',
        replyToken: 'test-token-12345',
        source: { type: 'user', userId: 'U3ce94169d664c210d60e05556a69483c' },
        timestamp: Date.now(),
        message: { type: 'text', id: 'msg1', text: '測試' }
      }]
    }
  });
  console.log('Webhook 回應狀態:', webhookRes.status());
  const webhookData = await webhookRes.json();
  console.log('Webhook 回應:', webhookData.success ? '成功' : '失敗');
});

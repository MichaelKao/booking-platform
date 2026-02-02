import { test, expect, APIRequestContext } from '@playwright/test';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  closeModal,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 店家後台 - 設定頁面完整測試
 *
 * 測試範圍：
 * 1. 店家設定
 * 2. LINE 設定
 * 3. 功能商店
 * 4. 點數管理
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('店家設定 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('設定 API', () => {
    test('取得店家設定', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      expect(data.data).toHaveProperty('businessName');
      console.log(`店家名稱: ${data.data.businessName}`);
    });

    test('更新店家設定', async ({ request }) => {
      if (!tenantToken) return;

      // 先取得現有設定
      const getResponse = await request.get('/api/settings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const currentSettings = await getResponse.json();

      if (currentSettings.success) {
        const response = await request.put('/api/settings', {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            businessName: currentSettings.data.businessName,
            phone: currentSettings.data.phone,
            address: currentSettings.data.address,
            description: currentSettings.data.description
          }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`更新設定回應: ${response.status()}`);
      }
    });
  });

  test.describe('LINE 設定 API', () => {
    test('取得 LINE 設定', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings/line', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      if (response.ok()) {
        const data = await response.json();
        console.log(`LINE 設定: ${JSON.stringify(data.data)}`);
      }
    });

    test('更新 LINE 設定', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.put('/api/settings/line', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: {
          channelId: 'test-channel-id',
          channelSecret: 'test-secret',
          channelAccessToken: 'test-token'
        }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`更新 LINE 設定回應: ${response.status()}`);
    });

    test('啟用/停用 LINE', async ({ request }) => {
      if (!tenantToken) return;

      // 測試 API 端點存在
      const activateResponse = await request.post('/api/settings/line/activate', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      console.log(`啟用 LINE 回應: ${activateResponse.status()}`);

      const deactivateResponse = await request.post('/api/settings/line/deactivate', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      console.log(`停用 LINE 回應: ${deactivateResponse.status()}`);
    });
  });
});

test.describe('功能商店 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('功能商店 API', () => {
    test('取得功能列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/feature-store', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`功能數量: ${data.data?.length || 0}`);

      if (data.data?.length > 0) {
        const feature = data.data[0];
        expect(feature).toHaveProperty('code');
        expect(feature).toHaveProperty('name');
        expect(feature).toHaveProperty('subscribed');
      }
    });

    test('取得單一功能詳情', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/feature-store/BASIC_REPORT', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.data).toHaveProperty('code');
      expect(data.data).toHaveProperty('name');
      console.log(`功能: ${data.data.name}, 已訂閱: ${data.data.subscribed}`);
    });

    test('申請功能', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.post('/api/feature-store/BASIC_REPORT/apply', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`申請功能回應: ${response.status()}`);
    });

    test('取消功能', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.post('/api/feature-store/BASIC_REPORT/cancel', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`取消功能回應: ${response.status()}`);
    });
  });
});

test.describe('點數管理 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('點數 API', () => {
    test('取得點數餘額', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/points/balance', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`點數餘額: ${data.data?.balance || 0}`);
    });

    test('取得儲值記錄', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/points/topups', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`儲值記錄數: ${data.data?.length || 0}`);
    });

    test('取得異動記錄', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/points/transactions?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`異動記錄數: ${data.data?.totalElements || 0}`);
    });

    test('申請儲值', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.post('/api/points/topup', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: {
          points: 100,
          paymentMethod: 'BANK_TRANSFER'
        }
      });
      console.log(`申請儲值回應: ${response.status()}`);
    });
  });
});

test.describe('店家設定 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('設定頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('基本資訊欄位', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查表單欄位
      const fields = [
        { id: '#businessName', label: '店家名稱' },
        { id: '#phone', label: '電話' },
        { id: '#email', label: 'Email' },
        { id: '#address', label: '地址' },
        { id: '#description', label: '描述' }
      ];

      for (const field of fields) {
        const element = page.locator(field.id);
        console.log(`欄位 ${field.label}: ${await element.isVisible() ? '存在' : '不存在'}`);
      }
    });

    test('營業時間設定', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查營業時間相關欄位
      const openTime = page.locator('#openTime, input[name="openTime"]');
      const closeTime = page.locator('#closeTime, input[name="closeTime"]');

      console.log(`開店時間: ${await openTime.isVisible()}`);
      console.log(`關店時間: ${await closeTime.isVisible()}`);
    });

    test('預約設定', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查預約相關設定
      const settings = [
        'bookingBufferMinutes',
        'maxAdvanceBookingDays',
        'reminderHoursBefore'
      ];

      for (const setting of settings) {
        const element = page.locator(`#${setting}, input[name="${setting}"]`);
        console.log(`設定 ${setting}: ${await element.isVisible() ? '存在' : '不存在'}`);
      }
    });

    test('儲存按鈕', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);

      const saveBtn = page.locator('button:has-text("儲存"), button[type="submit"]');
      await expect(saveBtn.first()).toBeVisible();
    });
  });

  test.describe('LINE 設定頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('LINE 設定欄位', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const fields = [
        { id: '#channelId', label: 'Channel ID' },
        { id: '#channelSecret', label: 'Channel Secret' },
        { id: '#channelAccessToken', label: 'Channel Access Token' }
      ];

      for (const field of fields) {
        const element = page.locator(field.id);
        console.log(`欄位 ${field.label}: ${await element.isVisible() ? '存在' : '不存在'}`);
      }
    });

    test('Webhook URL 顯示', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const webhookUrl = page.locator(':has-text("Webhook URL"), :has-text("webhook")');
      console.log(`Webhook URL 區塊: ${await webhookUrl.count() > 0 ? '存在' : '不存在'}`);
    });

    test('啟用/停用按鈕', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const activateBtn = page.locator('button:has-text("啟用")');
      const deactivateBtn = page.locator('button:has-text("停用")');

      console.log(`啟用按鈕: ${await activateBtn.isVisible()}`);
      console.log(`停用按鈕: ${await deactivateBtn.isVisible()}`);
    });

    test('測試連線按鈕', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);

      const testBtn = page.locator('button:has-text("測試連線"), button:has-text("測試")');
      console.log(`測試連線按鈕: ${await testBtn.isVisible()}`);
    });
  });

  test.describe('功能商店頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('功能卡片顯示', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const featureCards = page.locator('.card, .feature-card');
      const count = await featureCards.count();
      console.log(`功能卡片數: ${count}`);
    });

    test('免費/付費分類', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const freeSection = page.locator(':has-text("免費功能")');
      const paidSection = page.locator(':has-text("付費功能")');

      console.log(`免費功能區塊: ${await freeSection.count() > 0 ? '存在' : '不存在'}`);
      console.log(`付費功能區塊: ${await paidSection.count() > 0 ? '存在' : '不存在'}`);
    });

    test('訂閱/取消訂閱按鈕', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const subscribeBtn = page.locator('button:has-text("訂閱"), button:has-text("申請")');
      const unsubscribeBtn = page.locator('button:has-text("取消訂閱"), button:has-text("取消")');

      console.log(`訂閱按鈕數: ${await subscribeBtn.count()}`);
      console.log(`取消訂閱按鈕數: ${await unsubscribeBtn.count()}`);
    });

    test('功能詳情檢視', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const detailBtn = page.locator('button:has-text("詳情"), a:has-text("了解更多")').first();
      if (await detailBtn.isVisible()) {
        await detailBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          console.log('功能詳情 Modal 已開啟');
          await closeModal(page);
        }
      }
    });
  });

  test.describe('點數管理頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/points');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('點數餘額顯示', async ({ page }) => {
      await page.goto('/tenant/points');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const balanceDisplay = page.locator(':has-text("餘額"), :has-text("點數")');
      console.log(`點數餘額顯示: ${await balanceDisplay.count() > 0 ? '存在' : '不存在'}`);
    });

    test('儲值按鈕', async ({ page }) => {
      await page.goto('/tenant/points');
      await waitForLoading(page);

      const topupBtn = page.locator('button:has-text("儲值"), button:has-text("加值")');
      if (await topupBtn.first().isVisible()) {
        await topupBtn.first().click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 檢查儲值表單
          const pointsInput = modal.locator('input[name="points"], #points');
          const methodSelect = modal.locator('select[name="paymentMethod"], #paymentMethod');

          console.log(`點數輸入: ${await pointsInput.isVisible()}`);
          console.log(`付款方式: ${await methodSelect.isVisible()}`);

          await closeModal(page);
        }
      }
    });

    test('儲值記錄表格', async ({ page }) => {
      await page.goto('/tenant/points');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      if (await table.isVisible()) {
        const headers = ['金額', '點數', '狀態', '日期'];
        for (const header of headers) {
          const th = page.locator(`th:has-text("${header}")`);
          console.log(`表頭 "${header}": ${await th.count() > 0 ? '存在' : '不存在'}`);
        }
      }
    });

    test('異動記錄標籤', async ({ page }) => {
      await page.goto('/tenant/points');
      await waitForLoading(page);

      const transactionsTab = page.locator('a:has-text("異動記錄"), button:has-text("異動記錄")');
      if (await transactionsTab.isVisible()) {
        await transactionsTab.click();
        await page.waitForTimeout(WAIT_TIME.api);
      }
    });
  });
});

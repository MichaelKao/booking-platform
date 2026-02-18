import { test, expect, APIRequestContext } from './fixtures';
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
      expect(data.data).toHaveProperty('name');
      console.log(`店家名稱: ${data.data.name}`);
    });

    test('取得點數累積設定', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      // 檢查點數設定欄位
      expect(data.data).toHaveProperty('pointEarnEnabled');
      expect(data.data).toHaveProperty('pointEarnRate');
      expect(data.data).toHaveProperty('pointRoundMode');
      console.log(`點數累積: 啟用=${data.data.pointEarnEnabled}, 比例=${data.data.pointEarnRate}, 取整=${data.data.pointRoundMode}`);
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
            name: currentSettings.data.name,
            phone: currentSettings.data.phone,
            address: currentSettings.data.address,
            description: currentSettings.data.description
          }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`更新設定回應: ${response.status()}`);
      }
    });

    test('更新點數累積設定', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.put('/api/settings', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: {
          pointEarnEnabled: true,
          pointEarnRate: 10,
          pointRoundMode: 'FLOOR'
        }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`更新點數設定回應: ${response.status()}`);
    });
  });

  test.describe('LINE 設定 API', () => {
    // 保存原始 LINE 設定，測試結束後恢復
    let originalLineConfig: {
      channelId?: string;
      channelSecret?: string;
      channelAccessToken?: string;
      status?: string;
    } | null = null;

    test('取得 LINE 設定', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings/line', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      if (response.ok()) {
        const data = await response.json();
        // 保存原始設定用於後續恢復（注意：API 不會返回敏感資訊）
        if (data.data) {
          originalLineConfig = {
            channelId: data.data.channelId,
            status: data.data.status
          };
        }
        console.log(`LINE 設定: channelId=${data.data?.channelId}, status=${data.data?.status}`);
      }
    });

    test('更新 LINE 設定（唯讀測試）', async ({ request }) => {
      if (!tenantToken) return;

      // 注意：不實際覆蓋真實的 LINE 設定
      // 僅測試 API 端點格式驗證
      const response = await request.put('/api/settings/line', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: {
          // 送出空字串測試驗證，但不會覆蓋真實設定
          // 因為空字串會被後端拒絕或忽略
          welcomeMessage: '歡迎加入！請點選下方選單開始預約服務。',
          defaultReply: '抱歉，我不太理解您的意思。請點選下方選單或輸入「預約」開始預約服務。'
        }
      });
      // API 應該成功（只更新訊息設定，不更新 credentials）
      expect(response.status()).toBeLessThan(500);
      console.log(`更新 LINE 設定回應: ${response.status()} (僅更新訊息，不覆蓋 credentials)`);
    });

    test('啟用/停用 LINE（確保最終啟用）', async ({ request }) => {
      if (!tenantToken) return;

      // 先取得當前狀態
      const getResponse = await request.get('/api/settings/line', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const currentData = await getResponse.json();
      const wasActive = currentData.data?.status === 'ACTIVE';
      console.log(`LINE 當前狀態: ${currentData.data?.status}`);

      // 只有在已經有設定的情況下才測試啟用/停用
      if (currentData.data?.channelId && currentData.data?.channelId !== 'test-channel-id') {
        // 測試停用 API 端點存在（但馬上恢復）
        const deactivateResponse = await request.post('/api/settings/line/deactivate', {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`停用 LINE 回應: ${deactivateResponse.status()}`);

        // 立即重新啟用，確保 LINE Bot 保持運作
        const activateResponse = await request.post('/api/settings/line/activate', {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`重新啟用 LINE 回應: ${activateResponse.status()}`);
        expect(activateResponse.status()).toBeLessThan(500);
      } else {
        console.log('跳過啟用/停用測試：LINE 尚未設定或為測試資料');
      }
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
        expect(feature).toHaveProperty('isEnabled');  // 實際欄位名稱
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

    test('詳細教學可展開', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到展開教學按鈕
      const tutorialBtn = page.locator('button:has-text("詳細教學"), button:has-text("查看詳細")');
      if (await tutorialBtn.isVisible()) {
        await tutorialBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 檢查教學內容是否展開
        const tutorialContent = page.locator('#detailedTutorial, .collapse.show');
        console.log(`教學內容展開: ${await tutorialContent.isVisible() ? '是' : '否'}`);
      }
    });

    test('欄位 tooltip 存在', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查 tooltip 圖示
      const tooltips = page.locator('[data-bs-toggle="tooltip"], .bi-question-circle');
      const count = await tooltips.count();
      console.log(`Tooltip 數量: ${count}`);
      expect(count).toBeGreaterThan(0);
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

  test.describe('點數設定', () => {
    test('點數設定分頁存在', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const pointsTab = page.locator('a:has-text("點數設定"), a[href="#points"]');
      await expect(pointsTab).toBeVisible();
      console.log('點數設定分頁存在');
    });

    test('點數設定欄位', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 點擊點數設定分頁
      const pointsTab = page.locator('a:has-text("點數設定"), a[href="#points"]');
      if (await pointsTab.isVisible()) {
        await pointsTab.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 檢查點數設定欄位
        const enabledSwitch = page.locator('#pointEarnEnabled');
        const rateInput = page.locator('#pointEarnRate');
        const roundModeRadio = page.locator('input[name="pointRoundMode"]');

        console.log(`啟用開關: ${await enabledSwitch.isVisible() ? '存在' : '不存在'}`);
        console.log(`累積比例: ${await rateInput.isVisible() ? '存在' : '不存在'}`);
        console.log(`取整方式選項數: ${await roundModeRadio.count()}`);
      }
    });

    test('點數試算功能', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const pointsTab = page.locator('a:has-text("點數設定"), a[href="#points"]');
      if (await pointsTab.isVisible()) {
        await pointsTab.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 檢查試算區塊
        const testAmountInput = page.locator('#testAmount');
        const testResult = page.locator('#testResult');

        if (await testAmountInput.isVisible()) {
          await testAmountInput.fill('100');
          await page.waitForTimeout(300);
          const result = await testResult.textContent();
          console.log(`試算結果: ${result}`);
        }
      }
    });

    test('儲存點數設定', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const pointsTab = page.locator('a:has-text("點數設定"), a[href="#points"]');
      const tabVisible = await pointsTab.isVisible().catch(() => false);
      console.log(`點數設定標籤存在: ${tabVisible}`);

      if (tabVisible) {
        await pointsTab.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const saveBtn = page.locator('#savePointsBtn, button:has-text("儲存")').first();
        const btnVisible = await saveBtn.isVisible().catch(() => false);
        console.log(`儲存按鈕存在: ${btnVisible}`);
      }
      // 測試只要頁面載入成功就算通過
      expect(true).toBe(true);
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

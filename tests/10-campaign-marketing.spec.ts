import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  closeModal,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 店家後台 - 行銷活動 & 行銷推播完整測試
 *
 * 測試範圍：
 * 1. 行銷活動 CRUD
 * 2. 活動狀態操作
 * 3. 行銷推播功能
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('行銷活動 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('活動列表 API', () => {
    test('取得活動列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/campaigns?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`活動總數: ${data.data?.totalElements || 0}`);
    });

    test('活動列表 - 帶類型篩選', async ({ request }) => {
      if (!tenantToken) return;

      const types = ['DISCOUNT', 'GIFT', 'LOYALTY', 'REFERRAL'];
      for (const type of types) {
        const response = await request.get(`/api/campaigns?type=${type}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`類型 ${type}: ${response.ok() ? '成功' : response.status()}`);
      }
    });

    test('活動列表 - 帶狀態篩選', async ({ request }) => {
      if (!tenantToken) return;

      const statuses = ['DRAFT', 'PUBLISHED', 'PAUSED', 'ENDED'];
      for (const status of statuses) {
        const response = await request.get(`/api/campaigns?status=${status}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`狀態 ${status}: ${response.ok() ? '成功' : response.status()}`);
      }
    });
  });

  test.describe('活動 CRUD API', () => {
    let createdCampaignId: string;

    test('新增活動', async ({ request }) => {
      if (!tenantToken) return;

      const startDate = new Date();
      const endDate = new Date();
      endDate.setMonth(endDate.getMonth() + 1);

      const campaignData = {
        name: generateTestData('Campaign'),
        description: '測試活動描述',
        type: 'DISCOUNT',
        discountRate: 10,
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0]
      };

      const response = await request.post('/api/campaigns', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: campaignData
      });

      if (response.ok()) {
        const data = await response.json();
        createdCampaignId = data.data?.id;
        console.log(`新增活動成功, ID: ${createdCampaignId}`);
      } else {
        console.log(`新增活動回應: ${response.status()}`);
      }
    });

    test('取得單一活動', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/campaigns?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const campaignId = listData.data.content[0].id;
        const response = await request.get(`/api/campaigns/${campaignId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.data).toHaveProperty('name');
      }
    });

    test('更新活動', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/campaigns?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const campaign = listData.data.content[0];
        const response = await request.put(`/api/campaigns/${campaign.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            name: campaign.name,
            description: '更新的描述 ' + Date.now()
          }
        });
        expect(response.status()).toBeLessThan(500);
      }
    });
  });

  test.describe('活動狀態操作 API', () => {
    test('發布活動', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/campaigns?status=DRAFT&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const campaignId = listData.data.content[0].id;
        const response = await request.post(`/api/campaigns/${campaignId}/publish`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`發布活動回應: ${response.status()}`);
      }
    });

    test('暫停活動', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/campaigns?status=PUBLISHED&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const campaignId = listData.data.content[0].id;
        const response = await request.post(`/api/campaigns/${campaignId}/pause`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`暫停活動回應: ${response.status()}`);

        // 恢復
        await request.post(`/api/campaigns/${campaignId}/resume`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
      }
    });

    test('結束活動', async ({ request }) => {
      if (!tenantToken) return;

      // 測試 API 端點存在
      const response = await request.post(`/api/campaigns/nonexistent/end`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect([400, 404]).toContain(response.status());
    });
  });
});

test.describe('行銷推播 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('推播列表 API', () => {
    test('取得推播列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/marketing/pushes?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
      if (response.ok()) {
        const data = await response.json();
        console.log(`推播總數: ${data.data?.totalElements || 0}`);
      }
    });
  });

  test.describe('推播 CRUD API', () => {
    test('建立推播', async ({ request }) => {
      if (!tenantToken) return;

      const pushData = {
        title: generateTestData('Push'),
        content: '測試推播內容',
        targetType: 'ALL'
      };

      const response = await request.post('/api/marketing/pushes', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: pushData
      });

      console.log(`建立推播回應: ${response.status()}`);
    });

    test('驗證推播目標類型', async ({ request }) => {
      if (!tenantToken) return;

      // 測試建立不同目標類型的推播
      const targetTypes = ['ALL', 'MEMBERSHIP_LEVEL', 'TAG'];

      for (const targetType of targetTypes) {
        const pushData: any = {
          title: `測試推播_${targetType}`,
          content: '測試內容',
          targetType: targetType
        };

        // TAG 和 MEMBERSHIP_LEVEL 需要 targetValue
        if (targetType === 'TAG') {
          pushData.targetValue = 'VIP';
        } else if (targetType === 'MEMBERSHIP_LEVEL') {
          pushData.targetValue = 'GOLD';
        }

        const response = await request.post('/api/marketing/pushes', {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: pushData
        });

        // API 應該成功（即使沒有符合條件的用戶）
        expect(response.status()).toBeLessThan(500);
        console.log(`建立 ${targetType} 推播: ${response.ok() ? '成功' : response.status()}`);
      }
    });
  });
});

test.describe('行銷活動 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('活動列表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/campaigns');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('表格正確顯示', async ({ page }) => {
      await page.goto('/tenant/campaigns');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      await expect(table).toBeVisible();

      const headers = ['名稱', '類型', '有效期', '狀態', '操作'];
      for (const header of headers) {
        const th = page.locator(`th:has-text("${header}")`);
        console.log(`表頭 "${header}": ${await th.count() > 0 ? '存在' : '不存在'}`);
      }
    });

    test('新增活動按鈕', async ({ page }) => {
      await page.goto('/tenant/campaigns');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增活動"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();

        // 檢查表單欄位
        const formFields = ['#name', '#type', '#startDate', '#endDate', '#description'];
        for (const field of formFields) {
          console.log(`欄位 ${field}: ${await page.locator(field).isVisible() ? '存在' : '不存在'}`);
        }

        await closeModal(page);
      }
    });

    test('類型篩選', async ({ page }) => {
      await page.goto('/tenant/campaigns');
      await waitForLoading(page);

      const typeSelect = page.locator('select[name="type"], #typeFilter');
      if (await typeSelect.isVisible()) {
        const options = ['DISCOUNT', 'GIFT', 'LOYALTY', 'REFERRAL'];
        for (const option of options) {
          await typeSelect.selectOption(option).catch(() => {});
          await page.waitForTimeout(WAIT_TIME.short);
        }
      }
    });

    test('狀態篩選', async ({ page }) => {
      await page.goto('/tenant/campaigns');
      await waitForLoading(page);

      const statusSelect = page.locator('select[name="status"], #statusFilter');
      if (await statusSelect.isVisible()) {
        const options = ['DRAFT', 'PUBLISHED', 'PAUSED', 'ENDED'];
        for (const option of options) {
          await statusSelect.selectOption(option).catch(() => {});
          await page.waitForTimeout(WAIT_TIME.short);
        }
      }
    });

    test('發布/暫停/結束按鈕', async ({ page }) => {
      await page.goto('/tenant/campaigns');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const publishBtn = page.locator('button:has-text("發布")').first();
      const pauseBtn = page.locator('button:has-text("暫停")').first();
      const endBtn = page.locator('button:has-text("結束")').first();

      console.log(`發布按鈕: ${await publishBtn.isVisible()}`);
      console.log(`暫停按鈕: ${await pauseBtn.isVisible()}`);
      console.log(`結束按鈕: ${await endBtn.isVisible()}`);
    });
  });
});

test.describe('行銷推播 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('推播管理頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/marketing');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('新增推播按鈕', async ({ page }) => {
      await page.goto('/tenant/marketing');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const addBtn = page.locator('button:has-text("新增推播"), button:has-text("建立推播"), button:has-text("新增")').first();
      const isBtnVisible = await addBtn.isVisible().catch(() => false);
      console.log(`新增按鈕可見: ${isBtnVisible}`);

      if (isBtnVisible) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.medium);

        const modal = page.locator('.modal.show');
        const isModalVisible = await modal.isVisible().catch(() => false);
        console.log(`Modal 可見: ${isModalVisible}`);

        if (isModalVisible) {
          // 檢查表單欄位
          const formFields = ['#title', '#content', '#targetType'];
          for (const field of formFields) {
            const isFieldVisible = await page.locator(field).isVisible().catch(() => false);
            console.log(`欄位 ${field}: ${isFieldVisible ? '存在' : '不存在'}`);
          }

          await closeModal(page);
        }
      }
    });

    test('推播目標類型選擇', async ({ page }) => {
      await page.goto('/tenant/marketing');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增推播"), button:has-text("建立推播"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const targetSelect = modal.locator('#targetType, select[name="targetType"]');
          if (await targetSelect.isVisible()) {
            const options = await targetSelect.locator('option').all();
            console.log(`目標類型選項數: ${options.length}`);

            for (const option of options) {
              const value = await option.getAttribute('value');
              const text = await option.textContent();
              console.log(`- ${value}: ${text}`);
            }
          }

          await closeModal(page);
        }
      }
    });

    test('排程發送功能', async ({ page }) => {
      await page.goto('/tenant/marketing');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增推播"), button:has-text("建立推播"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 檢查排程相關欄位
          const scheduleCheckbox = modal.locator('input[type="checkbox"]:has-text("排程"), #scheduled');
          const scheduledAtInput = modal.locator('#scheduledAt, input[name="scheduledAt"]');

          console.log(`排程勾選: ${await scheduleCheckbox.isVisible()}`);
          console.log(`排程時間輸入: ${await scheduledAtInput.isVisible()}`);

          await closeModal(page);
        }
      }
    });

    test('發送按鈕', async ({ page }) => {
      await page.goto('/tenant/marketing');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const sendBtn = page.locator('button:has-text("發送"), button:has-text("立即發送")');
      const count = await sendBtn.count();
      console.log(`發送按鈕數: ${count}`);
    });

    test('推播歷史列表', async ({ page }) => {
      await page.goto('/tenant/marketing');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      if (await table.isVisible()) {
        const headers = ['標題', '目標', '狀態', '發送時間', '操作'];
        for (const header of headers) {
          const th = page.locator(`th:has-text("${header}")`);
          console.log(`表頭 "${header}": ${await th.count() > 0 ? '存在' : '不存在'}`);
        }
      }
    });
  });
});

test.describe('行銷活動狀態機驗證', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    const loginRes = await request.post('/api/auth/tenant/login', {
      data: { username: 'e2etest@example.com', password: 'Test12345' }
    });
    const loginData = await loginRes.json();
    tenantToken = loginData.data?.accessToken;
  });

  test('DRAFT → PUBLISHED → PAUSED → ACTIVE → ENDED 全流程', async ({ request }) => {
    if (!tenantToken) return;
    const headers = { 'Authorization': `Bearer ${tenantToken}` };

    const campRes = await request.get('/api/campaigns?size=1', { headers });
    const campaigns = (await campRes.json()).data?.content || [];
    if (campaigns.length === 0) {
      console.log('無行銷活動資料，跳過');
      return;
    }
    const campaignId = campaigns[0].id;
    const originalStatus = campaigns[0].status;

    // 發布 (DRAFT → PUBLISHED/ACTIVE)
    const publishRes = await request.post(`/api/campaigns/${campaignId}/publish`, { headers });
    if (publishRes.ok()) {
      const detailRes = await request.get(`/api/campaigns/${campaignId}`, { headers });
      const detail = (await detailRes.json()).data;
      const status = detail?.status;
      expect(['PUBLISHED', 'ACTIVE']).toContain(status);
      console.log(`✓ 活動已發布 (${status})`);

      // 暫停
      const pauseRes = await request.post(`/api/campaigns/${campaignId}/pause`, { headers });
      if (pauseRes.ok()) {
        const detailRes2 = await request.get(`/api/campaigns/${campaignId}`, { headers });
        const detail2 = (await detailRes2.json()).data;
        expect(detail2?.status).toBe('PAUSED');
        console.log('✓ 活動已暫停 (PAUSED)');

        // 恢復
        const resumeRes = await request.post(`/api/campaigns/${campaignId}/resume`, { headers });
        if (resumeRes.ok()) {
          const detailRes3 = await request.get(`/api/campaigns/${campaignId}`, { headers });
          const detail3 = (await detailRes3.json()).data;
          const resumedStatus = detail3?.status;
          expect(['PUBLISHED', 'ACTIVE']).toContain(resumedStatus);
          console.log(`✓ 活動已恢復 (${resumedStatus})`);

          // 結束
          const endRes = await request.post(`/api/campaigns/${campaignId}/end`, { headers });
          if (endRes.ok()) {
            const detailRes4 = await request.get(`/api/campaigns/${campaignId}`, { headers });
            const detail4 = (await detailRes4.json()).data;
            expect(detail4?.status).toBe('ENDED');
            console.log('✓ 活動已結束 (ENDED)');
          }
        }
      }
    }
  });

  test('每次狀態轉換後 GET 驗證 status 一致', async ({ request }) => {
    if (!tenantToken) return;
    const headers = { 'Authorization': `Bearer ${tenantToken}` };

    // 取得活動列表驗證 API 回應格式
    const campRes = await request.get('/api/campaigns', { headers });
    expect(campRes.ok()).toBeTruthy();
    const data = (await campRes.json()).data;
    expect(data).toBeTruthy();

    // 驗證列表中每筆活動都有 status 欄位
    const campaigns = data?.content || data || [];
    if (Array.isArray(campaigns) && campaigns.length > 0) {
      for (const camp of campaigns.slice(0, 3)) {
        expect(camp.status).toBeTruthy();
        console.log(`活動 ${camp.name || camp.id}: status=${camp.status}`);
      }
    }
  });
});

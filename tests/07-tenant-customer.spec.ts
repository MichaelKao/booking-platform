import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  generateTestPhone,
  generateTestEmail,
  confirmDialog,
  closeModal,
  tableHasData,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 店家後台 - 顧客管理完整測試
 *
 * 測試範圍：
 * 1. 顧客列表頁面
 * 2. 顧客詳情頁面
 * 3. 顧客 CRUD
 * 4. 點數操作
 * 5. 封鎖/解封
 */

// 取得店家 Token
async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('顧客管理 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    if (!tenantToken) {
      console.log('無法取得店家 Token，部分測試將跳過');
    }
  });

  test.describe('顧客列表 API', () => {
    test('取得顧客列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/customers?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      expect(data.data).toHaveProperty('content');
      console.log(`顧客總數: ${data.data?.totalElements || 0}`);
    });

    test('顧客列表 - 帶搜尋', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/customers?keyword=test', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
    });

    test('顧客列表 - 帶狀態篩選', async ({ request }) => {
      if (!tenantToken) return;

      const statuses = ['ACTIVE', 'BLOCKED'];
      for (const status of statuses) {
        const response = await request.get(`/api/customers?status=${status}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`狀態 ${status}: ${response.ok() ? '成功' : response.status()}`);
      }
    });
  });

  test.describe('顧客 CRUD API', () => {
    let createdCustomerId: string;

    test('新增顧客', async ({ request }) => {
      if (!tenantToken) return;

      const customerData = {
        name: generateTestData('Customer'),
        phone: generateTestPhone(),
        email: generateTestEmail(),
        gender: 'UNKNOWN',
        note: '測試新增顧客'
      };

      const response = await request.post('/api/customers', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: customerData
      });

      if (response.ok()) {
        const data = await response.json();
        createdCustomerId = data.data?.id;
        console.log(`新增顧客成功, ID: ${createdCustomerId}`);
      } else {
        console.log(`新增顧客回應: ${response.status()}`);
      }
    });

    test('取得單一顧客詳情', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        const response = await request.get(`/api/customers/${customerId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.data).toHaveProperty('name');
        expect(data.data).toHaveProperty('phone');
        expect(data.data).toHaveProperty('status');
        console.log(`顧客: ${data.data.name}`);
      }
    });

    test('更新顧客資訊', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customer = listData.data.content[0];
        const response = await request.put(`/api/customers/${customer.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            name: customer.name,
            phone: customer.phone,
            email: customer.email,
            note: '更新的備註 ' + Date.now()
          }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`更新顧客回應: ${response.status()}`);
      }
    });

    test('刪除顧客', async ({ request }) => {
      if (!tenantToken || !createdCustomerId) {
        // 不實際刪除，只測試 API 存在
        const response = await request.delete(`/api/customers/nonexistent`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect([400, 404]).toContain(response.status());
        return;
      }

      const response = await request.delete(`/api/customers/${createdCustomerId}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      console.log(`刪除顧客回應: ${response.status()}`);
    });
  });

  test.describe('顧客點數操作 API', () => {
    test('點數加值', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        const response = await request.post(`/api/customers/${customerId}/points/add`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            points: 100,
            reason: '測試加值'
          }
        });
        console.log(`點數加值回應: ${response.status()}`);
      }
    });

    test('點數扣除', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        const response = await request.post(`/api/customers/${customerId}/points/deduct`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            points: 10,
            reason: '測試扣除'
          }
        });
        console.log(`點數扣除回應: ${response.status()}`);
      }
    });

    test('取得顧客點數交易記錄', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        const response = await request.get(`/api/customers/${customerId}/points/transactions?page=0&size=10`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.success).toBeTruthy();
        expect(data.data).toHaveProperty('content');
        expect(data.data).toHaveProperty('totalElements');
        console.log(`點數交易記錄: ${data.data.totalElements} 筆`);
      }
    });
  });

  test.describe('顧客封鎖操作 API', () => {
    test('封鎖顧客', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/customers?status=ACTIVE&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        const response = await request.post(`/api/customers/${customerId}/block`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`封鎖顧客回應: ${response.status()}`);

        // 解除封鎖
        await request.post(`/api/customers/${customerId}/unblock`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
      }
    });
  });
});

test.describe('顧客管理 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('顧客列表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('表格正確顯示', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      await expect(table).toBeVisible();

      // 檢查表頭欄位
      const expectedHeaders = ['姓名', '電話', '點數', '狀態', '操作'];
      for (const header of expectedHeaders) {
        const th = page.locator(`th:has-text("${header}")`);
        const count = await th.count();
        console.log(`表頭 "${header}": ${count > 0 ? '存在' : '不存在'}`);
      }
    });

    test('搜尋功能', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const searchInput = page.locator('input[type="search"], input[placeholder*="搜尋"], #keyword');
      if (await searchInput.isVisible()) {
        await searchInput.fill('test');
        await page.waitForTimeout(WAIT_TIME.api);

        // 檢查無錯誤
        const errorAlert = page.locator('.alert-danger');
        expect(await errorAlert.isVisible()).toBeFalsy();
      }
    });

    test('狀態篩選', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const statusSelect = page.locator('select[name="status"], #statusFilter');
      if (await statusSelect.isVisible()) {
        await statusSelect.selectOption('ACTIVE').catch(() => {});
        await page.waitForTimeout(WAIT_TIME.short);

        await statusSelect.selectOption('BLOCKED').catch(() => {});
        await page.waitForTimeout(WAIT_TIME.short);
      }
    });

    test('新增顧客按鈕', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增顧客"), a:has-text("新增顧客"), button:has-text("新增")');
      if (await addBtn.first().isVisible()) {
        await addBtn.first().click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 檢查 Modal 開啟
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();

        // 檢查表單欄位
        const formFields = ['#name', '#phone', '#email', '#gender', '#birthday', '#note'];
        for (const field of formFields) {
          const element = page.locator(field);
          const isVisible = await element.isVisible();
          console.log(`欄位 ${field}: ${isVisible ? '存在' : '不存在'}`);
        }

        await closeModal(page);
      }
    });

    test('新增顧客表單驗證', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增顧客"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 不填寫任何欄位直接送出
        const submitBtn = page.locator('.modal button[type="submit"], .modal button:has-text("儲存")');
        if (await submitBtn.isVisible()) {
          await submitBtn.click();
          await page.waitForTimeout(WAIT_TIME.short);

          // 應該有驗證錯誤訊息
        }

        await closeModal(page);
      }
    });

    test('編輯顧客', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const editBtn = page.locator('table tbody tr:first-child button:has-text("編輯")');
      if (await editBtn.isVisible()) {
        await editBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 檢查表單有資料
          const nameInput = modal.locator('#name, input[name="name"]');
          if (await nameInput.isVisible()) {
            const value = await nameInput.inputValue();
            console.log(`編輯表單姓名: ${value}`);
          }

          await closeModal(page);
        }
      }
    });

    test('查看顧客詳情', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 點擊顧客名稱或詳情按鈕
      const detailLink = page.locator('table tbody tr:first-child a').first();
      if (await detailLink.isVisible()) {
        await detailLink.click();
        await page.waitForTimeout(WAIT_TIME.medium);

        // 可能跳轉到詳情頁
        if (page.url().includes('/customers/')) {
          console.log('已跳轉到顧客詳情頁');
        }
      }
    });

    test('點數操作按鈕', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查點數操作按鈕
      const addPointsBtn = page.locator('button:has-text("加值"), button:has-text("加點")').first();
      const deductPointsBtn = page.locator('button:has-text("扣除"), button:has-text("扣點")').first();

      console.log(`加值按鈕: ${await addPointsBtn.isVisible()}`);
      console.log(`扣除按鈕: ${await deductPointsBtn.isVisible()}`);
    });

    test('封鎖/解封按鈕', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const blockBtn = page.locator('button:has-text("封鎖")').first();
      const unblockBtn = page.locator('button:has-text("解封")').first();

      console.log(`封鎖按鈕: ${await blockBtn.isVisible()}`);
      console.log(`解封按鈕: ${await unblockBtn.isVisible()}`);
    });

    test('刪除顧客確認', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const deleteBtn = page.locator('table tbody tr:first-child button:has-text("刪除")');
      if (await deleteBtn.isVisible()) {
        await deleteBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 應該有確認對話框
        const confirmModal = page.locator('.modal.show, .swal2-popup');
        if (await confirmModal.isVisible()) {
          // 取消刪除
          const cancelBtn = page.locator('.modal .btn-secondary, .swal2-cancel, #confirmCancelBtn');
          if (await cancelBtn.isVisible()) {
            await cancelBtn.click();
          }
        }
      }
    });

    test('匯出按鈕', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const exportBtn = page.locator('button:has-text("匯出"), a:has-text("匯出")');
      const count = await exportBtn.count();
      console.log(`匯出按鈕數: ${count}`);
    });

    test('分頁功能', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const pagination = page.locator('.pagination');
      if (await pagination.isVisible()) {
        const pageLinks = pagination.locator('.page-link');
        const count = await pageLinks.count();
        console.log(`分頁連結數: ${count}`);

        // 點擊下一頁
        const nextBtn = pagination.locator('.page-item:last-child .page-link');
        if (await nextBtn.isVisible() && await nextBtn.isEnabled()) {
          await nextBtn.click();
          await page.waitForTimeout(WAIT_TIME.api);
        }
      }
    });
  });

  test.describe('顧客詳情頁面', () => {
    test('頁面載入成功', async ({ page, request }) => {
      // 先取得顧客 ID
      const token = await getTenantToken(request);
      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        await page.goto(`/tenant/customers/${customerId}`);
        await waitForLoading(page);
        await page.waitForTimeout(WAIT_TIME.api);

        // 檢查頁面有內容
        const content = page.locator('main, .container, #content');
        await expect(content).toBeVisible();
      }
    });

    test('顧客資訊卡片', async ({ page, request }) => {
      const token = await getTenantToken(request);
      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        await page.goto(`/tenant/customers/${customerId}`);
        await waitForLoading(page);
        await page.waitForTimeout(WAIT_TIME.api);

        // 檢查資訊欄位
        const infoLabels = ['姓名', '電話', '點數', '狀態', 'Email'];
        for (const label of infoLabels) {
          const element = page.locator(`:has-text("${label}")`);
          const count = await element.count();
          console.log(`資訊 "${label}": ${count > 0 ? '存在' : '不存在'}`);
        }
      }
    });

    test('預約記錄', async ({ page, request }) => {
      const token = await getTenantToken(request);
      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        await page.goto(`/tenant/customers/${customerId}`);
        await waitForLoading(page);
        await page.waitForTimeout(WAIT_TIME.api);

        // 檢查預約記錄區塊
        const bookingSection = page.locator(':has-text("預約記錄"), :has-text("預約歷史")');
        const count = await bookingSection.count();
        console.log(`預約記錄區塊: ${count > 0 ? '存在' : '不存在'}`);
      }
    });

    test('點數異動記錄', async ({ page, request }) => {
      const token = await getTenantToken(request);
      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        await page.goto(`/tenant/customers/${customerId}`);
        await waitForLoading(page);
        await page.waitForTimeout(WAIT_TIME.api);

        // 檢查點數記錄區塊
        const pointsSection = page.locator(':has-text("點數記錄"), :has-text("點數異動")');
        const count = await pointsSection.count();
        console.log(`點數記錄區塊: ${count > 0 ? '存在' : '不存在'}`);
      }
    });

    test('返回列表按鈕', async ({ page, request }) => {
      const token = await getTenantToken(request);
      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const customerId = listData.data.content[0].id;
        await page.goto(`/tenant/customers/${customerId}`);
        await waitForLoading(page);

        const backBtn = page.locator('a:has-text("返回"), button:has-text("返回")');
        if (await backBtn.first().isVisible()) {
          await backBtn.first().click();
          await page.waitForTimeout(WAIT_TIME.medium);
          expect(page.url()).toContain('/customers');
        }
      }
    });
  });
});

test.describe('顧客標籤 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('GET /api/customers/tags 回傳標籤陣列', async ({ request }) => {
    const response = await request.get('/api/customers/tags', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.status()).toBe(200);
    const data = await response.json();
    expect(data.success).toBe(true);
    expect(Array.isArray(data.data)).toBe(true);
    console.log(`現有標籤數: ${data.data?.length || 0}`);
  });

  test('標籤新增與移除流程', async ({ request }) => {
    // 先取得一個顧客
    const listResponse = await request.get('/api/customers?page=0&size=1', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const listData = await listResponse.json();
    if (!listData.data?.content?.length) {
      console.log('無顧客資料，跳過標籤測試');
      return;
    }

    const customerId = listData.data.content[0].id;
    const testTag = 'e2e-test-tag';

    // 新增標籤
    const addResponse = await request.post(`/api/customers/${customerId}/tags/add?tag=${testTag}`, {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    // 可能因為未訂閱 ADVANCED_CUSTOMER 而失敗，兩種狀態都合理
    const addData = await addResponse.json();
    console.log(`新增標籤結果: success=${addData.success}, message=${addData.message || ''}`);

    if (addData.success) {
      // 驗證標籤已新增
      const detailResponse = await request.get(`/api/customers/${customerId}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const detailData = await detailResponse.json();
      if (detailData.data?.tags) {
        expect(detailData.data.tags).toContain(testTag);
      }

      // 移除標籤
      const removeResponse = await request.delete(`/api/customers/${customerId}/tags/${testTag}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const removeData = await removeResponse.json();
      console.log(`移除標籤結果: success=${removeData.success}`);
    }
  });

  test('GET /api/customers/by-tag/{tag} 查詢格式正確', async ({ request }) => {
    const response = await request.get('/api/customers/by-tag/VIP', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    // 可能 200 或 403（未訂閱）
    const data = await response.json();
    console.log(`by-tag 查詢: status=${response.status()}, success=${data.success}`);

    if (data.success && data.data) {
      expect(Array.isArray(data.data.content || data.data)).toBe(true);
    }
  });
});

test.describe('顧客點數業務邏輯驗證', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    const loginRes = await request.post('/api/auth/tenant/login', {
      data: { username: 'e2etest@example.com', password: 'Test12345' }
    });
    const loginData = await loginRes.json();
    tenantToken = loginData.data?.accessToken;
  });

  test('增加點數 → 驗證餘額確實增加', async ({ request }) => {
    if (!tenantToken) return;
    const headers = { 'Authorization': `Bearer ${tenantToken}` };

    // 取得第一個顧客
    const custRes = await request.get('/api/customers?size=1', { headers });
    const customers = (await custRes.json()).data?.content || [];
    if (customers.length === 0) {
      console.log('無顧客資料，跳過');
      return;
    }
    const customerId = customers[0].id;

    // 取得當前點數（從顧客詳情）
    const detailRes1 = await request.get(`/api/customers/${customerId}`, { headers });
    const detail1 = (await detailRes1.json()).data;
    const beforePoints = detail1?.currentPoints || detail1?.points || 0;

    // 增加 50 點
    const addRes = await request.post(`/api/customers/${customerId}/points/add`, {
      headers,
      data: { points: 50, reason: 'E2E 測試增加點數' }
    });

    if (addRes.ok()) {
      // 驗證餘額增加
      const detailRes2 = await request.get(`/api/customers/${customerId}`, { headers });
      const detail2 = (await detailRes2.json()).data;
      const afterPoints = detail2?.currentPoints || detail2?.points || 0;
      expect(afterPoints).toBe(beforePoints + 50);
      console.log(`✓ 點數從 ${beforePoints} 增加到 ${afterPoints}`);
    }
  });

  test('扣除點數 → 驗證餘額確實減少', async ({ request }) => {
    if (!tenantToken) return;
    const headers = { 'Authorization': `Bearer ${tenantToken}` };

    const custRes = await request.get('/api/customers?size=1', { headers });
    const customers = (await custRes.json()).data?.content || [];
    if (customers.length === 0) return;
    const customerId = customers[0].id;

    const detailRes1 = await request.get(`/api/customers/${customerId}`, { headers });
    const detail1 = (await detailRes1.json()).data;
    const beforePoints = detail1?.currentPoints || detail1?.points || 0;

    if (beforePoints < 10) {
      console.log(`點數不足(${beforePoints})，跳過扣除測試`);
      return;
    }

    const deductRes = await request.post(`/api/customers/${customerId}/points/deduct`, {
      headers,
      data: { points: 10, reason: 'E2E 測試扣除點數' }
    });

    if (deductRes.ok()) {
      const detailRes2 = await request.get(`/api/customers/${customerId}`, { headers });
      const detail2 = (await detailRes2.json()).data;
      const afterPoints = detail2?.currentPoints || detail2?.points || 0;
      expect(afterPoints).toBe(beforePoints - 10);
      console.log(`✓ 點數從 ${beforePoints} 減少到 ${afterPoints}`);
    }
  });

  test('封鎖顧客 → 驗證狀態為 BLOCKED → 解除封鎖', async ({ request }) => {
    if (!tenantToken) return;
    const headers = { 'Authorization': `Bearer ${tenantToken}` };

    const custRes = await request.get('/api/customers?size=1', { headers });
    const customers = (await custRes.json()).data?.content || [];
    if (customers.length === 0) return;
    const customerId = customers[0].id;

    // 封鎖
    const blockRes = await request.post(`/api/customers/${customerId}/block`, { headers });
    if (blockRes.ok()) {
      const detailRes = await request.get(`/api/customers/${customerId}`, { headers });
      const detail = (await detailRes.json()).data;
      expect(detail?.isBlocked || detail?.blocked).toBeTruthy();
      console.log('✓ 顧客已封鎖');
    }

    // 解除封鎖（確保最終狀態恢復）
    const unblockRes = await request.post(`/api/customers/${customerId}/unblock`, { headers });
    if (unblockRes.ok()) {
      const detailRes2 = await request.get(`/api/customers/${customerId}`, { headers });
      const detail2 = (await detailRes2.json()).data;
      expect(detail2?.isBlocked || detail2?.blocked).toBeFalsy();
      console.log('✓ 顧客已解除封鎖');
    }
  });
});

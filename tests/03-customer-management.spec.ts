import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  generateTestPhone,
  generateTestEmail,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 03 - 顧客管理完整測試
 *
 * 合併自舊檔案: 07-tenant-customer.spec.ts
 *
 * 測試範圍:
 * 1. 顧客列表 API (分頁、搜尋、篩選)
 * 2. 顧客 CRUD (新增、查詢、更新、刪除)
 * 3. 顧客點數業務邏輯 (加值、扣除、餘額驗證、不足檢查)
 * 4. 顧客封鎖/解封業務邏輯
 * 5. 顧客標籤 API
 * 6. 顧客管理 UI
 */

// ========================================
// 共用輔助函式
// ========================================

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  expect(response.ok()).toBeTruthy();
  const data = await response.json();
  expect(data.data.accessToken).toBeTruthy();
  return data.data.accessToken;
}

function authHeaders(token: string) {
  return {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  };
}

// ========================================
// 1. 顧客列表 API
// ========================================

test.describe('顧客列表 API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('取得顧客列表 - 驗證分頁結構與內容', async ({ request }) => {
    const response = await request.get('/api/customers?page=0&size=20', {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(data.data).toHaveProperty('content');
    expect(data.data).toHaveProperty('totalElements');
    expect(data.data).toHaveProperty('totalPages');
    expect(Array.isArray(data.data.content)).toBe(true);
    expect(typeof data.data.totalElements).toBe('number');
    expect(data.data.totalElements).toBeGreaterThanOrEqual(0);

    // 驗證每筆顧客資料的基本欄位
    if (data.data.content.length > 0) {
      const customer = data.data.content[0];
      expect(customer).toHaveProperty('id');
      expect(customer).toHaveProperty('name');
      expect(customer).toHaveProperty('status');
    }
  });

  test('搜尋顧客 - keyword 參數', async ({ request }) => {
    const response = await request.get('/api/customers?keyword=test&page=0&size=10', {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(Array.isArray(data.data.content)).toBe(true);
    // 搜尋結果可能為空，但結構應正確
    expect(typeof data.data.totalElements).toBe('number');
  });

  test('篩選 ACTIVE 顧客', async ({ request }) => {
    const response = await request.get('/api/customers?status=ACTIVE&page=0&size=10', {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);

    // 所有回傳的顧客應為 ACTIVE 狀態
    for (const customer of data.data.content) {
      expect(customer.status).toBe('ACTIVE');
    }
  });

  test('篩選 BLOCKED 顧客', async ({ request }) => {
    const response = await request.get('/api/customers?status=BLOCKED&page=0&size=10', {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);

    // 所有回傳的顧客應為 BLOCKED 狀態
    for (const customer of data.data.content) {
      expect(customer.status).toBe('BLOCKED');
    }
  });
});

// ========================================
// 2. 顧客 CRUD (serial)
// ========================================

test.describe('顧客 CRUD', () => {
  test.describe.configure({ mode: 'serial' });

  let token: string;
  let createdCustomerId: string;
  const testCustomerName = generateTestData('CustCRUD');
  const testPhone = generateTestPhone();
  const testEmail = generateTestEmail();

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('新增顧客 - 驗證建立成功與回傳欄位', async ({ request }) => {
    const response = await request.post('/api/customers', {
      headers: authHeaders(token),
      data: {
        name: testCustomerName,
        phone: testPhone,
        email: testEmail,
        gender: 'OTHER',
        note: 'E2E CRUD 測試建立'
      }
    });

    expect(response.status()).toBe(201);
    const data = await response.json();
    expect(data.success).toBe(true);
    expect(data.data).toHaveProperty('id');
    expect(data.data.name).toBe(testCustomerName);
    expect(data.data.phone).toBe(testPhone);
    expect(data.data.email).toBe(testEmail);
    expect(data.data.gender).toBe('OTHER');
    expect(data.data.status).toBe('ACTIVE');

    createdCustomerId = data.data.id;
    expect(createdCustomerId).toBeTruthy();
  });

  test('取得顧客詳情 - 驗證所有欄位', async ({ request }) => {
    expect(createdCustomerId).toBeTruthy();

    const response = await request.get(`/api/customers/${createdCustomerId}`, {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(data.data.id).toBe(createdCustomerId);
    expect(data.data.name).toBe(testCustomerName);
    expect(data.data.phone).toBe(testPhone);
    expect(data.data.email).toBe(testEmail);
    expect(data.data.status).toBe('ACTIVE');
    // pointBalance 預設應為 0
    expect(data.data.pointBalance).toBe(0);
  });

  test('更新顧客備註 - 驗證更新持久化', async ({ request }) => {
    expect(createdCustomerId).toBeTruthy();

    const updatedNote = `E2E 更新備註 ${Date.now()}`;
    const response = await request.put(`/api/customers/${createdCustomerId}`, {
      headers: authHeaders(token),
      data: {
        name: testCustomerName,
        phone: testPhone,
        email: testEmail,
        note: updatedNote
      }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(data.data.note).toBe(updatedNote);

    // 重新取得詳情驗證持久化
    const verifyResponse = await request.get(`/api/customers/${createdCustomerId}`, {
      headers: authHeaders(token)
    });
    const verifyData = await verifyResponse.json();
    expect(verifyData.data.note).toBe(updatedNote);
  });

  test('刪除顧客 - 驗證軟刪除成功', async ({ request }) => {
    expect(createdCustomerId).toBeTruthy();

    const response = await request.delete(`/api/customers/${createdCustomerId}`, {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);

    // 刪除後應查不到（404 或類似錯誤）
    const verifyResponse = await request.get(`/api/customers/${createdCustomerId}`, {
      headers: authHeaders(token)
    });
    expect(verifyResponse.ok()).toBe(false);
  });
});

// ========================================
// 3. 顧客點數業務邏輯 (CRITICAL - serial)
// ========================================

test.describe('顧客點數業務邏輯', () => {
  test.describe.configure({ mode: 'serial' });

  let token: string;
  let testCustomerId: string;
  let initialPointBalance: number;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);

    // 建立一個專門用於點數測試的顧客
    const createResponse = await request.post('/api/customers', {
      headers: authHeaders(token),
      data: {
        name: generateTestData('PointsTest'),
        phone: generateTestPhone(),
        email: generateTestEmail(),
        gender: 'UNKNOWN',
        note: 'E2E 點數測試專用'
      }
    });

    if (createResponse.status() === 201) {
      const createData = await createResponse.json();
      testCustomerId = createData.data.id;
    } else {
      // 若無法建立，取得現有的第一個顧客
      const listResponse = await request.get('/api/customers?page=0&size=1', {
        headers: authHeaders(token)
      });
      const listData = await listResponse.json();
      if (listData.data?.content?.length > 0) {
        testCustomerId = listData.data.content[0].id;
      }
    }
  });

  test('取得顧客當前點數餘額', async ({ request }) => {
    expect(testCustomerId).toBeTruthy();

    const response = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(typeof data.data.pointBalance).toBe('number');
    expect(data.data.pointBalance).toBeGreaterThanOrEqual(0);

    initialPointBalance = data.data.pointBalance;
  });

  test('加 50 點 -> 驗證餘額 = 原始 + 50', async ({ request }) => {
    expect(testCustomerId).toBeTruthy();
    expect(typeof initialPointBalance).toBe('number');

    // 注意: add/deduct 端點使用 @RequestParam，非 JSON body
    const response = await request.post(
      `/api/customers/${testCustomerId}/points/add?points=50&description=E2E+%E6%B8%AC%E8%A9%A6%E5%8A%A050%E9%BB%9E`,
      { headers: authHeaders(token) }
    );
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);

    // 驗證回傳的顧客資料中點數已更新
    expect(data.data.pointBalance).toBe(initialPointBalance + 50);

    // 再次 GET 詳情確認持久化
    const verifyResponse = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    const verifyData = await verifyResponse.json();
    expect(verifyData.data.pointBalance).toBe(initialPointBalance + 50);
  });

  test('扣 10 點 -> 驗證餘額 = 原始 + 50 - 10', async ({ request }) => {
    expect(testCustomerId).toBeTruthy();

    const response = await request.post(
      `/api/customers/${testCustomerId}/points/deduct?points=10&description=E2E+%E6%B8%AC%E8%A9%A6%E6%89%A310%E9%BB%9E`,
      { headers: authHeaders(token) }
    );
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(data.data.pointBalance).toBe(initialPointBalance + 50 - 10);

    // 再次 GET 詳情確認持久化
    const verifyResponse = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    const verifyData = await verifyResponse.json();
    expect(verifyData.data.pointBalance).toBe(initialPointBalance + 40);
  });

  test('扣除超過餘額的點數 -> 應失敗 (點數不足)', async ({ request }) => {
    expect(testCustomerId).toBeTruthy();

    // 先取得當前餘額
    const detailResponse = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    const currentBalance = (await detailResponse.json()).data.pointBalance;

    // 嘗試扣除超過餘額的點數
    const overDeductAmount = currentBalance + 999;
    const response = await request.post(
      `/api/customers/${testCustomerId}/points/deduct?points=${overDeductAmount}&description=E2E+%E9%BB%9E%E6%95%B8%E4%B8%8D%E8%B6%B3%E6%B8%AC%E8%A9%A6`,
      { headers: authHeaders(token) }
    );

    // 預期失敗 (400 或 409 或 422)
    expect(response.ok()).toBe(false);
    expect(response.status()).toBeGreaterThanOrEqual(400);
    expect(response.status()).toBeLessThan(500);

    // 驗證餘額未改變
    const verifyResponse = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    const verifyData = await verifyResponse.json();
    expect(verifyData.data.pointBalance).toBe(currentBalance);
  });

  // afterAll: 清理測試顧客
  test.afterAll(async ({ request }) => {
    if (testCustomerId && token) {
      await request.delete(`/api/customers/${testCustomerId}`, {
        headers: authHeaders(token)
      });
    }
  });
});

// ========================================
// 4. 顧客封鎖/解封業務邏輯 (serial)
// ========================================

test.describe('顧客封鎖/解封業務邏輯', () => {
  test.describe.configure({ mode: 'serial' });

  let token: string;
  let testCustomerId: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);

    // 建立專門用於封鎖測試的顧客
    const createResponse = await request.post('/api/customers', {
      headers: authHeaders(token),
      data: {
        name: generateTestData('BlockTest'),
        phone: generateTestPhone(),
        email: generateTestEmail(),
        gender: 'UNKNOWN',
        note: 'E2E 封鎖測試專用'
      }
    });

    if (createResponse.status() === 201) {
      const createData = await createResponse.json();
      testCustomerId = createData.data.id;
    } else {
      // 取得現有 ACTIVE 顧客
      const listResponse = await request.get('/api/customers?status=ACTIVE&page=0&size=1', {
        headers: authHeaders(token)
      });
      const listData = await listResponse.json();
      if (listData.data?.content?.length > 0) {
        testCustomerId = listData.data.content[0].id;
      }
    }
  });

  test('確認顧客初始狀態為 ACTIVE', async ({ request }) => {
    expect(testCustomerId).toBeTruthy();

    const response = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.data.status).toBe('ACTIVE');
  });

  test('封鎖顧客 -> GET 詳情 -> 狀態為 BLOCKED', async ({ request }) => {
    expect(testCustomerId).toBeTruthy();

    const blockResponse = await request.post(`/api/customers/${testCustomerId}/block`, {
      headers: authHeaders(token)
    });
    expect(blockResponse.ok()).toBeTruthy();

    const blockData = await blockResponse.json();
    expect(blockData.success).toBe(true);
    expect(blockData.data.status).toBe('BLOCKED');

    // 重新 GET 驗證持久化
    const verifyResponse = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    const verifyData = await verifyResponse.json();
    expect(verifyData.data.status).toBe('BLOCKED');
  });

  test('解封顧客 -> GET 詳情 -> 狀態恢復 ACTIVE', async ({ request }) => {
    expect(testCustomerId).toBeTruthy();

    const unblockResponse = await request.post(`/api/customers/${testCustomerId}/unblock`, {
      headers: authHeaders(token)
    });
    expect(unblockResponse.ok()).toBeTruthy();

    const unblockData = await unblockResponse.json();
    expect(unblockData.success).toBe(true);
    expect(unblockData.data.status).toBe('ACTIVE');

    // 重新 GET 驗證持久化
    const verifyResponse = await request.get(`/api/customers/${testCustomerId}`, {
      headers: authHeaders(token)
    });
    const verifyData = await verifyResponse.json();
    expect(verifyData.data.status).toBe('ACTIVE');
  });

  // afterAll: 清理測試顧客（確保為 ACTIVE 狀態後刪除）
  test.afterAll(async ({ request }) => {
    if (testCustomerId && token) {
      // 確保恢復 ACTIVE
      await request.post(`/api/customers/${testCustomerId}/unblock`, {
        headers: authHeaders(token)
      });
      await request.delete(`/api/customers/${testCustomerId}`, {
        headers: authHeaders(token)
      });
    }
  });
});

// ========================================
// 5. 顧客標籤 API
// ========================================

test.describe('顧客標籤 API', () => {
  let token: string;
  let testCustomerId: string;
  const testTag = `e2e-tag-${Date.now()}`;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);

    // 取得一個現有顧客用於標籤測試
    const listResponse = await request.get('/api/customers?page=0&size=1', {
      headers: authHeaders(token)
    });
    const listData = await listResponse.json();
    if (listData.data?.content?.length > 0) {
      testCustomerId = listData.data.content[0].id;
    }
  });

  test('GET /api/customers/tags - 回傳標籤陣列', async ({ request }) => {
    const response = await request.get('/api/customers/tags', {
      headers: authHeaders(token)
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBe(true);
    expect(Array.isArray(data.data)).toBe(true);
  });

  test('新增標籤到顧客 -> 驗證標籤存在於詳情', async ({ request }) => {
    test.skip(!testCustomerId, '無可用顧客');

    const addResponse = await request.post(
      `/api/customers/${testCustomerId}/tags/add?tag=${testTag}`,
      { headers: authHeaders(token) }
    );

    // 可能因未訂閱 ADVANCED_CUSTOMER 功能而回 403
    if (addResponse.ok()) {
      const addData = await addResponse.json();
      expect(addData.success).toBe(true);

      // 驗證標籤存在 (tags 為逗號分隔字串)
      const detailResponse = await request.get(`/api/customers/${testCustomerId}`, {
        headers: authHeaders(token)
      });
      const detailData = await detailResponse.json();
      expect(detailData.data.tags).toBeTruthy();
      expect(detailData.data.tags).toContain(testTag);
    } else {
      // 未訂閱功能，標籤 API 不可用，驗證回傳的是合理 HTTP 狀態
      expect(addResponse.status()).toBeGreaterThanOrEqual(400);
      expect(addResponse.status()).toBeLessThan(500);
    }
  });

  test('移除標籤 -> 驗證標籤已移除', async ({ request }) => {
    test.skip(!testCustomerId, '無可用顧客');

    // 先嘗試新增（確保有標籤可移除）
    await request.post(
      `/api/customers/${testCustomerId}/tags/add?tag=${testTag}`,
      { headers: authHeaders(token) }
    );

    const removeResponse = await request.delete(
      `/api/customers/${testCustomerId}/tags/${testTag}`,
      { headers: authHeaders(token) }
    );

    if (removeResponse.ok()) {
      const removeData = await removeResponse.json();
      expect(removeData.success).toBe(true);

      // 驗證標籤已移除
      const detailResponse = await request.get(`/api/customers/${testCustomerId}`, {
        headers: authHeaders(token)
      });
      const detailData = await detailResponse.json();
      // tags 為 null 或不包含該標籤
      if (detailData.data.tags) {
        expect(detailData.data.tags).not.toContain(testTag);
      }
    } else {
      // 未訂閱功能
      expect(removeResponse.status()).toBeGreaterThanOrEqual(400);
      expect(removeResponse.status()).toBeLessThan(500);
    }
  });

  test('GET /api/customers/by-tag/{tag} - 依標籤查詢顧客', async ({ request }) => {
    const response = await request.get('/api/customers/by-tag/VIP', {
      headers: authHeaders(token)
    });

    // 可能 200 (有權限) 或 4xx (未訂閱 ADVANCED_CUSTOMER)
    expect(response.status()).toBeLessThan(500);

    const data = await response.json();
    if (data.success && data.data) {
      expect(Array.isArray(data.data)).toBe(true);
    }
  });
});

// ========================================
// 6. 顧客管理 UI
// ========================================

test.describe('顧客管理 UI', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('顧客列表頁面載入 - 表格正確顯示', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 頁面標題可見
    const title = page.locator('h1, h2, .page-title');
    await expect(title.first()).toBeVisible();

    // 表格可見
    const table = page.locator('table');
    await expect(table.first()).toBeVisible();

    // 表格有表頭
    const headerRow = page.locator('table thead tr');
    await expect(headerRow.first()).toBeVisible();
  });

  test('新增顧客 Modal 包含必要表單欄位', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 點擊新增按鈕
    const addBtn = page.locator('button:has-text("新增顧客"), button:has-text("新增")').first();
    await expect(addBtn).toBeVisible();
    await addBtn.click();
    await page.waitForTimeout(WAIT_TIME.short);

    // Modal 開啟
    const modal = page.locator('.modal.show');
    await expect(modal).toBeVisible();

    // 驗證必要表單欄位存在
    const nameField = modal.locator('#name, input[name="name"]');
    const phoneField = modal.locator('#phone, input[name="phone"]');
    const emailField = modal.locator('#email, input[name="email"]');

    await expect(nameField).toBeVisible();
    await expect(phoneField).toBeVisible();
    await expect(emailField).toBeVisible();

    // 檢查其他欄位 (gender, birthday, note) 是否存在
    const genderField = modal.locator('#gender, select[name="gender"]');
    const birthdayField = modal.locator('#birthday, input[name="birthday"]');
    const noteField = modal.locator('#note, textarea[name="note"]');

    // 這些是常見的選填欄位，至少其中部分應存在
    const optionalFieldsCount =
      (await genderField.count() > 0 ? 1 : 0) +
      (await birthdayField.count() > 0 ? 1 : 0) +
      (await noteField.count() > 0 ? 1 : 0);
    expect(optionalFieldsCount).toBeGreaterThanOrEqual(1);

    // 關閉 Modal
    const closeBtn = modal.locator('.btn-close, [data-bs-dismiss="modal"]').first();
    if (await closeBtn.isVisible()) {
      await closeBtn.click();
    }
  });

  test('顧客詳情頁面載入 - 顯示資訊卡片', async ({ page, request }) => {
    // 先透過 API 取得一個顧客 ID
    const token = await getTenantToken(request);
    const listResponse = await request.get('/api/customers?page=0&size=1', {
      headers: authHeaders(token)
    });
    const listData = await listResponse.json();

    test.skip(!listData.data?.content?.length, '無顧客資料可測試');

    const customerId = listData.data.content[0].id;
    await page.goto(`/tenant/customers/${customerId}`);
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 頁面主內容區域存在
    const content = page.locator('main, .container, .container-fluid, #content');
    await expect(content.first()).toBeVisible();

    // 頁面不應顯示錯誤
    const errorAlert = page.locator('.alert-danger');
    const errorCount = await errorAlert.count();
    const visibleErrors = [];
    for (let i = 0; i < errorCount; i++) {
      if (await errorAlert.nth(i).isVisible()) {
        visibleErrors.push(await errorAlert.nth(i).textContent());
      }
    }
    expect(visibleErrors.length).toBe(0);
  });

  test('搜尋功能不會產生錯誤', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const searchInput = page.locator('input[type="search"], input[placeholder*="搜尋"], #keyword');

    if (await searchInput.count() > 0 && await searchInput.first().isVisible()) {
      await searchInput.first().fill('test');
      // 等待搜尋結果 (可能有 debounce)
      await page.waitForTimeout(WAIT_TIME.api);

      // 不應有錯誤提示
      const errorAlert = page.locator('.alert-danger:visible');
      expect(await errorAlert.count()).toBe(0);

      // 表格仍然存在 (搜尋結果可能為空，但表格結構應在)
      const table = page.locator('table');
      await expect(table.first()).toBeVisible();
    } else {
      // 沒有搜尋框也是一種合理的 UI 設計，跳過
      test.skip(true, '頁面無搜尋輸入框');
    }
  });
});

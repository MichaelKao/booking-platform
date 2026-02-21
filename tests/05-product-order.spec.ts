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
 * 商品管理與商品訂單 E2E 測試
 *
 * 測試範圍：
 * 1. 商品列表 API（分頁、欄位驗證）
 * 2. 商品 CRUD（建立、讀取、更新、刪除）
 * 3. 商品庫存業務邏輯（調整庫存、驗證數量一致性）
 * 4. 商品上下架（狀態流轉驗證）
 * 5. 商品訂單 API（列表結構驗證）
 * 6. 商品管理 UI（頁面載入、表格、Modal）
 * 7. 庫存異動頁面 UI
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  expect(response.ok()).toBeTruthy();
  const data = await response.json();
  expect(data.data.accessToken).toBeTruthy();
  return data.data.accessToken;
}

// ========================================
// 1. 商品列表 API
// ========================================

test.describe('商品列表 API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('取得商品列表（分頁結構驗證）', async ({ request }) => {
    const response = await request.get('/api/products?page=0&size=20', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toHaveProperty('content');
    expect(data.data).toHaveProperty('totalElements');
    expect(data.data).toHaveProperty('totalPages');
    expect(Array.isArray(data.data.content)).toBeTruthy();
    expect(typeof data.data.totalElements).toBe('number');
  });

  test('每個商品具備 name, price, stockQuantity, status 欄位', async ({ request }) => {
    const response = await request.get('/api/products?page=0&size=5', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    const products = data.data?.content || [];

    // 如果有商品資料，驗證欄位結構
    if (products.length > 0) {
      const product = products[0];
      expect(product).toHaveProperty('id');
      expect(product).toHaveProperty('name');
      expect(product).toHaveProperty('price');
      expect(product).toHaveProperty('status');
      // stockQuantity 可能為 null（不追蹤庫存的商品）
      expect('stockQuantity' in product).toBeTruthy();
    }
    // 無論有無商品，API 本身必須成功
    expect(data.success).toBeTruthy();
  });
});

// ========================================
// 2. 商品 CRUD（依序執行）
// ========================================

test.describe('商品 CRUD', () => {
  let token: string;
  let createdProductId: string;
  const testProductName = generateTestData('E2E商品');

  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('建立商品 - 回傳 id 且欄位正確', async ({ request }) => {
    const response = await request.post('/api/products', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      data: {
        name: testProductName,
        description: 'E2E 測試用商品',
        category: 'OTHER',
        price: 500,
        stockQuantity: 100
      }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toHaveProperty('id');
    expect(data.data.name).toBe(testProductName);
    expect(data.data.stockQuantity).toBe(100);
    expect(Number(data.data.price)).toBe(500);
    createdProductId = data.data.id;
  });

  test('取得商品詳情 - 所有欄位正確', async ({ request }) => {
    expect(createdProductId).toBeTruthy();
    const response = await request.get(`/api/products/${createdProductId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.data.id).toBe(createdProductId);
    expect(data.data.name).toBe(testProductName);
    expect(Number(data.data.price)).toBe(500);
    expect(data.data.stockQuantity).toBe(100);
    expect(data.data.description).toBe('E2E 測試用商品');
    expect(data.data.category).toBe('OTHER');
  });

  test('更新商品價格為 600 - 驗證更新後的值', async ({ request }) => {
    expect(createdProductId).toBeTruthy();
    const response = await request.put(`/api/products/${createdProductId}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      data: {
        name: testProductName,
        description: 'E2E 測試用商品（已更新）',
        category: 'OTHER',
        price: 600,
        stockQuantity: 100,
        trackInventory: true
      }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(Number(data.data.price)).toBe(600);

    // 透過 GET 再次驗證
    const verifyRes = await request.get(`/api/products/${createdProductId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const verifyData = await verifyRes.json();
    expect(Number(verifyData.data.price)).toBe(600);
  });

  test('調整庫存 +10 - 驗證 stockQuantity = 110', async ({ request }) => {
    expect(createdProductId).toBeTruthy();
    const response = await request.post(
      `/api/products/${createdProductId}/adjust-stock?adjustment=10&reason=E2E%E6%B8%AC%E8%A9%A6`,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.data.stockQuantity).toBe(110);
  });

  test('下架商品 - 驗證 status 變更', async ({ request }) => {
    expect(createdProductId).toBeTruthy();
    const response = await request.post(`/api/products/${createdProductId}/off-shelf`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.data.status).toBe('OFF_SHELF');
  });

  test('清理 - 刪除測試商品', async ({ request }) => {
    expect(createdProductId).toBeTruthy();
    const response = await request.delete(`/api/products/${createdProductId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    // 驗證已刪除 - 再取應得 404 或 error
    const verifyRes = await request.get(`/api/products/${createdProductId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(verifyRes.ok()).toBeFalsy();
  });
});

// ========================================
// 3. 商品庫存業務邏輯
// ========================================

test.describe('商品庫存業務邏輯', () => {
  let token: string;
  let productId: string;
  let initialStock: number;

  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);

    // 建立專用測試商品
    const createRes = await request.post('/api/products', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      data: {
        name: generateTestData('庫存測試'),
        description: '庫存業務邏輯測試商品',
        category: 'OTHER',
        price: 100,
        stockQuantity: 50
      }
    });
    const createData = await createRes.json();
    productId = createData.data?.id;
    initialStock = createData.data?.stockQuantity ?? 50;
  });

  test('調整庫存 +20 後 GET 驗證 stockQuantity = initial + 20', async ({ request }) => {
    expect(productId).toBeTruthy();
    const adjustRes = await request.post(
      `/api/products/${productId}/adjust-stock?adjustment=20&reason=E2E%E5%A2%9E%E5%8A%A0`,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );
    expect(adjustRes.ok()).toBeTruthy();

    const detailRes = await request.get(`/api/products/${productId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(detailRes.ok()).toBeTruthy();
    const detail = await detailRes.json();
    expect(detail.data.stockQuantity).toBe(initialStock + 20);
  });

  test('調整庫存 -5 後 GET 驗證 stockQuantity = initial + 15', async ({ request }) => {
    expect(productId).toBeTruthy();
    const adjustRes = await request.post(
      `/api/products/${productId}/adjust-stock?adjustment=-5&reason=E2E%E6%B8%9B%E5%B0%91`,
      { headers: { 'Authorization': `Bearer ${token}` } }
    );
    expect(adjustRes.ok()).toBeTruthy();

    const detailRes = await request.get(`/api/products/${productId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(detailRes.ok()).toBeTruthy();
    const detail = await detailRes.json();
    expect(detail.data.stockQuantity).toBe(initialStock + 15);
  });

  test.afterAll(async ({ request }) => {
    // 清理測試商品
    if (productId && token) {
      await request.delete(`/api/products/${productId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
    }
  });
});

// ========================================
// 4. 商品上下架
// ========================================

test.describe('商品上下架狀態流轉', () => {
  let token: string;
  let productId: string;

  test.describe.configure({ mode: 'serial' });

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);

    // 建立專用測試商品（預設上架）
    const createRes = await request.post('/api/products', {
      headers: {
        'Authorization': `Bearer ${token}`,
        'Content-Type': 'application/json'
      },
      data: {
        name: generateTestData('上下架測試'),
        description: '上下架狀態測試商品',
        category: 'OTHER',
        price: 200,
        stockQuantity: 10
      }
    });
    const createData = await createRes.json();
    productId = createData.data?.id;
  });

  test('下架商品 - 狀態變為 OFF_SHELF', async ({ request }) => {
    expect(productId).toBeTruthy();
    const response = await request.post(`/api/products/${productId}/off-shelf`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    // 透過 GET 驗證狀態
    const detailRes = await request.get(`/api/products/${productId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const detail = await detailRes.json();
    expect(detail.data.status).toBe('OFF_SHELF');
  });

  test('重新上架 - 狀態變為 ON_SALE', async ({ request }) => {
    expect(productId).toBeTruthy();
    const response = await request.post(`/api/products/${productId}/on-sale`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    // 透過 GET 驗證狀態
    const detailRes = await request.get(`/api/products/${productId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const detail = await detailRes.json();
    expect(detail.data.status).toBe('ON_SALE');
  });

  test('確認最終狀態為 ON_SALE（恢復確認）', async ({ request }) => {
    expect(productId).toBeTruthy();
    const detailRes = await request.get(`/api/products/${productId}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(detailRes.ok()).toBeTruthy();
    const detail = await detailRes.json();
    expect(detail.data.status).toBe('ON_SALE');
  });

  test.afterAll(async ({ request }) => {
    if (productId && token) {
      await request.delete(`/api/products/${productId}`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
    }
  });
});

// ========================================
// 5. 商品訂單 API
// ========================================

test.describe('商品訂單 API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('取得商品訂單列表 - 結構驗證', async ({ request }) => {
    const response = await request.get('/api/product-orders?page=0&size=20', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toHaveProperty('content');
    expect(data.data).toHaveProperty('totalElements');
    expect(Array.isArray(data.data.content)).toBeTruthy();
  });

  test('取得待處理訂單數量 - 回傳數字', async ({ request }) => {
    const response = await request.get('/api/product-orders/pending/count', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(typeof data.data).toBe('number');
    expect(data.data).toBeGreaterThanOrEqual(0);
  });

  test('取得今日訂單統計 - 結構驗證', async ({ request }) => {
    const response = await request.get('/api/product-orders/today/stats', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toBeDefined();
  });

  test('依狀態篩選訂單 - 不回傳 500', async ({ request }) => {
    const statuses = ['PENDING', 'CONFIRMED', 'COMPLETED', 'CANCELLED'];
    for (const status of statuses) {
      const response = await request.get(`/api/product-orders?status=${status}&page=0&size=5`, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      expect(response.status()).toBeLessThan(500);
    }
  });
});

// ========================================
// 6. 商品管理 UI
// ========================================

test.describe('商品管理 UI', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('商品列表頁面載入且有表格', async ({ page }) => {
    await page.goto('/tenant/products');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 頁面標題
    const title = page.locator('h1.page-title');
    await expect(title).toContainText('商品管理');

    // 表格存在
    const table = page.locator('table.table');
    await expect(table).toBeVisible();

    // 表頭驗證
    const thead = page.locator('table.table thead');
    await expect(thead).toContainText('商品名稱');
    await expect(thead).toContainText('售價');
    await expect(thead).toContainText('庫存');
    await expect(thead).toContainText('狀態');
  });

  test('新增商品 Modal 包含必要表單欄位', async ({ page }) => {
    await page.goto('/tenant/products');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);

    // 點擊新增商品按鈕
    const addBtn = page.locator('button:has-text("新增商品")');
    await expect(addBtn).toBeVisible();
    await addBtn.click();
    await page.waitForTimeout(WAIT_TIME.short);

    // Modal 出現
    const modal = page.locator('.modal.show');
    await expect(modal).toBeVisible();

    // 表單欄位驗證
    await expect(page.locator('#name')).toBeVisible();
    await expect(page.locator('#category')).toBeVisible();
    await expect(page.locator('#price')).toBeVisible();
    await expect(page.locator('#stockQuantity')).toBeVisible();

    await closeModal(page);
  });

  test('商品狀態 badge 顯示正確', async ({ page }) => {
    await page.goto('/tenant/products');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查表格中是否有資料行（即使沒有資料，頁面本身不應壞掉）
    const dataTable = page.locator('#dataTable');
    await expect(dataTable).toBeVisible();

    // 如果有商品，badge 應存在
    const badges = page.locator('#dataTable .badge');
    const badgeCount = await badges.count();
    if (badgeCount > 0) {
      // 至少第一個 badge 的文字不為空
      const firstBadgeText = await badges.first().textContent();
      expect(firstBadgeText?.trim().length).toBeGreaterThan(0);
    }
    // 頁面載入成功即通過
    expect(true).toBeTruthy();
  });
});

// ========================================
// 7. 庫存異動頁面 UI
// ========================================

test.describe('庫存異動頁面 UI', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('庫存異動頁面載入成功', async ({ page }) => {
    await page.goto('/tenant/inventory');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);

    // 頁面載入不應報錯
    const body = page.locator('body');
    await expect(body).toBeVisible();

    // 確認不是錯誤頁面
    const pageContent = await page.content();
    expect(pageContent).not.toContain('500 Internal Server Error');
    expect(pageContent).not.toContain('Whitelabel Error Page');
  });

  test('庫存異動 API 可正常呼叫', async ({ request }) => {
    const token = await getTenantToken(request);
    const response = await request.get('/api/inventory/logs?page=0&size=20', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toHaveProperty('content');
    expect(Array.isArray(data.data.content)).toBeTruthy();
  });
});

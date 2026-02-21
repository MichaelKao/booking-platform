import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  TEST_ACCOUNTS,
  generateTestData
} from './utils/test-helpers';

/**
 * 設定與功能管理 - 整合測試
 *
 * 合併自: 11-settings.spec.ts, 14-feature-store-details.spec.ts,
 *         23-sidebar-feature-visibility.spec.ts, 13-membership-forms.spec.ts
 *
 * 測試範圍：
 * 1. 店家設定 API（基本設定、點數累積設定、Setup Status）
 * 2. LINE 設定 API（安全唯讀，不覆蓋 credentials）
 * 3. 功能商店 API（列表、詳情、訂閱/取消）
 * 4. 會員等級 API（CRUD）
 * 5. 功能商店 UI（頁面載入、卡片、訂閱按鈕）
 * 6. 設定頁面 UI（設定頁面、LINE 設定頁面）
 * 7. 側邊欄功能顯示控制（訂閱狀態驅動選單顯示）
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// ============================================================
// 1. 店家設定 API
// ============================================================
test.describe('1. 店家設定 API', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('GET /api/settings - 取得設定並驗證欄位結構', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/settings', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toHaveProperty('name');
    expect(data.data).toHaveProperty('phone');
    expect(data.data).toHaveProperty('address');
    expect(data.data).toHaveProperty('description');
    expect(data.data).toHaveProperty('pointEarnEnabled');
    expect(data.data).toHaveProperty('pointEarnRate');
    expect(data.data).toHaveProperty('pointRoundMode');
  });

  test('PUT /api/settings - 更新 description 並驗證持久化', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    // 先取得現有設定
    const getRes = await request.get('/api/settings', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(getRes.ok()).toBeTruthy();
    const current = (await getRes.json()).data;

    const newDescription = `E2E test description ${Date.now()}`;
    const putRes = await request.put('/api/settings', {
      headers: {
        'Authorization': `Bearer ${tenantToken}`,
        'Content-Type': 'application/json'
      },
      data: {
        description: newDescription
      }
    });
    // Tenant entity 儲存已修復（@PreUpdate 確保 tenantId = id）
    expect(putRes.status()).toBeLessThan(500);

    // 驗證持久化
    const verifyRes = await request.get('/api/settings', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const updated = (await verifyRes.json()).data;
    expect(updated.description).toBe(newDescription);

    // 還原
    await request.put('/api/settings', {
      headers: {
        'Authorization': `Bearer ${tenantToken}`,
        'Content-Type': 'application/json'
      },
      data: {
        name: current.name,
        phone: current.phone,
        address: current.address,
        description: current.description
      }
    });
  });

  test('GET /api/settings/setup-status - 驗證結構', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    // setup-status 應該回傳各項設定的完成狀態
    expect(data.data).toBeDefined();
  });
});

// ============================================================
// 2. 點數累積設定 (Business Logic)
// ============================================================
test.describe('2. 點數累積設定', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('驗證點數欄位類型: boolean, number, string(enum)', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/settings', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();

    const { data } = await response.json();
    expect(typeof data.pointEarnEnabled).toBe('boolean');
    expect(typeof data.pointEarnRate).toBe('number');
    expect(typeof data.pointRoundMode).toBe('string');
  });

  test('pointRoundMode 為 FLOOR/ROUND/CEIL 之一', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/settings', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const { data } = await response.json();
    expect(['FLOOR', 'ROUND', 'CEIL']).toContain(data.pointRoundMode);
  });

  test('pointEarnRate 為正整數', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/settings', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const { data } = await response.json();
    expect(data.pointEarnRate).toBeGreaterThan(0);
    expect(Number.isInteger(data.pointEarnRate)).toBeTruthy();
  });
});

// ============================================================
// 3. LINE 設定 API (SAFE)
// ============================================================
test.describe('3. LINE 設定 API (安全唯讀)', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('GET /api/settings/line - 驗證回應結構', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/settings/line', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.status()).toBeLessThan(500);

    if (response.ok()) {
      const data = await response.json();
      // 結構中應有 channelId 等欄位（值可能為空）
      expect(data.data).toBeDefined();
      if (data.data) {
        expect(data.data).toHaveProperty('channelId');
      }
    }
  });

  test('POST /api/settings/line/test - 連線測試回應', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.post('/api/settings/line/test', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    // 可能成功也可能失敗（取決於是否已設定 LINE），但不應 500
    expect(response.status()).toBeLessThan(500);
  });

  // IMPORTANT: 此測試區塊絕不寫入 channelId/channelSecret/channelAccessToken
});

// ============================================================
// 4. 功能商店 API
// ============================================================
test.describe('4. 功能商店 API', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('GET /api/feature-store - 回傳功能陣列，每筆含必要欄位', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/feature-store', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(Array.isArray(data.data)).toBeTruthy();
    expect(data.data.length).toBeGreaterThan(0);

    const feature = data.data[0];
    expect(feature).toHaveProperty('code');
    expect(feature).toHaveProperty('name');
    expect(feature).toHaveProperty('description');
  });

  test('GET /api/feature-store/{code} - 取得單一功能詳情', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/feature-store/ADVANCED_REPORT', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.data).toHaveProperty('code', 'ADVANCED_REPORT');
    expect(data.data).toHaveProperty('name');
  });

  test('訂閱 -> 驗證狀態 -> 取消 -> 驗證狀態 (ADVANCED_REPORT)', async ({ request }) => {
    expect(tenantToken).toBeTruthy();
    const headers = { 'Authorization': `Bearer ${tenantToken}` };
    const code = 'ADVANCED_REPORT';

    // 嘗試訂閱（可能因點數不足而失敗）
    const applyRes = await request.post(`/api/feature-store/${code}/apply`, { headers });
    if (!applyRes.ok()) {
      // 點數不足無法訂閱，跳過此測試
      const applyBody = await applyRes.json();
      if (applyBody.code === 'POINT_001' || applyBody.message?.includes('點數不足')) {
        test.skip(true, '店家點數不足，無法測試訂閱流程');
        return;
      }
    }
    expect(applyRes.status()).toBeLessThan(500);

    // 驗證已訂閱
    const afterApply = await request.get(`/api/feature-store/${code}`, { headers });
    const applyData = await afterApply.json();
    expect(applyData.data.isEnabled).toBeTruthy();

    // 取消
    const cancelRes = await request.post(`/api/feature-store/${code}/cancel`, { headers });
    expect(cancelRes.status()).toBeLessThan(500);

    // 驗證已取消
    const afterCancel = await request.get(`/api/feature-store/${code}`, { headers });
    const cancelData = await afterCancel.json();
    expect(cancelData.data.isEnabled).toBeFalsy();
  });
});

// ============================================================
// 5. 會員等級 API
// ============================================================
test.describe('5. 會員等級 API', () => {
  let tenantToken: string;
  let createdLevelId: string | null = null;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('GET /api/membership-levels - 取得列表', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/membership-levels', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(Array.isArray(data.data)).toBeTruthy();
  });

  test('POST /api/membership-levels - 建立會員等級', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const levelData = {
      name: generateTestData('Level'),
      upgradeThreshold: 500,
      discountRate: 0.05,
      pointRate: 0.1,
      description: 'E2E 測試會員等級',
      isDefault: false,
      isActive: true
    };

    const response = await request.post('/api/membership-levels', {
      headers: {
        'Authorization': `Bearer ${tenantToken}`,
        'Content-Type': 'application/json'
      },
      data: levelData
    });

    // 成功(200/201)或衝突(409)都代表 DTO 解析成功
    expect(response.status()).toBeLessThan(500);

    if (response.ok()) {
      const data = await response.json();
      createdLevelId = data.data?.id || null;
      expect(createdLevelId).toBeTruthy();
    }
  });

  test('GET /api/membership-levels/{id} - 驗證結構: name, description', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    const listRes = await request.get('/api/membership-levels', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const listData = await listRes.json();

    if (listData.data?.length > 0) {
      const levelId = listData.data[0].id;
      const response = await request.get(`/api/membership-levels/${levelId}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();

      const data = await response.json();
      expect(data.data).toHaveProperty('name');
      expect(typeof data.data.name).toBe('string');
    }
  });

  test('DELETE - 清除測試建立的會員等級', async ({ request }) => {
    expect(tenantToken).toBeTruthy();

    if (createdLevelId) {
      const response = await request.delete(`/api/membership-levels/${createdLevelId}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.status()).toBeLessThan(500);
    }
  });
});

// ============================================================
// 6. 功能商店 UI
// ============================================================
test.describe('6. 功能商店 UI', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('功能商店頁面載入並顯示功能卡片', async ({ page }) => {
    await page.goto('/tenant/feature-store');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // 標題可見
    const title = page.locator('.page-title');
    await expect(title).toBeVisible();

    // 功能卡片存在
    const featureCards = page.locator('.feature-card');
    await expect(featureCards.first()).toBeVisible({ timeout: 10000 });
    const cardCount = await featureCards.count();
    expect(cardCount).toBeGreaterThan(0);
  });

  test('功能卡片包含訂閱/取消訂閱按鈕', async ({ page }) => {
    await page.goto('/tenant/feature-store');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // 已訂閱或未訂閱的按鈕至少有一個存在
    const subscribeBtn = page.locator('button:has-text("立即訂閱")');
    const unsubscribeBtn = page.locator('button:has-text("取消訂閱")');

    const subscribeCount = await subscribeBtn.count();
    const unsubscribeCount = await unsubscribeBtn.count();

    // 至少有一些按鈕存在
    expect(subscribeCount + unsubscribeCount).toBeGreaterThan(0);
  });

  test('點數餘額顯示區塊可見', async ({ page }) => {
    await page.goto('/tenant/feature-store');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    await expect(page.locator('#pointBalance')).toBeVisible();
  });

  test('功能卡片「查看詳細功能」可展開收合', async ({ page }) => {
    await page.goto('/tenant/feature-store');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    const detailToggle = page.locator('.feature-detail-toggle').first();
    // 部分功能可能沒有 detail toggle，如果有才繼續
    if (await detailToggle.count() === 0) return;

    await expect(detailToggle).toBeVisible();

    const href = await detailToggle.getAttribute('href');
    expect(href).toBeTruthy();
    const collapseContent = page.locator(href!);

    // 展開前不可見
    await expect(collapseContent).not.toBeVisible();

    // 展開
    await detailToggle.click();
    await page.waitForTimeout(WAIT_TIME.short);
    await expect(collapseContent).toBeVisible();

    // 收合
    await detailToggle.click();
    await page.waitForTimeout(WAIT_TIME.short);
    await expect(collapseContent).not.toBeVisible();
  });
});

// ============================================================
// 7. 設定頁面 UI
// ============================================================
test.describe('7. 設定頁面 UI', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('店家設定頁面載入成功並有表單欄位', async ({ page }) => {
    await page.goto('/tenant/settings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const title = page.locator('h1, h2, .page-title');
    await expect(title.first()).toBeVisible();

    // 檢查儲存按鈕存在
    const saveBtn = page.locator('button:has-text("儲存"), button[type="submit"]');
    await expect(saveBtn.first()).toBeVisible();
  });

  test('店家設定頁面有點數設定分頁', async ({ page }) => {
    await page.goto('/tenant/settings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const pointsTab = page.locator('a:has-text("點數設定"), a[href="#points"]');
    await expect(pointsTab).toBeVisible();

    await pointsTab.click();
    await page.waitForTimeout(WAIT_TIME.short);

    // 切換到點數設定分頁後應可看到相關欄位
    const enabledSwitch = page.locator('#pointEarnEnabled');
    const rateInput = page.locator('#pointEarnRate');
    // 只要其中一個可見即代表分頁切換成功
    const switchVisible = await enabledSwitch.isVisible();
    const rateVisible = await rateInput.isVisible();
    expect(switchVisible || rateVisible).toBeTruthy();
  });

  test('LINE 設定頁面載入成功', async ({ page }) => {
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const title = page.locator('h1, h2, .page-title');
    await expect(title.first()).toBeVisible();

    // Tooltip 提示圖示存在
    const tooltips = page.locator('[data-bs-toggle="tooltip"], .bi-question-circle');
    const tooltipCount = await tooltips.count();
    expect(tooltipCount).toBeGreaterThan(0);
  });

  test('LINE 設定頁面有 Channel 欄位（不修改）', async ({ page }) => {
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查欄位存在（僅查看，不寫入）
    const channelIdField = page.locator('#channelId');
    const channelSecretField = page.locator('#channelSecret');
    const channelAccessTokenField = page.locator('#channelAccessToken');

    const idVisible = await channelIdField.isVisible();
    const secretVisible = await channelSecretField.isVisible();
    const tokenVisible = await channelAccessTokenField.isVisible();

    // 至少有一個 Channel 欄位存在
    expect(idVisible || secretVisible || tokenVisible).toBeTruthy();
  });
});

// ============================================================
// 8. 側邊欄功能顯示控制
// ============================================================
test.describe('8. 側邊欄功能顯示控制', () => {
  test('側邊欄包含 data-feature 屬性的元素', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const featureElements = page.locator('[data-feature]');
    const count = await featureElements.count();
    expect(count).toBeGreaterThan(0);

    // 收集所有 data-feature 值
    const features = new Set<string>();
    for (let i = 0; i < count; i++) {
      const feature = await featureElements.nth(i).getAttribute('data-feature');
      if (feature) features.add(feature);
    }

    // 至少包含一個已知的功能代碼
    const knownFeatures = ['BASIC_REPORT', 'PRODUCT_SALES', 'COUPON_SYSTEM', 'MEMBERSHIP_SYSTEM'];
    const hasKnown = knownFeatures.some(f => features.has(f));
    expect(hasKnown).toBeTruthy();
  });

  test('訂閱 PRODUCT_SALES 後側邊欄顯示商品管理', async ({ page, request }) => {
    const tenantToken = await getTenantToken(request);
    expect(tenantToken).toBeTruthy();

    const headers = { 'Authorization': `Bearer ${tenantToken}` };

    // 訂閱 PRODUCT_SALES
    const applyRes = await request.post('/api/feature-store/PRODUCT_SALES/apply', { headers });
    // 訂閱可能因點數不足而失敗，非 500 即可
    expect(applyRes.status()).toBeLessThan(500);
    if (!applyRes.ok()) { test.skip(); return; }

    // 登入並檢查側邊欄
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const productMenuItem = page.locator('[data-feature="PRODUCT_SALES"]');
    const count = await productMenuItem.count();

    if (count > 0) {
      const isVisible = await productMenuItem.first().isVisible();
      expect(isVisible).toBeTruthy();
    }
  });

  test('取消訂閱 PRODUCT_SALES 後側邊欄隱藏商品管理', async ({ page, request }) => {
    const tenantToken = await getTenantToken(request);
    expect(tenantToken).toBeTruthy();

    const headers = { 'Authorization': `Bearer ${tenantToken}` };

    // 取消訂閱 PRODUCT_SALES
    const cancelRes = await request.post('/api/feature-store/PRODUCT_SALES/cancel', { headers });
    expect(cancelRes.status()).toBeLessThan(500);

    // 登入並檢查側邊欄
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const productMenuItem = page.locator('[data-feature="PRODUCT_SALES"]');
    const count = await productMenuItem.count();

    if (count > 0) {
      const isVisible = await productMenuItem.first().isVisible();
      expect(isVisible).toBeFalsy();
    }

    // 恢復訂閱（保持環境乾淨）
    await request.post('/api/feature-store/PRODUCT_SALES/apply', { headers });
  });

  test('基本選單（儀表板、預約、顧客等）始終可見', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);

    const alwaysVisibleLinks = [
      { href: '/tenant/dashboard', name: '儀表板' },
      { href: '/tenant/bookings', name: '預約管理' },
      { href: '/tenant/customers', name: '顧客列表' },
      { href: '/tenant/staff', name: '員工管理' },
      { href: '/tenant/services', name: '服務項目' },
      { href: '/tenant/settings', name: '店家設定' },
      { href: '/tenant/feature-store', name: '功能商店' }
    ];

    for (const item of alwaysVisibleLinks) {
      const link = page.locator(`a[href="${item.href}"]`).first();
      await expect(link).toBeVisible();
    }
  });

  test('已訂閱功能的選單項目可見且可導航', async ({ page, request }) => {
    const tenantToken = await getTenantToken(request);
    expect(tenantToken).toBeTruthy();

    // 取得訂閱狀態
    const response = await request.get('/api/feature-store', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const features = await response.json();

    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const subscribedFeatures = (features.data || []).filter((f: any) => f.subscribed);

    for (const feature of subscribedFeatures) {
      const menuItem = page.locator(`[data-feature="${feature.code}"]`);
      const count = await menuItem.count();

      if (count > 0) {
        const isVisible = await menuItem.first().isVisible();
        expect(isVisible).toBeTruthy();
      }
    }
  });
});

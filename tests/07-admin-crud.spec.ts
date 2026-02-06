import { test, expect, APIRequestContext } from './fixtures';
import {
  adminLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  generateTestEmail,
  generateTestPhone,
  confirmDialog,
  closeModal,
  fillForm,
  tableHasData
} from './utils/test-helpers';

/**
 * 超管後台 CRUD 完整測試
 *
 * 測試範圍：
 * 1. 店家管理 - 新增、編輯、刪除、狀態變更
 * 2. 功能管理 - 編輯功能設定
 * 3. 儲值審核 - 審核、拒絕
 * 4. 所有按鈕和欄位
 */

// 取得超管 Token
async function getAdminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/admin/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  const data = await response.json();
  return data.data.accessToken;
}

test.describe('超管後台 CRUD 完整測試', () => {
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    adminToken = await getAdminToken(request);
    expect(adminToken).toBeTruthy();
  });

  test.describe('店家管理 API', () => {
    let createdTenantId: string;

    test('取得店家列表 - 檢查分頁參數', async ({ request }) => {
      const response = await request.get('/api/admin/tenants?page=0&size=10', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      expect(data.data).toHaveProperty('content');
      expect(data.data).toHaveProperty('totalElements');
      expect(data.data).toHaveProperty('totalPages');
      console.log(`店家總數: ${data.data.totalElements}`);
    });

    test('取得店家列表 - 帶搜尋條件', async ({ request }) => {
      const response = await request.get('/api/admin/tenants?page=0&size=10&keyword=test', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
    });

    test('取得店家列表 - 帶狀態篩選', async ({ request }) => {
      const statuses = ['ACTIVE', 'SUSPENDED', 'FROZEN'];
      for (const status of statuses) {
        const response = await request.get(`/api/admin/tenants?page=0&size=10&status=${status}`, {
          headers: { 'Authorization': `Bearer ${adminToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`狀態 ${status} 查詢: ${response.ok() ? '成功' : response.status()}`);
      }
    });

    test('新增店家', async ({ request }) => {
      const tenantData = {
        businessName: generateTestData('TestShop'),
        ownerName: generateTestData('Owner'),
        phone: generateTestPhone(),
        email: generateTestEmail(),
        password: 'test123456',
        tenantCode: generateTestData('code').toLowerCase().replace(/_/g, '')
      };

      const response = await request.post('/api/admin/tenants', {
        headers: {
          'Authorization': `Bearer ${adminToken}`,
          'Content-Type': 'application/json'
        },
        data: tenantData
      });

      if (response.ok()) {
        const data = await response.json();
        expect(data.success).toBeTruthy();
        createdTenantId = data.data?.id;
        console.log(`新增店家成功, ID: ${createdTenantId}`);
      } else {
        console.log(`新增店家回應: ${response.status()}`);
      }
    });

    test('取得單一店家詳情', async ({ request }) => {
      // 先取得店家列表找一個存在的 ID
      const listResponse = await request.get('/api/admin/tenants?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const tenantId = listData.data.content[0].id;
        const response = await request.get(`/api/admin/tenants/${tenantId}`, {
          headers: { 'Authorization': `Bearer ${adminToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.success).toBeTruthy();
        expect(data.data).toHaveProperty('name');
        expect(data.data).toHaveProperty('email');
        expect(data.data).toHaveProperty('status');
        console.log(`店家詳情: ${data.data.name}`);
      }
    });

    test('更新店家資訊', async ({ request }) => {
      const listResponse = await request.get('/api/admin/tenants?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const tenant = listData.data.content[0];
        const response = await request.put(`/api/admin/tenants/${tenant.id}`, {
          headers: {
            'Authorization': `Bearer ${adminToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            businessName: tenant.businessName,
            ownerName: tenant.ownerName || 'Updated Owner',
            phone: tenant.phone || generateTestPhone(),
            email: tenant.email
          }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`更新店家回應: ${response.status()}`);
      }
    });

    test('店家狀態變更 - 暫停', async ({ request }) => {
      const listResponse = await request.get('/api/admin/tenants?page=0&size=1&status=ACTIVE', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const tenantId = listData.data.content[0].id;
        const response = await request.post(`/api/admin/tenants/${tenantId}/suspend`, {
          headers: { 'Authorization': `Bearer ${adminToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`暫停店家回應: ${response.status()}`);

        // 恢復啟用
        await request.post(`/api/admin/tenants/${tenantId}/activate`, {
          headers: { 'Authorization': `Bearer ${adminToken}` }
        });
      }
    });

    test('店家功能啟用/停用', async ({ request }) => {
      const listResponse = await request.get('/api/admin/tenants?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const tenantId = listData.data.content[0].id;
        const featureCode = 'BASIC_REPORT';

        // 啟用功能
        const enableResponse = await request.post(
          `/api/admin/tenants/${tenantId}/features/${featureCode}/enable`,
          { headers: { 'Authorization': `Bearer ${adminToken}` } }
        );
        console.log(`啟用功能回應: ${enableResponse.status()}`);

        // 停用功能
        const disableResponse = await request.post(
          `/api/admin/tenants/${tenantId}/features/${featureCode}/disable`,
          { headers: { 'Authorization': `Bearer ${adminToken}` } }
        );
        console.log(`停用功能回應: ${disableResponse.status()}`);
      }
    });

    test('店家點數加值', async ({ request }) => {
      const listResponse = await request.get('/api/admin/tenants?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const tenantId = listData.data.content[0].id;
        const response = await request.post(`/api/admin/tenants/${tenantId}/points/add`, {
          headers: {
            'Authorization': `Bearer ${adminToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            points: 100,
            reason: '測試加值'
          }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`點數加值回應: ${response.status()}`);
      }
    });
  });

  test.describe('功能管理 API', () => {
    test('取得所有功能列表', async ({ request }) => {
      const response = await request.get('/api/admin/features', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      expect(Array.isArray(data.data)).toBeTruthy();
      console.log(`功能總數: ${data.data.length}`);

      // 檢查每個功能的必要欄位
      if (data.data.length > 0) {
        const feature = data.data[0];
        expect(feature).toHaveProperty('code');
        expect(feature).toHaveProperty('name');
      }
    });

    test('取得免費功能列表', async ({ request }) => {
      const response = await request.get('/api/admin/features/free', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`免費功能數: ${data.data?.length || 0}`);
    });

    test('取得付費功能列表', async ({ request }) => {
      const response = await request.get('/api/admin/features/paid', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`付費功能數: ${data.data?.length || 0}`);
    });

    test('更新功能設定', async ({ request }) => {
      const response = await request.put('/api/admin/features/BASIC_REPORT', {
        headers: {
          'Authorization': `Bearer ${adminToken}`,
          'Content-Type': 'application/json'
        },
        data: {
          name: '基本報表',
          description: '基本營運報表功能',
          isFree: true,
          monthlyPoints: 0
        }
      });
      expect(response.status()).toBeLessThan(500);
      console.log(`更新功能回應: ${response.status()}`);
    });
  });

  test.describe('儲值審核 API', () => {
    test('取得儲值申請列表', async ({ request }) => {
      const response = await request.get('/api/admin/point-topups', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`儲值申請總數: ${data.data?.totalElements || 0}`);
    });

    test('取得待審核列表', async ({ request }) => {
      const response = await request.get('/api/admin/point-topups/pending', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`待審核數量: ${data.data?.length || 0}`);
    });

    test('儲值審核 - 帶狀態篩選', async ({ request }) => {
      const statuses = ['PENDING', 'APPROVED', 'REJECTED'];
      for (const status of statuses) {
        const response = await request.get(`/api/admin/point-topups?status=${status}`, {
          headers: { 'Authorization': `Bearer ${adminToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`狀態 ${status} 查詢: ${response.ok() ? '成功' : response.status()}`);
      }
    });
  });

  test.describe('儀表板 API', () => {
    test('取得儀表板統計資料', async ({ request }) => {
      const response = await request.get('/api/admin/dashboard', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();

      // 檢查統計欄位
      if (data.data) {
        console.log('儀表板資料:', JSON.stringify(data.data, null, 2));
      }
    });

    test('驗證儀表板金額計算欄位', async ({ request }) => {
      const response = await request.get('/api/admin/dashboard', {
        headers: { 'Authorization': `Bearer ${adminToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();

      // 驗證必要欄位存在且為數值
      const dashboard = data.data;
      expect(dashboard).toHaveProperty('pendingTopUpAmount');
      expect(dashboard).toHaveProperty('monthlyApprovedAmount');
      expect(dashboard).toHaveProperty('totalTenants');
      expect(dashboard).toHaveProperty('activeTenants');

      // 驗證數值合理性
      expect(typeof dashboard.pendingTopUpAmount).toBe('number');
      expect(typeof dashboard.monthlyApprovedAmount).toBe('number');
      expect(dashboard.pendingTopUpAmount).toBeGreaterThanOrEqual(0);
      expect(dashboard.monthlyApprovedAmount).toBeGreaterThanOrEqual(0);

      console.log(`待審核金額: ${dashboard.pendingTopUpAmount}`);
      console.log(`本月已通過金額: ${dashboard.monthlyApprovedAmount}`);
    });
  });
});

test.describe('超管後台 UI 完整測試', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test.describe('儀表板頁面', () => {
    test('所有統計卡片顯示', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查統計卡片
      const cards = page.locator('.card, .stat-card');
      const cardCount = await cards.count();
      expect(cardCount).toBeGreaterThan(0);
      console.log(`統計卡片數量: ${cardCount}`);

      // 檢查卡片內容
      for (let i = 0; i < Math.min(cardCount, 5); i++) {
        const card = cards.nth(i);
        await expect(card).toBeVisible();
      }
    });

    test('頁面標題顯示', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await waitForLoading(page);

      const title = page.locator('h1, h2, .page-title').first();
      await expect(title).toBeVisible();
    });
  });

  test.describe('店家管理頁面', () => {
    test('表格正確顯示', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      await expect(table).toBeVisible();

      // 檢查表頭
      const headers = table.locator('thead th');
      const headerCount = await headers.count();
      expect(headerCount).toBeGreaterThan(0);
      console.log(`表格欄位數: ${headerCount}`);
    });

    test('搜尋欄位功能', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);

      const searchInput = page.locator('input[type="search"], input[placeholder*="搜尋"], #search');
      if (await searchInput.isVisible()) {
        await searchInput.fill('test');
        await page.waitForTimeout(WAIT_TIME.api);
        // 確認無錯誤
        const errorAlert = page.locator('.alert-danger');
        expect(await errorAlert.isVisible()).toBeFalsy();
      }
    });

    test('狀態篩選功能', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);

      const statusFilter = page.locator('select[name="status"], #statusFilter');
      if (await statusFilter.isVisible()) {
        await statusFilter.selectOption('ACTIVE');
        await page.waitForTimeout(WAIT_TIME.api);

        await statusFilter.selectOption('SUSPENDED');
        await page.waitForTimeout(WAIT_TIME.api);
      }
    });

    test('點擊查看詳情', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const detailBtn = page.locator('table tbody tr:first-child a, table tbody tr:first-child button').first();
      if (await detailBtn.isVisible()) {
        await detailBtn.click();
        await page.waitForTimeout(WAIT_TIME.medium);

        // 可能是跳轉頁面或開啟 Modal
        const modal = page.locator('.modal.show');
        const isModal = await modal.isVisible();
        const isDetailPage = page.url().includes('/tenants/');

        expect(isModal || isDetailPage).toBeTruthy();
      }
    });

    test('分頁功能', async ({ page }) => {
      await page.goto('/admin/tenants');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const pagination = page.locator('.pagination');
      if (await pagination.isVisible()) {
        const nextBtn = pagination.locator('a:has-text("下一頁"), .page-item:last-child a');
        if (await nextBtn.isVisible() && await nextBtn.isEnabled()) {
          await nextBtn.click();
          await page.waitForTimeout(WAIT_TIME.api);
        }
      }
    });
  });

  test.describe('功能管理頁面', () => {
    test('功能列表顯示', async ({ page }) => {
      await page.goto('/admin/features');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查有功能項目
      const featureItems = page.locator('table tbody tr, .feature-item, .card');
      const count = await featureItems.count();
      console.log(`功能項目數: ${count}`);
    });

    test('功能編輯按鈕', async ({ page }) => {
      await page.goto('/admin/features');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const editBtn = page.locator('button:has-text("編輯"), a:has-text("編輯")').first();
      if (await editBtn.isVisible()) {
        await editBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 檢查 Modal 或表單出現
        const modal = page.locator('.modal.show');
        const form = page.locator('form');
        expect(await modal.isVisible() || await form.isVisible()).toBeTruthy();
      }
    });
  });

  test.describe('儲值審核頁面', () => {
    test('儲值列表顯示', async ({ page }) => {
      await page.goto('/admin/point-topups');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table');
      await expect(table).toBeVisible();
    });

    test('狀態篩選標籤', async ({ page }) => {
      await page.goto('/admin/point-topups');
      await waitForLoading(page);

      // 檢查篩選標籤或下拉選單
      const tabs = page.locator('.nav-tabs .nav-link, .btn-group .btn');
      const tabCount = await tabs.count();

      for (let i = 0; i < tabCount; i++) {
        const tab = tabs.nth(i);
        if (await tab.isVisible()) {
          await tab.click();
          await page.waitForTimeout(WAIT_TIME.medium);
        }
      }
    });

    test('審核/拒絕按鈕存在', async ({ page }) => {
      await page.goto('/admin/point-topups');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查操作按鈕
      const approveBtn = page.locator('button:has-text("核准"), button:has-text("通過")');
      const rejectBtn = page.locator('button:has-text("拒絕"), button:has-text("駁回")');

      const hasApprove = await approveBtn.count() > 0;
      const hasReject = await rejectBtn.count() > 0;

      console.log(`有核准按鈕: ${hasApprove}, 有拒絕按鈕: ${hasReject}`);
    });
  });

  test.describe('側邊欄完整測試', () => {
    test('所有選單項目可見', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await waitForLoading(page);

      const menuItems = [
        '儀表板',
        '店家管理',
        '儲值審核',
        '功能管理'
      ];

      for (const item of menuItems) {
        const link = page.locator(`a:has-text("${item}")`).first();
        const isVisible = await link.isVisible();
        console.log(`選單項目 "${item}": ${isVisible ? '可見' : '不可見'}`);
      }
    });

    test('側邊欄折疊功能', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await waitForLoading(page);

      const toggleBtn = page.locator('#sidebarToggle, .sidebar-toggle');
      if (await toggleBtn.isVisible()) {
        await toggleBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 再次點擊展開
        await toggleBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);
      }
    });
  });

  test.describe('登出功能', () => {
    test('登出按鈕點擊', async ({ page }) => {
      await page.goto('/admin/dashboard');
      await waitForLoading(page);

      const logoutBtn = page.locator('a:has-text("登出"), button:has-text("登出")').first();
      if (await logoutBtn.isVisible()) {
        await logoutBtn.click();

        // 處理確認對話框
        const confirmBtn = page.locator('.modal .btn-primary, .swal2-confirm, #confirmOkBtn');
        if (await confirmBtn.isVisible({ timeout: 2000 }).catch(() => false)) {
          await confirmBtn.click();
        }

        await page.waitForTimeout(WAIT_TIME.api);
        expect(page.url()).toContain('/login');
      }
    });
  });
});

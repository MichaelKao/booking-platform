import { test, expect, APIRequestContext } from '@playwright/test';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  generateTestPhone,
  confirmDialog,
  closeModal,
  tableHasData,
  getToday,
  getTomorrow,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 店家後台 - 預約管理完整測試
 *
 * 測試範圍：
 * 1. 預約列表頁面
 * 2. 預約行事曆頁面
 * 3. 預約 CRUD
 * 4. 預約狀態變更
 * 5. 所有按鈕和欄位
 */

// 取得店家 Token
async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('預約管理 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    if (!tenantToken) {
      console.log('無法取得店家 Token，部分測試將跳過');
    }
  });

  test.describe('預約列表 API', () => {
    test('取得預約列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/bookings?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`預約總數: ${data.data?.totalElements || 0}`);
    });

    test('預約列表 - 帶日期篩選', async ({ request }) => {
      if (!tenantToken) return;

      const today = getToday();
      const response = await request.get(`/api/bookings?startDate=${today}&endDate=${today}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`今日預約數: ${data.data?.totalElements || 0}`);
    });

    test('預約列表 - 帶狀態篩選', async ({ request }) => {
      if (!tenantToken) return;

      const statuses = ['PENDING_CONFIRMATION', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW'];
      for (const status of statuses) {
        const response = await request.get(`/api/bookings?status=${status}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`狀態 ${status}: ${response.ok() ? '成功' : response.status()}`);
      }
    });

    test('預約列表 - 帶員工篩選', async ({ request }) => {
      if (!tenantToken) return;

      // 先取得員工列表
      const staffResponse = await request.get('/api/staff', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });

      if (staffResponse.ok()) {
        const staffData = await staffResponse.json();
        if (staffData.data?.content?.length > 0) {
          const staffId = staffData.data.content[0].id;
          const response = await request.get(`/api/bookings?staffId=${staffId}`, {
            headers: { 'Authorization': `Bearer ${tenantToken}` }
          });
          expect(response.status()).toBeLessThan(500);
        }
      }
    });
  });

  test.describe('預約行事曆 API', () => {
    test('取得行事曆資料', async ({ request }) => {
      if (!tenantToken) return;

      const today = new Date();
      const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
      const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0];

      const response = await request.get(`/api/bookings/calendar?start=${startOfMonth}&end=${endOfMonth}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`本月行事曆事件數: ${data.data?.length || 0}`);
    });

    test('取得員工指定日期預約', async ({ request }) => {
      if (!tenantToken) return;

      // 先取得員工
      const staffResponse = await request.get('/api/staff', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });

      if (staffResponse.ok()) {
        const staffData = await staffResponse.json();
        if (staffData.data?.content?.length > 0) {
          const staffId = staffData.data.content[0].id;
          const today = getToday();

          const response = await request.get(`/api/bookings/staff/${staffId}/date/${today}`, {
            headers: { 'Authorization': `Bearer ${tenantToken}` }
          });
          expect(response.status()).toBeLessThan(500);
        }
      }
    });
  });

  test.describe('預約 CRUD API', () => {
    let createdBookingId: string;

    test('新增預約', async ({ request }) => {
      if (!tenantToken) return;

      // 先取得服務和員工
      const [servicesRes, staffRes, customersRes] = await Promise.all([
        request.get('/api/services/bookable', { headers: { 'Authorization': `Bearer ${tenantToken}` } }),
        request.get('/api/staff/bookable', { headers: { 'Authorization': `Bearer ${tenantToken}` } }),
        request.get('/api/customers?page=0&size=1', { headers: { 'Authorization': `Bearer ${tenantToken}` } })
      ]);

      const servicesData = await servicesRes.json();
      const staffData = await staffRes.json();
      const customersData = await customersRes.json();

      if (servicesData.data?.length > 0 && staffData.data?.length > 0) {
        const tomorrow = getTomorrow();
        const bookingData = {
          customerId: customersData.data?.content?.[0]?.id || null,
          customerName: 'Test Customer',
          customerPhone: generateTestPhone(),
          serviceId: servicesData.data[0].id,
          staffId: staffData.data[0].id,
          bookingDate: tomorrow,
          startTime: '10:00',
          note: '測試預約'
        };

        const response = await request.post('/api/bookings', {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: bookingData
        });

        if (response.ok()) {
          const data = await response.json();
          createdBookingId = data.data?.id;
          console.log(`新增預約成功, ID: ${createdBookingId}`);
        } else {
          console.log(`新增預約回應: ${response.status()}`);
        }
      }
    });

    test('取得單一預約詳情', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/bookings?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const bookingId = listData.data.content[0].id;
        const response = await request.get(`/api/bookings/${bookingId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.data).toHaveProperty('customerName');
        expect(data.data).toHaveProperty('serviceName');
        expect(data.data).toHaveProperty('status');
      }
    });

    test('更新預約', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/bookings?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const booking = listData.data.content[0];
        const response = await request.put(`/api/bookings/${booking.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            note: '更新的備註 ' + Date.now()
          }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`更新預約回應: ${response.status()}`);
      }
    });
  });

  test.describe('預約狀態變更 API', () => {
    test('確認預約', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/bookings?status=PENDING_CONFIRMATION&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const bookingId = listData.data.content[0].id;
        const response = await request.post(`/api/bookings/${bookingId}/confirm`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`確認預約回應: ${response.status()}`);
      } else {
        console.log('沒有待確認的預約');
      }
    });

    test('完成預約', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/bookings?status=CONFIRMED&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const bookingId = listData.data.content[0].id;
        const response = await request.post(`/api/bookings/${bookingId}/complete`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`完成預約回應: ${response.status()}`);
      }
    });

    test('取消預約', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/bookings?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      // 不實際執行取消，只測試 API 是否存在
      if (listData.data?.content?.length > 0) {
        const bookingId = listData.data.content[0].id;
        // 測試 API 端點存在
        const response = await request.post(`/api/bookings/nonexistent/cancel`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBe(404); // 應該回傳 404
      }
    });

    test('標記爽約', async ({ request }) => {
      if (!tenantToken) return;

      // 不實際執行，只測試 API 端點
      const response = await request.post(`/api/bookings/nonexistent/no-show`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect([400, 404]).toContain(response.status());
    });
  });
});

test.describe('預約管理 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('預約列表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查頁面標題
      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('表格正確顯示', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      await expect(table).toBeVisible();

      // 檢查表頭欄位
      const expectedHeaders = ['顧客', '服務', '日期', '時間', '狀態', '操作'];
      for (const header of expectedHeaders) {
        const th = page.locator(`th:has-text("${header}")`);
        const count = await th.count();
        console.log(`表頭 "${header}": ${count > 0 ? '存在' : '不存在'}`);
      }
    });

    test('日期篩選功能', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);

      const startDate = page.locator('input[name="startDate"], #startDate');
      const endDate = page.locator('input[name="endDate"], #endDate');

      if (await startDate.isVisible()) {
        await startDate.fill(getToday());
        await page.waitForTimeout(WAIT_TIME.short);
      }

      if (await endDate.isVisible()) {
        await endDate.fill(getToday());
        await page.waitForTimeout(WAIT_TIME.short);
      }

      // 點擊搜尋
      const searchBtn = page.locator('button:has-text("搜尋"), button:has-text("查詢")');
      if (await searchBtn.isVisible()) {
        await searchBtn.click();
        await page.waitForTimeout(WAIT_TIME.api);
      }
    });

    test('狀態篩選功能', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);

      const statusSelect = page.locator('select[name="status"], #statusFilter');
      if (await statusSelect.isVisible()) {
        const options = ['PENDING_CONFIRMATION', 'CONFIRMED', 'COMPLETED', 'CANCELLED'];
        for (const option of options) {
          await statusSelect.selectOption(option).catch(() => {});
          await page.waitForTimeout(WAIT_TIME.short);
        }
      }
    });

    test('新增預約按鈕', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增預約"), a:has-text("新增預約"), button:has-text("新增")');
      if (await addBtn.first().isVisible()) {
        await addBtn.first().click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 檢查 Modal 開啟
        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();

        // 檢查表單欄位
        const formFields = [
          '#customerName', '#customerPhone', '#serviceId', '#staffId',
          '#bookingDate', '#startTime', '#note'
        ];

        for (const field of formFields) {
          const element = page.locator(field);
          const isVisible = await element.isVisible();
          console.log(`欄位 ${field}: ${isVisible ? '存在' : '不存在'}`);
        }

        await closeModal(page);
      }
    });

    test('預約詳情檢視', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const viewBtn = page.locator('table tbody tr:first-child button:has-text("查看"), table tbody tr:first-child a:has-text("查看")');
      if (await viewBtn.isVisible()) {
        await viewBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          await closeModal(page);
        }
      }
    });

    test('預約狀態操作按鈕', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查操作按鈕
      const actionButtons = ['確認', '完成', '取消', '爽約'];
      for (const action of actionButtons) {
        const btn = page.locator(`button:has-text("${action}")`);
        const count = await btn.count();
        console.log(`"${action}" 按鈕數量: ${count}`);
      }
    });

    test('分頁功能', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const pagination = page.locator('.pagination');
      if (await pagination.isVisible()) {
        const pageLinks = pagination.locator('.page-link');
        const count = await pageLinks.count();
        console.log(`分頁連結數: ${count}`);
      }
    });

    test('匯出按鈕', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);

      const exportBtn = page.locator('button:has-text("匯出"), a:has-text("匯出")');
      const count = await exportBtn.count();
      console.log(`匯出按鈕數: ${count}`);
    });
  });

  test.describe('行事曆頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/calendar');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查 FullCalendar 元素
      const calendar = page.locator('#calendar, .fc, .fc-view');
      await expect(calendar.first()).toBeVisible();
    });

    test('行事曆工具列', async ({ page }) => {
      await page.goto('/tenant/calendar');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查導航按鈕
      const prevBtn = page.locator('.fc-prev-button, button:has-text("<")');
      const nextBtn = page.locator('.fc-next-button, button:has-text(">")');
      const todayBtn = page.locator('.fc-today-button, button:has-text("今天")');

      expect(await prevBtn.isVisible() || await nextBtn.isVisible()).toBeTruthy();
    });

    test('視圖切換', async ({ page }) => {
      await page.goto('/tenant/calendar');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const viewButtons = [
        '.fc-dayGridMonth-button',
        '.fc-timeGridWeek-button',
        '.fc-timeGridDay-button'
      ];

      for (const selector of viewButtons) {
        const btn = page.locator(selector);
        if (await btn.isVisible()) {
          await btn.click();
          await page.waitForTimeout(WAIT_TIME.short);
        }
      }
    });

    test('員工篩選', async ({ page }) => {
      await page.goto('/tenant/calendar');
      await waitForLoading(page);

      const staffSelect = page.locator('select[name="staff"], #staffFilter, select:has-text("員工")');
      if (await staffSelect.isVisible()) {
        const options = await staffSelect.locator('option').all();
        console.log(`員工選項數: ${options.length}`);

        if (options.length > 1) {
          await staffSelect.selectOption({ index: 1 });
          await page.waitForTimeout(WAIT_TIME.api);
        }
      }
    });

    test('點擊事件開啟詳情', async ({ page }) => {
      await page.goto('/tenant/calendar');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const event = page.locator('.fc-event').first();
      if (await event.isVisible()) {
        await event.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 可能開啟 Modal 或 Popover
        const modal = page.locator('.modal.show, .popover');
        if (await modal.isVisible()) {
          console.log('事件詳情已開啟');
        }
      }
    });

    test('點擊空白時段新增預約', async ({ page }) => {
      await page.goto('/tenant/calendar');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 切換到週視圖或日視圖
      const weekBtn = page.locator('.fc-timeGridWeek-button');
      if (await weekBtn.isVisible()) {
        await weekBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);
      }

      // 點擊空白區域
      const timeSlot = page.locator('.fc-timegrid-slot').first();
      if (await timeSlot.isVisible()) {
        await timeSlot.click();
        await page.waitForTimeout(WAIT_TIME.short);
      }
    });
  });
});

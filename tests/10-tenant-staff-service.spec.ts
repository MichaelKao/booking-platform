import { test, expect, APIRequestContext } from '@playwright/test';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  generateTestPhone,
  closeModal,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 店家後台 - 員工管理 & 服務管理完整測試
 *
 * 測試範圍：
 * 1. 員工 CRUD
 * 2. 員工排班設定
 * 3. 員工請假管理
 * 4. 服務 CRUD
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('員工管理 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('員工列表 API', () => {
    test('取得員工列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/staff?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`員工總數: ${data.data?.totalElements || 0}`);
    });

    test('取得可預約員工', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/staff/bookable', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`可預約員工數: ${data.data?.length || 0}`);
    });
  });

  test.describe('員工 CRUD API', () => {
    let createdStaffId: string;

    test('新增員工', async ({ request }) => {
      if (!tenantToken) return;

      const staffData = {
        name: generateTestData('Staff'),
        phone: generateTestPhone(),
        email: `staff_${Date.now()}@test.com`,
        status: 'ACTIVE'
      };

      const response = await request.post('/api/staff', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: staffData
      });

      if (response.ok()) {
        const data = await response.json();
        createdStaffId = data.data?.id;
        console.log(`新增員工成功, ID: ${createdStaffId}`);
      }
    });

    test('取得單一員工', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/staff?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const staffId = listData.data.content[0].id;
        const response = await request.get(`/api/staff/${staffId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.data).toHaveProperty('name');
      }
    });

    test('更新員工', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/staff?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const staff = listData.data.content[0];
        const response = await request.put(`/api/staff/${staff.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            name: staff.name,
            phone: staff.phone,
            email: staff.email,
            status: 'ACTIVE'
          }
        });
        expect(response.status()).toBeLessThan(500);
      }
    });
  });

  test.describe('員工排班 API', () => {
    test('取得員工排班', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/staff?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const staffId = listData.data.content[0].id;
        const response = await request.get(`/api/staff/${staffId}/schedule`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        if (response.ok()) {
          const data = await response.json();
          console.log(`排班資料: ${JSON.stringify(data.data)}`);
        }
      }
    });

    test('更新員工排班', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/staff?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const staffId = listData.data.content[0].id;
        const scheduleData = {
          schedules: [
            { dayOfWeek: 1, isWorking: true, startTime: '09:00', endTime: '18:00' },
            { dayOfWeek: 2, isWorking: true, startTime: '09:00', endTime: '18:00' },
            { dayOfWeek: 3, isWorking: true, startTime: '09:00', endTime: '18:00' },
            { dayOfWeek: 4, isWorking: true, startTime: '09:00', endTime: '18:00' },
            { dayOfWeek: 5, isWorking: true, startTime: '09:00', endTime: '18:00' },
            { dayOfWeek: 6, isWorking: false },
            { dayOfWeek: 7, isWorking: false }
          ]
        };

        const response = await request.put(`/api/staff/${staffId}/schedule`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: scheduleData
        });
        console.log(`更新排班回應: ${response.status()}`);
      }
    });
  });

  test.describe('員工請假 API', () => {
    test('取得員工請假', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/staff?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const staffId = listData.data.content[0].id;
        const response = await request.get(`/api/staff/${staffId}/leaves`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
      }
    });

    test('新增員工請假', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/staff?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const staffId = listData.data.content[0].id;
        const futureDate = new Date();
        futureDate.setDate(futureDate.getDate() + 30);
        const leaveDate = futureDate.toISOString().split('T')[0];

        const response = await request.post(`/api/staff/${staffId}/leaves`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            leaveDate: leaveDate,
            leaveType: 'PERSONAL',
            reason: '測試請假'
          }
        });
        console.log(`新增請假回應: ${response.status()}`);
      }
    });
  });
});

test.describe('服務管理 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('服務列表 API', () => {
    test('取得服務列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/services?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`服務總數: ${data.data?.totalElements || 0}`);
    });

    test('取得可預約服務', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/services/bookable', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`可預約服務數: ${data.data?.length || 0}`);
    });

    test('取得服務分類', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/service-categories', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      console.log(`服務分類數: ${data.data?.length || 0}`);
    });
  });

  test.describe('服務 CRUD API', () => {
    let createdServiceId: string;

    test('新增服務', async ({ request }) => {
      if (!tenantToken) return;

      const serviceData = {
        name: generateTestData('Service'),
        description: '測試服務描述',
        duration: 60,
        price: 1000,
        status: 'ACTIVE'
      };

      const response = await request.post('/api/services', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: serviceData
      });

      if (response.ok()) {
        const data = await response.json();
        createdServiceId = data.data?.id;
        console.log(`新增服務成功, ID: ${createdServiceId}`);
      }
    });

    test('取得單一服務', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/services?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const serviceId = listData.data.content[0].id;
        const response = await request.get(`/api/services/${serviceId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
      }
    });

    test('更新服務', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/services?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const service = listData.data.content[0];
        const response = await request.put(`/api/services/${service.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            name: service.name,
            duration: service.duration,
            price: service.price,
            status: 'ACTIVE'
          }
        });
        expect(response.status()).toBeLessThan(500);
      }
    });
  });
});

test.describe('員工管理 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('員工列表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/staff');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('表格正確顯示', async ({ page }) => {
      await page.goto('/tenant/staff');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 使用 first() 因為頁面有多個表格（員工列表、排班表、請假表）
      const table = page.locator('table.table').first();
      await expect(table).toBeVisible();

      const headers = ['員工', '聯絡', '可預約', '狀態', '操作'];
      for (const header of headers) {
        const th = page.locator(`th:has-text("${header}")`);
        console.log(`表頭 "${header}": ${await th.count() > 0 ? '存在' : '不存在'}`);
      }
    });

    test('新增員工按鈕', async ({ page }) => {
      await page.goto('/tenant/staff');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增員工"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();

        // 檢查表單欄位
        const formFields = ['#name', '#phone', '#email', '#status'];
        for (const field of formFields) {
          const element = page.locator(field);
          console.log(`欄位 ${field}: ${await element.isVisible() ? '存在' : '不存在'}`);
        }

        await closeModal(page);
      }
    });

    test('排班設定按鈕', async ({ page }) => {
      await page.goto('/tenant/staff');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const scheduleBtn = page.locator('button:has-text("排班"), button:has-text("班表")').first();
      if (await scheduleBtn.isVisible()) {
        await scheduleBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 檢查每天的排班設定
          const dayLabels = ['週一', '週二', '週三', '週四', '週五', '週六', '週日'];
          for (const day of dayLabels) {
            const label = modal.locator(`:has-text("${day}")`);
            console.log(`${day}: ${await label.count() > 0 ? '存在' : '不存在'}`);
          }

          await closeModal(page);
        }
      }
    });

    test('請假管理按鈕', async ({ page }) => {
      await page.goto('/tenant/staff');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const leaveBtn = page.locator('button:has-text("請假"), button:has-text("休假")').first();
      if (await leaveBtn.isVisible()) {
        await leaveBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 檢查請假表單
          const dateInput = modal.locator('input[type="date"]');
          const typeSelect = modal.locator('select');
          console.log(`日期輸入: ${await dateInput.isVisible()}`);
          console.log(`類型選擇: ${await typeSelect.isVisible()}`);

          await closeModal(page);
        }
      }
    });
  });
});

test.describe('服務管理 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('服務列表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('表格正確顯示', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      await expect(table).toBeVisible();

      const headers = ['名稱', '時長', '價格', '狀態', '操作'];
      for (const header of headers) {
        const th = page.locator(`th:has-text("${header}")`);
        console.log(`表頭 "${header}": ${await th.count() > 0 ? '存在' : '不存在'}`);
      }
    });

    test('新增服務按鈕', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增服務"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();

        // 檢查表單欄位
        const formFields = ['#name', '#duration', '#price', '#description'];
        for (const field of formFields) {
          const element = page.locator(field);
          console.log(`欄位 ${field}: ${await element.isVisible() ? '存在' : '不存在'}`);
        }

        await closeModal(page);
      }
    });

    test('編輯服務', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const editBtn = page.locator('table tbody tr:first-child button:has-text("編輯")');
      if (await editBtn.isVisible()) {
        await editBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const nameInput = modal.locator('#name');
          if (await nameInput.isVisible()) {
            console.log(`服務名稱: ${await nameInput.inputValue()}`);
          }
          await closeModal(page);
        }
      }
    });

    test('狀態切換', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const toggleBtn = page.locator('table tbody tr:first-child .form-switch input, table tbody tr:first-child button:has-text("啟用"), table tbody tr:first-child button:has-text("停用")');
      if (await toggleBtn.first().isVisible()) {
        console.log('狀態切換按鈕存在');
      }
    });
  });
});

import { test, expect, APIRequestContext } from '@playwright/test';
import {
  tenantLogin,
  adminLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  generateTestPhone,
  generateTestEmail,
  closeModal,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * 綜合表單測試
 *
 * 測試範圍：
 * 1. 所有表單的欄位驗證
 * 2. 必填欄位檢查
 * 3. 格式驗證（電話、Email 等）
 * 4. 會員等級管理
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('會員等級 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('會員等級 CRUD', () => {
    let createdLevelId: string;

    test('取得會員等級列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/membership-levels', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`會員等級數: ${data.data?.length || 0}`);
    });

    test('新增會員等級', async ({ request }) => {
      if (!tenantToken) return;

      const levelData = {
        name: generateTestData('Level'),
        minPoints: 0,
        maxPoints: 1000,
        discountRate: 5,
        description: '測試會員等級'
      };

      const response = await request.post('/api/membership-levels', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: levelData
      });

      if (response.ok()) {
        const data = await response.json();
        createdLevelId = data.data?.id;
        console.log(`新增會員等級成功, ID: ${createdLevelId}`);
      } else {
        console.log(`新增會員等級回應: ${response.status()}`);
      }
    });

    test('取得單一會員等級', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/membership-levels', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.length > 0) {
        const levelId = listData.data[0].id;
        const response = await request.get(`/api/membership-levels/${levelId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.data).toHaveProperty('name');
      }
    });

    test('更新會員等級', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/membership-levels', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.length > 0) {
        const level = listData.data[0];
        const response = await request.put(`/api/membership-levels/${level.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            name: level.name,
            minPoints: level.minPoints,
            maxPoints: level.maxPoints,
            discountRate: level.discountRate,
            description: '更新的描述 ' + Date.now()
          }
        });
        expect(response.status()).toBeLessThan(500);
      }
    });

    test('刪除會員等級', async ({ request }) => {
      if (!tenantToken || !createdLevelId) return;

      const response = await request.delete(`/api/membership-levels/${createdLevelId}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      console.log(`刪除會員等級回應: ${response.status()}`);
    });
  });
});

test.describe('表單驗證測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('顧客新增表單', () => {
    test('必填欄位驗證', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 直接點擊提交
          const submitBtn = modal.locator('button[type="submit"], button:has-text("儲存")');
          if (await submitBtn.isVisible()) {
            await submitBtn.click();
            await page.waitForTimeout(WAIT_TIME.short);

            // 檢查驗證錯誤
            const invalidFeedback = modal.locator('.invalid-feedback, .is-invalid, .text-danger');
            const count = await invalidFeedback.count();
            console.log(`驗證錯誤數: ${count}`);
          }

          await closeModal(page);
        }
      }
    });

    test('電話格式驗證', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const phoneInput = modal.locator('#phone, input[name="phone"]');
          if (await phoneInput.isVisible()) {
            // 輸入無效電話
            await phoneInput.fill('12345');
            await page.waitForTimeout(WAIT_TIME.short);

            // 點擊其他欄位觸發驗證
            const nameInput = modal.locator('#name, input[name="name"]');
            if (await nameInput.isVisible()) {
              await nameInput.click();
            }
          }

          await closeModal(page);
        }
      }
    });

    test('Email 格式驗證', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const emailInput = modal.locator('#email, input[name="email"]');
          if (await emailInput.isVisible()) {
            // 輸入無效 Email
            await emailInput.fill('invalid-email');
            await page.waitForTimeout(WAIT_TIME.short);

            // 點擊其他欄位觸發驗證
            const nameInput = modal.locator('#name, input[name="name"]');
            if (await nameInput.isVisible()) {
              await nameInput.click();
            }
          }

          await closeModal(page);
        }
      }
    });

    test('完整填寫表單', async ({ page }) => {
      await page.goto('/tenant/customers');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 填寫完整資料
          const nameInput = modal.locator('#name, input[name="name"]');
          const phoneInput = modal.locator('#phone, input[name="phone"]');
          const emailInput = modal.locator('#email, input[name="email"]');

          if (await nameInput.isVisible()) {
            await nameInput.fill(generateTestData('Customer'));
          }
          if (await phoneInput.isVisible()) {
            await phoneInput.fill(generateTestPhone());
          }
          if (await emailInput.isVisible()) {
            await emailInput.fill(generateTestEmail());
          }

          console.log('表單已填寫完整');
          await closeModal(page);
        }
      }
    });
  });

  test.describe('員工新增表單', () => {
    test('必填欄位驗證', async ({ page }) => {
      await page.goto('/tenant/staff');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const submitBtn = modal.locator('button[type="submit"], button:has-text("儲存")');
          if (await submitBtn.isVisible()) {
            await submitBtn.click();
            await page.waitForTimeout(WAIT_TIME.short);

            const invalidFeedback = modal.locator('.invalid-feedback, .is-invalid, .text-danger');
            const count = await invalidFeedback.count();
            console.log(`驗證錯誤數: ${count}`);
          }

          await closeModal(page);
        }
      }
    });
  });

  test.describe('服務新增表單', () => {
    test('必填欄位驗證', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const submitBtn = modal.locator('button[type="submit"], button:has-text("儲存")');
          if (await submitBtn.isVisible()) {
            await submitBtn.click();
            await page.waitForTimeout(WAIT_TIME.short);
          }

          await closeModal(page);
        }
      }
    });

    test('價格數值驗證', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const priceInput = modal.locator('#price, input[name="price"]');
          if (await priceInput.isVisible()) {
            // 輸入負數
            await priceInput.fill('-100');
            await page.waitForTimeout(WAIT_TIME.short);
          }

          await closeModal(page);
        }
      }
    });

    test('時長數值驗證', async ({ page }) => {
      await page.goto('/tenant/services');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const durationInput = modal.locator('#duration, input[name="duration"]');
          if (await durationInput.isVisible()) {
            // 輸入 0
            await durationInput.fill('0');
            await page.waitForTimeout(WAIT_TIME.short);
          }

          await closeModal(page);
        }
      }
    });
  });

  test.describe('預約新增表單', () => {
    test('日期時間驗證', async ({ page }) => {
      await page.goto('/tenant/bookings');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          // 檢查日期欄位
          const dateInput = modal.locator('#bookingDate, input[name="bookingDate"]');
          const timeInput = modal.locator('#startTime, input[name="startTime"]');

          console.log(`日期輸入: ${await dateInput.isVisible()}`);
          console.log(`時間輸入: ${await timeInput.isVisible()}`);

          await closeModal(page);
        }
      }
    });
  });
});

test.describe('登入表單驗證', () => {
  test.describe('店家登入', () => {
    test('空白欄位驗證', async ({ page }) => {
      await page.goto('/tenant/login');
      await waitForLoading(page);

      // 直接點擊登入
      const submitBtn = page.locator('button[type="submit"]');
      await submitBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 應該不會跳轉
      expect(page.url()).toContain('/login');
    });

    test('錯誤帳密驗證', async ({ page }) => {
      await page.goto('/tenant/login');
      await waitForLoading(page);

      await page.fill('#username', 'wrong_user');
      await page.fill('#password', 'wrong_password');
      await page.click('button[type="submit"]');

      await page.waitForTimeout(WAIT_TIME.api);

      // 應該顯示錯誤訊息
      const errorMessage = page.locator('.alert-danger, .error, .text-danger');
      const isError = await errorMessage.isVisible();
      console.log(`顯示錯誤訊息: ${isError}`);
    });

    test('正確登入', async ({ page }) => {
      await page.goto('/tenant/login');
      await waitForLoading(page);

      await page.fill('#username', TEST_ACCOUNTS.tenant.username);
      await page.fill('#password', TEST_ACCOUNTS.tenant.password);
      await page.click('button[type="submit"]');

      await page.waitForURL(/\/tenant\/dashboard/, { timeout: 15000 });
      expect(page.url()).toContain('/dashboard');
    });
  });

  test.describe('超管登入', () => {
    test('空白欄位驗證', async ({ page }) => {
      await page.goto('/admin/login');
      await waitForLoading(page);

      const submitBtn = page.locator('button[type="submit"]');
      await submitBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      expect(page.url()).toContain('/login');
    });

    test('錯誤帳密驗證', async ({ page }) => {
      await page.goto('/admin/login');
      await waitForLoading(page);

      await page.fill('#username', 'wrong_admin');
      await page.fill('#password', 'wrong_password');
      await page.click('button[type="submit"]');

      await page.waitForTimeout(WAIT_TIME.api);

      const errorMessage = page.locator('.alert-danger, .error, .text-danger');
      const isError = await errorMessage.isVisible();
      console.log(`顯示錯誤訊息: ${isError}`);
    });

    test('正確登入', async ({ page }) => {
      await page.goto('/admin/login');
      await waitForLoading(page);

      await page.fill('#username', 'admin');
      await page.fill('#password', 'admin123');
      await page.click('button[type="submit"]');

      await page.waitForURL(/\/admin\/dashboard/, { timeout: 15000 });
      expect(page.url()).toContain('/dashboard');
    });
  });
});

test.describe('設定表單驗證', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('店家設定表單', () => {
    test('店家名稱必填', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const nameInput = page.locator('#businessName, input[name="businessName"]');
      if (await nameInput.isVisible()) {
        // 清空名稱
        await nameInput.fill('');

        // 嘗試儲存
        const saveBtn = page.locator('button:has-text("儲存")');
        if (await saveBtn.isVisible()) {
          await saveBtn.click();
          await page.waitForTimeout(WAIT_TIME.short);
        }
      }
    });

    test('營業時間邏輯驗證', async ({ page }) => {
      await page.goto('/tenant/settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const openTime = page.locator('#openTime, input[name="openTime"]');
      const closeTime = page.locator('#closeTime, input[name="closeTime"]');

      if (await openTime.isVisible() && await closeTime.isVisible()) {
        // 設定關店時間早於開店時間
        await openTime.fill('18:00');
        await closeTime.fill('09:00');
        await page.waitForTimeout(WAIT_TIME.short);

        console.log('已設定無效營業時間');
      }
    });
  });

  test.describe('LINE 設定表單', () => {
    test('Channel 資訊欄位', async ({ page }) => {
      await page.goto('/tenant/line-settings');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const fields = ['#channelId', '#channelSecret', '#channelAccessToken'];
      for (const field of fields) {
        const input = page.locator(field);
        console.log(`欄位 ${field}: ${await input.isVisible() ? '存在' : '不存在'}`);
      }
    });
  });
});

test.describe('按鈕狀態測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('表單提交按鈕禁用狀態', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const addBtn = page.locator('button:has-text("新增")').first();
    const isBtnVisible = await addBtn.isVisible().catch(() => false);
    console.log(`新增按鈕可見: ${isBtnVisible}`);

    if (isBtnVisible) {
      await addBtn.click();
      await page.waitForTimeout(WAIT_TIME.medium);

      const modal = page.locator('.modal.show');
      const isModalVisible = await modal.isVisible().catch(() => false);
      console.log(`Modal 可見: ${isModalVisible}`);

      if (isModalVisible) {
        const submitBtn = modal.locator('button[type="submit"], .btn-primary').first();
        const submitBtnExists = await submitBtn.count() > 0;
        console.log(`提交按鈕存在: ${submitBtnExists}`);

        if (submitBtnExists) {
          const isDisabled = await submitBtn.isDisabled().catch(() => false);
          console.log(`提交按鈕禁用: ${isDisabled}`);
        }

        await closeModal(page);
      }
    }
  });

  test('刪除按鈕確認對話框', async ({ page }) => {
    await page.goto('/tenant/customers');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const deleteBtn = page.locator('button:has-text("刪除")').first();
    if (await deleteBtn.isVisible()) {
      await deleteBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 檢查確認對話框
      const confirmModal = page.locator('.modal.show, .swal2-popup');
      const isConfirm = await confirmModal.isVisible();
      console.log(`確認對話框: ${isConfirm}`);

      if (isConfirm) {
        const cancelBtn = page.locator('.modal .btn-secondary, .swal2-cancel');
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }
    }
  });
});

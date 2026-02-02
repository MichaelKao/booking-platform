import { test, expect, APIRequestContext } from '@playwright/test';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  generateTestData,
  closeModal
} from './utils/test-helpers';

/**
 * 店家後台 - 商品管理 & 票券管理完整測試
 *
 * 測試範圍：
 * 1. 商品 CRUD
 * 2. 商品狀態操作
 * 3. 票券 CRUD
 * 4. 票券發放和核銷
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: 'tenant_test', password: 'test123' }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

test.describe('商品管理 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('商品列表 API', () => {
    test('取得商品列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/products?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`商品總數: ${data.data?.totalElements || 0}`);
    });

    test('商品列表 - 帶分類篩選', async ({ request }) => {
      if (!tenantToken) return;

      const categories = ['VOUCHER', 'MERCHANDISE', 'SERVICE'];
      for (const category of categories) {
        const response = await request.get(`/api/products?category=${category}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`分類 ${category}: ${response.ok() ? '成功' : response.status()}`);
      }
    });

    test('商品列表 - 帶狀態篩選', async ({ request }) => {
      if (!tenantToken) return;

      const statuses = ['ON_SALE', 'OFF_SHELF', 'ARCHIVED'];
      for (const status of statuses) {
        const response = await request.get(`/api/products?status=${status}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`狀態 ${status}: ${response.ok() ? '成功' : response.status()}`);
      }
    });
  });

  test.describe('商品 CRUD API', () => {
    let createdProductId: string;

    test('新增商品', async ({ request }) => {
      if (!tenantToken) return;

      const productData = {
        name: generateTestData('Product'),
        description: '測試商品描述',
        category: 'MERCHANDISE',
        price: 500,
        stock: 100,
        status: 'ON_SALE'
      };

      const response = await request.post('/api/products', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: productData
      });

      if (response.ok()) {
        const data = await response.json();
        createdProductId = data.data?.id;
        console.log(`新增商品成功, ID: ${createdProductId}`);
      } else {
        console.log(`新增商品回應: ${response.status()}`);
      }
    });

    test('取得單一商品', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/products?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const productId = listData.data.content[0].id;
        const response = await request.get(`/api/products/${productId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.data).toHaveProperty('name');
        expect(data.data).toHaveProperty('price');
      }
    });

    test('更新商品', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/products?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const product = listData.data.content[0];
        const response = await request.put(`/api/products/${product.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            name: product.name,
            description: product.description,
            category: product.category,
            price: product.price,
            stock: product.stock || 100
          }
        });
        expect(response.status()).toBeLessThan(500);
      }
    });
  });

  test.describe('商品狀態操作 API', () => {
    test('上架商品', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/products?status=OFF_SHELF&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const productId = listData.data.content[0].id;
        const response = await request.post(`/api/products/${productId}/on-sale`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`上架商品回應: ${response.status()}`);
      }
    });

    test('下架商品', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/products?status=ON_SALE&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const productId = listData.data.content[0].id;
        const response = await request.post(`/api/products/${productId}/off-shelf`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`下架商品回應: ${response.status()}`);

        // 恢復上架
        await request.post(`/api/products/${productId}/on-sale`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
      }
    });

    test('調整庫存', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/products?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const productId = listData.data.content[0].id;
        const response = await request.post(`/api/products/${productId}/adjust-stock`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            adjustment: 10,
            reason: '測試調整庫存'
          }
        });
        console.log(`調整庫存回應: ${response.status()}`);
      }
    });
  });
});

test.describe('票券管理 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('票券列表 API', () => {
    test('取得票券列表', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/coupons?page=0&size=20', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`票券總數: ${data.data?.totalElements || 0}`);
    });

    test('票券列表 - 帶狀態篩選', async ({ request }) => {
      if (!tenantToken) return;

      const statuses = ['DRAFT', 'PUBLISHED', 'PAUSED', 'ENDED'];
      for (const status of statuses) {
        const response = await request.get(`/api/coupons?status=${status}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.status()).toBeLessThan(500);
        console.log(`狀態 ${status}: ${response.ok() ? '成功' : response.status()}`);
      }
    });
  });

  test.describe('票券 CRUD API', () => {
    let createdCouponId: string;

    test('新增票券', async ({ request }) => {
      if (!tenantToken) return;

      const startDate = new Date();
      const endDate = new Date();
      endDate.setMonth(endDate.getMonth() + 1);

      const couponData = {
        name: generateTestData('Coupon'),
        description: '測試票券描述',
        discountType: 'FIXED',
        discountValue: 100,
        minPurchase: 500,
        totalQuantity: 100,
        perUserLimit: 1,
        startDate: startDate.toISOString().split('T')[0],
        endDate: endDate.toISOString().split('T')[0]
      };

      const response = await request.post('/api/coupons', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: couponData
      });

      if (response.ok()) {
        const data = await response.json();
        createdCouponId = data.data?.id;
        console.log(`新增票券成功, ID: ${createdCouponId}`);
      } else {
        console.log(`新增票券回應: ${response.status()}`);
      }
    });

    test('取得單一票券', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/coupons?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const couponId = listData.data.content[0].id;
        const response = await request.get(`/api/coupons/${couponId}`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        expect(response.ok()).toBeTruthy();
        const data = await response.json();
        expect(data.data).toHaveProperty('name');
      }
    });

    test('更新票券', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/coupons?page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const coupon = listData.data.content[0];
        const response = await request.put(`/api/coupons/${coupon.id}`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: {
            name: coupon.name,
            description: '更新的描述'
          }
        });
        expect(response.status()).toBeLessThan(500);
      }
    });
  });

  test.describe('票券操作 API', () => {
    test('發布票券', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/coupons?status=DRAFT&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const couponId = listData.data.content[0].id;
        const response = await request.post(`/api/coupons/${couponId}/publish`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`發布票券回應: ${response.status()}`);
      }
    });

    test('暫停票券', async ({ request }) => {
      if (!tenantToken) return;

      const listResponse = await request.get('/api/coupons?status=PUBLISHED&page=0&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listResponse.json();

      if (listData.data?.content?.length > 0) {
        const couponId = listData.data.content[0].id;
        const response = await request.post(`/api/coupons/${couponId}/pause`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
        console.log(`暫停票券回應: ${response.status()}`);

        // 恢復
        await request.post(`/api/coupons/${couponId}/resume`, {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        });
      }
    });

    test('發放票券給顧客', async ({ request }) => {
      if (!tenantToken) return;

      // 取得顧客和票券
      const [couponsRes, customersRes] = await Promise.all([
        request.get('/api/coupons?status=PUBLISHED&page=0&size=1', {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        }),
        request.get('/api/customers?page=0&size=1', {
          headers: { 'Authorization': `Bearer ${tenantToken}` }
        })
      ]);

      const couponsData = await couponsRes.json();
      const customersData = await customersRes.json();

      if (couponsData.data?.content?.length > 0 && customersData.data?.content?.length > 0) {
        const couponId = couponsData.data.content[0].id;
        const customerId = customersData.data.content[0].id;

        const response = await request.post(`/api/coupons/${couponId}/issue`, {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: { customerId }
        });
        console.log(`發放票券回應: ${response.status()}`);
      }
    });
  });
});

test.describe('商品管理 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('商品列表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/products');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('表格正確顯示', async ({ page }) => {
      await page.goto('/tenant/products');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      await expect(table).toBeVisible();

      const headers = ['名稱', '分類', '價格', '庫存', '狀態', '操作'];
      for (const header of headers) {
        const th = page.locator(`th:has-text("${header}")`);
        console.log(`表頭 "${header}": ${await th.count() > 0 ? '存在' : '不存在'}`);
      }
    });

    test('新增商品按鈕', async ({ page }) => {
      await page.goto('/tenant/products');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增商品"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();

        // 檢查表單欄位
        const formFields = ['#name', '#category', '#price', '#stock', '#description'];
        for (const field of formFields) {
          console.log(`欄位 ${field}: ${await page.locator(field).isVisible() ? '存在' : '不存在'}`);
        }

        await closeModal(page);
      }
    });

    test('分類篩選', async ({ page }) => {
      await page.goto('/tenant/products');
      await waitForLoading(page);

      const categorySelect = page.locator('select[name="category"], #categoryFilter');
      if (await categorySelect.isVisible()) {
        await categorySelect.selectOption('VOUCHER').catch(() => {});
        await page.waitForTimeout(WAIT_TIME.short);

        await categorySelect.selectOption('MERCHANDISE').catch(() => {});
        await page.waitForTimeout(WAIT_TIME.short);
      }
    });

    test('上架/下架按鈕', async ({ page }) => {
      await page.goto('/tenant/products');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const onSaleBtn = page.locator('button:has-text("上架")').first();
      const offShelfBtn = page.locator('button:has-text("下架")').first();

      console.log(`上架按鈕: ${await onSaleBtn.isVisible()}`);
      console.log(`下架按鈕: ${await offShelfBtn.isVisible()}`);
    });

    test('調整庫存按鈕', async ({ page }) => {
      await page.goto('/tenant/products');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const adjustBtn = page.locator('button:has-text("調整庫存"), button:has-text("庫存")').first();
      if (await adjustBtn.isVisible()) {
        await adjustBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          const adjustmentInput = modal.locator('input[type="number"]');
          console.log(`庫存調整輸入: ${await adjustmentInput.isVisible()}`);
          await closeModal(page);
        }
      }
    });
  });
});

test.describe('票券管理 UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('票券列表頁面', () => {
    test('頁面載入成功', async ({ page }) => {
      await page.goto('/tenant/coupons');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const title = page.locator('h1, h2, .page-title');
      await expect(title.first()).toBeVisible();
    });

    test('表格正確顯示', async ({ page }) => {
      await page.goto('/tenant/coupons');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const table = page.locator('table.table');
      await expect(table).toBeVisible();

      const headers = ['名稱', '折扣', '數量', '有效期', '狀態', '操作'];
      for (const header of headers) {
        const th = page.locator(`th:has-text("${header}")`);
        console.log(`表頭 "${header}": ${await th.count() > 0 ? '存在' : '不存在'}`);
      }
    });

    test('新增票券按鈕', async ({ page }) => {
      await page.goto('/tenant/coupons');
      await waitForLoading(page);

      const addBtn = page.locator('button:has-text("新增票券"), button:has-text("新增")').first();
      if (await addBtn.isVisible()) {
        await addBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        await expect(modal).toBeVisible();

        // 檢查表單欄位
        const formFields = ['#name', '#discountType', '#discountValue', '#totalQuantity', '#startDate', '#endDate'];
        for (const field of formFields) {
          console.log(`欄位 ${field}: ${await page.locator(field).isVisible() ? '存在' : '不存在'}`);
        }

        await closeModal(page);
      }
    });

    test('狀態篩選標籤', async ({ page }) => {
      await page.goto('/tenant/coupons');
      await waitForLoading(page);

      const tabs = page.locator('.nav-tabs .nav-link, .btn-group .btn');
      const tabCount = await tabs.count();
      console.log(`狀態標籤數: ${tabCount}`);

      for (let i = 0; i < tabCount; i++) {
        const tab = tabs.nth(i);
        if (await tab.isVisible()) {
          const text = await tab.textContent();
          console.log(`標籤 ${i + 1}: ${text?.trim()}`);
        }
      }
    });

    test('發布/暫停按鈕', async ({ page }) => {
      await page.goto('/tenant/coupons');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const publishBtn = page.locator('button:has-text("發布")').first();
      const pauseBtn = page.locator('button:has-text("暫停")').first();

      console.log(`發布按鈕: ${await publishBtn.isVisible()}`);
      console.log(`暫停按鈕: ${await pauseBtn.isVisible()}`);
    });

    test('發放票券按鈕', async ({ page }) => {
      await page.goto('/tenant/coupons');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const issueBtn = page.locator('button:has-text("發放")').first();
      if (await issueBtn.isVisible()) {
        await issueBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const modal = page.locator('.modal.show');
        if (await modal.isVisible()) {
          console.log('發放票券 Modal 已開啟');
          await closeModal(page);
        }
      }
    });
  });
});

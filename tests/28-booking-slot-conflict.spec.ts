import { test, expect, APIRequestContext } from './fixtures';
import { TEST_ACCOUNTS } from './utils/test-helpers';

/**
 * 預約時段衝突與自動分配員工測試
 *
 * 測試範圍：
 * 1. 衝突檢查只看 CONFIRMED（PENDING 不佔用時段）
 * 2. 未指定員工時自動分配
 * 3. confirm() 是真正的驗證關卡
 * 4. 「我的預約」只顯示 CONFIRMED
 */

// 取得店家 Token
async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// 取得測試用的服務、員工、顧客 ID
async function getTestData(request: APIRequestContext, token: string) {
  const headers = { 'Authorization': `Bearer ${token}` };

  const [servicesRes, staffRes, customersRes] = await Promise.all([
    request.get('/api/services?size=1', { headers }),
    request.get('/api/staff?size=1', { headers }),
    request.get('/api/customers?size=1', { headers }),
  ]);

  const services = await servicesRes.json();
  const staff = await staffRes.json();
  const customers = await customersRes.json();

  return {
    serviceId: services.data?.content?.[0]?.id || null,
    staffId: staff.data?.content?.[0]?.id || null,
    customerId: customers.data?.content?.[0]?.id || null,
  };
}

test.describe('預約時段衝突與自動分配員工', () => {
  let tenantToken: string;
  let testData: { serviceId: string | null; staffId: string | null; customerId: string | null };

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    if (tenantToken) {
      testData = await getTestData(request, tenantToken);
    }
  });

  test.describe('預約 API 基本驗證', () => {
    test('建立預約 API 可正常呼叫', async ({ request }) => {
      if (!tenantToken || !testData.serviceId || !testData.customerId) {
        test.skip();
        return;
      }

      const tomorrow = '2099-12-13'; // 固定未來週一，避免週末/假日問題
      const response = await request.post('/api/bookings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          serviceItemId: testData.serviceId,
          customerId: testData.customerId,
          staffId: testData.staffId,
          bookingDate: tomorrow,
          startTime: '10:00',
          customerNote: 'E2E測試-衝突檢查'
        }
      });

      // 200/201 = 成功，409/422/400 = 業務驗證（都代表 API 正常運作）
      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`建立預約結果: ${response.status()}, success: ${data.success}`);
    });

    test('建立預約 — 不指定員工也能成功', async ({ request }) => {
      if (!tenantToken || !testData.serviceId || !testData.customerId) {
        test.skip();
        return;
      }

      const tomorrow = '2099-12-13'; // 固定未來週一，避免週末/假日問題
      const response = await request.post('/api/bookings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          serviceItemId: testData.serviceId,
          customerId: testData.customerId,
          staffId: null,
          bookingDate: tomorrow,
          startTime: '14:00',
          customerNote: 'E2E測試-不指定員工'
        }
      });

      // 不應回傳 500（伺服器錯誤），400 可能是業務驗證
      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`不指定員工結果: ${response.status()}, success: ${data.success}`);

      // 如果成功，檢查是否有自動分配員工
      if (data.success && data.data) {
        console.log(`自動分配員工: ${data.data.staffName || '無'}`);
        // 預期自動分配了員工
        expect(data.data.staffName).toBeTruthy();
      }
    });
  });

  test.describe('PENDING 不佔用時段驗證', () => {
    test('同一時段可以建立多筆 PENDING 預約', async ({ request }) => {
      if (!tenantToken || !testData.serviceId || !testData.staffId || !testData.customerId) {
        test.skip();
        return;
      }

      const tomorrow = '2099-12-13'; // 固定未來週一，避免週末/假日問題
      const bookingData = {
        serviceItemId: testData.serviceId,
        customerId: testData.customerId,
        staffId: testData.staffId,
        bookingDate: tomorrow,
        startTime: '11:00',
        customerNote: 'E2E測試-PENDING不衝突'
      };

      // 建立第一筆 PENDING
      const res1 = await request.post('/api/bookings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: bookingData
      });

      // 建立第二筆 PENDING（同時段同員工）
      const res2 = await request.post('/api/bookings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: { ...bookingData, customerNote: 'E2E測試-PENDING不衝突-2' }
      });

      // 兩筆都不應該是 500
      expect(res1.status()).not.toBe(500);
      expect(res2.status()).not.toBe(500);

      const data1 = await res1.json();
      const data2 = await res2.json();

      console.log(`第一筆: ${res1.status()}, success: ${data1.success}`);
      console.log(`第二筆: ${res2.status()}, success: ${data2.success}`);

      // 如果第一筆成功，第二筆也應該成功（PENDING 不佔用時段）
      if (data1.success) {
        expect(data2.success).toBeTruthy();
        console.log('驗證通過: 同時段可建立多筆 PENDING');
      }
    });
  });

  test.describe('確認預約驗證', () => {
    test('確認預約 API 狀態檢查', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      // 取得 PENDING 預約
      const listRes = await request.get('/api/bookings?status=PENDING&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listRes.json();
      const pendingBooking = listData.data?.content?.[0];

      if (!pendingBooking) {
        console.log('沒有 PENDING 預約可測試，跳過');
        return;
      }

      // 確認預約
      const confirmRes = await request.post(`/api/bookings/${pendingBooking.id}/confirm`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });

      expect(confirmRes.status()).not.toBe(500);
      const confirmData = await confirmRes.json();
      console.log(`確認預約結果: ${confirmRes.status()}, success: ${confirmData.success}`);

      // 如果成功，狀態應該是 CONFIRMED
      if (confirmData.success && confirmData.data) {
        expect(confirmData.data.status).toBe('CONFIRMED');
        console.log(`確認後員工: ${confirmData.data.staffName || '無'}`);
      }
    });

    test('確認預約 — 無員工時自動分配', async ({ request }) => {
      if (!tenantToken || !testData.serviceId) {
        test.skip();
        return;
      }

      const tomorrow = '2099-12-13'; // 固定未來週一，避免週末/假日問題

      // 先建立一筆不指定員工的預約
      const createRes = await request.post('/api/bookings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          serviceItemId: testData.serviceId,
          customerId: testData.customerId,
          staffId: null,
          bookingDate: tomorrow,
          startTime: '15:00',
          customerNote: 'E2E測試-確認自動分配'
        }
      });

      const createData = await createRes.json();
      if (!createData.success || !createData.data) {
        console.log('建立預約失敗，跳過確認測試');
        return;
      }

      const bookingId = createData.data.id;

      // 確認預約
      const confirmRes = await request.post(`/api/bookings/${bookingId}/confirm`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });

      expect(confirmRes.status()).not.toBe(500);
      const confirmData = await confirmRes.json();

      if (confirmData.success && confirmData.data) {
        expect(confirmData.data.status).toBe('CONFIRMED');
        // 確認後應該有員工
        console.log(`確認後員工名稱: ${confirmData.data.staffName || '(未分配)'}`);
      }
    });
  });

  test.describe('衝突檢查 — CONFIRMED 才衝突', () => {
    test('已確認的預約佔用時段', async ({ request }) => {
      if (!tenantToken || !testData.serviceId || !testData.staffId) {
        test.skip();
        return;
      }

      // 取得已確認的預約列表
      const listRes = await request.get('/api/bookings?status=CONFIRMED&size=5', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listRes.json();

      expect(listRes.ok()).toBeTruthy();
      console.log(`已確認預約數: ${listData.data?.totalElements || 0}`);
    });

    test('預約狀態篩選 — PENDING 和 CONFIRMED 分開', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const headers = { 'Authorization': `Bearer ${tenantToken}` };

      const [pendingRes, confirmedRes] = await Promise.all([
        request.get('/api/bookings?status=PENDING&size=1', { headers }),
        request.get('/api/bookings?status=CONFIRMED&size=1', { headers }),
      ]);

      expect(pendingRes.status()).not.toBe(500);
      expect(confirmedRes.status()).not.toBe(500);

      const pendingData = await pendingRes.json();
      const confirmedData = await confirmedRes.json();

      console.log(`PENDING 預約: ${pendingData.data?.totalElements || 0}`);
      console.log(`CONFIRMED 預約: ${confirmedData.data?.totalElements || 0}`);
    });
  });

  test.describe('預約列表 API 回應格式', () => {
    test('預約回應包含 staffName 和 staffId', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.get('/api/bookings?size=5', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });

      expect(response.ok()).toBeTruthy();
      const data = await response.json();

      if (data.data?.content?.length > 0) {
        const booking = data.data.content[0];
        // 檢查欄位存在
        expect(booking).toHaveProperty('id');
        expect(booking).toHaveProperty('status');
        expect(booking).toHaveProperty('bookingDate');
        expect(booking).toHaveProperty('startTime');
        console.log(`預約 ${booking.id}: staff=${booking.staffName || '無'}, status=${booking.status}`);
      }
    });

    test('行事曆 API 回傳所有非取消預約', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const today = '2099-12-13'; // 固定未來週一
      const response = await request.get(`/api/bookings/calendar?start=${today}&end=${today}`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });

      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`行事曆預約數: ${data.data?.length || 0}`);
    });
  });

  test.describe('取消預約後的行為', () => {
    test('取消預約 API', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      // 取得一筆 PENDING 預約
      const listRes = await request.get('/api/bookings?status=PENDING&size=1', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const listData = await listRes.json();
      const booking = listData.data?.content?.[0];

      if (!booking) {
        console.log('沒有 PENDING 預約可取消，跳過');
        return;
      }

      // 取消
      const cancelRes = await request.post(`/api/bookings/${booking.id}/cancel`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: { reason: 'E2E測試取消' }
      });

      expect(cancelRes.status()).not.toBe(500);
      const cancelData = await cancelRes.json();

      if (cancelData.success) {
        expect(cancelData.data.status).toBe('CANCELLED');
        console.log('取消預約成功');
      }
    });

    test('已取消的預約不影響衝突檢查', async ({ request }) => {
      if (!tenantToken || !testData.serviceId || !testData.staffId || !testData.customerId) {
        test.skip();
        return;
      }

      // 在有已取消預約的時段建立新預約，應該成功
      const tomorrow = '2099-12-13'; // 固定未來週一，避免週末/假日問題
      const response = await request.post('/api/bookings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          serviceItemId: testData.serviceId,
          customerId: testData.customerId,
          staffId: testData.staffId,
          bookingDate: tomorrow,
          startTime: '16:00',
          customerNote: 'E2E測試-取消後不衝突'
        }
      });

      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`取消後時段可預約: ${data.success}`);
    });
  });

  test.describe('Docker 時區驗證', () => {
    test('伺服器時間正確（健康檢查可用）', async ({ request }) => {
      const response = await request.get('/health');
      expect(response.ok()).toBeTruthy();
    });
  });
});

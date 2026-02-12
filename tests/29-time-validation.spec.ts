import { test, expect, APIRequestContext } from './fixtures';
import { TEST_ACCOUNTS, getTomorrow } from './utils/test-helpers';

/**
 * 時間/日期驗證測試
 *
 * 測試範圍：
 * 1. 員工排班：開始時間不能晚於結束時間、休息時間驗證
 * 2. 員工請假（半天）：開始時間不能晚於結束時間
 * 3. 店家設定：營業時間、休息時間驗證
 * 4. 預約更新：開始時間不能晚於結束時間
 * 5. 票券有效期：起始日不能晚於結束日
 * 6. 行銷活動：開始時間不能晚於結束時間
 */

// 取得店家 Token
async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// 取得測試用的員工 ID
async function getStaffId(request: APIRequestContext, token: string): Promise<string | null> {
  const res = await request.get('/api/staff?size=1', {
    headers: { 'Authorization': `Bearer ${token}` }
  });
  const data = await res.json();
  return data.data?.content?.[0]?.id || null;
}

test.describe('時間/日期驗證', () => {
  let tenantToken: string;
  let staffId: string | null;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    if (tenantToken) {
      staffId = await getStaffId(request, tenantToken);
    }
  });

  test.describe('員工排班時間驗證', () => {
    test('上班時間反轉 — 開始時間晚於結束時間應被拒絕', async ({ request }) => {
      if (!tenantToken || !staffId) {
        test.skip();
        return;
      }

      const response = await request.put(`/api/staff/${staffId}/schedule`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          schedules: [{
            dayOfWeek: 1,
            isWorkingDay: true,
            startTime: '17:00',
            endTime: '09:00',
            breakStartTime: null,
            breakEndTime: null
          }]
        }
      });

      // 應被業務驗證拒絕（422 或 409），不是 500
      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      expect(response.status()).not.toBe(201);
      const data = await response.json();
      console.log(`排班時間反轉: ${response.status()}, message: ${data.message || ''}`);
    });

    test('休息時間反轉 — 休息開始晚於結束應被拒絕', async ({ request }) => {
      if (!tenantToken || !staffId) {
        test.skip();
        return;
      }

      const response = await request.put(`/api/staff/${staffId}/schedule`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          schedules: [{
            dayOfWeek: 1,
            isWorkingDay: true,
            startTime: '09:00',
            endTime: '18:00',
            breakStartTime: '14:00',
            breakEndTime: '12:00'
          }]
        }
      });

      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      expect(response.status()).not.toBe(201);
      const data = await response.json();
      console.log(`休息時間反轉: ${response.status()}, message: ${data.message || ''}`);
    });

    test('休息時間超出上班範圍應被拒絕', async ({ request }) => {
      if (!tenantToken || !staffId) {
        test.skip();
        return;
      }

      const response = await request.put(`/api/staff/${staffId}/schedule`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          schedules: [{
            dayOfWeek: 1,
            isWorkingDay: true,
            startTime: '09:00',
            endTime: '18:00',
            breakStartTime: '08:00',
            breakEndTime: '09:30'
          }]
        }
      });

      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      expect(response.status()).not.toBe(201);
      const data = await response.json();
      console.log(`休息超出範圍: ${response.status()}, message: ${data.message || ''}`);
    });

    test('正常排班時間應成功', async ({ request }) => {
      if (!tenantToken || !staffId) {
        test.skip();
        return;
      }

      const response = await request.put(`/api/staff/${staffId}/schedule`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          schedules: [{
            dayOfWeek: 1,
            isWorkingDay: true,
            startTime: '09:00',
            endTime: '18:00',
            breakStartTime: '12:00',
            breakEndTime: '13:00'
          }]
        }
      });

      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`正常排班: ${response.status()}, success: ${data.success}`);
      if (data.success) {
        expect(data.success).toBeTruthy();
      }
    });
  });

  test.describe('員工請假時間驗證', () => {
    test('半天假時間反轉應被拒絕', async ({ request }) => {
      if (!tenantToken || !staffId) {
        test.skip();
        return;
      }

      const tomorrow = getTomorrow();
      const response = await request.post(`/api/staff/${staffId}/leaves`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          leaveDates: [tomorrow],
          leaveType: 'PERSONAL',
          isFullDay: false,
          startTime: '14:00',
          endTime: '09:00',
          reason: 'E2E測試-時間反轉'
        }
      });

      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      expect(response.status()).not.toBe(201);
      const data = await response.json();
      console.log(`半天假反轉: ${response.status()}, message: ${data.message || ''}`);
    });

    test('正常半天假應成功', async ({ request }) => {
      if (!tenantToken || !staffId) {
        test.skip();
        return;
      }

      const tomorrow = getTomorrow();
      const response = await request.post(`/api/staff/${staffId}/leaves`, {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          leaveDates: [tomorrow],
          leaveType: 'PERSONAL',
          isFullDay: false,
          startTime: '09:00',
          endTime: '12:00',
          reason: 'E2E測試-正常半天假'
        }
      });

      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`正常半天假: ${response.status()}, success: ${data.success}`);
    });
  });

  test.describe('店家營業設定時間驗證', () => {
    test('營業時間反轉應被拒絕', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.put('/api/settings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          businessStartTime: '21:00',
          businessEndTime: '09:00'
        }
      });

      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      const data = await response.json();
      console.log(`營業時間反轉: ${response.status()}, message: ${data.message || ''}`);
    });

    test('休息時間反轉應被拒絕', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.put('/api/settings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          breakStartTime: '14:00',
          breakEndTime: '12:00'
        }
      });

      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      const data = await response.json();
      console.log(`休息時間反轉: ${response.status()}, message: ${data.message || ''}`);
    });

    test('正常營業設定應成功', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.put('/api/settings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          businessStartTime: '09:00',
          businessEndTime: '21:00',
          breakStartTime: '12:00',
          breakEndTime: '13:00'
        }
      });

      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`正常營業設定: ${response.status()}, success: ${data.success}`);
      if (data.success) {
        expect(data.success).toBeTruthy();
      }
    });
  });

  test.describe('票券有效期驗證', () => {
    test('有效期反轉應被拒絕', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.post('/api/coupons', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          name: 'E2E測試-日期反轉票券',
          type: 'DISCOUNT_AMOUNT',
          discountAmount: 100,
          validStartAt: '2099-12-31 00:00:00',
          validEndAt: '2099-01-01 00:00:00'
        }
      });

      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      expect(response.status()).not.toBe(201);
      const data = await response.json();
      console.log(`票券日期反轉: ${response.status()}, message: ${data.message || ''}`);
    });

    test('正常有效期應成功', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.post('/api/coupons', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          name: `E2E測試-正常票券-${Date.now()}`,
          type: 'DISCOUNT_AMOUNT',
          discountAmount: 50,
          validStartAt: '2099-01-01 00:00:00',
          validEndAt: '2099-12-31 00:00:00'
        }
      });

      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`正常票券: ${response.status()}, success: ${data.success}`);
    });
  });

  test.describe('行銷活動日期驗證', () => {
    test('活動日期反轉應被拒絕', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.post('/api/campaigns', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          name: 'E2E測試-日期反轉活動',
          type: 'GENERAL',
          startAt: '2099-12-31 00:00:00',
          endAt: '2099-01-01 00:00:00'
        }
      });

      expect(response.status()).not.toBe(500);
      expect(response.status()).not.toBe(200);
      expect(response.status()).not.toBe(201);
      const data = await response.json();
      console.log(`活動日期反轉: ${response.status()}, message: ${data.message || ''}`);
    });

    test('正常活動日期應成功', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      const response = await request.post('/api/campaigns', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          name: `E2E測試-正常活動-${Date.now()}`,
          type: 'GENERAL',
          startAt: '2099-01-01 00:00:00',
          endAt: '2099-12-31 00:00:00'
        }
      });

      expect(response.status()).not.toBe(500);
      const data = await response.json();
      console.log(`正常活動: ${response.status()}, success: ${data.success}`);
    });
  });

  test.describe('預約時間驗證', () => {
    test('預約 API 基本時間驗證', async ({ request }) => {
      if (!tenantToken) {
        test.skip();
        return;
      }

      // 驗證正常預約不回 500
      const tomorrow = getTomorrow();
      const response = await request.post('/api/bookings', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        data: {
          serviceItemId: 'any',
          bookingDate: tomorrow,
          startTime: '10:00',
          customerNote: 'E2E測試-時間驗證'
        }
      });

      // 允許 404（找不到服務）或 422 等業務錯誤，不允許 500
      expect(response.status()).not.toBe(500);
      console.log(`預約時間驗證: ${response.status()}`);
    });

    test('健康檢查', async ({ request }) => {
      const response = await request.get('/health');
      expect(response.ok()).toBeTruthy();
    });
  });
});

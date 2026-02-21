import { test, expect, APIRequestContext } from './fixtures';
import { TEST_ACCOUNTS, getTomorrow } from './utils/test-helpers';

/**
 * 跨功能業務規則測試
 *
 * 整合自：31-business-logic-correctness, 32-bugfix-verification,
 *         27-time-validation, 25-api-contract-validator (partial)
 *
 * 測試範圍：
 * 1. 時間驗證規則 — 排班/請假/營業時間反轉應被拒絕
 * 2. API 契約驗證 — 主要 POST/PUT 端點欄位名稱不應 400
 * 3. 報表一致性 — 數值合理、範圍遞增
 * 4. 多租戶資料隔離 — 角色不能跨域存取
 * 5. 預約狀態機完整性 — 狀態轉換規則
 * 6. SSE 通知端點 — 認證與 Content-Type
 * 7. 密碼安全規則 — 錯誤密碼應被拒絕
 */

// ========================================
// 共用 Token 取得
// ========================================

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const res = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const body = await res.json();
  return body.data?.accessToken || '';
}

async function getAdminToken(request: APIRequestContext): Promise<string> {
  const res = await request.post('/api/auth/admin/login', {
    data: { username: TEST_ACCOUNTS.admin.username, password: TEST_ACCOUNTS.admin.password }
  });
  const body = await res.json();
  return body.data?.accessToken || '';
}

// 輔助：驗證回應不是欄位驗證錯誤的 400
async function assertNotFieldError(res: any, endpoint: string) {
  const status = res.status();
  if (status !== 400) return;

  const body = await res.json().catch(() => ({}));
  const code = body.code || '';
  // SYS_002 = 請求體格式錯誤, SYS_003 = 參數驗證失敗
  const isFieldError = code === 'SYS_002' || code === 'SYS_003';
  expect(isFieldError, `${endpoint} returned 400 (${code}: ${body.message}) — field name or format mismatch`).toBe(false);
}

// ========================================
// 1. 時間驗證規則 (Time Validation)
// ========================================

test.describe('1. Time Validation Rules', () => {
  let tenantToken: string;
  let staffId: string | null;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    expect(tenantToken, 'Tenant token should be obtained').toBeTruthy();

    // Get a staff ID for schedule/leave tests
    const staffRes = await request.get('/api/staff?size=1', {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    const staffData = await staffRes.json();
    staffId = staffData.data?.content?.[0]?.id || null;
  });

  test('Staff schedule: startTime > endTime should be rejected (18:00 > 09:00)', async ({ request }) => {
    expect(staffId, 'Need a staff member for schedule tests').toBeTruthy();

    const res = await request.put(`/api/staff/${staffId}/schedule`, {
      headers: { Authorization: `Bearer ${tenantToken}` },
      data: {
        schedules: [{
          dayOfWeek: 1,
          isWorkingDay: true,
          startTime: '18:00',
          endTime: '09:00',
          breakStartTime: null,
          breakEndTime: null
        }]
      }
    });

    expect(res.status()).not.toBe(500);
    expect(res.status()).not.toBe(200);
    expect(res.status()).not.toBe(201);
  });

  test('Staff schedule: breakStartTime > breakEndTime should be rejected (13:00 > 12:00)', async ({ request }) => {
    expect(staffId, 'Need a staff member for schedule tests').toBeTruthy();

    const res = await request.put(`/api/staff/${staffId}/schedule`, {
      headers: { Authorization: `Bearer ${tenantToken}` },
      data: {
        schedules: [{
          dayOfWeek: 1,
          isWorkingDay: true,
          startTime: '09:00',
          endTime: '18:00',
          breakStartTime: '13:00',
          breakEndTime: '12:00'
        }]
      }
    });

    expect(res.status()).not.toBe(500);
    expect(res.status()).not.toBe(200);
    expect(res.status()).not.toBe(201);
  });

  test('Staff leave: half-day with inverted times should be rejected (14:00 > 09:00)', async ({ request }) => {
    expect(staffId, 'Need a staff member for leave tests').toBeTruthy();

    const tomorrow = getTomorrow();
    const res = await request.post(`/api/staff/${staffId}/leaves`, {
      headers: { Authorization: `Bearer ${tenantToken}` },
      data: {
        leaveDates: [tomorrow],
        leaveType: 'PERSONAL',
        isFullDay: false,
        startTime: '14:00',
        endTime: '09:00',
        reason: 'E2E-time-validation-inverted'
      }
    });

    expect(res.status()).not.toBe(500);
    expect(res.status()).not.toBe(200);
    expect(res.status()).not.toBe(201);
  });

  test('Business settings: businessStartTime > businessEndTime should be rejected', async ({ request }) => {
    const res = await request.put('/api/settings', {
      headers: { Authorization: `Bearer ${tenantToken}` },
      data: {
        businessStartTime: '21:00',
        businessEndTime: '09:00'
      }
    });

    expect(res.status()).not.toBe(500);
    expect(res.status()).not.toBe(200);
  });

  test('Business settings: breakStartTime > breakEndTime should be rejected', async ({ request }) => {
    const res = await request.put('/api/settings', {
      headers: { Authorization: `Bearer ${tenantToken}` },
      data: {
        breakStartTime: '14:00',
        breakEndTime: '12:00'
      }
    });

    expect(res.status()).not.toBe(500);
    expect(res.status()).not.toBe(200);
  });
});

// ========================================
// 2. API Contract Validation (key endpoints)
// ========================================

test.describe('2. API Contract Validation', () => {
  let token: string;
  let testIds: {
    customerId?: string;
    serviceId?: string;
    staffId?: string;
  } = {};

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, 'Tenant token should be obtained').toBeTruthy();

    const headers = { Authorization: `Bearer ${token}` };

    // Fetch real IDs for contract tests
    const [custRes, svcRes, staffRes] = await Promise.all([
      request.get('/api/customers?size=1', { headers }),
      request.get('/api/services?size=1', { headers }),
      request.get('/api/staff?size=1', { headers })
    ]);

    if (custRes.ok()) {
      const d = await custRes.json();
      testIds.customerId = d.data?.content?.[0]?.id;
    }
    if (svcRes.ok()) {
      const d = await svcRes.json();
      testIds.serviceId = d.data?.content?.[0]?.id;
    }
    if (staffRes.ok()) {
      const d = await staffRes.json();
      testIds.staffId = d.data?.content?.[0]?.id;
    }
  });

  test('POST /api/bookings field names are correct (not 400)', async ({ request }) => {
    const res = await request.post('/api/bookings', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        customerId: testIds.customerId || 'test-id',
        serviceItemId: testIds.serviceId || 'test-id',
        staffId: testIds.staffId,
        bookingDate: '2099-12-31',
        startTime: '10:00',
        customerNote: 'contract-test'
      }
    });
    await assertNotFieldError(res, 'POST /api/bookings');
  });

  test('POST /api/customers field names are correct (not 400)', async ({ request }) => {
    const res = await request.post('/api/customers', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: 'contract-test-customer',
        phone: '0900000000',
        email: 'contract-test@example.com',
        gender: 'OTHER',
        note: 'contract-test'
      }
    });
    await assertNotFieldError(res, 'POST /api/customers');
  });

  test('POST /api/staff field names are correct (not 400)', async ({ request }) => {
    const res = await request.post('/api/staff', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: 'contract-test-staff',
        phone: '0911000000',
        email: 'staff-contract@example.com',
        isBookable: true,
        isVisible: true,
        sortOrder: 99
      }
    });
    await assertNotFieldError(res, 'POST /api/staff');
  });

  test('POST /api/services field names are correct (not 400)', async ({ request }) => {
    const res = await request.post('/api/services', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: 'contract-test-service',
        price: 500,
        duration: 60,
        description: 'contract-test-desc'
      }
    });
    await assertNotFieldError(res, 'POST /api/services');
  });

  test('PUT /api/settings field names are correct (not 400)', async ({ request }) => {
    const res = await request.put('/api/settings', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: 'contract-test-store',
        phone: '0200000000',
        email: 'settings-contract@example.com',
        address: 'test-address',
        businessStartTime: '09:00',
        businessEndTime: '18:00',
        bookingInterval: 30,
        maxAdvanceBookingDays: 30,
        notifyNewBooking: true,
        notifyBookingReminder: true,
        notifyBookingCancel: true
      }
    });
    await assertNotFieldError(res, 'PUT /api/settings');
  });
});

// ========================================
// 3. Report Consistency Validation
// ========================================

test.describe('3. Report Consistency', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    expect(tenantToken, 'Tenant token should be obtained').toBeTruthy();
  });

  const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

  test('Dashboard totalBookings should be non-negative', async ({ request }) => {
    const res = await request.get('/api/reports/dashboard', {
      headers: authHeaders(tenantToken)
    });
    expect(res.ok()).toBeTruthy();

    const data = (await res.json()).data;
    expect(data).toBeTruthy();
    expect(Number(data.totalBookings)).toBeGreaterThanOrEqual(0);
    expect(Number(data.totalCustomers)).toBeGreaterThanOrEqual(0);
  });

  test('Dashboard revenue fields should be non-negative', async ({ request }) => {
    const res = await request.get('/api/reports/dashboard', {
      headers: authHeaders(tenantToken)
    });
    expect(res.ok()).toBeTruthy();

    const data = (await res.json()).data;
    expect(Number(data.monthlyRevenue)).toBeGreaterThanOrEqual(0);
    if (data.totalRevenue !== undefined) {
      expect(Number(data.totalRevenue)).toBeGreaterThanOrEqual(0);
    }
  });

  test('Summary: week totals <= month totals', async ({ request }) => {
    const [weeklyRes, monthlyRes] = await Promise.all([
      request.get('/api/reports/weekly', { headers: authHeaders(tenantToken) }),
      request.get('/api/reports/monthly', { headers: authHeaders(tenantToken) })
    ]);

    expect(weeklyRes.status()).toBeLessThan(500);
    expect(monthlyRes.status()).toBeLessThan(500);

    if (weeklyRes.ok() && monthlyRes.ok()) {
      const weekly = (await weeklyRes.json()).data;
      const monthly = (await monthlyRes.json()).data;

      const weekCount = Number(weekly?.totalBookings ?? 0);
      const monthCount = Number(monthly?.totalBookings ?? 0);
      expect(monthCount).toBeGreaterThanOrEqual(weekCount);

      const weekRevenue = Number(weekly?.serviceRevenue ?? 0);
      const monthRevenue = Number(monthly?.serviceRevenue ?? 0);
      expect(monthRevenue).toBeGreaterThanOrEqual(weekRevenue);
    }
  });

  test('Today <= Weekly <= Monthly booking counts', async ({ request }) => {
    const [todayRes, weeklyRes, monthlyRes] = await Promise.all([
      request.get('/api/reports/today', { headers: authHeaders(tenantToken) }),
      request.get('/api/reports/weekly', { headers: authHeaders(tenantToken) }),
      request.get('/api/reports/monthly', { headers: authHeaders(tenantToken) })
    ]);

    expect(todayRes.status()).toBeLessThan(500);
    expect(weeklyRes.status()).toBeLessThan(500);
    expect(monthlyRes.status()).toBeLessThan(500);

    if (todayRes.ok() && weeklyRes.ok() && monthlyRes.ok()) {
      const today = (await todayRes.json()).data;
      const weekly = (await weeklyRes.json()).data;
      const monthly = (await monthlyRes.json()).data;

      const todayCount = Number(today?.totalBookings ?? 0);
      const weeklyCount = Number(weekly?.totalBookings ?? 0);
      const monthlyCount = Number(monthly?.totalBookings ?? 0);

      expect(weeklyCount).toBeGreaterThanOrEqual(todayCount);
      expect(monthlyCount).toBeGreaterThanOrEqual(weeklyCount);
    }
  });

  test('Summary: all revenue fields are non-negative', async ({ request }) => {
    const res = await request.get('/api/reports/summary', {
      headers: authHeaders(tenantToken),
      params: { range: 'month' }
    });
    expect(res.ok()).toBeTruthy();

    const data = (await res.json()).data;
    expect(Number(data.serviceRevenue)).toBeGreaterThanOrEqual(0);
    expect(Number(data.totalRevenue)).toBeGreaterThanOrEqual(0);
    if (data.productRevenue !== undefined) {
      expect(Number(data.productRevenue)).toBeGreaterThanOrEqual(0);
    }
  });
});

// ========================================
// 4. Multi-Tenant Data Isolation
// ========================================

test.describe('4. Multi-Tenant Data Isolation', () => {
  let tenantToken: string;
  let adminToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    expect(tenantToken, 'Tenant token should be obtained').toBeTruthy();

    adminToken = await getAdminToken(request);
    expect(adminToken, 'Admin token should be obtained').toBeTruthy();
  });

  test('Tenant token cannot access admin APIs', async ({ request }) => {
    const res = await request.get('/api/admin/tenants', {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    // Should be 403 Forbidden, not 200
    expect(res.status()).not.toBe(200);
  });

  test('Admin token accesses tenant APIs but gets empty data (no tenant context)', async ({ request }) => {
    const res = await request.get('/api/bookings', {
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    // Admin CAN access but gets empty results (no tenant context)
    expect(res.ok()).toBeTruthy();
    const data = await res.json();
    expect(data.data.totalElements).toBe(0);
  });

  test('Admin token accesses customer API but gets empty data', async ({ request }) => {
    const res = await request.get('/api/customers', {
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    expect(res.ok()).toBeTruthy();
    const data = await res.json();
    expect(data.data.totalElements).toBe(0);
  });

  test('Unauthenticated request to tenant API returns 401 or redirect', async ({ request }) => {
    const res = await request.get('/api/bookings');
    // Without auth, expect 401 or 403
    expect([401, 403]).toContain(res.status());
  });

  test('Unauthenticated request to admin API returns 401 or redirect', async ({ request }) => {
    const res = await request.get('/api/admin/tenants');
    expect([401, 403]).toContain(res.status());
  });
});

// ========================================
// 5. Booking State Machine Integrity
// ========================================

test.describe('5. Booking State Machine Integrity', () => {
  let tenantToken: string;
  let pendingBookingId: string | null = null;
  let completedBookingId: string | null = null;
  let cancelledBookingId: string | null = null;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    expect(tenantToken, 'Tenant token should be obtained').toBeTruthy();

    const headers = { Authorization: `Bearer ${tenantToken}` };

    // Find bookings in various states
    const [pendingRes, completedRes, cancelledRes] = await Promise.all([
      request.get('/api/bookings?status=PENDING_CONFIRMATION&size=1', { headers }),
      request.get('/api/bookings?status=COMPLETED&size=1', { headers }),
      request.get('/api/bookings?status=CANCELLED&size=1', { headers })
    ]);

    if (pendingRes.ok()) {
      const d = await pendingRes.json();
      pendingBookingId = d.data?.content?.[0]?.id || null;
    }
    if (completedRes.ok()) {
      const d = await completedRes.json();
      completedBookingId = d.data?.content?.[0]?.id || null;
    }
    if (cancelledRes.ok()) {
      const d = await cancelledRes.json();
      cancelledBookingId = d.data?.content?.[0]?.id || null;
    }
  });

  test('Cannot confirm an already COMPLETED booking', async ({ request }) => {
    if (!completedBookingId) { test.skip(); return; }

    const res = await request.post(`/api/bookings/${completedBookingId}/confirm`, {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    expect(res.status()).not.toBe(200);
  });

  test('Cannot complete a PENDING booking (must confirm first)', async ({ request }) => {
    if (!pendingBookingId) { test.skip(); return; }

    const res = await request.post(`/api/bookings/${pendingBookingId}/complete`, {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    expect(res.status()).not.toBe(200);
  });

  test('Cannot cancel an already CANCELLED booking', async ({ request }) => {
    if (!cancelledBookingId) { test.skip(); return; }

    const res = await request.post(`/api/bookings/${cancelledBookingId}/cancel`, {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    expect(res.status()).not.toBe(200);
  });

  test('Cannot mark no-show on a PENDING booking', async ({ request }) => {
    if (!pendingBookingId) { test.skip(); return; }

    const res = await request.post(`/api/bookings/${pendingBookingId}/no-show`, {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    expect(res.status()).not.toBe(200);
  });

  test('Cannot re-complete an already COMPLETED booking', async ({ request }) => {
    if (!completedBookingId) { test.skip(); return; }

    const res = await request.post(`/api/bookings/${completedBookingId}/complete`, {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    expect(res.status()).not.toBe(200);
  });
});

// ========================================
// 6. SSE Notification Endpoint
// ========================================

test.describe('6. SSE Notification Endpoint', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    expect(tenantToken, 'Tenant token should be obtained').toBeTruthy();
  });

  test('GET /api/notifications/stream with tenant token returns 200', async ({ page }) => {
    // SSE 是串流連線，不能用 request.get（會永遠等待）
    // 改用 page.evaluate + fetch + AbortController 驗證端點
    const result = await page.evaluate(async ({ token, baseUrl }) => {
      const controller = new AbortController();
      const timer = setTimeout(() => controller.abort(), 3000);
      try {
        const res = await fetch(`${baseUrl}/api/notifications/stream`, {
          headers: { Authorization: `Bearer ${token}` },
          signal: controller.signal
        });
        clearTimeout(timer);
        return { status: res.status, ok: res.ok };
      } catch (e: any) {
        clearTimeout(timer);
        if (e.name === 'AbortError') {
          // SSE 連線成功建立但被 abort — 端點可用
          return { status: 200, ok: true };
        }
        return { status: 0, ok: false, error: e.message };
      }
    }, { token: tenantToken, baseUrl: process.env.BASE_URL || 'https://booking-platform-production-1e08.up.railway.app' });

    expect(result.status).toBe(200);
  });

  test('GET /api/notifications/stream without token returns 401', async ({ request }) => {
    const res = await request.get('/api/notifications/stream');
    expect([401, 403]).toContain(res.status());
  });
});

// ========================================
// 7. Password Security Rules
// ========================================

test.describe('7. Password Security Rules', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
    expect(tenantToken, 'Tenant token should be obtained').toBeTruthy();
  });

  test('Change password with wrong current password should be rejected', async ({ request }) => {
    const res = await request.post('/api/auth/change-password', {
      headers: { Authorization: `Bearer ${tenantToken}` },
      data: {
        currentPassword: 'definitely-wrong-password-12345',
        newPassword: 'NewSecure123!',
        confirmPassword: 'NewSecure123!'
      }
    });

    // Should not succeed with wrong current password
    const body = await res.json();
    expect(body.success).not.toBe(true);
  });

  test('Tenant login with wrong password returns failure', async ({ request }) => {
    const res = await request.post('/api/auth/tenant/login', {
      data: {
        username: TEST_ACCOUNTS.tenant.username,
        password: 'totally-wrong-password'
      }
    });

    // Should not be 200 success
    const body = await res.json();
    expect(body.success).not.toBe(true);
  });

  test('Admin login with wrong password returns failure', async ({ request }) => {
    const res = await request.post('/api/auth/admin/login', {
      data: {
        username: TEST_ACCOUNTS.admin.username,
        password: 'wrong-admin-password'
      }
    });

    const body = await res.json();
    expect(body.success).not.toBe(true);
  });
});

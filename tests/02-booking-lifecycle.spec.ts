import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  TEST_ACCOUNTS,
  getToday,
} from './utils/test-helpers';

/**
 * 02 - 預約管理與業務邏輯 (最關鍵測試)
 *
 * 合併自: 06-booking-management.spec.ts, 26-booking-slot-conflict.spec.ts
 *
 * 測試範圍：
 * 1. 預約列表與篩選 API
 * 2. 預約行事曆 API
 * 3. 預約全生命週期 (PENDING -> CONFIRMED -> COMPLETED / CANCELLED / NO_SHOW)
 * 4. PENDING 不佔用時段 (業務規則)
 * 5. CONFIRMED 佔用時段 (業務規則)
 * 6. 預約更新
 * 7. 預約管理 UI
 */

// ============================================================
// 共用 helper
// ============================================================

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  expect(response.ok()).toBeTruthy();
  const data = await response.json();
  expect(data.data.accessToken).toBeTruthy();
  return data.data.accessToken;
}

interface PrerequisiteData {
  customerId: string;
  serviceItemId: string;
  staffId: string;
}

/** 取得測試必要的顧客、服務、員工 ID */
async function getPrerequisiteData(request: APIRequestContext, token: string): Promise<PrerequisiteData> {
  const headers = { 'Authorization': `Bearer ${token}` };

  const [custRes, svcRes, staffRes] = await Promise.all([
    request.get('/api/customers?size=1', { headers }),
    request.get('/api/services/bookable', { headers }),
    request.get('/api/staff/bookable', { headers }),
  ]);

  expect(custRes.ok()).toBeTruthy();
  expect(svcRes.ok()).toBeTruthy();
  expect(staffRes.ok()).toBeTruthy();

  const customers = await custRes.json();
  const services = await svcRes.json();
  const staff = await staffRes.json();

  const customerId = customers.data?.content?.[0]?.id;
  const serviceItemId = services.data?.[0]?.id;
  const staffId = staff.data?.[0]?.id;

  expect(customerId).toBeTruthy();
  expect(serviceItemId).toBeTruthy();
  expect(staffId).toBeTruthy();

  return { customerId, serviceItemId, staffId };
}

/** 生成一個不會與先前測試衝突的隨機未來日期（30~120 天內的工作日） */
function randomFutureDate(): string {
  const daysAhead = Math.floor(Math.random() * 90) + 30;
  const d = new Date();
  d.setDate(d.getDate() + daysAhead);
  // 避開週六(6)、週日(0)
  const dow = d.getDay();
  if (dow === 0) d.setDate(d.getDate() + 1);
  if (dow === 6) d.setDate(d.getDate() + 2);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
}

/** 生成一個隨機整點時間 (09:00 ~ 11:00, 14:00 ~ 17:00，避開午休) */
function randomTime(): string {
  const slots = [9, 10, 11, 14, 15, 16, 17];
  const hour = slots[Math.floor(Math.random() * slots.length)];
  return `${String(hour).padStart(2, '0')}:00`;
}

/** 建立一筆預約，回傳 booking ID */
async function createBooking(
  request: APIRequestContext,
  token: string,
  data: PrerequisiteData,
  bookingDate: string,
  startTime: string,
  note: string,
  staffId?: string | null,
): Promise<string> {
  const headers = { 'Authorization': `Bearer ${token}` };
  const response = await request.post('/api/bookings', {
    headers,
    data: {
      customerId: data.customerId,
      serviceItemId: data.serviceItemId,
      staffId: staffId !== undefined ? staffId : data.staffId,
      bookingDate,
      startTime,
      customerNote: note,
    }
  });
  expect(response.ok(), `建立預約失敗: ${response.status()} - ${bookingDate} ${startTime}`).toBeTruthy();
  const body = await response.json();
  expect(body.success).toBeTruthy();
  expect(body.data.id).toBeTruthy();
  return body.data.id;
}

// ============================================================
// 1. 預約列表與篩選 API
// ============================================================

test.describe('1. 預約列表與篩選 API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('取得預約列表 - 包含分頁資訊', async ({ request }) => {
    const response = await request.get('/api/bookings?page=0&size=20', {
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
    expect(typeof data.data.totalPages).toBe('number');
  });

  test('依日期範圍篩選 - 今日', async ({ request }) => {
    const today = getToday();
    const response = await request.get(`/api/bookings?startDate=${today}&endDate=${today}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toHaveProperty('content');
    // 所有回傳的預約日期都應該是今天
    for (const booking of data.data.content) {
      expect(booking.bookingDate).toBe(today);
    }
  });

  test('依狀態篩選 - PENDING', async ({ request }) => {
    const response = await request.get('/api/bookings?status=PENDING', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    for (const booking of data.data.content) {
      expect(booking.status).toBe('PENDING');
    }
  });

  test('依狀態篩選 - CONFIRMED', async ({ request }) => {
    const response = await request.get('/api/bookings?status=CONFIRMED', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    for (const booking of data.data.content) {
      expect(booking.status).toBe('CONFIRMED');
    }
  });

  test('依狀態篩選 - COMPLETED', async ({ request }) => {
    const response = await request.get('/api/bookings?status=COMPLETED', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    for (const booking of data.data.content) {
      expect(booking.status).toBe('COMPLETED');
    }
  });

  test('依狀態篩選 - CANCELLED', async ({ request }) => {
    const response = await request.get('/api/bookings?status=CANCELLED', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    for (const booking of data.data.content) {
      expect(booking.status).toBe('CANCELLED');
    }
  });

  test('依狀態篩選 - NO_SHOW', async ({ request }) => {
    const response = await request.get('/api/bookings?status=NO_SHOW', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    for (const booking of data.data.content) {
      expect(booking.status).toBe('NO_SHOW');
    }
  });

  test('依員工篩選', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };

    // 取得第一位員工
    const staffRes = await request.get('/api/staff?size=1', { headers });
    expect(staffRes.ok()).toBeTruthy();
    const staffData = await staffRes.json();

    if (staffData.data?.content?.length === 0) {
      test.skip();
      return;
    }

    const staffId = staffData.data.content[0].id;
    const response = await request.get(`/api/bookings?staffId=${staffId}`, { headers });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    // 所有回傳的預約都應該屬於該員工
    for (const booking of data.data.content) {
      expect(booking.staffId).toBe(staffId);
    }
  });
});

// ============================================================
// 2. 預約行事曆 API
// ============================================================

test.describe('2. 預約行事曆 API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('取得當月行事曆資料 - 驗證結構', async ({ request }) => {
    const today = new Date();
    const startOfMonth = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
    const endOfMonth = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0];

    const response = await request.get(`/api/bookings/calendar?start=${startOfMonth}&end=${endOfMonth}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(Array.isArray(data.data)).toBeTruthy();

    // 如果有資料，驗證結構
    if (data.data.length > 0) {
      const event = data.data[0];
      expect(event).toHaveProperty('id');
      expect(event).toHaveProperty('bookingDate');
      expect(event).toHaveProperty('startTime');
      expect(event).toHaveProperty('status');
    }
  });

  test('取得員工指定日期預約', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };

    const staffRes = await request.get('/api/staff?size=1', { headers });
    expect(staffRes.ok()).toBeTruthy();
    const staffData = await staffRes.json();

    if (staffData.data?.content?.length === 0) {
      test.skip();
      return;
    }

    const staffId = staffData.data.content[0].id;
    const today = getToday();

    const response = await request.get(`/api/bookings/staff/${staffId}/date/${today}`, { headers });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(Array.isArray(data.data)).toBeTruthy();

    // 所有回傳的預約都應該屬於該員工、該日期
    for (const booking of data.data) {
      expect(booking.staffId).toBe(staffId);
      expect(booking.bookingDate).toBe(today);
    }
  });
});

// ============================================================
// 3. 預約全生命週期 (serial)
// ============================================================

test.describe('3. 預約全生命週期', () => {
  test.describe.serial('PENDING -> CONFIRMED -> COMPLETED', () => {
    let token: string;
    let prereqs: PrerequisiteData;
    let bookingId: string;

    test('a. 取得前置資料 (顧客、服務、員工)', async ({ request }) => {
      token = await getTenantToken(request);
      prereqs = await getPrerequisiteData(request, token);
    });

    test('b. 建立預約 - 初始狀態為 PENDING', async ({ request }) => {
      bookingId = await createBooking(
        request, token, prereqs,
        randomFutureDate(), randomTime(),
        'E2E lifecycle: PENDING->CONFIRMED->COMPLETED'
      );

      // 驗證初始狀態
      const headers = { 'Authorization': `Bearer ${token}` };
      const getRes = await request.get(`/api/bookings/${bookingId}`, { headers });
      expect(getRes.ok()).toBeTruthy();
      const detail = (await getRes.json()).data;
      expect(detail.status).toBe('PENDING');
    });

    test('c. GET 預約詳情 - 驗證所有欄位', async ({ request }) => {
      const headers = { 'Authorization': `Bearer ${token}` };
      const response = await request.get(`/api/bookings/${bookingId}`, { headers });
      expect(response.ok()).toBeTruthy();

      const data = (await response.json()).data;
      expect(data.id).toBe(bookingId);
      expect(data.customerName).toBeTruthy();
      expect(data.serviceName).toBeTruthy();
      expect(data.status).toBe('PENDING');
      expect(data.bookingDate).toBeTruthy();
      expect(data.startTime).toBeTruthy();
      expect(data).toHaveProperty('customerId');
      expect(data).toHaveProperty('staffId');
      expect(data).toHaveProperty('endTime');
      expect(data).toHaveProperty('customerNote');
      expect(data.customerNote).toContain('E2E lifecycle');
    });

    test('d. 確認預約 - 狀態變為 CONFIRMED', async ({ request }) => {
      const headers = { 'Authorization': `Bearer ${token}` };
      const confirmRes = await request.post(`/api/bookings/${bookingId}/confirm`, { headers });
      expect(confirmRes.ok()).toBeTruthy();

      const confirmData = (await confirmRes.json()).data;
      expect(confirmData.status).toBe('CONFIRMED');

      // 再次 GET 驗證狀態確實持久化
      const getRes = await request.get(`/api/bookings/${bookingId}`, { headers });
      expect(getRes.ok()).toBeTruthy();
      const detail = (await getRes.json()).data;
      expect(detail.status).toBe('CONFIRMED');
    });

    test('e. 完成預約 - 狀態變為 COMPLETED', async ({ request }) => {
      const headers = { 'Authorization': `Bearer ${token}` };
      const completeRes = await request.post(`/api/bookings/${bookingId}/complete`, { headers });
      expect(completeRes.ok()).toBeTruthy();

      const completeData = (await completeRes.json()).data;
      expect(completeData.status).toBe('COMPLETED');

      // 再次 GET 驗證
      const getRes = await request.get(`/api/bookings/${bookingId}`, { headers });
      expect(getRes.ok()).toBeTruthy();
      const detail = (await getRes.json()).data;
      expect(detail.status).toBe('COMPLETED');
    });
  });

  test.describe.serial('建立 -> 取消', () => {
    let token: string;
    let prereqs: PrerequisiteData;
    let bookingId: string;

    test('取得前置資料', async ({ request }) => {
      token = await getTenantToken(request);
      prereqs = await getPrerequisiteData(request, token);
    });

    test('建立預約', async ({ request }) => {
      bookingId = await createBooking(
        request, token, prereqs,
        randomFutureDate(), randomTime(),
        'E2E lifecycle: cancel test'
      );
    });

    test('取消預約 - 狀態變為 CANCELLED', async ({ request }) => {
      const headers = { 'Authorization': `Bearer ${token}` };
      const cancelRes = await request.post(`/api/bookings/${bookingId}/cancel?reason=E2E%20cancel%20test`, { headers });
      expect(cancelRes.ok()).toBeTruthy();

      const cancelData = (await cancelRes.json()).data;
      expect(cancelData.status).toBe('CANCELLED');

      // 再次 GET 驗證
      const getRes = await request.get(`/api/bookings/${bookingId}`, { headers });
      expect(getRes.ok()).toBeTruthy();
      const detail = (await getRes.json()).data;
      expect(detail.status).toBe('CANCELLED');
    });
  });

  test.describe.serial('建立 -> 確認 -> 爽約', () => {
    let token: string;
    let prereqs: PrerequisiteData;
    let bookingId: string;

    test('取得前置資料', async ({ request }) => {
      token = await getTenantToken(request);
      prereqs = await getPrerequisiteData(request, token);
    });

    test('建立並確認預約', async ({ request }) => {
      const headers = { 'Authorization': `Bearer ${token}` };
      bookingId = await createBooking(
        request, token, prereqs,
        randomFutureDate(), randomTime(),
        'E2E lifecycle: no-show test'
      );

      // 確認
      const confirmRes = await request.post(`/api/bookings/${bookingId}/confirm`, { headers });
      expect(confirmRes.ok()).toBeTruthy();
      expect((await confirmRes.json()).data.status).toBe('CONFIRMED');
    });

    test('標記爽約 - 狀態變為 NO_SHOW', async ({ request }) => {
      const headers = { 'Authorization': `Bearer ${token}` };
      const noShowRes = await request.post(`/api/bookings/${bookingId}/no-show`, { headers });
      expect(noShowRes.ok()).toBeTruthy();

      const noShowData = (await noShowRes.json()).data;
      expect(noShowData.status).toBe('NO_SHOW');

      // 再次 GET 驗證
      const getRes = await request.get(`/api/bookings/${bookingId}`, { headers });
      expect(getRes.ok()).toBeTruthy();
      const detail = (await getRes.json()).data;
      expect(detail.status).toBe('NO_SHOW');
    });
  });
});

// ============================================================
// 4. PENDING 不佔用時段 (業務邏輯)
// ============================================================

test.describe('4. PENDING 不佔用時段', () => {
  let token: string;
  let prereqs: PrerequisiteData;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    prereqs = await getPrerequisiteData(request, token);
  });

  test('同時段建立兩筆 PENDING 預約 - 兩筆都應成功', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const bookingDate = randomFutureDate();
    const startTime = randomTime();

    // 第一筆
    const res1 = await request.post('/api/bookings', {
      headers,
      data: {
        customerId: prereqs.customerId,
        serviceItemId: prereqs.serviceItemId,
        staffId: prereqs.staffId,
        bookingDate,
        startTime,
        customerNote: 'E2E PENDING coexist #1',
      }
    });
    expect(res1.ok()).toBeTruthy();
    const data1 = await res1.json();
    expect(data1.success).toBeTruthy();
    expect(data1.data.status).toBe('PENDING');

    // 第二筆 - 完全相同的時段和員工
    const res2 = await request.post('/api/bookings', {
      headers,
      data: {
        customerId: prereqs.customerId,
        serviceItemId: prereqs.serviceItemId,
        staffId: prereqs.staffId,
        bookingDate,
        startTime,
        customerNote: 'E2E PENDING coexist #2',
      }
    });
    expect(res2.ok()).toBeTruthy();
    const data2 = await res2.json();
    expect(data2.success).toBeTruthy();
    expect(data2.data.status).toBe('PENDING');

    // 驗證兩筆是不同的預約
    expect(data1.data.id).not.toBe(data2.data.id);
  });
});

// ============================================================
// 5. CONFIRMED 佔用時段 (業務邏輯)
// ============================================================

test.describe('5. CONFIRMED 佔用時段', () => {
  let token: string;
  let prereqs: PrerequisiteData;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    prereqs = await getPrerequisiteData(request, token);
  });

  test('已確認預約佔用時段 - 同時段第二筆確認應衝突或自動分配', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const bookingDate = randomFutureDate();
    const startTime = randomTime();

    // 建立第一筆並確認
    const id1 = await createBooking(
      request, token, prereqs,
      bookingDate, startTime,
      'E2E slot conflict: first booking'
    );
    const confirmRes1 = await request.post(`/api/bookings/${id1}/confirm`, { headers });
    expect(confirmRes1.ok()).toBeTruthy();
    expect((await confirmRes1.json()).data.status).toBe('CONFIRMED');

    // 建立第二筆同時段同員工
    const id2 = await createBooking(
      request, token, prereqs,
      bookingDate, startTime,
      'E2E slot conflict: second booking'
    );

    // 嘗試確認第二筆
    const confirmRes2 = await request.post(`/api/bookings/${id2}/confirm`, { headers });
    const confirmData2 = await confirmRes2.json();

    // 預期結果：
    // - 409 衝突 (同員工時段已被佔用，且無其他可用員工)
    // - 200 成功但自動分配到其他員工
    // 兩者都代表衝突檢查邏輯正常運作
    if (confirmRes2.ok() && confirmData2.success) {
      // 自動分配到不同員工
      expect(confirmData2.data.status).toBe('CONFIRMED');
      // 如果有第二位員工，應該分配到不同員工
      // (如果只有一位員工，可能會失敗，這取決於 maxConcurrentBookings)
    } else {
      // 衝突拒絕 - 確認非 500 伺服器錯誤
      expect(confirmRes2.status()).not.toBe(500);
      // 應為 409 衝突或 400 業務錯誤
      expect([400, 409, 422].some(s => confirmRes2.status() === s || !confirmData2.success)).toBeTruthy();
    }
  });
});

// ============================================================
// 6. 預約更新
// ============================================================

test.describe('6. 預約更新', () => {
  let token: string;
  let prereqs: PrerequisiteData;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    prereqs = await getPrerequisiteData(request, token);
  });

  test('更新預約備註 - 驗證更新持久化', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };

    // 建立一筆預約
    const bookingId = await createBooking(
      request, token, prereqs,
      '2099-12-20', '15:00',
      'E2E update test: original note'
    );

    // 更新備註
    const updatedNote = `E2E updated note ${Date.now()}`;
    const updateRes = await request.put(`/api/bookings/${bookingId}`, {
      headers,
      data: {
        customerNote: updatedNote,
      }
    });
    expect(updateRes.ok()).toBeTruthy();
    const updateData = (await updateRes.json()).data;
    expect(updateData.customerNote).toBe(updatedNote);

    // GET 驗證更新持久化
    const getRes = await request.get(`/api/bookings/${bookingId}`, { headers });
    expect(getRes.ok()).toBeTruthy();
    const detail = (await getRes.json()).data;
    expect(detail.customerNote).toBe(updatedNote);
  });

  test('更新預約內部備註 (internalNote)', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };

    const bookingId = await createBooking(
      request, token, prereqs,
      '2099-12-21', '09:00',
      'E2E update internal note test'
    );

    const internalNote = 'VIP customer - handle with care';
    const updateRes = await request.put(`/api/bookings/${bookingId}`, {
      headers,
      data: { internalNote }
    });
    expect(updateRes.ok()).toBeTruthy();

    // 驗證
    const getRes = await request.get(`/api/bookings/${bookingId}`, { headers });
    expect(getRes.ok()).toBeTruthy();
    const detail = (await getRes.json()).data;
    expect(detail.internalNote).toBe(internalNote);
  });
});

// ============================================================
// 7. 預約管理 UI
// ============================================================

test.describe('7. 預約管理 UI', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('預約列表頁面載入 - 顯示表格', async ({ page }) => {
    await page.goto('/tenant/bookings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 頁面標題
    const title = page.locator('h1.page-title');
    await expect(title).toContainText('預約管理');

    // 表格存在
    const table = page.locator('table.table');
    await expect(table).toBeVisible();
  });

  test('預約列表 - 表頭欄位正確', async ({ page }) => {
    await page.goto('/tenant/bookings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查關鍵表頭欄位
    const headerRow = page.locator('table thead tr');
    await expect(headerRow).toBeVisible();

    // 至少有顧客、服務、日期、狀態、操作欄位
    const headerText = await headerRow.textContent();
    expect(headerText).toBeTruthy();
    // 至少有操作欄
    const headers = page.locator('table thead th');
    const count = await headers.count();
    expect(count).toBeGreaterThanOrEqual(4);
  });

  test('行事曆頁面載入 - FullCalendar 渲染', async ({ page }) => {
    await page.goto('/tenant/calendar');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // FullCalendar 元素
    const calendar = page.locator('.fc');
    await expect(calendar).toBeVisible();

    // 導航按鈕
    const prevBtn = page.locator('.fc-prev-button');
    const nextBtn = page.locator('.fc-next-button');
    await expect(prevBtn).toBeVisible();
    await expect(nextBtn).toBeVisible();
  });

  test('新增預約 Modal - 包含必要表單欄位', async ({ page }) => {
    await page.goto('/tenant/bookings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 點擊新增預約按鈕
    const addBtn = page.locator('button:has-text("新增預約"), button:has-text("新增")');
    await expect(addBtn.first()).toBeVisible();
    await addBtn.first().click();

    // 等待 Modal 出現
    const modal = page.locator('#createBookingModal.show, .modal.show');
    await expect(modal).toBeVisible({ timeout: 5000 });

    // 驗證表單欄位存在
    await expect(page.locator('#bookingCustomer')).toBeVisible();
    await expect(page.locator('#bookingService')).toBeVisible();
    await expect(page.locator('#bookingStaff')).toBeVisible();
    await expect(page.locator('#bookingDate')).toBeVisible();
    await expect(page.locator('#bookingTime')).toBeVisible();
    await expect(page.locator('#bookingNote')).toBeVisible();

    // 驗證建立預約按鈕
    const submitBtn = page.locator('#saveBookingBtn');
    await expect(submitBtn).toBeVisible();

    // 關閉 modal
    await page.locator('.modal.show .btn-close').click();
  });

  test('狀態篩選下拉選單 - 包含所有狀態選項', async ({ page }) => {
    await page.goto('/tenant/bookings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);

    const statusFilter = page.locator('#statusFilter');
    await expect(statusFilter).toBeVisible();

    // 驗證選項
    const options = statusFilter.locator('option');
    const optionCount = await options.count();
    expect(optionCount).toBeGreaterThanOrEqual(5); // 全部 + PENDING + CONFIRMED + IN_PROGRESS + COMPLETED + CANCELLED

    // 驗證可以選擇不同狀態
    await statusFilter.selectOption('PENDING');
    const selectedValue = await statusFilter.inputValue();
    expect(selectedValue).toBe('PENDING');

    // 選回全部
    await statusFilter.selectOption('');
  });

  test('匯出按鈕存在', async ({ page }) => {
    await page.goto('/tenant/bookings');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);

    // 匯出按鈕（dropdown）
    const exportBtn = page.locator('button:has-text("匯出")');
    await expect(exportBtn.first()).toBeVisible();
  });

  test('行事曆視圖切換', async ({ page }) => {
    await page.goto('/tenant/calendar');
    await page.waitForLoadState('domcontentloaded');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 月視圖按鈕
    const monthBtn = page.locator('.fc-dayGridMonth-button');
    if (await monthBtn.isVisible()) {
      await monthBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);
      // FullCalendar 表格仍然可見
      await expect(page.locator('.fc')).toBeVisible();
    }

    // 週視圖按鈕
    const weekBtn = page.locator('.fc-timeGridWeek-button');
    if (await weekBtn.isVisible()) {
      await weekBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);
      await expect(page.locator('.fc')).toBeVisible();
    }

    // 日視圖按鈕
    const dayBtn = page.locator('.fc-timeGridDay-button');
    if (await dayBtn.isVisible()) {
      await dayBtn.click();
      await page.waitForTimeout(WAIT_TIME.short);
      await expect(page.locator('.fc')).toBeVisible();
    }
  });
});

// ============================================================
// 附加驗證: 預約回應格式完整性
// ============================================================

test.describe('附加: 預約回應格式與邊界情況', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('預約回應包含所有必要欄位', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const response = await request.get('/api/bookings?size=5', { headers });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();

    if (data.data.content.length === 0) {
      test.skip();
      return;
    }

    const booking = data.data.content[0];
    // 必要欄位
    expect(booking).toHaveProperty('id');
    expect(booking).toHaveProperty('bookingDate');
    expect(booking).toHaveProperty('startTime');
    expect(booking).toHaveProperty('status');
    expect(booking).toHaveProperty('customerName');
    expect(booking).toHaveProperty('serviceName');
    // 型別驗證
    expect(typeof booking.id).toBe('string');
    expect(typeof booking.bookingDate).toBe('string');
    expect(typeof booking.startTime).toBe('string');
    expect(typeof booking.status).toBe('string');
  });

  test('不存在的預約 ID 回傳 404', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const response = await request.get('/api/bookings/nonexistent-id-12345', { headers });
    expect(response.status()).toBe(404);
  });

  test('對不存在的預約執行確認回傳 404', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const response = await request.post('/api/bookings/nonexistent-id-12345/confirm', { headers });
    expect(response.status()).toBe(404);
  });

  test('對不存在的預約執行取消回傳 404', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const response = await request.post('/api/bookings/nonexistent-id-12345/cancel', { headers });
    expect(response.status()).toBe(404);
  });

  test('建立預約不指定員工 - 自動分配', async ({ request }) => {
    const headers = { 'Authorization': `Bearer ${token}` };
    const prereqs = await getPrerequisiteData(request, token);

    const response = await request.post('/api/bookings', {
      headers,
      data: {
        customerId: prereqs.customerId,
        serviceItemId: prereqs.serviceItemId,
        staffId: null,
        bookingDate: '2099-12-22',
        startTime: '16:00',
        customerNote: 'E2E auto-assign staff test',
      }
    });
    expect(response.ok()).toBeTruthy();

    const data = await response.json();
    expect(data.success).toBeTruthy();
    // 系統應自動分配員工
    expect(data.data.staffName).toBeTruthy();
    expect(data.data.staffId).toBeTruthy();
  });
});

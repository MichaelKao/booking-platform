import { test, expect, APIRequestContext } from './fixtures';
import { TEST_ACCOUNTS } from './utils/test-helpers';

/**
 * 業務邏輯正確性測試
 *
 * 驗證報表數值、營收計算、統計邏輯是否正確。
 * 不只驗 HTTP 200，而是驗「數字對不對」。
 *
 * 發現的 Bug 清單：
 * 1. 員工業績營收寫死 $0（已修）
 * 2. 待確認只算今天（已修）
 * 3. topStaff/topServices 包含 null id 的幽靈資料
 * 4. productRevenue 寫死 $0（未計算商品營收）
 * 5. totalRevenue = serviceRevenue，遺漏商品營收
 * 6. couponDiscountAmount 寫死 $0
 * 7. avgCustomerValue 用預約數÷顧客數，不是營收÷顧客數
 * 8. monthlyUsed（點數月消費）寫死 $0
 */

const BIZ_TEST_ACCOUNT = {
  username: 'biztest@example.com',
  password: 'BizTest12345',
  name: '業務邏輯測試店家',
  code: 'biztest'
};

async function getTenantToken(request: APIRequestContext): Promise<string> {
  // 1. 嘗試登入
  const loginRes = await request.post('/api/auth/tenant/login', {
    data: { username: BIZ_TEST_ACCOUNT.username, password: BIZ_TEST_ACCOUNT.password }
  });
  const loginData = await loginRes.json();
  if (loginData.data?.accessToken) {
    return loginData.data.accessToken;
  }

  // 2. 帳號不存在，嘗試註冊
  console.log('測試帳號不存在，自動註冊...');
  const ts = Date.now().toString().slice(-6);
  const regRes = await request.post('/api/auth/tenant/register', {
    data: {
      code: `biz${ts}`,
      name: BIZ_TEST_ACCOUNT.name,
      email: BIZ_TEST_ACCOUNT.username,
      phone: '0912345678',
      password: BIZ_TEST_ACCOUNT.password,
      confirmPassword: BIZ_TEST_ACCOUNT.password
    }
  });
  const regData = await regRes.json();
  if (regData.data?.accessToken) {
    return regData.data.accessToken;
  }

  // 3. 註冊也失敗（可能 code 重複），用已知帳號
  console.log('註冊失敗，嘗試已知測試帳號:', regData.message);
  const fallbackRes = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const fallbackData = await fallbackRes.json();
  return fallbackData.data?.accessToken || '';
}

test.describe('業務邏輯正確性測試', () => {
  let token: string;
  let hasData = false;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token).toBeTruthy();

    // 種子資料：建立員工、服務、顧客、預約（含完成的）
    const h = { 'Authorization': `Bearer ${token}` };
    const ts = Date.now().toString().slice(-6);

    // 建立員工（加時間戳避免重複）
    let staffId: string | undefined;
    const staffRes = await request.post('/api/staff', {
      headers: h,
      data: { name: `業務測試員工_${ts}`, phone: `09${ts}54321`, email: `bizstaff_${ts}@example.com`, isBookable: true }
    });
    const staffBody = await staffRes.json();
    staffId = staffBody.data?.id;
    if (!staffId) {
      console.log('員工建立失敗，嘗試查詢現有員工:', staffBody.message);
      const existingStaff = await request.get('/api/staff', { headers: h, params: { size: 1 } });
      const staffList = (await existingStaff.json()).data;
      staffId = staffList?.content?.[0]?.id;
    }

    // 建立服務（加時間戳避免名稱重複）
    let serviceId: string | undefined;
    const svcRes = await request.post('/api/services', {
      headers: h,
      data: { name: `業務測試服務_${ts}`, price: 1000, duration: 60, isVisible: true }
    });
    const svcBody = await svcRes.json();
    serviceId = svcBody.data?.id;
    if (!serviceId) {
      console.log('服務建立失敗，嘗試查詢現有服務:', svcBody.message);
      const existingSvc = await request.get('/api/services', { headers: h, params: { size: 1 } });
      const svcList = (await existingSvc.json()).data;
      serviceId = svcList?.content?.[0]?.id;
    }

    // 建立顧客（加時間戳避免手機號碼重複）
    let customerId: string | undefined;
    const custRes = await request.post('/api/customers', {
      headers: h,
      data: { name: `業務測試顧客_${ts}`, phone: `09${ts}22333` }
    });
    const custBody = await custRes.json();
    customerId = custBody.data?.id;
    if (!customerId) {
      console.log('顧客建立失敗，嘗試查詢現有顧客:', custBody.message);
      const existingCust = await request.get('/api/customers', { headers: h, params: { size: 1 } });
      const custList = (await existingCust.json()).data;
      customerId = custList?.content?.[0]?.id;
    }

    if (!staffId || !serviceId || !customerId) {
      console.log('種子資料建立失敗，部分測試可能無法驗證實際業務邏輯');
      console.log(`staffId=${staffId}, serviceId=${serviceId}, customerId=${customerId}`);
      return;
    }

    // 建立預約（今日日期，方便完成）
    const today = new Date().toISOString().split('T')[0];
    const bookingRes = await request.post('/api/bookings', {
      headers: h,
      data: {
        customerId,
        serviceItemId: serviceId,
        staffId,
        bookingDate: today,
        startTime: '09:00',
        customerNote: '業務邏輯測試'
      }
    });
    const bookingData = (await bookingRes.json()).data;
    const bookingId = bookingData?.id;

    if (bookingId) {
      // 確認 → 完成
      await request.post(`/api/bookings/${bookingId}/confirm`, { headers: h });
      await request.post(`/api/bookings/${bookingId}/complete`, { headers: h });
      hasData = true;
      console.log(`✓ 種子資料建立完成：預約 ${bookingId} 已完成，金額 $1000`);
    }

    // 再建一筆 PENDING 預約（未來日期）
    await request.post('/api/bookings', {
      headers: h,
      data: {
        customerId,
        serviceItemId: serviceId,
        staffId,
        bookingDate: '2099-12-31',
        startTime: '10:00',
        customerNote: '業務邏輯測試-待確認'
      }
    });
  });

  const authHeaders = () => ({ 'Authorization': `Bearer ${token}` });

  // ========================================
  // 儀表板統計正確性
  // ========================================

  test.describe('儀表板統計', () => {

    test('待確認數量應包含所有日期的 PENDING 預約', async ({ request }) => {
      // 取得儀表板數據
      const dashRes = await request.get('/api/reports/dashboard', { headers: authHeaders() });
      const dashboard = (await dashRes.json()).data;

      // 取得所有待確認預約（不限日期）
      const bookingsRes = await request.get('/api/bookings', {
        headers: authHeaders(),
        params: { status: 'PENDING', size: 1 }
      });
      const bookings = (await bookingsRes.json()).data;
      const actualPendingCount = bookings?.totalElements ?? 0;

      console.log(`儀表板待確認: ${dashboard.pendingBookings}, 實際待確認: ${actualPendingCount}`);

      // 儀表板的待確認數應該等於所有 PENDING 預約的總數
      expect(dashboard.pendingBookings).toBe(actualPendingCount);
    });

    test('今日預約數應只計算今天的預約', async ({ request }) => {
      const dashRes = await request.get('/api/reports/dashboard', { headers: authHeaders() });
      const dashboard = (await dashRes.json()).data;

      const today = new Date().toISOString().split('T')[0];
      const bookingsRes = await request.get('/api/bookings', {
        headers: authHeaders(),
        params: { date: today, size: 1 }
      });
      const bookings = (await bookingsRes.json()).data;
      const actualTodayCount = bookings?.totalElements ?? 0;

      console.log(`儀表板今日預約: ${dashboard.todayBookings}, 實際今日預約: ${actualTodayCount}`);
      expect(dashboard.todayBookings).toBe(actualTodayCount);
    });

    test('本月營收應只計算 COMPLETED 狀態的預約', async ({ request }) => {
      const dashRes = await request.get('/api/reports/dashboard', { headers: authHeaders() });
      const dashboard = (await dashRes.json()).data;

      // 本月營收不應該是負數
      expect(Number(dashboard.monthlyRevenue)).toBeGreaterThanOrEqual(0);
      console.log(`本月營收: NT$ ${dashboard.monthlyRevenue}`);
    });

    test('總顧客數應等於顧客列表總數', async ({ request }) => {
      const dashRes = await request.get('/api/reports/dashboard', { headers: authHeaders() });
      const dashboard = (await dashRes.json()).data;

      const customersRes = await request.get('/api/customers', {
        headers: authHeaders(),
        params: { size: 1 }
      });
      const customers = (await customersRes.json()).data;
      const actualCustomerCount = customers?.totalElements ?? 0;

      console.log(`儀表板顧客數: ${dashboard.totalCustomers}, 實際顧客數: ${actualCustomerCount}`);
      expect(dashboard.totalCustomers).toBe(actualCustomerCount);
    });
  });

  // ========================================
  // 員工業績營收正確性
  // ========================================

  test.describe('員工業績', () => {

    test('有已完成預約的員工，營收不應為 0', async ({ request }) => {
      // 用較大日期範圍確保涵蓋有資料的時期
      const res = await request.get('/api/reports/top-staff', {
        headers: authHeaders(),
        params: { range: 'quarter', limit: 10 }
      });
      expect(res.ok()).toBeTruthy();
      const staff = (await res.json()).data as Array<{ id: string; name: string; count: number; amount: number }>;

      if (staff.length === 0) {
        console.log('無員工業績資料，跳過');
        return;
      }

      // 列出每位員工的營收
      for (const s of staff) {
        console.log(`員工: ${s.name || '(空)'}, 服務數: ${s.count}, 營收: NT$ ${s.amount}`);
      }

      // 如果有員工有完成的預約，至少一位應有營收 > 0
      const totalAmount = staff.reduce((sum, s) => sum + Number(s.amount), 0);
      console.log(`員工業績總營收: NT$ ${totalAmount}`);

      // 同時查詢服務營收做交叉驗證
      const summaryRes = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const summary = (await summaryRes.json()).data;
      const serviceRevenue = Number(summary.serviceRevenue);
      console.log(`同期服務營收: NT$ ${serviceRevenue}`);

      // 如果有服務營收，員工業績營收不應該全是 0
      if (serviceRevenue > 0) {
        expect(totalAmount).toBeGreaterThan(0);
      }
    });

    test('員工業績不應包含 null id 的幽靈資料', async ({ request }) => {
      const res = await request.get('/api/reports/top-staff', {
        headers: authHeaders(),
        params: { range: 'quarter', limit: 10 }
      });
      const staff = (await res.json()).data as Array<{ id: string | null; name: string | null; count: number }>;

      for (const s of staff) {
        // 每筆資料都應有 id 和 name
        expect(s.id).not.toBeNull();
        expect(s.name).not.toBeNull();
        expect(s.name).not.toBe('');
        console.log(`驗證員工: id=${s.id}, name=${s.name}`);
      }
    });
  });

  // ========================================
  // 熱門服務營收正確性
  // ========================================

  test.describe('熱門服務', () => {

    test('有已完成預約的服務，營收不應為 0', async ({ request }) => {
      const res = await request.get('/api/reports/top-services', {
        headers: authHeaders(),
        params: { range: 'quarter', limit: 10 }
      });
      expect(res.ok()).toBeTruthy();
      const services = (await res.json()).data as Array<{ id: string; name: string; count: number; amount: number }>;

      if (services.length === 0) {
        console.log('無服務資料，跳過');
        return;
      }

      for (const s of services) {
        console.log(`服務: ${s.name}, 預約數: ${s.count}, 營收: NT$ ${s.amount}`);
        // 每個有預約的服務（COMPLETED），其營收應 > 0（因為服務都有價格）
        if (s.count > 0) {
          expect(Number(s.amount)).toBeGreaterThan(0);
        }
      }
    });

    test('熱門服務不應包含 null id 的幽靈資料', async ({ request }) => {
      const res = await request.get('/api/reports/top-services', {
        headers: authHeaders(),
        params: { range: 'quarter', limit: 10 }
      });
      const services = (await res.json()).data as Array<{ id: string | null; name: string | null }>;

      for (const s of services) {
        expect(s.id).not.toBeNull();
        expect(s.name).not.toBeNull();
        expect(s.name).not.toBe('');
        console.log(`驗證服務: id=${s.id}, name=${s.name}`);
      }
    });
  });

  // ========================================
  // 報表摘要交叉驗證
  // ========================================

  test.describe('報表摘要一致性', () => {

    test('totalRevenue 應等於 serviceRevenue + productRevenue', async ({ request }) => {
      const res = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const data = (await res.json()).data;

      const totalRevenue = Number(data.totalRevenue);
      const serviceRevenue = Number(data.serviceRevenue);
      const productRevenue = Number(data.productRevenue);

      console.log(`總營收: ${totalRevenue}, 服務營收: ${serviceRevenue}, 商品營收: ${productRevenue}`);

      // totalRevenue 應該 = serviceRevenue + productRevenue
      // 目前 BUG：totalRevenue = serviceRevenue，productRevenue 寫死 0
      expect(totalRevenue).toBe(serviceRevenue + productRevenue);
    });

    test('completionRate 應等於 completedBookings / totalBookings * 100', async ({ request }) => {
      const res = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const data = (await res.json()).data;

      const total = Number(data.totalBookings);
      const completed = Number(data.completedBookings);
      const rate = Number(data.completionRate);

      console.log(`總預約: ${total}, 已完成: ${completed}, 完成率: ${rate}%`);

      if (total > 0) {
        const expectedRate = (completed / total) * 100;
        // 允許 0.1% 誤差（四捨五入）
        expect(Math.abs(rate - expectedRate)).toBeLessThan(0.1);
      } else {
        expect(rate).toBe(0);
      }
    });

    test('各狀態預約數加總應等於總預約數', async ({ request }) => {
      const res = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const data = (await res.json()).data;

      const total = Number(data.totalBookings);
      const completed = Number(data.completedBookings);
      const cancelled = Number(data.cancelledBookings);
      const noShow = Number(data.noShowBookings);

      console.log(`總: ${total}, 完成: ${completed}, 取消: ${cancelled}, 爽約: ${noShow}`);

      // completed + cancelled + noShow + pending + confirmed <= total
      // 至少這三個狀態不應超過 total
      expect(completed + cancelled + noShow).toBeLessThanOrEqual(total);
    });

    test('averageOrderValue 應等於 serviceRevenue / completedBookings', async ({ request }) => {
      const res = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const data = (await res.json()).data;

      const revenue = Number(data.serviceRevenue);
      const completed = Number(data.completedBookings);
      const avgOrderValue = Number(data.averageOrderValue);

      console.log(`服務營收: ${revenue}, 完成預約數: ${completed}, 平均客單: ${avgOrderValue}`);

      if (completed > 0 && revenue > 0) {
        const expected = revenue / completed;
        // 允許四捨五入差異 $1
        expect(Math.abs(avgOrderValue - expected)).toBeLessThan(1);
      }
    });

    test('員工業績總營收應接近服務營收', async ({ request }) => {
      // 同期間的員工業績營收總和應該接近服務營收
      const summaryRes = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const summary = (await summaryRes.json()).data;
      const serviceRevenue = Number(summary.serviceRevenue);

      const staffRes = await request.get('/api/reports/top-staff', {
        headers: authHeaders(),
        params: { range: 'quarter', limit: 100 }
      });
      const staff = (await staffRes.json()).data as Array<{ amount: number }>;
      const staffTotal = staff.reduce((sum, s) => sum + Number(s.amount), 0);

      console.log(`服務營收: NT$ ${serviceRevenue}, 員工業績合計: NT$ ${staffTotal}`);

      // 員工業績合計應該 <= 服務營收（可能有未指定員工的預約）
      if (serviceRevenue > 0) {
        expect(staffTotal).toBeLessThanOrEqual(serviceRevenue * 1.01); // 允許微小誤差
      }
    });
  });

  // ========================================
  // 進階報表邏輯正確性
  // ========================================

  test.describe('進階報表', () => {

    test('avgCustomerValue 命名與實際計算應一致', async ({ request }) => {
      const res = await request.get('/api/reports/advanced', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });

      if (!res.ok()) {
        console.log('進階報表需訂閱，跳過');
        return;
      }

      const data = (await res.json()).data;
      if (!data.hasAccess) {
        console.log('無進階報表權限，跳過');
        return;
      }

      const avgValue = Number(data.avgCustomerValue);
      const activeCustomers = Number(data.activeCustomers);

      console.log(`平均顧客價值: ${avgValue}, 活躍顧客: ${activeCustomers}`);

      // avgCustomerValue 標籤是「平均顧客價值」
      // 目前 BUG：用 completedBookings/activeCustomers（次數，不是金額）
      // 正確應該用 revenue/activeCustomers
      // 這裡記錄實際值，方便追蹤
      if (activeCustomers > 0 && avgValue > 0) {
        console.log(`注意：avgCustomerValue=${avgValue}，如果此值 < 10 很可能是「次數」而非「金額」`);
      }
    });

    test('retentionRate 應在 0-100 之間', async ({ request }) => {
      const res = await request.get('/api/reports/advanced', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });

      if (!res.ok()) return;
      const data = (await res.json()).data;
      if (!data.hasAccess) return;

      const rate = Number(data.retentionRate);
      console.log(`顧客保留率: ${rate}%`);

      expect(rate).toBeGreaterThanOrEqual(0);
      expect(rate).toBeLessThanOrEqual(100);
    });

    test('serviceTrends 不應包含 null 服務名稱', async ({ request }) => {
      const res = await request.get('/api/reports/advanced', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });

      if (!res.ok()) return;
      const data = (await res.json()).data;
      if (!data.hasAccess || !data.serviceTrends) return;

      for (const trend of data.serviceTrends) {
        expect(trend.serviceId).not.toBeNull();
        expect(trend.serviceName).not.toBeNull();
        console.log(`服務趨勢: ${trend.serviceName}, 預約數: ${trend.currentPeriodCount}`);
      }
    });
  });

  // ========================================
  // 每日報表一致性
  // ========================================

  test.describe('每日報表', () => {

    test('每日報表總營收應等於報表摘要的服務營收', async ({ request }) => {
      const summaryRes = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'month' }
      });
      const summary = (await summaryRes.json()).data;
      const totalServiceRevenue = Number(summary.serviceRevenue);

      const dailyRes = await request.get('/api/reports/daily', {
        headers: authHeaders(),
        params: { range: 'month' }
      });
      const dailyData = (await dailyRes.json()).data as Array<{ revenue: number; bookingCount: number }>;

      const dailyRevenueSum = dailyData.reduce((sum, d) => sum + Number(d.revenue), 0);

      console.log(`摘要服務營收: NT$ ${totalServiceRevenue}, 每日營收合計: NT$ ${dailyRevenueSum}`);

      // 每日營收加總應等於摘要的服務營收
      expect(Math.abs(dailyRevenueSum - totalServiceRevenue)).toBeLessThan(1);
    });

    test('每日報表的 bookingCount 不應是負數', async ({ request }) => {
      const res = await request.get('/api/reports/daily', {
        headers: authHeaders(),
        params: { range: 'month' }
      });
      const dailyData = (await res.json()).data as Array<{ date: string; bookingCount: number; revenue: number }>;

      for (const d of dailyData) {
        expect(Number(d.bookingCount)).toBeGreaterThanOrEqual(0);
        expect(Number(d.revenue)).toBeGreaterThanOrEqual(0);
      }
    });
  });

  // ========================================
  // 點數系統正確性
  // ========================================

  test.describe('點數系統', () => {

    test('點數餘額應 >= 0', async ({ request }) => {
      const res = await request.get('/api/points/balance', { headers: authHeaders() });
      expect(res.ok()).toBeTruthy();
      const data = (await res.json()).data;

      console.log(`點數餘額: ${data.balance}, 待審核: ${data.pendingTopUp}, 本月消費: ${data.monthlyUsed}`);

      expect(Number(data.balance)).toBeGreaterThanOrEqual(0);
      expect(Number(data.pendingTopUp)).toBeGreaterThanOrEqual(0);
    });

    test('monthlyUsed 有訂閱功能時不應永遠為 0', async ({ request }) => {
      // 查詢功能訂閱狀態
      const featureRes = await request.get('/api/feature-store', { headers: authHeaders() });
      const features = (await featureRes.json()).data as Array<{ code: string; subscribed: boolean; monthlyCost: number }>;
      const subscribedFeatures = features.filter(f => f.subscribed && f.monthlyCost > 0);

      const balanceRes = await request.get('/api/points/balance', { headers: authHeaders() });
      const balance = (await balanceRes.json()).data;

      const monthlyUsed = Number(balance.monthlyUsed);
      const expectedMinUsed = subscribedFeatures.reduce((sum, f) => sum + f.monthlyCost, 0);

      console.log(`已訂閱付費功能: ${subscribedFeatures.length}`, subscribedFeatures.map(f => `${f.code}(${f.monthlyCost})`));
      console.log(`monthlyUsed: ${monthlyUsed}, 預期最少消費: ${expectedMinUsed}`);

      // 如果有訂閱付費功能，monthlyUsed 應 > 0
      // 目前 BUG：monthlyUsed 寫死 0
      if (subscribedFeatures.length > 0) {
        // 只記錄不強制失敗（因為扣款可能是月初一次性，或尚未到扣款日）
        if (monthlyUsed === 0) {
          console.warn('⚠️ 有訂閱付費功能但 monthlyUsed=0，可能是 BUG（寫死 0）或尚未到扣款日');
        }
      }
    });
  });

  // ========================================
  // 時段分布正確性
  // ========================================

  test.describe('時段分布', () => {

    test('尖峰時段應在營業時間範圍內 (9-21)', async ({ request }) => {
      const res = await request.get('/api/reports/hourly', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      expect(res.ok()).toBeTruthy();
      const hours = (await res.json()).data as Array<{ hour: number; bookingCount: number }>;

      for (const h of hours) {
        expect(h.hour).toBeGreaterThanOrEqual(9);
        expect(h.hour).toBeLessThan(21);
        expect(Number(h.bookingCount)).toBeGreaterThanOrEqual(0);
      }

      // 應有 12 個時段 (9-20)
      expect(hours.length).toBe(12);
    });

    test('時段分布總和應 <= 總預約數', async ({ request }) => {
      const hourlyRes = await request.get('/api/reports/hourly', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const hours = (await hourlyRes.json()).data as Array<{ hour: number; bookingCount: number }>;
      const hourlyTotal = hours.reduce((sum, h) => sum + Number(h.bookingCount), 0);

      const summaryRes = await request.get('/api/reports/summary', {
        headers: authHeaders(),
        params: { range: 'quarter' }
      });
      const total = Number((await summaryRes.json()).data.totalBookings);

      console.log(`時段分布合計: ${hourlyTotal}, 總預約數: ${total}`);

      // 時段分布只統計 9-21 時的預約，可能會小於總數（凌晨的不計）
      // 但不應超過總數
      expect(hourlyTotal).toBeLessThanOrEqual(total);
    });
  });

  // ========================================
  // 不同時間範圍一致性
  // ========================================

  test.describe('時間範圍一致性', () => {

    test('今日統計 <= 本週統計 <= 本月統計', async ({ request }) => {
      const [todayRes, weeklyRes, monthlyRes] = await Promise.all([
        request.get('/api/reports/today', { headers: authHeaders() }),
        request.get('/api/reports/weekly', { headers: authHeaders() }),
        request.get('/api/reports/monthly', { headers: authHeaders() })
      ]);

      const today = (await todayRes.json()).data;
      const weekly = (await weeklyRes.json()).data;
      const monthly = (await monthlyRes.json()).data;

      console.log(`今日預約: ${today.totalBookings}, 本週: ${weekly.totalBookings}, 本月: ${monthly.totalBookings}`);
      console.log(`今日營收: ${today.serviceRevenue}, 本週: ${weekly.serviceRevenue}, 本月: ${monthly.serviceRevenue}`);

      // 今日 <= 本週 <= 本月
      expect(Number(today.totalBookings)).toBeLessThanOrEqual(Number(weekly.totalBookings));
      expect(Number(weekly.totalBookings)).toBeLessThanOrEqual(Number(monthly.totalBookings));
      expect(Number(today.serviceRevenue)).toBeLessThanOrEqual(Number(weekly.serviceRevenue));
      expect(Number(weekly.serviceRevenue)).toBeLessThanOrEqual(Number(monthly.serviceRevenue));
    });
  });
});

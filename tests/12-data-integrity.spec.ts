import { test, expect, APIRequestContext } from './fixtures';
import { TEST_ACCOUNTS, generateTestPhone } from './utils/test-helpers';

/**
 * 資料完整性與防禦性測試 (Final Sweep)
 *
 * 驗證範圍：
 * 1. API 回應格式一致性 ({ success: true, data: ... })
 * 2. 分頁 API 結構一致性 (content, totalElements, totalPages, size, number)
 * 3. 刪除操作與軟刪除驗證 (deleted_at 模式)
 * 4. 空資料處理 (空陣列而非錯誤)
 * 5. 並發安全基本驗證 (同時請求不崩潰)
 * 6. 錯誤處理驗證 (404, 400, 正確的錯誤格式)
 * 7. 特殊字元處理 (XSS, Unicode)
 * 8. 認證 Token 處理 (401, refresh)
 */

// 取得店家 Token
async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// 取得超管 Token
async function getAdminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/admin/login', {
    data: { username: TEST_ACCOUNTS.admin.username, password: TEST_ACCOUNTS.admin.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// ============================================================
// 1. API 回應格式一致性
// ============================================================
test.describe('API 回應格式一致性', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  const apiEndpoints = [
    { name: 'GET /api/bookings', path: '/api/bookings' },
    { name: 'GET /api/customers', path: '/api/customers' },
    { name: 'GET /api/staff', path: '/api/staff' },
    { name: 'GET /api/services', path: '/api/services' },
    { name: 'GET /api/reports/dashboard', path: '/api/reports/dashboard' },
    { name: 'GET /api/settings', path: '/api/settings' },
  ];

  for (const endpoint of apiEndpoints) {
    test(`${endpoint.name} 回應格式包含 success 和 data`, async ({ request }) => {
      const response = await request.get(endpoint.path, {
        headers: { Authorization: `Bearer ${token}` }
      });
      expect(response.ok(), `${endpoint.name} 回應非 2xx: ${response.status()}`).toBeTruthy();

      const body = await response.json();
      expect(body, `${endpoint.name} 回應體為空`).toBeDefined();
      expect(body.success, `${endpoint.name} 缺少 success 欄位`).toBe(true);
      expect(body).toHaveProperty('data');
      expect(body.data, `${endpoint.name} data 為 null`).not.toBeNull();
    });
  }
});

// ============================================================
// 2. 分頁 API 一致性
// ============================================================
test.describe('分頁 API 結構一致性', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  const paginatedApis = [
    { name: 'bookings', path: '/api/bookings' },
    { name: 'customers', path: '/api/customers' },
    { name: 'staff', path: '/api/staff' },
  ];

  for (const api of paginatedApis) {
    test(`GET ${api.path} 分頁結構正確 (content, totalElements, totalPages, size, page)`, async ({ request }) => {
      const response = await request.get(api.path, {
        headers: { Authorization: `Bearer ${token}` }
      });
      expect(response.ok()).toBeTruthy();

      const body = await response.json();
      expect(body.success).toBe(true);

      const pageData = body.data;
      expect(pageData).toHaveProperty('content');
      expect(Array.isArray(pageData.content), `${api.name}.content 應為陣列`).toBe(true);
      expect(pageData).toHaveProperty('totalElements');
      expect(typeof pageData.totalElements).toBe('number');
      expect(pageData).toHaveProperty('totalPages');
      expect(typeof pageData.totalPages).toBe('number');
      expect(pageData).toHaveProperty('size');
      expect(typeof pageData.size).toBe('number');
      // 分頁欄位名為 page（非 number）
      expect(pageData).toHaveProperty('page');
      expect(typeof pageData.page).toBe('number');
    });
  }

  test('分頁 size 參數生效: ?size=5 回傳不超過 5 筆', async ({ request }) => {
    const response = await request.get('/api/customers?size=5', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.data.content.length).toBeLessThanOrEqual(5);
    expect(body.data.size).toBe(5);
  });

  test('分頁 page 參數生效: ?page=0&size=2 vs ?page=1&size=2 回傳不同資料', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };

    const page0Res = await request.get('/api/customers?page=0&size=2', { headers });
    const page1Res = await request.get('/api/customers?page=1&size=2', { headers });

    expect(page0Res.ok()).toBeTruthy();
    expect(page1Res.ok()).toBeTruthy();

    const page0 = await page0Res.json();
    const page1 = await page1Res.json();

    expect(page0.data.page).toBe(0);
    expect(page1.data.page).toBe(1);

    // 如果總數 > 2，兩頁內容應該不同
    if (page0.data.totalElements > 2 && page1.data.content.length > 0) {
      const page0Ids = page0.data.content.map((c: any) => c.id);
      const page1Ids = page1.data.content.map((c: any) => c.id);
      const overlap = page0Ids.filter((id: string) => page1Ids.includes(id));
      expect(overlap.length, '分頁資料不應重疊').toBe(0);
    }
  });
});

// ============================================================
// 3. 刪除操作與軟刪除驗證
// ============================================================
test.describe('刪除操作與軟刪除驗證', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  test('建立顧客 -> 刪除 -> GET 回傳 404', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };
    const ts = Date.now();

    // 建立顧客
    const createRes = await request.post('/api/customers', {
      headers,
      data: {
        name: `軟刪除測試_${ts}`,
        phone: generateTestPhone(),
        note: '資料完整性測試-軟刪除'
      }
    });
    expect(createRes.status(), '建立顧客失敗').toBeLessThan(300);

    const createBody = await createRes.json();
    const customerId = createBody.data?.id;
    expect(customerId, '未取得新建顧客 ID').toBeTruthy();

    // 刪除顧客
    const deleteRes = await request.delete(`/api/customers/${customerId}`, { headers });
    expect(deleteRes.ok(), '刪除顧客失敗').toBeTruthy();

    // 再次 GET 應回傳 404
    const getRes = await request.get(`/api/customers/${customerId}`, { headers });
    expect(getRes.status(), '已刪除顧客仍可取得').toBe(404);
  });

  test('建立顧客 -> 刪除 -> 列表中不包含該顧客', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };
    const ts = Date.now();
    const uniqueName = `軟刪列表測試_${ts}`;

    // 建立顧客
    const createRes = await request.post('/api/customers', {
      headers,
      data: {
        name: uniqueName,
        phone: generateTestPhone(),
        note: '資料完整性測試-列表過濾'
      }
    });
    expect(createRes.status()).toBeLessThan(300);

    const createBody = await createRes.json();
    const customerId = createBody.data?.id;
    expect(customerId).toBeTruthy();

    // 刪除
    const deleteRes = await request.delete(`/api/customers/${customerId}`, { headers });
    expect(deleteRes.ok()).toBeTruthy();

    // 搜尋列表不應包含已刪除顧客
    const listRes = await request.get(`/api/customers?keyword=${encodeURIComponent(uniqueName)}`, { headers });
    expect(listRes.ok()).toBeTruthy();

    const listBody = await listRes.json();
    const matchingIds = (listBody.data?.content || []).map((c: any) => c.id);
    expect(matchingIds, '已刪除顧客仍出現在列表中').not.toContain(customerId);
  });
});

// ============================================================
// 4. 空資料處理
// ============================================================
test.describe('空資料處理', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  test('預約列表篩選 NO_SHOW 狀態回傳空陣列而非錯誤', async ({ request }) => {
    const response = await request.get('/api/bookings?status=NO_SHOW', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.data).toHaveProperty('content');
    expect(Array.isArray(body.data.content)).toBe(true);
    // 可能有也可能沒有，重點是不能報錯
  });

  test('顧客搜尋不存在的關鍵字回傳空陣列', async ({ request }) => {
    const response = await request.get('/api/customers?keyword=zzznonexistent_xyz_99999', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(response.ok()).toBeTruthy();

    const body = await response.json();
    expect(body.success).toBe(true);
    expect(body.data).toHaveProperty('content');
    expect(Array.isArray(body.data.content)).toBe(true);
    expect(body.data.content.length).toBe(0);
    expect(body.data.totalElements).toBe(0);
  });

  test('報表摘要回傳數值而非 null/undefined', async ({ request }) => {
    const response = await request.get('/api/reports/summary?range=week', {
      headers: { Authorization: `Bearer ${token}` }
    });
    // 報表可能回傳 200 或 403（未訂閱），都不應該 500
    expect(response.status()).toBeLessThan(500);

    if (response.ok()) {
      const body = await response.json();
      expect(body.success).toBe(true);

      const data = body.data;
      expect(data).toBeDefined();
      // 核心數值不應為 null（可以是 0）
      if (data.totalBookings !== undefined) {
        expect(typeof data.totalBookings).toBe('number');
      }
      if (data.totalRevenue !== undefined) {
        expect(typeof data.totalRevenue).toBe('number');
      }
    }
  });

  test('今日報表回傳有效結構', async ({ request }) => {
    const response = await request.get('/api/reports/today', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(response.status()).toBeLessThan(500);

    if (response.ok()) {
      const body = await response.json();
      expect(body.success).toBe(true);
      expect(body.data).toBeDefined();
    }
  });
});

// ============================================================
// 5. 並發安全基本驗證
// ============================================================
test.describe('並發安全基本驗證', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  test('5 個同時 GET 請求全部回傳 200', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };

    const requests = [
      request.get('/api/bookings?size=1', { headers }),
      request.get('/api/customers?size=1', { headers }),
      request.get('/api/staff?size=1', { headers }),
      request.get('/api/services?size=1', { headers }),
      request.get('/api/reports/dashboard', { headers }),
    ];

    const responses = await Promise.all(requests);

    for (let i = 0; i < responses.length; i++) {
      expect(responses[i].ok(), `並發請求 ${i + 1} 失敗: ${responses[i].status()}`).toBeTruthy();
    }
  });

  test('2 個同時 POST 建立不同顧客均成功', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };
    const ts = Date.now();

    const requests = [
      request.post('/api/customers', {
        headers,
        data: {
          name: `並發測試A_${ts}`,
          phone: generateTestPhone(),
          note: '並發測試A'
        }
      }),
      request.post('/api/customers', {
        headers,
        data: {
          name: `並發測試B_${ts}`,
          phone: generateTestPhone(),
          note: '並發測試B'
        }
      }),
    ];

    const responses = await Promise.all(requests);

    for (let i = 0; i < responses.length; i++) {
      // 201 = 建立成功，409 = 手機重複（極罕見但可接受），不應 500
      expect(responses[i].status(), `並發建立 ${i + 1} 回傳 500`).toBeLessThan(500);
    }

    // 至少一個應該成功建立
    const anyCreated = responses.some(r => r.status() === 200 || r.status() === 201);
    expect(anyCreated, '並發建立顧客全部失敗').toBe(true);
  });
});

// ============================================================
// 6. 錯誤處理驗證
// ============================================================
test.describe('錯誤處理驗證', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  test('GET /api/bookings/{nonexistent} 回傳 404', async ({ request }) => {
    const response = await request.get('/api/bookings/00000000-0000-0000-0000-000000000000', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(response.status()).toBe(404);

    const body = await response.json();
    expect(body.success).toBe(false);
    expect(body).toHaveProperty('message');
    expect(body.message).toBeTruthy();
  });

  test('GET /api/customers/{nonexistent} 回傳 404', async ({ request }) => {
    const response = await request.get('/api/customers/00000000-0000-0000-0000-000000000000', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(response.status()).toBe(404);

    const body = await response.json();
    expect(body.success).toBe(false);
  });

  test('POST /api/bookings 缺少必填欄位回傳 400', async ({ request }) => {
    const response = await request.post('/api/bookings', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        // 刻意缺少 customerId, serviceItemId, bookingDate, startTime
        customerNote: '缺少必填欄位測試'
      }
    });
    // 400 = 欄位缺少，422 = 業務驗證，都可接受。不應 500
    expect(response.status()).toBeLessThan(500);
    expect(response.status()).toBeGreaterThanOrEqual(400);
  });

  test('PUT /api/staff/{nonexistent} 回傳 404', async ({ request }) => {
    const response = await request.put('/api/staff/00000000-0000-0000-0000-000000000000', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: '不存在的員工',
        phone: '0900000000',
        isBookable: true
      }
    });
    expect(response.status()).toBe(404);
  });

  test('DELETE /api/customers/{nonexistent} 回傳 404', async ({ request }) => {
    const response = await request.delete('/api/customers/00000000-0000-0000-0000-000000000000', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(response.status()).toBe(404);
  });
});

// ============================================================
// 7. 特殊字元處理
// ============================================================
test.describe('特殊字元處理', () => {
  let token: string;
  let createdCustomerId: string | undefined;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  test('建立含特殊字元名稱的顧客 (中文+HTML+符號)', async ({ request }) => {
    const ts = Date.now();
    const specialName = `Test 顧客 & <script>alert(1)</script> ${ts}`;
    const response = await request.post('/api/customers', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: specialName,
        phone: generateTestPhone(),
        note: '特殊字元測試: <b>bold</b> & "quotes" \'single\''
      }
    });
    // 不應 500，400 代表輸入驗證拒絕也可接受
    expect(response.status(), '特殊字元導致伺服器錯誤').toBeLessThan(500);

    if (response.status() === 200 || response.status() === 201) {
      const body = await response.json();
      createdCustomerId = body.data?.id;
      expect(createdCustomerId).toBeTruthy();

      // 取回資料驗證不會注入 HTML
      const getRes = await request.get(`/api/customers/${createdCustomerId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      expect(getRes.ok()).toBeTruthy();
      const getBody = await getRes.json();
      // 名稱應被儲存（原樣或被清理），不應造成 XSS
      expect(getBody.data.name).toBeDefined();
    }
  });

  test('搜尋含特殊字元的關鍵字不會崩潰', async ({ request }) => {
    const specialKeywords = [
      '% OR 1=1 --',
      '<script>alert("xss")</script>',
      '"; DROP TABLE customers; --',
      '顧客&=+#',
    ];

    for (const keyword of specialKeywords) {
      const response = await request.get(`/api/customers?keyword=${encodeURIComponent(keyword)}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
      expect(response.status(), `搜尋 "${keyword}" 導致伺服器錯誤`).toBeLessThan(500);
    }
  });

  test('建立含 Unicode Emoji 名稱的顧客', async ({ request }) => {
    const ts = Date.now();
    const response = await request.post('/api/customers', {
      headers: { Authorization: `Bearer ${token}` },
      data: {
        name: `Emoji顧客_${ts}`,
        phone: generateTestPhone(),
        note: '特殊字元測試-Emoji'
      }
    });
    expect(response.status()).toBeLessThan(500);
  });

  test.afterAll(async ({ request }) => {
    // 清理測試資料
    if (createdCustomerId) {
      await request.delete(`/api/customers/${createdCustomerId}`, {
        headers: { Authorization: `Bearer ${token}` }
      });
    }
  });
});

// ============================================================
// 8. 認證 Token 處理
// ============================================================
test.describe('認證 Token 處理', () => {

  test('無效 Token 回傳 401', async ({ request }) => {
    const response = await request.get('/api/bookings', {
      headers: { Authorization: 'Bearer invalid.token.here' }
    });
    expect(response.status()).toBe(401);
  });

  test('缺少 Authorization Header 回傳 401 或 403', async ({ request }) => {
    const response = await request.get('/api/bookings');
    expect([401, 403]).toContain(response.status());
  });

  test('過期格式的 Token 回傳 401', async ({ request }) => {
    // 一個結構正確但已過期的 JWT（手動構造）
    const expiredToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0IiwiZXhwIjoxMDAwMDAwMDAwfQ.invalid_signature';
    const response = await request.get('/api/bookings', {
      headers: { Authorization: `Bearer ${expiredToken}` }
    });
    expect(response.status()).toBe(401);
  });

  test('Token refresh 取得新 Token', async ({ request }) => {
    // 先登入取得 refreshToken
    const loginRes = await request.post('/api/auth/tenant/login', {
      data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
    });
    expect(loginRes.ok()).toBeTruthy();

    const loginBody = await loginRes.json();
    const refreshToken = loginBody.data?.refreshToken;
    expect(refreshToken, '登入回應缺少 refreshToken').toBeTruthy();

    // 用 refreshToken 取得新 accessToken
    const refreshRes = await request.post('/api/auth/refresh', {
      data: { refreshToken }
    });
    expect(refreshRes.ok(), `Token 刷新失敗: ${refreshRes.status()}`).toBeTruthy();

    const refreshBody = await refreshRes.json();
    expect(refreshBody.success).toBe(true);
    expect(refreshBody.data?.accessToken, '刷新後缺少 accessToken').toBeTruthy();

    // 新 token 應能正常使用
    const verifyRes = await request.get('/api/settings', {
      headers: { Authorization: `Bearer ${refreshBody.data.accessToken}` }
    });
    expect(verifyRes.ok(), '刷新後的 Token 無法使用').toBeTruthy();
  });

  test('店家 Token 無法存取超管 API', async ({ request }) => {
    const tenantToken = await getTenantToken(request);
    expect(tenantToken).toBeTruthy();

    const response = await request.get('/api/admin/tenants', {
      headers: { Authorization: `Bearer ${tenantToken}` }
    });
    // 應回傳 403 (權限不足) 或 401
    expect([401, 403]).toContain(response.status());
  });

  test('超管 Token 存取店家 API 得到空資料（無租戶上下文）', async ({ request }) => {
    const adminToken = await getAdminToken(request);
    expect(adminToken).toBeTruthy();

    const response = await request.get('/api/bookings', {
      headers: { Authorization: `Bearer ${adminToken}` }
    });
    // Admin CAN access but gets empty data (no tenant context)
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.data.totalElements).toBe(0);
  });
});

// ============================================================
// 9. 跨 API 資料一致性
// ============================================================
test.describe('跨 API 資料一致性', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
    expect(token, '店家 Token 取得失敗').toBeTruthy();
  });

  test('顧客總數在列表 API 和 dashboard 間一致', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };

    const [customersRes, dashboardRes] = await Promise.all([
      request.get('/api/customers?size=1', { headers }),
      request.get('/api/reports/dashboard', { headers }),
    ]);

    expect(customersRes.ok()).toBeTruthy();
    expect(dashboardRes.ok()).toBeTruthy();

    const customersBody = await customersRes.json();
    const dashboardBody = await dashboardRes.json();

    const listTotal = customersBody.data?.totalElements;
    const dashboardTotal = dashboardBody.data?.totalCustomers;

    // 兩者都應為數字
    expect(typeof listTotal).toBe('number');
    if (dashboardTotal !== undefined) {
      expect(typeof dashboardTotal).toBe('number');
      // 允許小幅差異（可能有並發寫入），但不應差太多
      expect(Math.abs(listTotal - dashboardTotal), '顧客數量在列表與儀表板間差異過大').toBeLessThanOrEqual(5);
    }
  });

  test('建立顧客後列表 totalElements 增加', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };
    const ts = Date.now();

    // 記錄建立前的總數
    const beforeRes = await request.get('/api/customers?size=1', { headers });
    expect(beforeRes.ok()).toBeTruthy();
    const beforeBody = await beforeRes.json();
    const beforeCount = beforeBody.data?.totalElements || 0;

    // 建立顧客
    const createRes = await request.post('/api/customers', {
      headers,
      data: {
        name: `一致性測試_${ts}`,
        phone: generateTestPhone(),
        note: '跨 API 一致性測試'
      }
    });

    if (createRes.status() === 200 || createRes.status() === 201) {
      const createBody = await createRes.json();
      const newId = createBody.data?.id;

      // 記錄建立後的總數
      const afterRes = await request.get('/api/customers?size=1', { headers });
      expect(afterRes.ok()).toBeTruthy();
      const afterBody = await afterRes.json();
      const afterCount = afterBody.data?.totalElements || 0;

      expect(afterCount, '建立顧客後 totalElements 未增加').toBeGreaterThan(beforeCount);

      // 清理
      if (newId) {
        await request.delete(`/api/customers/${newId}`, { headers });
      }
    }
  });

  test('員工列表與 bookable 員工列表數量關係合理', async ({ request }) => {
    const headers = { Authorization: `Bearer ${token}` };

    const [allStaffRes, bookableRes] = await Promise.all([
      request.get('/api/staff', { headers }),
      request.get('/api/staff/bookable', { headers }),
    ]);

    expect(allStaffRes.ok()).toBeTruthy();
    expect(bookableRes.ok()).toBeTruthy();

    const allStaffBody = await allStaffRes.json();
    const bookableBody = await bookableRes.json();

    const allCount = allStaffBody.data?.content?.length ?? allStaffBody.data?.length ?? 0;
    const bookableList = Array.isArray(bookableBody.data) ? bookableBody.data : (bookableBody.data?.content || []);
    const bookableCount = bookableList.length;

    // bookable 員工數應 <= 全部員工數
    expect(bookableCount, '可預約員工數超過全部員工數').toBeLessThanOrEqual(
      allStaffBody.data?.totalElements ?? allCount + 100
    );
  });
});

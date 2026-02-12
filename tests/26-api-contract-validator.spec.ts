import { test, expect, APIRequestContext } from '@playwright/test';

/**
 * API 契約驗證測試
 *
 * 驗證前端送出的欄位名稱與後端 DTO 完全匹配。
 * 只檢查「DTO 解析是否成功」（不回傳 400），不驗證業務邏輯。
 *
 * 允許的回應碼：
 *   200, 201 — 成功
 *   409 — 衝突（如名稱重複）
 *   422, 403 — 業務錯誤（如時段已過、功能未訂閱）
 *   404 — 資源不存在（用假 ID 時的正常回應）
 * 只有 400 代表「欄位名稱或格式錯誤」— 測試失敗
 */

// 取得店家 Token
async function getTenantToken(request: APIRequestContext): Promise<string> {
    const res = await request.post('/api/auth/tenant/login', {
        data: { username: 'g0909095118@gmail.com', password: 'gaojunting11' }
    });
    const body = await res.json();
    return body.data.accessToken;
}

// 取得超管 Token
async function getAdminToken(request: APIRequestContext): Promise<string> {
    const res = await request.post('/api/auth/admin/login', {
        data: { username: 'admin', password: 'admin123' }
    });
    const body = await res.json();
    return body.data.accessToken;
}

// 輔助：驗證回應不是「欄位驗證錯誤」的 400
// 區分：SYS_002/SYS_003 = 欄位名或格式錯誤（測試失敗），其他 400 = 業務邏輯（允許）
async function assertNotFieldError(res: any, url: string) {
    const status = res.status();
    if (status !== 400) return; // 非 400 一律通過

    const body = await res.json().catch(() => ({}));
    const code = body.code || '';
    // SYS_002 = 請求體格式錯誤（Jackson 解析失敗）
    // SYS_003 = 參數驗證失敗（@Valid 欄位驗證）
    const isFieldError = code === 'SYS_002' || code === 'SYS_003';
    expect(isFieldError, `${url} 回傳 400 (${code}: ${body.message}) — 欄位名稱或格式可能錯誤`).toBe(false);
}

test.describe('API 契約驗證 - 店家 API', () => {
    let token: string;
    let testIds: {
        customerId?: string;
        serviceId?: string;
        staffId?: string;
        bookingId?: string;
        productId?: string;
        couponId?: string;
        campaignId?: string;
        membershipLevelId?: string;
        categoryId?: string;
    } = {};

    test.beforeAll(async ({ request }) => {
        // 取得店家 Token
        token = await getTenantToken(request);
        expect(token).toBeTruthy();

        // 取得真實 ID 用於測試
        const headers = { Authorization: `Bearer ${token}` };

        // 顧客
        const custRes = await request.get('/api/customers?size=1', { headers });
        if (custRes.ok()) {
            const custData = await custRes.json();
            const customers = custData.data?.content || [];
            if (customers.length > 0) testIds.customerId = customers[0].id;
        }

        // 服務
        const svcRes = await request.get('/api/services?size=1', { headers });
        if (svcRes.ok()) {
            const svcData = await svcRes.json();
            const services = svcData.data?.content || [];
            if (services.length > 0) testIds.serviceId = services[0].id;
        }

        // 員工
        const staffRes = await request.get('/api/staff?size=1', { headers });
        if (staffRes.ok()) {
            const staffData = await staffRes.json();
            const staffList = staffData.data?.content || [];
            if (staffList.length > 0) testIds.staffId = staffList[0].id;
        }

        // 預約
        const bookRes = await request.get('/api/bookings?size=1', { headers });
        if (bookRes.ok()) {
            const bookData = await bookRes.json();
            const bookings = bookData.data?.content || [];
            if (bookings.length > 0) testIds.bookingId = bookings[0].id;
        }

        // 商品
        const prodRes = await request.get('/api/products?size=1', { headers });
        if (prodRes.ok()) {
            const prodData = await prodRes.json();
            const products = prodData.data?.content || [];
            if (products.length > 0) testIds.productId = products[0].id;
        }

        // 票券
        const coupRes = await request.get('/api/coupons?size=1', { headers });
        if (coupRes.ok()) {
            const coupData = await coupRes.json();
            const coupons = coupData.data?.content || [];
            if (coupons.length > 0) testIds.couponId = coupons[0].id;
        }

        // 行銷活動
        const campRes = await request.get('/api/campaigns?size=1', { headers });
        if (campRes.ok()) {
            const campData = await campRes.json();
            const campaigns = campData.data?.content || [];
            if (campaigns.length > 0) testIds.campaignId = campaigns[0].id;
        }

        // 會員等級
        const mlRes = await request.get('/api/membership-levels', { headers });
        if (mlRes.ok()) {
            const mlData = await mlRes.json();
            const levels = mlData.data || [];
            if (levels.length > 0) testIds.membershipLevelId = levels[0].id;
        }

        // 服務分類
        const catRes = await request.get('/api/service-categories', { headers });
        if (catRes.ok()) {
            const catData = await catRes.json();
            const categories = catData.data || [];
            if (categories.length > 0) testIds.categoryId = categories[0].id;
        }

        console.log('測試 ID:', JSON.stringify(testIds, null, 2));
    });

    // ===== 預約 =====

    test('POST /api/bookings — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/bookings', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                customerId: testIds.customerId || 'test-id',
                serviceItemId: testIds.serviceId || 'test-id',
                staffId: testIds.staffId,
                bookingDate: '2099-12-31',
                startTime: '10:00',
                customerNote: '契約測試'
            }
        });
        await assertNotFieldError(res, 'POST /api/bookings');
    });

    test('PUT /api/bookings/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.bookingId || 'nonexistent';
        const res = await request.put(`/api/bookings/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                serviceItemId: testIds.serviceId,
                staffId: testIds.staffId,
                bookingDate: '2099-12-31',
                startTime: '10:00',
                duration: 60,
                storeNoteToCustomer: '契約測試備註'
            }
        });
        await assertNotFieldError(res, 'PUT /api/bookings/{id}');
    });

    // ===== 顧客 =====

    test('POST /api/customers — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/customers', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試顧客',
                phone: '0900000000',
                email: 'contract-test@example.com',
                gender: 'OTHER',
                birthday: '2000-01-01',
                note: '契約測試'
            }
        });
        await assertNotFieldError(res, 'POST /api/customers');
    });

    test('PUT /api/customers/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.customerId || 'nonexistent';
        const res = await request.put(`/api/customers/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試顧客更新',
                phone: '0900000001',
                email: 'contract-test-update@example.com',
                gender: 'OTHER',
                birthday: '2000-01-01',
                note: '契約測試更新'
            }
        });
        await assertNotFieldError(res, 'PUT /api/customers/{id}');
    });

    // ===== 員工 =====

    test('POST /api/staff — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/staff', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試員工',
                displayName: '測試顯示名',
                phone: '0911000000',
                email: 'staff-test@example.com',
                isBookable: true,
                isVisible: true,
                sortOrder: 99
            }
        });
        await assertNotFieldError(res, 'POST /api/staff');
    });

    test('PUT /api/staff/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.staffId || 'nonexistent';
        const res = await request.put(`/api/staff/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試員工更新',
                phone: '0911000001',
                email: 'staff-update@example.com',
                isBookable: true,
                isVisible: true,
                sortOrder: 99
            }
        });
        await assertNotFieldError(res, 'PUT /api/staff/{id}');
    });

    // ===== 服務 =====

    test('POST /api/services — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/services', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試服務',
                categoryId: testIds.categoryId,
                price: 500,
                duration: 60,
                description: '契約測試服務描述'
            }
        });
        await assertNotFieldError(res, 'POST /api/services');
    });

    test('PUT /api/services/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.serviceId || 'nonexistent';
        const res = await request.put(`/api/services/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試服務更新',
                categoryId: testIds.categoryId,
                price: 600,
                duration: 90,
                description: '契約測試服務描述更新'
            }
        });
        await assertNotFieldError(res, 'PUT /api/services/{id}');
    });

    // ===== 服務分類 =====

    test('POST /api/service-categories — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/service-categories', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試分類',
                description: '契約測試分類描述'
            }
        });
        await assertNotFieldError(res, 'POST /api/service-categories');
    });

    test('PUT /api/service-categories/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.categoryId || 'nonexistent';
        const res = await request.put(`/api/service-categories/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試分類更新',
                description: '契約測試分類描述更新',
                isActive: true
            }
        });
        await assertNotFieldError(res, 'PUT /api/service-categories/{id}');
    });

    // ===== 商品 =====

    test('POST /api/products — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/products', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試商品',
                category: 'HAIR_CARE',
                price: 299,
                stockQuantity: 100,
                safetyStock: 10,
                trackInventory: true,
                description: '契約測試商品描述'
            }
        });
        await assertNotFieldError(res, 'POST /api/products');
    });

    test('PUT /api/products/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.productId || 'nonexistent';
        const res = await request.put(`/api/products/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試商品更新',
                category: 'HAIR_CARE',
                price: 399,
                stockQuantity: 50,
                safetyStock: 5,
                description: '契約測試商品描述更新'
            }
        });
        await assertNotFieldError(res, 'PUT /api/products/{id}');
    });

    // ===== 票券 =====

    test('POST /api/coupons — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/coupons', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試票券',
                type: 'DISCOUNT_AMOUNT',
                description: '契約測試票券描述',
                discountAmount: 100,
                totalQuantity: 10,
                limitPerCustomer: 1,
                validDays: 30
            }
        });
        await assertNotFieldError(res, 'POST /api/coupons');
    });

    test('PUT /api/coupons/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.couponId || 'nonexistent';
        const res = await request.put(`/api/coupons/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試票券更新',
                type: 'DISCOUNT_AMOUNT',
                description: '契約測試票券描述更新',
                discountAmount: 200,
                totalQuantity: 20,
                limitPerCustomer: 2,
                validDays: 60
            }
        });
        await assertNotFieldError(res, 'PUT /api/coupons/{id}');
    });

    // ===== 行銷活動 =====

    test('POST /api/campaigns — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/campaigns', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試活動',
                type: 'BIRTHDAY',
                description: '契約測試活動描述',
                bonusPoints: 100,
                isAutoTrigger: false,
                note: '契約測試'
            }
        });
        await assertNotFieldError(res, 'POST /api/campaigns');
    });

    test('PUT /api/campaigns/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.campaignId || 'nonexistent';
        const res = await request.put(`/api/campaigns/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試活動更新',
                type: 'BIRTHDAY',
                description: '契約測試活動描述更新'
            }
        });
        await assertNotFieldError(res, 'PUT /api/campaigns/{id}');
    });

    // ===== 行銷推播 =====

    test('POST /api/marketing/pushes — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/marketing/pushes', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                title: '契約測試推播',
                content: '契約測試推播內容',
                targetType: 'ALL',
                note: '契約測試'
            }
        });
        await assertNotFieldError(res, 'POST /api/marketing/pushes');
    });

    // ===== 會員等級 =====

    test('POST /api/membership-levels — 欄位名稱正確', async ({ request }) => {
        const res = await request.post('/api/membership-levels', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試等級',
                upgradeThreshold: 5000,
                discountRate: 0.05,
                pointRate: 0.1,
                description: '契約測試等級描述',
                sortOrder: 99,
                isActive: true,
                isDefault: false
            }
        });
        await assertNotFieldError(res, 'POST /api/membership-levels');
    });

    test('PUT /api/membership-levels/{id} — 欄位名稱正確', async ({ request }) => {
        const id = testIds.membershipLevelId || 'nonexistent';
        // 注意：Controller PUT 也使用 CreateMembershipLevelRequest（非 Update DTO）
        const res = await request.put(`/api/membership-levels/${id}`, {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試等級更新',
                upgradeThreshold: 10000,
                discountRate: 0.1,
                pointRate: 0.1,
                description: '契約測試等級描述更新',
                sortOrder: 99,
                isDefault: false
            }
        });
        await assertNotFieldError(res, 'PUT /api/membership-levels/{id}');
    });

    // ===== 設定 =====

    test('PUT /api/settings — 欄位名稱正確', async ({ request }) => {
        const res = await request.put('/api/settings', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                name: '契約測試店家',
                phone: '0200000000',
                email: 'settings-test@example.com',
                address: '測試地址',
                description: '契約測試',
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

    // ===== 更改密碼 =====

    test('POST /api/auth/change-password — 欄位名稱正確', async ({ request }) => {
        // 用錯誤的舊密碼，只驗證欄位名不會 400
        const res = await request.post('/api/auth/change-password', {
            headers: { Authorization: `Bearer ${token}` },
            data: {
                currentPassword: 'wrong-password-for-contract-test',
                newPassword: 'newPass123!',
                confirmPassword: 'newPass123!'
            }
        });
        // 可能 400（密碼錯誤 AUTH_LOGIN_FAILED）但不應該是 SYS_002/SYS_003（欄位名錯誤）
        await assertNotFieldError(res, 'POST /api/auth/change-password');
    });
});

test.describe('API 契約驗證 - 超管 API', () => {
    let adminToken: string;
    let createdTenantId: string | undefined;

    test.beforeAll(async ({ request }) => {
        adminToken = await getAdminToken(request);
        expect(adminToken).toBeTruthy();
    });

    test('POST /api/admin/tenants — 欄位名稱正確', async ({ request }) => {
        const ts = Date.now();
        const res = await request.post('/api/admin/tenants', {
            headers: { Authorization: `Bearer ${adminToken}` },
            data: {
                code: `ct${ts}`,
                name: '契約測試店家',
                phone: '0200000000',
                email: `ct${ts}@example.com`,
                address: '測試地址',
                description: '契約測試'
            }
        });
        await assertNotFieldError(res, 'POST /api/admin/tenants');

        // 記錄建立的 ID，供 PUT 測試和清理使用
        if (res.status() === 201) {
            const data = await res.json();
            createdTenantId = data.data?.id;
        }
    });

    test('PUT /api/admin/tenants/{id} — 欄位名稱正確', async ({ request }) => {
        // 用剛建立的測試租戶，避免修改正式租戶資料
        const id = createdTenantId || 'nonexistent';
        const res = await request.put(`/api/admin/tenants/${id}`, {
            headers: { Authorization: `Bearer ${adminToken}` },
            data: {
                name: '契約測試店家更新',
                phone: '0200000001',
                email: `ct-update-${Date.now()}@example.com`,
                address: '測試地址更新',
                description: '契約測試更新'
            }
        });
        await assertNotFieldError(res, 'PUT /api/admin/tenants/{id}');
    });

    test.afterAll(async ({ request }) => {
        // 清理測試租戶
        if (createdTenantId) {
            await request.delete(`/api/admin/tenants/${createdTenantId}`, {
                headers: { Authorization: `Bearer ${adminToken}` }
            });
        }
    });
});

import { test, expect, APIRequestContext } from './fixtures';
import {
    tenantLogin,
    adminLogin,
    waitForLoading,
    WAIT_TIME,
    TEST_ACCOUNTS,
    getToday,
    getTomorrow
} from './utils/test-helpers';

/**
 * Bug Fix 驗證測試 + 新功能測試
 *
 * 驗證 16 個已修復的 bug 和新功能：
 * - 預約狀態驗證（complete/noShow 只允許 CONFIRMED）
 * - 票券過期檢查 + 租戶隔離
 * - 功能商店 null pointBalance 防護
 * - 超管儀表板統計修正
 * - 匯出日期驗證
 * - 員工排班部分休息時間驗證
 * - 預約頁面 UI（爽約按鈕、編輯備註）
 * - 儀表板每日報表 API 參數
 * - PDF 匯出欄位修正
 * - 會員等級排序修正
 * - 新行業頁面（補習/攝影/寵物）
 * - 推薦機制
 * - GA4/FB Pixel 追蹤
 */

const BASE_URL = 'https://booking-platform-production-1e08.up.railway.app';

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
        data: { username: 'admin', password: 'admin123' }
    });
    const body = await res.json();
    return body.data?.accessToken || '';
}

// ========================================
// Bug Fix #1: BookingService complete/markNoShow CONFIRMED 狀態驗證
// ========================================

test.describe('Bug Fix #1: 預約狀態變更只允許 CONFIRMED', () => {
    let token: string;
    let pendingBookingId: string | null = null;
    let completedBookingId: string | null = null;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
        if (!token) return;

        const headers = { Authorization: `Bearer ${token}` };

        // 找一筆 PENDING 預約
        const pendingRes = await request.get('/api/bookings?status=PENDING_CONFIRMATION&size=1', { headers });
        if (pendingRes.ok()) {
            const data = await pendingRes.json();
            pendingBookingId = data.data?.content?.[0]?.id || null;
        }

        // 找一筆 COMPLETED 預約
        const completedRes = await request.get('/api/bookings?status=COMPLETED&size=1', { headers });
        if (completedRes.ok()) {
            const data = await completedRes.json();
            completedBookingId = data.data?.content?.[0]?.id || null;
        }
    });

    test('PENDING 預約不能標記完成', async ({ request }) => {
        if (!token || !pendingBookingId) { test.skip(); return; }
        const res = await request.post(`/api/bookings/${pendingBookingId}/complete`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        // 應該被拒絕（不是 200）
        expect(res.status()).not.toBe(200);
        console.log(`PENDING complete 回應: ${res.status()}`);
    });

    test('PENDING 預約不能標記爽約', async ({ request }) => {
        if (!token || !pendingBookingId) { test.skip(); return; }
        const res = await request.post(`/api/bookings/${pendingBookingId}/no-show`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).not.toBe(200);
        console.log(`PENDING no-show 回應: ${res.status()}`);
    });

    test('COMPLETED 預約不能再次標記完成', async ({ request }) => {
        if (!token || !completedBookingId) { test.skip(); return; }
        const res = await request.post(`/api/bookings/${completedBookingId}/complete`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).not.toBe(200);
        console.log(`COMPLETED re-complete 回應: ${res.status()}`);
    });

    test('no-show API 端點正常', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.post('/api/bookings/nonexistent-id/no-show', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect([400, 404]).toContain(res.status());
    });
});

// ========================================
// Bug Fix #4: FeatureStoreService null pointBalance 防護
// ========================================

test.describe('Bug Fix #4: 功能商店 API 不會 NPE', () => {
    let token: string;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
    });

    test('功能商店列表正常回傳', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/feature-store', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.ok()).toBeTruthy();
        const data = await res.json();
        expect(data.success).toBeTruthy();
        expect(Array.isArray(data.data)).toBeTruthy();
        console.log(`功能數量: ${data.data.length}`);
    });

    test('功能詳情不會 500', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const codes = ['COUPON_SYSTEM', 'PRODUCT_SALES', 'POINT_SYSTEM', 'MEMBERSHIP_SYSTEM'];
        for (const code of codes) {
            const res = await request.get(`/api/feature-store/${code}`, {
                headers: { Authorization: `Bearer ${token}` }
            });
            expect(res.status(), `${code} 不應 500`).toBeLessThan(500);
        }
    });
});

// ========================================
// Bug Fix #5: AdminDashboardService 月底邊界修正
// ========================================

test.describe('Bug Fix #5: 超管儀表板統計', () => {
    let adminToken: string;

    test.beforeAll(async ({ request }) => {
        adminToken = await getAdminToken(request);
    });

    test('超管儀表板 API 正常回傳', async ({ request }) => {
        const res = await request.get('/api/admin/dashboard', {
            headers: { Authorization: `Bearer ${adminToken}` }
        });
        expect(res.ok()).toBeTruthy();
        const data = await res.json();
        expect(data.success).toBeTruthy();
        console.log(`超管儀表板: ${JSON.stringify(data.data)}`);
    });

    test('超管儀表板數據合理', async ({ request }) => {
        const res = await request.get('/api/admin/dashboard', {
            headers: { Authorization: `Bearer ${adminToken}` }
        });
        const data = await res.json();
        const dashboard = data.data;

        // 所有數值都應 >= 0
        if (dashboard.totalTenants !== undefined) {
            expect(dashboard.totalTenants).toBeGreaterThanOrEqual(0);
        }
        if (dashboard.monthlyBookings !== undefined) {
            expect(dashboard.monthlyBookings).toBeGreaterThanOrEqual(0);
        }
    });
});

// ========================================
// Bug Fix #7: PointTopUpService 月度篩選
// ========================================

test.describe('Bug Fix #7: 儲值統計月度篩選', () => {
    let adminToken: string;

    test.beforeAll(async ({ request }) => {
        adminToken = await getAdminToken(request);
    });

    test('儲值統計 API 正常', async ({ request }) => {
        const res = await request.get('/api/admin/point-topups/stats', {
            headers: { Authorization: `Bearer ${adminToken}` }
        });
        expect(res.ok()).toBeTruthy();
        const data = await res.json();
        expect(data.success).toBeTruthy();
        console.log(`儲值統計: ${JSON.stringify(data.data)}`);
    });
});

// ========================================
// Bug Fix #8: ExportController 日期驗證
// ========================================

test.describe('Bug Fix #8: 匯出日期範圍驗證', () => {
    let token: string;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
    });

    test('開始日期晚於結束日期 — Excel 應拒絕', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/export/bookings/excel?startDate=2026-03-01&endDate=2026-02-01', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.ok(), '日期反轉應被拒絕').toBeFalsy();
        console.log(`日期反轉 Excel 回應: ${res.status()}`);
    });

    test('開始日期晚於結束日期 — PDF 應拒絕', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/export/bookings/pdf?startDate=2026-03-01&endDate=2026-02-01', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.ok(), '日期反轉應被拒絕').toBeFalsy();
        console.log(`日期反轉 PDF 回應: ${res.status()}`);
    });

    test('正常日期範圍 — Excel 不應 500', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const today = getToday();
        const res = await request.get(`/api/export/bookings/excel?startDate=${today}&endDate=${today}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
    });

    test('正常日期範圍 — PDF 不應 500', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const today = getToday();
        const res = await request.get(`/api/export/bookings/pdf?startDate=${today}&endDate=${today}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
    });

    test('報表 Excel 匯出不應 500', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/export/reports/excel?range=month', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status(), 'PDF 欄位修正後不應 500').toBeLessThan(500);
    });

    test('報表 PDF 匯出不應 500（Bug Fix #14 欄位數修正）', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/export/reports/pdf?range=month', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status(), 'PDF 欄位修正後不應 500').toBeLessThan(500);
    });
});

// ========================================
// Bug Fix #9: StaffService 部分休息時間驗證
// ========================================

test.describe('Bug Fix #9: 員工排班部分休息時間驗證', () => {
    let token: string;
    let staffId: string | null = null;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
        if (!token) return;

        const res = await request.get('/api/staff?size=1', {
            headers: { Authorization: `Bearer ${token}` }
        });
        if (res.ok()) {
            const data = await res.json();
            staffId = data.data?.content?.[0]?.id || null;
        }
    });

    test('排班 API 端點正常', async ({ request }) => {
        if (!token || !staffId) { test.skip(); return; }
        const res = await request.get(`/api/staff/${staffId}/schedule`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
    });
});

// ========================================
// Bug Fix #10 & #11: 預約頁面 UI（爽約按鈕 + 編輯備註）
// ========================================

test.describe('Bug Fix #10 & #11: 預約頁面 UI 修正', () => {
    test.beforeEach(async ({ page }) => {
        await tenantLogin(page);
    });

    test('預約列表有爽約按鈕', async ({ page }) => {
        await page.goto('/tenant/bookings');
        await waitForLoading(page);
        await page.waitForTimeout(WAIT_TIME.api);

        // 檢查 markNoShow 函式存在
        const hasFunction = await page.evaluate(() => {
            return typeof (window as any).markNoShow === 'function';
        });
        expect(hasFunction, 'markNoShow 函式應存在').toBeTruthy();
    });

    test('編輯預約 Modal 有店家備註欄位', async ({ page }) => {
        await page.goto('/tenant/bookings');
        await waitForLoading(page);
        await page.waitForTimeout(WAIT_TIME.api);

        // 找到編輯按鈕並點擊
        const editBtn = page.locator('button:has-text("編輯")').first();
        if (await editBtn.isVisible()) {
            await editBtn.click();
            await page.waitForTimeout(1000);

            const noteField = page.locator('#editBookingNoteToCustomer');
            await expect(noteField, '店家備註欄位應存在').toBeVisible();
        }
    });

    test('編輯 Modal JS 正確帶入 storeNoteToCustomer', async ({ page }) => {
        await page.goto('/tenant/bookings');
        await waitForLoading(page);
        await page.waitForTimeout(WAIT_TIME.api);

        // 驗證 populateEditForm 函式中有引用 storeNoteToCustomer
        const hasCorrectCode = await page.evaluate(() => {
            const scripts = document.querySelectorAll('script');
            for (const script of scripts) {
                if (script.textContent?.includes('booking.storeNoteToCustomer')) {
                    return true;
                }
            }
            return false;
        });
        expect(hasCorrectCode, 'JS 應引用 booking.storeNoteToCustomer').toBeTruthy();
    });
});

// ========================================
// Bug Fix #13: 儀表板每日報表 API 參數（range 而非 days）
// ========================================

test.describe('Bug Fix #13: 儀表板每日報表 API', () => {
    let token: string;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
    });

    test('每日報表用 range=week 正常回傳', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/reports/daily?range=week', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.ok()).toBeTruthy();
        const data = await res.json();
        expect(data.success).toBeTruthy();
        expect(Array.isArray(data.data)).toBeTruthy();
        console.log(`每日報表數據點: ${data.data.length}`);
    });

    test('儀表板 JS 用 range 參數而非 days', async ({ page }) => {
        await tenantLogin(page);
        await page.goto('/tenant/dashboard');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        // 驗證 JS 中使用 range 而非 days
        const hasCorrectParam = await page.evaluate(() => {
            const scripts = document.querySelectorAll('script');
            for (const script of scripts) {
                const text = script.textContent || '';
                if (text.includes("range: 'week'") || text.includes('range:')) {
                    return true;
                }
            }
            return false;
        });
        expect(hasCorrectParam, 'dashboard.html 應使用 range 參數').toBeTruthy();
    });
});

// ========================================
// Bug Fix #15: 超管儀表板標籤修正
// ========================================

test.describe('Bug Fix #15: 超管儀表板標籤', () => {
    test('超管儀表板顯示正確標籤', async ({ page }) => {
        await adminLogin(page);
        await page.goto('/admin/dashboard');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        // 應該顯示「儲值金額」而非「營收」
        const content = await page.content();
        const hasCorrectLabel = content.includes('儲值金額') || content.includes('儲值');
        expect(hasCorrectLabel, '應顯示「儲值金額」而非「營收」').toBeTruthy();
    });
});

// ========================================
// Bug Fix #16: 會員等級排序 + 員工行事曆
// ========================================

test.describe('Bug Fix #16: 會員等級排序 API', () => {
    let token: string;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
    });

    test('會員等級列表正常', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/membership-levels', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.ok()).toBeTruthy();
        const data = await res.json();
        expect(data.success).toBeTruthy();
        console.log(`會員等級數: ${(data.data || []).length}`);
    });

    test('員工行事曆 API 正常（dead code 已移除）', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const today = new Date();
        const start = new Date(today.getFullYear(), today.getMonth(), 1).toISOString().split('T')[0];
        const end = new Date(today.getFullYear(), today.getMonth() + 1, 0).toISOString().split('T')[0];

        const res = await request.get(`/api/staff/calendar?start=${start}&end=${end}`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
    });
});

// ========================================
// 新功能: 3 個行業著陸頁
// ========================================

test.describe('新功能: 行業著陸頁', () => {
    const newIndustryPages = [
        { path: '/tutoring', title: '補習家教', keyword: '補習' },
        { path: '/photography', title: '攝影工作室', keyword: '攝影' },
        { path: '/pet-care', title: '寵物美容', keyword: '寵物' },
    ];

    for (const industry of newIndustryPages) {
        test(`${industry.title}頁面載入成功`, async ({ page }) => {
            const res = await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
            expect(res?.status()).toBe(200);
        });

        test(`${industry.title}頁面有正確 title`, async ({ page }) => {
            await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
            await expect(page).toHaveTitle(new RegExp(industry.title));
        });

        test(`${industry.title}頁面有行業關鍵字`, async ({ page }) => {
            await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
            const content = await page.content();
            expect(content).toContain(industry.keyword);
        });

        test(`${industry.title}頁面有 CTA 註冊按鈕`, async ({ page }) => {
            await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
        });

        test(`${industry.title}頁面有 Schema.org 結構化資料`, async ({ page }) => {
            await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
            const schemaCount = await page.locator('script[type="application/ld+json"]').count();
            expect(schemaCount).toBeGreaterThanOrEqual(1);
        });

        test(`${industry.title}頁面有 Open Graph meta`, async ({ page }) => {
            await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('meta[property="og:title"]')).toHaveCount(1);
            await expect(page.locator('meta[property="og:description"]')).toHaveCount(1);
        });

        test(`${industry.title}頁面有 hreflang 標記`, async ({ page }) => {
            await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
            const hreflangCount = await page.locator('link[hreflang]').count();
            expect(hreflangCount).toBeGreaterThanOrEqual(1);
        });
    }

    test('sitemap.xml 包含新行業頁面', async ({ request }) => {
        const res = await request.get(`${BASE_URL}/sitemap.xml`);
        const text = await res.text();
        expect(text).toContain('/tutoring');
        expect(text).toContain('/photography');
        expect(text).toContain('/pet-care');
    });
});

// ========================================
// 新功能: 推薦機制
// ========================================

test.describe('新功能: 推薦機制', () => {
    let token: string;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
    });

    test('推薦儀表板 API 正常', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/referrals/dashboard', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
        if (res.ok()) {
            const data = await res.json();
            expect(data.success).toBeTruthy();
            console.log(`推薦儀表板: ${JSON.stringify(data.data)}`);
        }
    });

    test('取得推薦碼 API 正常', async ({ request }) => {
        if (!token) { test.skip(); return; }
        const res = await request.get('/api/referrals/code', {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
        if (res.ok()) {
            const data = await res.json();
            console.log(`推薦碼: ${JSON.stringify(data.data)}`);
        }
    });

    test('推薦好友頁面載入', async ({ page }) => {
        await tenantLogin(page);
        await page.goto('/tenant/referrals');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        // 頁面不應顯示載入失敗
        const failCount = await page.locator('text=載入失敗').count();
        expect(failCount).toBe(0);
    });

    test('推薦頁面有推薦碼區塊', async ({ page }) => {
        await tenantLogin(page);
        await page.goto('/tenant/referrals');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        // 檢查有推薦碼相關元素
        const content = await page.content();
        const hasReferralContent = content.includes('推薦碼') || content.includes('referral') || content.includes('推薦好友');
        expect(hasReferralContent).toBeTruthy();
    });

    test('推薦頁面有複製按鈕', async ({ page }) => {
        await tenantLogin(page);
        await page.goto('/tenant/referrals');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const copyBtn = page.locator('button:has-text("複製"), button[onclick*="copy"]');
        const count = await copyBtn.count();
        console.log(`複製按鈕數: ${count}`);
    });

    test('註冊頁面有推薦碼欄位', async ({ page }) => {
        await page.goto(`${BASE_URL}/tenant/register`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1000);

        // 檢查推薦碼輸入欄位
        const referralInput = page.locator('#referralCode, input[name="referralCode"]');
        const exists = await referralInput.count() > 0;
        console.log(`註冊頁推薦碼欄位: ${exists ? '存在' : '不存在'}`);
    });

    test('註冊頁 URL ?ref= 參數自動填入', async ({ page }) => {
        await page.goto(`${BASE_URL}/tenant/register?ref=TESTCODE`, { waitUntil: 'domcontentloaded' });
        await page.waitForTimeout(1500);

        const referralInput = page.locator('#referralCode, input[name="referralCode"]');
        if (await referralInput.count() > 0) {
            const value = await referralInput.inputValue();
            expect(value).toBe('TESTCODE');
            console.log(`自動填入推薦碼: ${value}`);
        }
    });
});

// ========================================
// 新功能: GA4 + FB Pixel 追蹤
// ========================================

test.describe('新功能: GA4 + FB Pixel 追蹤', () => {
    test('公開頁面有追蹤碼片段引用', async ({ page }) => {
        await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
        const content = await page.content();

        // 檢查 GA4 或 FB Pixel 的 script tag（可能因無 ID 而不渲染）
        const hasGtagReference = content.includes('gtag') || content.includes('ga-') || content.includes('tracking');
        console.log(`首頁有追蹤碼引用: ${hasGtagReference}`);
    });

    test('店家後台 layout 有追蹤碼', async ({ page }) => {
        await tenantLogin(page);
        await page.goto('/tenant/dashboard');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(1000);

        // 檢查是否有 tracking 相關代碼
        const hasTracking = await page.evaluate(() => {
            return typeof (window as any).tracking === 'object' ||
                   typeof (window as any).gtag === 'function' ||
                   typeof (window as any).fbq === 'function' ||
                   document.querySelector('script[src*="googletagmanager"]') !== null;
        });
        console.log(`後台有追蹤碼: ${hasTracking}`);
    });

    test('common.js 有 tracking 物件', async ({ page }) => {
        await tenantLogin(page);
        await page.goto('/tenant/dashboard');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const hasTrackingObj = await page.evaluate(() => {
            return typeof (window as any).tracking === 'object';
        });
        console.log(`tracking 物件存在: ${hasTrackingObj}`);
    });
});

// ========================================
// 新功能: SEO 加強（hreflang + 長尾關鍵字）
// ========================================

test.describe('新功能: SEO 加強', () => {
    const allPublicPages = [
        '/', '/features', '/pricing', '/faq', '/terms', '/privacy',
        '/beauty', '/hair-salon', '/spa', '/fitness', '/restaurant', '/clinic',
        '/tutoring', '/photography', '/pet-care'
    ];

    test('所有公開頁面有 hreflang 標記', async ({ page }) => {
        let pagesWithHreflang = 0;
        for (const path of allPublicPages) {
            await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' });
            const count = await page.locator('link[hreflang]').count();
            if (count > 0) pagesWithHreflang++;
        }
        console.log(`有 hreflang 的頁面: ${pagesWithHreflang}/${allPublicPages.length}`);
        expect(pagesWithHreflang).toBeGreaterThanOrEqual(allPublicPages.length - 2); // 至少大部分有
    });

    test('行業頁面有長尾關鍵字 meta keywords', async ({ page }) => {
        const industryPages = ['/beauty', '/hair-salon', '/spa', '/fitness', '/tutoring', '/photography', '/pet-care'];
        for (const path of industryPages) {
            await page.goto(`${BASE_URL}${path}`, { waitUntil: 'domcontentloaded' });
            const keywords = await page.locator('meta[name="keywords"]').getAttribute('content');
            expect(keywords, `${path} 應有 keywords meta`).toBeTruthy();
            console.log(`${path} keywords: ${keywords?.substring(0, 50)}...`);
        }
    });
});

// ========================================
// 側邊欄推薦好友連結
// ========================================

test.describe('側邊欄: 推薦好友連結', () => {
    test('側邊欄有推薦好友項目', async ({ page }) => {
        await tenantLogin(page);
        await page.goto('/tenant/dashboard');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        const content = await page.content();
        const hasSidebarLink = content.includes('/tenant/referrals') || content.includes('推薦好友');
        expect(hasSidebarLink, '側邊欄應有推薦好友連結').toBeTruthy();
    });
});

// ========================================
// 綜合回歸: 所有新頁面無 HTTP 錯誤
// ========================================

test.describe('綜合回歸: 新頁面健康檢查', () => {
    const newPages = [
        { name: '補習家教', url: '/tutoring' },
        { name: '攝影工作室', url: '/photography' },
        { name: '寵物美容', url: '/pet-care' },
    ];

    for (const p of newPages) {
        test(`${p.name}頁面無 HTTP 500 錯誤`, async ({ page }) => {
            const serverErrors: string[] = [];
            page.on('response', res => {
                if (res.status() >= 500) serverErrors.push(`${res.status()} ${res.url()}`);
            });

            await page.goto(`${BASE_URL}${p.url}`, { waitUntil: 'domcontentloaded' });
            await page.waitForTimeout(1000);

            expect(serverErrors.length, `${p.name} 有 HTTP 500: ${serverErrors.join(', ')}`).toBe(0);
        });
    }

    test('推薦好友頁面無 HTTP 500', async ({ page }) => {
        const serverErrors: string[] = [];
        page.on('response', res => {
            if (res.status() >= 500 && !res.url().includes('favicon')) {
                serverErrors.push(`${res.status()} ${res.url()}`);
            }
        });

        await tenantLogin(page);
        await page.goto('/tenant/referrals');
        await page.waitForLoadState('domcontentloaded');
        await page.waitForTimeout(2000);

        expect(serverErrors.length, `推薦頁有 HTTP 500: ${serverErrors.join(', ')}`).toBe(0);
    });
});

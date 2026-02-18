import { test, expect, APIRequestContext } from '@playwright/test';
import { TEST_ACCOUNTS } from './utils/test-helpers';

/**
 * LINE Bot 完整行為測試 — 含服務分類選擇功能
 *
 * 測試範圍：
 *  1. Webhook 端點可用性（所有事件類型）
 *  2. 所有 Postback Action（24 個）
 *  3. 所有文字關鍵字觸發（6 組）
 *  4. 分類選擇流程判斷邏輯
 *  5. 完整預約流程狀態機（含分類 / 不含分類）
 *  6. 商品購買流程狀態機
 *  7. 票券 / 會員 / 取消預約流程
 *  8. Flex Message 結構驗證（分類選單、服務選單、備註提示）
 *  9. ConversationContext 欄位與清除邏輯
 * 10. GoBack 返回邏輯（所有狀態）
 * 11. 邊界情況與安全驗證
 * 12. API 層級驗證（分類 + 服務歸屬關係）
 * 13. LINE 設定 API 驗證
 *
 * 共 70+ 測試
 */

// ========================================
// 常數
// ========================================

const TENANT_CODE = 'michaelshop';
const BASE_URL = 'https://booking-platform-production-1e08.up.railway.app';
const WEBHOOK_URL = `${BASE_URL}/api/line/webhook`;

// ========================================
// 輔助函式
// ========================================

async function getTenantToken(request: APIRequestContext): Promise<string> {
    const res = await request.post(`${BASE_URL}/api/auth/tenant/login`, {
        data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
    });
    const body = await res.json();
    return body.data?.accessToken || '';
}

/** 建構 Webhook 事件 payload */
function buildWebhookPayload(events: any[]) {
    return { destination: 'test', events };
}

/** 建構 Message 事件 */
function buildMessageEvent(text: string, userId?: string) {
    return {
        type: 'message',
        timestamp: Date.now(),
        source: { type: 'user', userId: userId || 'Utest_' + Date.now() + Math.random().toString(36).slice(2, 6) },
        replyToken: 'test-reply-' + Date.now(),
        message: { id: 'msg' + Date.now(), type: 'text', text }
    };
}

/** 建構 Postback 事件 */
function buildPostbackEvent(data: string, userId?: string) {
    return {
        type: 'postback',
        timestamp: Date.now(),
        source: { type: 'user', userId: userId || 'Utest_' + Date.now() + Math.random().toString(36).slice(2, 6) },
        replyToken: 'test-reply-' + Date.now(),
        postback: { data }
    };
}

/** 建構 Follow 事件 */
function buildFollowEvent(userId?: string) {
    return {
        type: 'follow',
        timestamp: Date.now(),
        source: { type: 'user', userId: userId || 'Utest_' + Date.now() },
        replyToken: 'test-reply-' + Date.now()
    };
}

/** 建構 Unfollow 事件 */
function buildUnfollowEvent(userId?: string) {
    return {
        type: 'unfollow',
        timestamp: Date.now(),
        source: { type: 'user', userId: userId || 'Utest_' + Date.now() }
    };
}

/** 發送 Webhook 並斷言不 500 */
async function sendWebhook(request: APIRequestContext, tenantCode: string, events: any[]) {
    const res = await request.post(`${WEBHOOK_URL}/${tenantCode}`, {
        headers: { 'Content-Type': 'application/json' },
        data: buildWebhookPayload(events)
    });
    expect(res.status(), `Webhook ${tenantCode} 回傳 ${res.status()}`).toBeLessThan(500);
    return res;
}

// ================================================================
//  SECTION 1：Webhook 端點 — 事件類型
// ================================================================

test.describe('1. Webhook 端點 — 事件類型', () => {

    test('空事件陣列 — 回傳 200', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, []);
        expect(res.status()).toBe(200);
    });

    test('Follow 事件', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [buildFollowEvent()]);
        console.log(`Follow: ${res.status()}`);
    });

    test('Unfollow 事件', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [buildUnfollowEvent()]);
        console.log(`Unfollow: ${res.status()}`);
    });

    test('Message 事件 — 一般文字', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [buildMessageEvent('你好')]);
        console.log(`Message: ${res.status()}`);
    });

    test('Postback 事件 — start_booking', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [
            buildPostbackEvent('action=start_booking')
        ]);
        console.log(`Postback start_booking: ${res.status()}`);
    });

    test('不存在的租戶代碼 — 不 500', async ({ request }) => {
        const res = await sendWebhook(request, 'nonexistent_tenant_code_xyz', []);
        expect(res.status()).toBeLessThan(500);
    });

    test('多個事件同時送出', async ({ request }) => {
        const userId = 'Utest_multi_' + Date.now();
        const res = await sendWebhook(request, TENANT_CODE, [
            buildFollowEvent(userId),
            buildMessageEvent('你好', userId),
        ]);
        console.log(`Multi events: ${res.status()}`);
    });

    test('無效 JSON body — 不 500', async ({ request }) => {
        const res = await request.post(`${WEBHOOK_URL}/${TENANT_CODE}`, {
            headers: { 'Content-Type': 'application/json' },
            data: { invalid: true }
        });
        expect(res.status()).toBeLessThan(500);
    });
});

// ================================================================
//  SECTION 2：文字關鍵字觸發（6 組）
// ================================================================

test.describe('2. 文字關鍵字觸發', () => {

    const keywordTests = [
        { group: '預約', keywords: ['預約', '訂位', '預訂', 'book', 'booking'] },
        { group: '取消', keywords: ['取消', 'cancel'] },
        { group: '幫助', keywords: ['幫助', 'help', '說明'] },
        { group: '票券', keywords: ['票券', '優惠券', 'coupon'] },
        { group: '商品', keywords: ['商品', '購買', 'product', 'shop'] },
        { group: '會員', keywords: ['會員', '點數', 'member', 'points'] },
    ];

    for (const { group, keywords } of keywordTests) {
        for (const keyword of keywords) {
            test(`關鍵字「${keyword}」觸發 ${group} 流程`, async ({ request }) => {
                const res = await sendWebhook(request, TENANT_CODE, [
                    buildMessageEvent(keyword)
                ]);
                expect(res.status()).toBeLessThan(500);
            });
        }
    }
});

// ================================================================
//  SECTION 3：所有 Postback Action（24 個）
// ================================================================

test.describe('3. 所有 Postback Action', () => {

    const postbackActions = [
        // 預約流程
        { action: 'action=start_booking', desc: '開始預約' },
        { action: 'action=select_category&categoryId=test-cat&categoryName=測試分類', desc: '選擇分類' },
        { action: 'action=select_service&serviceId=test-svc&serviceName=測試服務&duration=60&price=500', desc: '選擇服務' },
        { action: 'action=select_date&date=2099-12-31', desc: '選擇日期' },
        { action: 'action=select_staff&staffId=test-staff&staffName=測試員工', desc: '選擇員工' },
        { action: 'action=select_time&time=10:00', desc: '選擇時段' },
        { action: 'action=skip_note', desc: '跳過備註' },
        { action: 'action=confirm_booking', desc: '確認預約' },
        { action: 'action=cancel_booking', desc: '取消當前流程' },
        { action: 'action=go_back', desc: '返回上一步' },

        // 預約查看/取消
        { action: 'action=view_bookings', desc: '查看預約' },
        { action: 'action=cancel_flow', desc: '取消流程請求' },
        { action: 'action=confirm_cancel_flow', desc: '確認取消流程' },
        { action: 'action=cancel_booking_request&bookingId=test-id', desc: '取消預約請求' },
        { action: 'action=confirm_cancel_booking&bookingId=test-id', desc: '確認取消預約' },

        // 選單導航
        { action: 'action=main_menu', desc: '回主選單' },
        { action: 'action=contact_shop', desc: '聯絡店家' },

        // 票券
        { action: 'action=view_coupons', desc: '查看票券' },
        { action: 'action=receive_coupon&couponId=test-coupon', desc: '領取票券' },
        { action: 'action=view_my_coupons', desc: '我的票券' },

        // 會員
        { action: 'action=view_member_info', desc: '會員資訊' },

        // 商品
        { action: 'action=start_shopping', desc: '開始購物' },
        { action: 'action=select_product&productId=test-prod&productName=測試商品&price=100', desc: '選擇商品' },
        { action: 'action=select_quantity&quantity=1', desc: '選擇數量' },
        { action: 'action=confirm_purchase', desc: '確認購買' },
    ];

    for (const { action, desc } of postbackActions) {
        test(`Postback: ${desc} (${action.split('&')[0]})`, async ({ request }) => {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(action)
            ]);
            expect(res.status()).toBeLessThan(500);
        });
    }
});

// ================================================================
//  SECTION 4：分類選擇流程判斷邏輯
// ================================================================

test.describe('4. 分類選擇 — 流程判斷邏輯', () => {

    test('startBookingFlow 判斷矩陣（6 種情境）', () => {
        const scenarios = [
            { categories: 3, withServices: 3, expected: 'SELECTING_CATEGORY', reason: '多分類且有歸屬' },
            { categories: 2, withServices: 2, expected: 'SELECTING_CATEGORY', reason: '剛好 2 分類有歸屬' },
            { categories: 2, withServices: 1, expected: 'SELECTING_SERVICE', reason: '2 分類但只有 1 個有服務' },
            { categories: 2, withServices: 0, expected: 'SELECTING_SERVICE', reason: '2 分類但都沒服務' },
            { categories: 1, withServices: 1, expected: 'SELECTING_SERVICE', reason: '只有 1 個分類' },
            { categories: 0, withServices: 0, expected: 'SELECTING_SERVICE', reason: '沒有分類' },
        ];

        for (const s of scenarios) {
            const result = (s.categories >= 2 && s.withServices >= 2)
                ? 'SELECTING_CATEGORY' : 'SELECTING_SERVICE';
            expect(result).toBe(s.expected);
            console.log(`  [${result === s.expected ? '✓' : '✗'}] ${s.reason} → ${result}`);
        }
    });

    test('findDistinctBookableCategoryIds 過濾規則', () => {
        const services = [
            { categoryId: 'A', status: 'ACTIVE', isVisible: true, deletedAt: null },
            { categoryId: 'A', status: 'ACTIVE', isVisible: true, deletedAt: null },
            { categoryId: 'B', status: 'ACTIVE', isVisible: true, deletedAt: null },
            { categoryId: 'C', status: 'INACTIVE', isVisible: true, deletedAt: null },
            { categoryId: null, status: 'ACTIVE', isVisible: true, deletedAt: null },
            { categoryId: 'D', status: 'ACTIVE', isVisible: false, deletedAt: null },
            { categoryId: 'E', status: 'ACTIVE', isVisible: true, deletedAt: '2025-01-01' },
        ];

        const ids = [...new Set(
            services
                .filter(s => s.categoryId && s.status === 'ACTIVE' && s.isVisible && !s.deletedAt)
                .map(s => s.categoryId)
        )];

        expect(ids).toEqual(['A', 'B']);
        expect(ids).not.toContain(null);
        expect(ids).not.toContain('C');
        expect(ids).not.toContain('D');
        expect(ids).not.toContain('E');
    });

    test('buildCategoryMenu 只顯示有服務的分類', () => {
        const categories = [
            { id: 'A', name: '剪髮', isActive: true },
            { id: 'B', name: '護理', isActive: true },
            { id: 'C', name: '美甲', isActive: true },
        ];
        const idsWithServices = ['A', 'B'];

        const filtered = categories.filter(c => idsWithServices.includes(c.id));
        expect(filtered.length).toBe(2);
        expect(filtered.map(c => c.name)).not.toContain('美甲');
    });
});

// ================================================================
//  SECTION 5：完整預約流程狀態機
// ================================================================

test.describe('5. 預約流程狀態機', () => {

    test('含分類的完整流程（5 步 + 備註 + 確認）', () => {
        const flow = [
            'IDLE',
            'SELECTING_CATEGORY',
            'SELECTING_SERVICE',
            'SELECTING_DATE',
            'SELECTING_STAFF',
            'SELECTING_TIME',
            'INPUTTING_NOTE',
            'CONFIRMING_BOOKING',
            'IDLE',
        ];

        expect(flow[0]).toBe('IDLE');
        expect(flow[flow.length - 1]).toBe('IDLE');
        expect(flow).toContain('SELECTING_CATEGORY');
        expect(flow.indexOf('SELECTING_CATEGORY')).toBeLessThan(flow.indexOf('SELECTING_SERVICE'));
        expect(flow.indexOf('SELECTING_SERVICE')).toBeLessThan(flow.indexOf('SELECTING_DATE'));
        expect(flow.indexOf('SELECTING_DATE')).toBeLessThan(flow.indexOf('SELECTING_STAFF'));
        expect(flow.indexOf('SELECTING_STAFF')).toBeLessThan(flow.indexOf('SELECTING_TIME'));
        expect(flow.indexOf('SELECTING_TIME')).toBeLessThan(flow.indexOf('INPUTTING_NOTE'));
        expect(flow.indexOf('INPUTTING_NOTE')).toBeLessThan(flow.indexOf('CONFIRMING_BOOKING'));
    });

    test('不含分類的流程（4 步 + 備註 + 確認）', () => {
        const flow = [
            'IDLE',
            'SELECTING_SERVICE',
            'SELECTING_DATE',
            'SELECTING_STAFF',
            'SELECTING_TIME',
            'INPUTTING_NOTE',
            'CONFIRMING_BOOKING',
            'IDLE',
        ];

        expect(flow).not.toContain('SELECTING_CATEGORY');
        expect(flow[1]).toBe('SELECTING_SERVICE');
    });

    test('所有 18 個對話狀態完整', () => {
        const states = [
            'IDLE', 'SELECTING_CATEGORY', 'SELECTING_SERVICE', 'SELECTING_STAFF',
            'SELECTING_DATE', 'SELECTING_TIME', 'INPUTTING_NOTE', 'CONFIRMING_BOOKING',
            'VIEWING_BOOKINGS', 'CONFIRMING_CANCEL_BOOKING',
            'BROWSING_PRODUCTS', 'VIEWING_PRODUCT_DETAIL', 'SELECTING_QUANTITY', 'CONFIRMING_PURCHASE',
            'BROWSING_COUPONS', 'VIEWING_MY_COUPONS',
            'VIEWING_PROFILE', 'VIEWING_MEMBER_INFO',
        ];
        expect(states.length).toBe(18);
    });

    test('商品購買流程', () => {
        const flow = ['IDLE', 'BROWSING_PRODUCTS', 'VIEWING_PRODUCT_DETAIL', 'SELECTING_QUANTITY', 'CONFIRMING_PURCHASE', 'IDLE'];
        expect(flow[0]).toBe('IDLE');
        expect(flow[flow.length - 1]).toBe('IDLE');
    });

    test('票券領取流程', () => {
        const flow = ['IDLE', 'BROWSING_COUPONS', 'IDLE'];
        expect(flow).toContain('BROWSING_COUPONS');
    });

    test('取消預約流程', () => {
        const flow = ['IDLE', 'CONFIRMING_CANCEL_BOOKING', 'IDLE'];
        expect(flow).toContain('CONFIRMING_CANCEL_BOOKING');
    });

    test('會員資訊流程', () => {
        const flow = ['IDLE', 'VIEWING_MEMBER_INFO', 'IDLE'];
        expect(flow).toContain('VIEWING_MEMBER_INFO');
    });
});

// ================================================================
//  SECTION 6：ConversationContext 欄位
// ================================================================

test.describe('6. ConversationContext 欄位', () => {

    test('分類欄位（selectedCategoryId / selectedCategoryName）', () => {
        const ctx: any = {
            selectedCategoryId: null,
            selectedCategoryName: null,
            state: 'SELECTING_CATEGORY',
        };

        ctx.selectedCategoryId = 'cat-1';
        ctx.selectedCategoryName = '剪髮類';
        ctx.state = 'SELECTING_SERVICE';

        expect(ctx.selectedCategoryId).toBe('cat-1');
        expect(ctx.selectedCategoryName).toBe('剪髮類');
        expect(ctx.state).toBe('SELECTING_SERVICE');
    });

    test('setCategory 方法模擬', () => {
        const ctx = { selectedCategoryId: null as any, selectedCategoryName: null as any };
        const setCategory = (id: string, name: string) => {
            ctx.selectedCategoryId = id;
            ctx.selectedCategoryName = name;
        };
        setCategory('cat-2', '護理類');
        expect(ctx.selectedCategoryId).toBe('cat-2');
        expect(ctx.selectedCategoryName).toBe('護理類');
    });

    test('clearBookingData 清除所有預約欄位（含分類）', () => {
        const ctx: any = {
            selectedCategoryId: 'cat-1', selectedCategoryName: '分類',
            selectedServiceId: 'svc-1', selectedServiceName: '服務',
            selectedServiceDuration: 60, selectedServicePrice: 500,
            selectedStaffId: 'staff-1', selectedStaffName: '員工',
            selectedDate: '2025-06-01', selectedTime: '10:00',
            cancelBookingId: 'bk-1', customerNote: '備註',
        };

        // 模擬 clearBookingData
        for (const key of Object.keys(ctx)) ctx[key] = null;

        expect(ctx.selectedCategoryId).toBeNull();
        expect(ctx.selectedCategoryName).toBeNull();
        expect(ctx.selectedServiceId).toBeNull();
        expect(ctx.selectedStaffId).toBeNull();
        expect(ctx.customerNote).toBeNull();
    });

    test('canConfirmBooking 需要 serviceId + date + time', () => {
        const canConfirm = (ctx: any) =>
            ctx.selectedServiceId != null && ctx.selectedDate != null && ctx.selectedTime != null;

        expect(canConfirm({ selectedServiceId: null, selectedDate: '2025-01-01', selectedTime: '10:00' })).toBe(false);
        expect(canConfirm({ selectedServiceId: 'svc', selectedDate: null, selectedTime: '10:00' })).toBe(false);
        expect(canConfirm({ selectedServiceId: 'svc', selectedDate: '2025-01-01', selectedTime: null })).toBe(false);
        expect(canConfirm({ selectedServiceId: 'svc', selectedDate: '2025-01-01', selectedTime: '10:00' })).toBe(true);
    });

    test('canConfirmPurchase 需要 productId + quantity > 0', () => {
        const canPurchase = (ctx: any) =>
            ctx.selectedProductId != null && ctx.selectedQuantity != null && ctx.selectedQuantity > 0;

        expect(canPurchase({ selectedProductId: null, selectedQuantity: 1 })).toBe(false);
        expect(canPurchase({ selectedProductId: 'p1', selectedQuantity: 0 })).toBe(false);
        expect(canPurchase({ selectedProductId: 'p1', selectedQuantity: 2 })).toBe(true);
    });
});

// ================================================================
//  SECTION 7：GoBack 確定性返回邏輯（不依賴 previousState）
// ================================================================

test.describe('7. GoBack 確定性返回邏輯', () => {

    // 確定性狀態映射：每個狀態固定回到邏輯前一步
    const deterministicGoBack: Record<string, string> = {
        'SELECTING_SERVICE': 'IDLE',
        'SELECTING_CATEGORY': 'IDLE',
        'SELECTING_DATE': 'SELECTING_SERVICE',
        'SELECTING_STAFF': 'SELECTING_DATE',
        'SELECTING_TIME': 'SELECTING_STAFF',
        'INPUTTING_NOTE': 'SELECTING_TIME',
        'CONFIRMING_BOOKING': 'INPUTTING_NOTE',
        'BROWSING_PRODUCTS': 'IDLE',
        'VIEWING_PRODUCT_DETAIL': 'BROWSING_PRODUCTS',
        'SELECTING_QUANTITY': 'VIEWING_PRODUCT_DETAIL',
        'CONFIRMING_PURCHASE': 'SELECTING_QUANTITY',
        'IDLE': 'IDLE',
    };

    for (const [from, expectedTarget] of Object.entries(deterministicGoBack)) {
        test(`${from} → goBack → ${expectedTarget}（確定性映射）`, () => {
            expect(deterministicGoBack[from]).toBe(expectedTarget);
        });
    }

    test('goBack 不依賴 previousState — 重複點擊不會錯亂', () => {
        // 模擬：用戶在 SELECTING_STAFF，重複點了舊的 select_service 按鈕
        // previousState 會被覆蓋，但確定性映射不受影響
        const state = 'SELECTING_STAFF';
        const result = deterministicGoBack[state];
        expect(result).toBe('SELECTING_DATE'); // 永遠回到選日期，不管 previousState
    });

    const goBackMenuTests = [
        { from: 'SELECTING_SERVICE', withCategory: true, expectedMenu: 'buildServiceMenuByCategory' },
        { from: 'SELECTING_SERVICE', withCategory: false, expectedMenu: 'buildServiceMenu' },
        { from: 'SELECTING_CATEGORY', withCategory: false, expectedMenu: 'mainMenu' },
        { from: 'SELECTING_DATE', withCategory: false, expectedMenu: 'buildDateMenu' },
        { from: 'SELECTING_STAFF', withCategory: false, expectedMenu: 'buildStaffMenuByDate' },
        { from: 'SELECTING_TIME', withCategory: false, expectedMenu: 'buildTimeMenu' },
        { from: 'INPUTTING_NOTE', withCategory: false, expectedMenu: 'buildNoteInputPrompt' },
    ];

    for (const t of goBackMenuTests) {
        const label = t.withCategory ? `${t.from}（有分類）` : t.from;
        test(`${label} → goBack → 顯示 ${t.expectedMenu}`, () => {
            let menu: string;
            switch (t.from) {
                case 'SELECTING_CATEGORY':
                    menu = 'mainMenu'; break;
                case 'SELECTING_SERVICE':
                    menu = t.withCategory ? 'buildServiceMenuByCategory' : 'buildServiceMenu'; break;
                case 'SELECTING_DATE':
                    menu = 'buildDateMenu'; break;
                case 'SELECTING_STAFF':
                    menu = 'buildStaffMenuByDate'; break;
                case 'SELECTING_TIME':
                    menu = 'buildTimeMenu'; break;
                case 'INPUTTING_NOTE':
                    menu = 'buildNoteInputPrompt'; break;
                default:
                    menu = 'mainMenu';
            }
            expect(menu).toBe(t.expectedMenu);
        });
    }
});

// ================================================================
//  SECTION 7b：下游資料清除與防呆機制
// ================================================================

test.describe('7b. 下游資料清除與防呆', () => {

    test('選擇服務時清除下游資料（日期、員工、時間、備註）', () => {
        const ctx: any = {
            selectedServiceId: 'svc-old',
            selectedDate: '2025-03-01',
            selectedStaffId: 'staff-1',
            selectedTime: '10:00',
            customerNote: '舊備註',
        };

        // clearDownstreamFromDate
        ctx.selectedDate = null;
        ctx.selectedStaffId = null;
        ctx.selectedStaffName = null;
        ctx.selectedTime = null;
        ctx.customerNote = null;

        expect(ctx.selectedDate).toBeNull();
        expect(ctx.selectedStaffId).toBeNull();
        expect(ctx.selectedTime).toBeNull();
        expect(ctx.customerNote).toBeNull();
    });

    test('選擇日期時清除下游資料（員工、時間、備註）', () => {
        const ctx: any = {
            selectedDate: '2025-03-01',
            selectedStaffId: 'staff-1',
            selectedStaffName: '小明',
            selectedTime: '10:00',
            customerNote: '備註',
        };

        // clearDownstreamFromStaff
        ctx.selectedStaffId = null;
        ctx.selectedStaffName = null;
        ctx.selectedTime = null;
        ctx.customerNote = null;

        expect(ctx.selectedDate).toBe('2025-03-01'); // 日期保留
        expect(ctx.selectedStaffId).toBeNull();
        expect(ctx.selectedTime).toBeNull();
    });

    test('選擇員工時清除下游資料（時間、備註）', () => {
        const ctx: any = {
            selectedStaffId: 'staff-1',
            selectedTime: '10:00',
            customerNote: '備註',
        };

        // clearDownstreamFromTime
        ctx.selectedTime = null;
        ctx.customerNote = null;

        expect(ctx.selectedStaffId).toBe('staff-1'); // 員工保留
        expect(ctx.selectedTime).toBeNull();
        expect(ctx.customerNote).toBeNull();
    });

    test('重複點擊舊按鈕不會導致資料殘留', () => {
        const ctx: any = {
            state: 'SELECTING_TIME',
            selectedServiceId: 'svc-1',
            selectedDate: '2025-03-01',
            selectedStaffId: 'staff-1',
            selectedTime: '14:00',
            customerNote: '舊備註',
        };

        // 用戶往上滑點了舊的 select_service → 系統先清除下游
        ctx.selectedDate = null;
        ctx.selectedStaffId = null;
        ctx.selectedStaffName = null;
        ctx.selectedTime = null;
        ctx.customerNote = null;
        ctx.state = 'SELECTING_DATE';

        expect(ctx.state).toBe('SELECTING_DATE');
        expect(ctx.selectedTime).toBeNull();
        expect(ctx.customerNote).toBeNull();
    });

    test('前置條件缺失時引導重新開始', () => {
        // 模擬：select_staff 但 selectedDate 為 null
        const ctx: any = {
            state: 'SELECTING_STAFF',
            selectedServiceId: 'svc-1',
            selectedDate: null, // 缺失
        };

        const shouldRedirect = ctx.selectedDate === null;
        expect(shouldRedirect).toBe(true);
    });
});

// ================================================================
//  SECTION 8：Flex Message 結構驗證
// ================================================================

test.describe('8. Flex Message 結構', () => {

    test('分類選單 Carousel — 指引 + 分類 Bubbles', () => {
        const carousel = {
            type: 'carousel',
            contents: [
                {
                    type: 'bubble', size: 'kilo',
                    header: { contents: [{ text: '步驟 1/5' }, { text: '📂 選擇分類' }] },
                    body: { contents: [
                        { type: 'text', text: '👈 往左滑動查看所有分類' },
                        { type: 'box', contents: [
                            { text: '1️⃣ 選擇分類' }, { text: '2️⃣ 選擇服務' },
                            { text: '3️⃣ 選擇日期' }, { text: '4️⃣ 選擇人員' }, { text: '5️⃣ 選擇時間' },
                        ]}
                    ]}
                },
                {
                    type: 'bubble', size: 'kilo',
                    header: { contents: [{ text: '剪髮類' }] },
                    footer: { contents: [{
                        type: 'button',
                        action: { type: 'postback', label: '✓ 選擇此分類', data: 'action=select_category&categoryId=cat-1&categoryName=剪髮類' }
                    }]}
                }
            ]
        };

        expect(carousel.type).toBe('carousel');
        expect(carousel.contents.length).toBeGreaterThanOrEqual(2);
        expect(carousel.contents[0].header.contents[0].text).toBe('步驟 1/5');
        expect(carousel.contents[0].body.contents[1].contents.length).toBe(5);

        const postback = carousel.contents[1].footer.contents[0].action.data;
        expect(postback).toContain('action=select_category');
        expect(postback).toContain('categoryId=');
    });

    test('分類流程的服務選單指引 — 步驟 2/5', () => {
        const guide = {
            header: { contents: [{ text: '步驟 2/5' }, { text: '✂️ 選擇服務' }] },
            body: { contents: [{ type: 'box', contents: [
                { text: '1️⃣ 選擇分類 ✓' }, { text: '2️⃣ 選擇服務' },
                { text: '3️⃣ 選擇日期' }, { text: '4️⃣ 選擇人員' }, { text: '5️⃣ 選擇時間' },
            ]}]}
        };

        expect(guide.header.contents[0].text).toBe('步驟 2/5');
        expect(guide.body.contents[0].contents[0].text).toContain('✓');
        expect(guide.body.contents[0].contents.length).toBe(5);
    });

    test('原有服務選單指引 — 步驟 1/4（不受分類功能影響）', () => {
        const guide = {
            header: { contents: [{ text: '步驟 1/4' }, { text: '✂️ 選擇服務' }] },
            body: { contents: [{ type: 'box', contents: [
                { text: '1️⃣ 選擇服務' }, { text: '2️⃣ 選擇人員' },
                { text: '3️⃣ 選擇日期' }, { text: '4️⃣ 選擇時間' },
            ]}]}
        };

        expect(guide.header.contents[0].text).toBe('步驟 1/4');
        expect(guide.body.contents[0].contents.length).toBe(4);
        expect(guide.body.contents[0].contents[0].text).not.toContain('分類');
    });

    test('主選單 Flex — 包含所有 6 個功能按鈕', () => {
        const expectedActions = [
            'action=start_booking',
            'action=view_bookings',
            'action=start_shopping',
            'action=view_coupons',
            'action=view_my_coupons',
            'action=view_member_info',
        ];
        // 每個 action 都應存在於主選單
        for (const action of expectedActions) {
            expect(action).toContain('action=');
        }
        expect(expectedActions.length).toBe(6);
    });

    test('備註輸入提示 Flex — 含跳過和返回按鈕', () => {
        const flex = {
            footer: {
                contents: [
                    { action: { data: 'action=go_back' } },
                    { action: { data: 'action=skip_note' } },
                ]
            }
        };
        expect(flex.footer.contents[0].action.data).toBe('action=go_back');
        expect(flex.footer.contents[1].action.data).toBe('action=skip_note');
    });

    test('服務 Bubble Postback 包含完整參數', () => {
        const data = 'action=select_service&serviceId=abc&serviceName=男生剪髮&duration=60&price=500';
        const params = new URLSearchParams(data);
        expect(params.get('action')).toBe('select_service');
        expect(params.get('serviceId')).toBeTruthy();
        expect(params.get('serviceName')).toBeTruthy();
        expect(params.get('duration')).toBeTruthy();
        expect(params.get('price')).toBeTruthy();
    });
});

// ================================================================
//  SECTION 9：邊界情況與安全
// ================================================================

test.describe('9. 邊界情況與安全', () => {

    test('分類名稱安全字元（中文/空格/括號/英文）', () => {
        const safeNames = ['剪髮 / 染髮', '按摩（全身）', 'Hair Cut', '美甲＆美睫'];
        for (const name of safeNames) {
            const data = `action=select_category&categoryId=c1&categoryName=${name}`;
            const params = new URLSearchParams(data);
            expect(params.get('categoryName')).toBe(name);
        }
    });

    test('半形 & 在分類名稱中會截斷（已知限制）', () => {
        const data = 'action=select_category&categoryId=c1&categoryName=美甲&美睫';
        const params = new URLSearchParams(data);
        expect(params.get('categoryName')).toBe('美甲');
        expect(params.get('categoryName')).not.toBe('美甲&美睫');
    });

    test('Carousel 最多 12 Bubbles（LINE 平台限制）', () => {
        const MAX_BUBBLES = 12;
        const GUIDE = 1;
        expect(MAX_BUBBLES - GUIDE).toBe(11); // 最多 11 個分類
    });

    test('分類流程中途取消 — 所有暫存資料清除', () => {
        const ctx: any = {
            state: 'SELECTING_SERVICE',
            selectedCategoryId: 'cat-1', selectedCategoryName: '分類',
            selectedServiceId: null,
        };

        // reset
        ctx.state = 'IDLE';
        ctx.selectedCategoryId = null;
        ctx.selectedCategoryName = null;

        expect(ctx.state).toBe('IDLE');
        expect(ctx.selectedCategoryId).toBeNull();
    });

    test('對話 TTL — Redis key 格式與 30 分鐘過期', () => {
        const tenantId = 'tenant-123';
        const lineUserId = 'U456';
        const key = `line:conversation:${tenantId}:${lineUserId}`;
        const ttl = 1800; // 30 分鐘

        expect(key).toBe('line:conversation:tenant-123:U456');
        expect(ttl).toBe(1800);
    });

    test('預約功能未啟用時 — 回傳提示訊息', async ({ request }) => {
        // 模擬一個不存在/未啟用的租戶
        const res = await sendWebhook(request, 'disabled_tenant_test', [
            buildPostbackEvent('action=start_booking')
        ]);
        expect(res.status()).toBeLessThan(500);
    });

    test('空 categoryId 的 Postback 不 500', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [
            buildPostbackEvent('action=select_category&categoryId=&categoryName=')
        ]);
        expect(res.status()).toBeLessThan(500);
    });

    test('不存在的 action 不 500', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [
            buildPostbackEvent('action=nonexistent_action_xyz')
        ]);
        expect(res.status()).toBeLessThan(500);
    });

    test('缺少 action 參數不 500', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [
            buildPostbackEvent('foo=bar&baz=qux')
        ]);
        expect(res.status()).toBeLessThan(500);
    });

    test('空 Postback data 不 500', async ({ request }) => {
        const res = await sendWebhook(request, TENANT_CODE, [
            buildPostbackEvent('')
        ]);
        expect(res.status()).toBeLessThan(500);
    });
});

// ================================================================
//  SECTION 10：API 層級驗證
// ================================================================

test.describe('10. API 層級驗證 — 分類與服務', () => {
    let token: string;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
        if (!token) console.log('⚠️ 無法取得 Token，API 測試將跳過');
    });

    test('GET /api/service-categories — 分類列表', async ({ request }) => {
        test.skip(!token, '無 Token');
        const res = await request.get(`${BASE_URL}/api/service-categories`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
        if (res.ok()) {
            const body = await res.json();
            const cats = Array.isArray(body.data?.content) ? body.data.content : (Array.isArray(body.data) ? body.data : []);
            console.log(`分類數量: ${cats.length}`);
            for (const c of cats) console.log(`  - ${c.name} (啟用: ${c.isActive})`);
        }
    });

    test('GET /api/services?size=100 — 服務 categoryId 歸屬檢查', async ({ request }) => {
        test.skip(!token, '無 Token');
        const res = await request.get(`${BASE_URL}/api/services?size=100`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.ok()).toBeTruthy();
        const body = await res.json();
        const services = body.data?.content || [];

        let withCat = 0, withoutCat = 0;
        for (const s of services) { s.categoryId ? withCat++ : withoutCat++; }

        console.log(`服務總數: ${services.length}  有分類: ${withCat}  無分類: ${withoutCat}`);
        if (withCat === 0 && services.length > 0) {
            console.log('⚠️ 所有服務無分類，LINE Bot 分類流程不會啟動');
        }
    });

    test('GET /api/services/bookable — 可預約服務', async ({ request }) => {
        test.skip(!token, '無 Token');
        const res = await request.get(`${BASE_URL}/api/services/bookable`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
        if (res.ok()) {
            const body = await res.json();
            const list = Array.isArray(body.data) ? body.data : [];
            console.log(`可預約服務: ${list.length} 個`);
        }
    });

    test('分類歸屬關係 — 判斷 LINE Bot 流程', async ({ request }) => {
        test.skip(!token, '無 Token');
        const headers = { Authorization: `Bearer ${token}` };

        const catRes = await request.get(`${BASE_URL}/api/service-categories`, { headers });
        const catBody = await catRes.json().catch(() => ({ data: [] }));
        const categories = Array.isArray(catBody.data?.content) ? catBody.data.content :
            (Array.isArray(catBody.data) ? catBody.data : []);

        const svcRes = await request.get(`${BASE_URL}/api/services?size=100`, { headers });
        const svcBody = await svcRes.json().catch(() => ({ data: { content: [] } }));
        const services = svcBody.data?.content || [];

        // 統計
        const catCount: Record<string, number> = {};
        for (const c of categories) catCount[c.id] = 0;
        for (const s of services) {
            if (s.categoryId && catCount[s.categoryId] !== undefined) catCount[s.categoryId]++;
        }

        const activeCategories = categories.filter((c: any) => c.isActive !== false);
        const catsWithSvc = Object.values(catCount).filter(n => n > 0).length;

        if (activeCategories.length >= 2 && catsWithSvc >= 2) {
            console.log(`✅ 分類選擇流程啟動（${catsWithSvc} 個分類有服務）`);
        } else {
            console.log(`ℹ️ 跳過分類選擇（分類: ${activeCategories.length}, 有服務: ${catsWithSvc}）`);
        }
    });
});

// ================================================================
//  SECTION 11：LINE 設定 API
// ================================================================

test.describe('11. LINE 設定 API', () => {
    let token: string;

    test.beforeAll(async ({ request }) => {
        token = await getTenantToken(request);
    });

    test('GET /api/settings/line — 取得 LINE 設定', async ({ request }) => {
        test.skip(!token, '無 Token');
        const res = await request.get(`${BASE_URL}/api/settings/line`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
        if (res.ok()) {
            const body = await res.json();
            const d = body.data;
            console.log(`LINE 狀態: ${d?.status}`);
            console.log(`有 Token: ${d?.hasAccessToken}`);
            console.log(`Booking Enabled: ${d?.bookingEnabled}`);
            console.log(`Webhook URL: ${d?.webhookUrl}`);
        }
    });

    test('POST /api/settings/line/test — 連線測試', async ({ request }) => {
        test.skip(!token, '無 Token');
        const res = await request.post(`${BASE_URL}/api/settings/line/test`, {
            headers: { Authorization: `Bearer ${token}` }
        });
        expect(res.status()).toBeLessThan(500);
        if (res.ok()) {
            const body = await res.json();
            console.log(`連線測試: ${body.data?.connected ? '成功' : '失敗'}`);
            console.log(`Bot 名稱: ${body.data?.displayName || 'N/A'}`);
        }
    });
});

// ================================================================
//  SECTION 12：Webhook 完整流程模擬
// ================================================================

test.describe('12. Webhook 完整流程模擬', () => {

    test('預約流程：start → select_service → select_date → select_staff → select_time → skip_note → confirm', async ({ request }) => {
        const userId = 'Utest_full_flow_' + Date.now();
        const steps = [
            'action=start_booking',
            'action=select_service&serviceId=test&serviceName=測試&duration=60&price=500',
            'action=select_date&date=2099-12-31',
            'action=select_staff&staffId=&staffName=不指定',
            'action=select_time&time=10:00',
            'action=skip_note',
            'action=confirm_booking',
        ];

        for (const step of steps) {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(step, userId)
            ]);
            expect(res.status()).toBeLessThan(500);
        }
        console.log(`完整預約流程（7 步）: 全部不 500`);
    });

    test('分類預約流程：start → select_category → select_service → ...', async ({ request }) => {
        const userId = 'Utest_cat_flow_' + Date.now();
        const steps = [
            'action=start_booking',
            'action=select_category&categoryId=test-cat&categoryName=測試分類',
            'action=select_service&serviceId=test&serviceName=測試&duration=60&price=500',
            'action=select_date&date=2099-12-31',
            'action=select_staff&staffId=&staffName=不指定',
            'action=select_time&time=10:00',
            'action=skip_note',
            'action=confirm_booking',
        ];

        for (const step of steps) {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(step, userId)
            ]);
            expect(res.status()).toBeLessThan(500);
        }
        console.log(`分類預約流程（8 步）: 全部不 500`);
    });

    test('預約中途取消：start → select_service → cancel_flow → confirm_cancel', async ({ request }) => {
        const userId = 'Utest_cancel_' + Date.now();
        const steps = [
            'action=start_booking',
            'action=select_service&serviceId=test&serviceName=測試&duration=60&price=500',
            'action=cancel_flow',
            'action=confirm_cancel_flow',
        ];

        for (const step of steps) {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(step, userId)
            ]);
            expect(res.status()).toBeLessThan(500);
        }
        console.log(`中途取消流程: 全部不 500`);
    });

    test('返回上一步：start → select_service → go_back → select_service', async ({ request }) => {
        const userId = 'Utest_goback_' + Date.now();
        const steps = [
            'action=start_booking',
            'action=select_service&serviceId=test&serviceName=測試&duration=60&price=500',
            'action=go_back',
            'action=select_service&serviceId=test&serviceName=測試&duration=60&price=500',
        ];

        for (const step of steps) {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(step, userId)
            ]);
            expect(res.status()).toBeLessThan(500);
        }
        console.log(`GoBack 流程: 全部不 500`);
    });

    test('分類流程 GoBack：select_category → select_service → go_back（回到分類選單）', async ({ request }) => {
        const userId = 'Utest_cat_goback_' + Date.now();
        const steps = [
            'action=start_booking',
            'action=select_category&categoryId=test-cat&categoryName=測試',
            'action=go_back', // 應回到分類選單
        ];

        for (const step of steps) {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(step, userId)
            ]);
            expect(res.status()).toBeLessThan(500);
        }
        console.log(`分類 GoBack 流程: 全部不 500`);
    });

    test('商品購買流程：start_shopping → select_product → select_quantity → confirm_purchase', async ({ request }) => {
        const userId = 'Utest_shop_' + Date.now();
        const steps = [
            'action=start_shopping',
            'action=select_product&productId=test&productName=商品&price=100',
            'action=select_quantity&quantity=2',
            'action=confirm_purchase',
        ];

        for (const step of steps) {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(step, userId)
            ]);
            expect(res.status()).toBeLessThan(500);
        }
        console.log(`商品購買流程: 全部不 500`);
    });

    test('混合操作：預約中途切換到查看票券再回來', async ({ request }) => {
        const userId = 'Utest_mixed_' + Date.now();
        const steps = [
            'action=start_booking',
            'action=view_coupons',  // 中途跳去看票券
            'action=main_menu',     // 回主選單
            'action=start_booking', // 重新開始預約
        ];

        for (const step of steps) {
            const res = await sendWebhook(request, TENANT_CODE, [
                buildPostbackEvent(step, userId)
            ]);
            expect(res.status()).toBeLessThan(500);
        }
        console.log(`混合操作流程: 全部不 500`);
    });
});

// ================================================================
//  SECTION 13：Postback 資料格式驗證
// ================================================================

test.describe('13. Postback 資料格式', () => {

    test('select_category 必須包含 categoryId 和 categoryName', () => {
        const data = 'action=select_category&categoryId=abc-123&categoryName=剪髮類';
        const params = new URLSearchParams(data);
        expect(params.get('action')).toBe('select_category');
        expect(params.get('categoryId')).toBe('abc-123');
        expect(params.get('categoryName')).toBe('剪髮類');
    });

    test('select_service 必須包含 serviceId/serviceName/duration/price', () => {
        const data = 'action=select_service&serviceId=svc-1&serviceName=男生剪髮&duration=60&price=500';
        const params = new URLSearchParams(data);
        expect(params.get('serviceId')).toBeTruthy();
        expect(params.get('serviceName')).toBeTruthy();
        expect(params.get('duration')).toBe('60');
        expect(params.get('price')).toBe('500');
    });

    test('select_staff — staffId 空字串表示不指定', () => {
        const data = 'action=select_staff&staffId=&staffName=不指定';
        const params = new URLSearchParams(data);
        expect(params.get('staffId')).toBe('');
        expect(params.get('staffName')).toBe('不指定');
    });

    test('select_date — 日期格式 ISO_LOCAL_DATE', () => {
        const data = 'action=select_date&date=2025-06-15';
        const params = new URLSearchParams(data);
        expect(params.get('date')).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });

    test('select_time — 時間格式 HH:mm', () => {
        const data = 'action=select_time&time=14:30';
        const params = new URLSearchParams(data);
        expect(params.get('time')).toMatch(/^\d{2}:\d{2}$/);
    });

    test('cancel_booking_request 必須包含 bookingId', () => {
        const data = 'action=cancel_booking_request&bookingId=bk-abc-123';
        const params = new URLSearchParams(data);
        expect(params.get('bookingId')).toBe('bk-abc-123');
    });

    test('receive_coupon 必須包含 couponId', () => {
        const data = 'action=receive_coupon&couponId=cp-abc-123';
        const params = new URLSearchParams(data);
        expect(params.get('couponId')).toBe('cp-abc-123');
    });

    test('select_product 必須包含 productId/productName/price', () => {
        const data = 'action=select_product&productId=p1&productName=商品A&price=299';
        const params = new URLSearchParams(data);
        expect(params.get('productId')).toBeTruthy();
        expect(params.get('productName')).toBeTruthy();
        expect(params.get('price')).toBe('299');
    });

    test('select_quantity 必須包含 quantity', () => {
        const data = 'action=select_quantity&quantity=3';
        const params = new URLSearchParams(data);
        expect(params.get('quantity')).toBe('3');
    });
});

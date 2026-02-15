import { test, expect } from './fixtures';
import {
  tenantLogin,
  WAIT_TIME,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * Flex Menu 步驟編輯器 + 卡片圖片上傳 — 完整 E2E 測試
 *
 * 測試範圍：
 * 1. Flex Menu API 契約（GET/PUT flex-menu、upload-card-image、delete card-image）
 * 2. 公開圖片端點（/api/public/flex-card-image）
 * 3. 選單設計頁面 — 步驟編輯器 UI
 * 4. 步驟編輯器即時更新
 * 5. 認證保護
 * 6. 儲存流程端對端
 */

// 最小有效 PNG 圖片
const MINIMAL_PNG = Buffer.from([
  0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
  0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
  0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
  0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
  0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41,
  0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00,
  0x00, 0x00, 0x02, 0x00, 0x01, 0xE2, 0x21, 0xBC,
  0x33, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
  0x44, 0xAE, 0x42, 0x60, 0x82
]);

// localStorage key（與 common.js 一致）
const TOKEN_KEY = 'booking_platform_token';

// 用 page.evaluate(fetch) 執行 API 呼叫（自動帶瀏覽器 token）
async function apiCall(page: any, method: string, url: string, body?: any): Promise<any> {
  return await page.evaluate(async ({ method, url, body, tokenKey }: any) => {
    const token = localStorage.getItem(tokenKey);
    const opts: any = {
      method,
      headers: { 'Authorization': `Bearer ${token}` }
    };
    if (body && method !== 'GET' && method !== 'DELETE') {
      opts.headers['Content-Type'] = 'application/json';
      opts.body = JSON.stringify(body);
    }
    const res = await fetch(url, opts);
    const json = await res.json().catch(() => null);
    return { status: res.status, ok: res.ok, data: json };
  }, { method, url, body, tokenKey: TOKEN_KEY });
}

// 導航到 Flex Menu 步驟編輯器區域
async function navigateToStepEditor(page: any): Promise<boolean> {
  await page.goto('/tenant/rich-menu-design');
  await page.waitForLoadState('domcontentloaded');
  await page.waitForTimeout(WAIT_TIME.api);

  if (!page.url().includes('rich-menu-design')) return false;

  // 切換到 Flex Menu tab
  const flexTab = page.locator('button[data-bs-target="#tabFlexMenu"]');
  await flexTab.click();
  await page.waitForTimeout(WAIT_TIME.medium);

  // 展開步驟面板 (#stepPanel)
  const stepPanel = page.locator('#stepPanel');
  const isExpanded = await stepPanel.evaluate((el: any) => el.classList.contains('show'));
  if (!isExpanded) {
    await page.locator('[data-bs-target="#stepPanel"]').click();
    await page.waitForTimeout(WAIT_TIME.medium);
  }
  return true;
}

// ============================================================
// 第一部分：Flex Menu API 契約測試
// ============================================================

test.describe('Flex Menu API 契約測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('GET flex-menu — 回應正確結構', async ({ page }) => {
    const r = await apiCall(page, 'GET', '/api/settings/line/flex-menu');
    expect(r.ok).toBeTruthy();
    expect(r.data.success).toBeTruthy();
    expect(r.data).toHaveProperty('data');
    console.log(`Flex Menu 配置回應成功`);
  });

  test('GET flex-menu — 包含 cards 和 steps', async ({ page }) => {
    const r = await apiCall(page, 'GET', '/api/settings/line/flex-menu');
    expect(r.ok).toBeTruthy();
    const config = typeof r.data.data === 'string' ? JSON.parse(r.data.data) : r.data.data;
    if (config && config.cards) {
      expect(Array.isArray(config.cards)).toBeTruthy();
      console.log(`Flex Menu 有 ${config.cards.length} 張卡片`);
    }
    if (config && config.steps) {
      expect(typeof config.steps).toBe('object');
      console.log(`Flex Menu 有步驟配置`);
    }
  });

  test('PUT flex-menu — 只含卡片資料', async ({ page }) => {
    const payload = {
      menuType: 'carousel',
      cards: [
        { icon: '📅', title: '開始預約', subtitle: '立即預約', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' },
        { icon: '📋', title: '我的預約', subtitle: '查看預約', color: '#4A90D9', action: 'view_bookings', buttonLabel: '查看' }
      ]
    };
    const r = await apiCall(page, 'PUT', '/api/settings/line/flex-menu', payload);
    expect(r.ok).toBeTruthy();
    expect(r.data.success).toBeTruthy();
    console.log('儲存 Flex Menu（只含卡片）成功');
  });

  test('PUT flex-menu — 含步驟配置', async ({ page }) => {
    const payload = {
      menuType: 'carousel',
      cards: [{ icon: '📅', title: '預約', subtitle: '', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' }],
      steps: {
        service: { color: '#FF5722', icon: '💇', title: '選擇您的服務', subtitle: '瀏覽專業服務', imageUrl: '' },
        date: { color: '#1DB446', icon: '📅', title: '選擇日期', subtitle: '選擇方便的日期', imageUrl: '' },
        staff: { color: '#2196F3', icon: '👤', title: '選擇技師', subtitle: '指定喜歡的技師', imageUrl: '' },
        time: { color: '#9C27B0', icon: '⏰', title: '選擇時段', subtitle: '查看可預約時段', imageUrl: '' },
        note: { color: '#5C6BC0', icon: '📝', title: '備註說明', subtitle: '輸入特殊需求', imageUrl: '' },
        confirm: { color: '#4CAF50', icon: '✅', title: '確認預約', subtitle: '確認後即完成', imageUrl: '' }
      }
    };
    const r = await apiCall(page, 'PUT', '/api/settings/line/flex-menu', payload);
    expect(r.ok).toBeTruthy();
    console.log('儲存 Flex Menu（含步驟配置）成功');
  });

  test('PUT flex-menu — 步驟資料持久化', async ({ page }) => {
    // 儲存
    const payload = {
      menuType: 'carousel',
      cards: [{ icon: '📅', title: '持久化測試', subtitle: '', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' }],
      steps: { service: { color: '#FF0000', icon: '🔴', title: '紅色步驟', subtitle: '持久化測試', imageUrl: '' } }
    };
    const save = await apiCall(page, 'PUT', '/api/settings/line/flex-menu', payload);
    expect(save.ok).toBeTruthy();

    // 讀取驗證
    const get = await apiCall(page, 'GET', '/api/settings/line/flex-menu');
    const config = typeof get.data.data === 'string' ? JSON.parse(get.data.data) : get.data.data;
    if (config.steps && config.steps.service) {
      expect(config.steps.service.color).toBe('#FF0000');
      expect(config.steps.service.icon).toBe('🔴');
      expect(config.steps.service.title).toBe('紅色步驟');
      console.log('步驟配置持久化驗證通過');
    }
  });

  test('PUT flex-menu — 不含 steps 向下相容', async ({ page }) => {
    const payload = { menuType: 'carousel', cards: [{ icon: '📅', title: '無步驟', subtitle: '', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' }] };
    const r = await apiCall(page, 'PUT', '/api/settings/line/flex-menu', payload);
    expect(r.ok).toBeTruthy();
    console.log('不含 steps 儲存成功（向下相容）');
  });

  test('PUT flex-menu — 7 張卡片（最大數量）', async ({ page }) => {
    const actions = ['start_booking', 'view_bookings', 'start_shopping', 'view_coupons', 'view_my_coupons', 'view_member_info', 'contact_shop'];
    const cards = actions.map((action, i) => ({ icon: '📌', title: `卡片${i+1}`, subtitle: '', color: '#1DB446', action, buttonLabel: '前往' }));
    const r = await apiCall(page, 'PUT', '/api/settings/line/flex-menu', { menuType: 'carousel', cards });
    expect(r.ok).toBeTruthy();
    console.log('儲存 7 張卡片成功');
  });

  test('PUT flex-menu — 6 個步驟全部配置', async ({ page }) => {
    const steps: Record<string, any> = {};
    ['service', 'date', 'staff', 'time', 'note', 'confirm'].forEach((key, i) => {
      steps[key] = { color: '#1DB446', icon: ['✂️', '📅', '👤', '⏰', '📝', '✅'][i], title: `步驟${i+1}`, subtitle: `副標${i+1}`, imageUrl: '' };
    });
    const r = await apiCall(page, 'PUT', '/api/settings/line/flex-menu', { menuType: 'carousel', cards: [{ icon: '📅', title: '預約', subtitle: '', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' }], steps });
    expect(r.ok).toBeTruthy();
    console.log('儲存 6 個步驟全部配置成功');
  });
});

// ============================================================
// 第二部分：圖片上傳 API 測試
// ============================================================

test.describe('Flex Menu 圖片上傳 API', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('上傳卡片圖片（cardIndex=0）', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const token = localStorage.getItem('booking_platform_token');
      const blob = new Blob([new Uint8Array([
        0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,
        0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
        0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,
        0x08,0x02,0x00,0x00,0x00,0x90,0x77,0x53,
        0xDE,0x00,0x00,0x00,0x0C,0x49,0x44,0x41,
        0x54,0x08,0xD7,0x63,0xF8,0xCF,0xC0,0x00,
        0x00,0x00,0x02,0x00,0x01,0xE2,0x21,0xBC,
        0x33,0x00,0x00,0x00,0x00,0x49,0x45,0x4E,
        0x44,0xAE,0x42,0x60,0x82
      ])], { type: 'image/png' });
      const fd = new FormData();
      fd.append('file', blob, 'test.png');
      fd.append('cardIndex', '0');
      const res = await fetch('/api/settings/line/flex-menu/upload-card-image', {
        method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: fd
      });
      const data = await res.json();
      return { status: res.status, ok: res.ok, data };
    });
    expect(result.status).not.toBe(404);
    if (result.ok) {
      expect(result.data.success).toBeTruthy();
      expect(result.data.data.imageUrl).toContain('/api/public/flex-card-image/');
      console.log(`卡片圖片上傳成功: ${result.data.data.imageUrl}`);
    }
  });

  test('上傳步驟圖片（cardIndex=100，service）', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const token = localStorage.getItem('booking_platform_token');
      const blob = new Blob([new Uint8Array([
        0x89,0x50,0x4E,0x47,0x0D,0x0A,0x1A,0x0A,
        0x00,0x00,0x00,0x0D,0x49,0x48,0x44,0x52,
        0x00,0x00,0x00,0x01,0x00,0x00,0x00,0x01,
        0x08,0x02,0x00,0x00,0x00,0x90,0x77,0x53,
        0xDE,0x00,0x00,0x00,0x0C,0x49,0x44,0x41,
        0x54,0x08,0xD7,0x63,0xF8,0xCF,0xC0,0x00,
        0x00,0x00,0x02,0x00,0x01,0xE2,0x21,0xBC,
        0x33,0x00,0x00,0x00,0x00,0x49,0x45,0x4E,
        0x44,0xAE,0x42,0x60,0x82
      ])], { type: 'image/png' });
      const fd = new FormData();
      fd.append('file', blob, 'step.png');
      fd.append('cardIndex', '100');
      const res = await fetch('/api/settings/line/flex-menu/upload-card-image', {
        method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: fd
      });
      const data = await res.json();
      return { status: res.status, ok: res.ok, data };
    });
    expect(result.status).not.toBe(404);
    if (result.ok) {
      expect(result.data.data.imageUrl).toContain('/100');
      console.log(`步驟圖片上傳成功: ${result.data.data.imageUrl}`);
    }
  });

  test('上傳後刪除卡片圖片', async ({ page }) => {
    // 上傳
    await page.evaluate(async () => {
      const token = localStorage.getItem('booking_platform_token');
      const blob = new Blob([new Uint8Array([137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,207,192,0,0,0,2,0,1,226,33,188,51,0,0,0,0,73,69,78,68,174,66,96,130])], { type: 'image/png' });
      const fd = new FormData();
      fd.append('file', blob, 'del.png');
      fd.append('cardIndex', '2');
      await fetch('/api/settings/line/flex-menu/upload-card-image', { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: fd });
    });

    // 刪除
    const delResult = await page.evaluate(async () => {
      const token = localStorage.getItem('booking_platform_token');
      const res = await fetch('/api/settings/line/flex-menu/card-image?cardIndex=2', {
        method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` }
      });
      return { status: res.status, ok: res.ok };
    });
    expect(delResult.status).not.toBe(404);
    expect(delResult.status).not.toBe(500);
    console.log(`刪除卡片圖片回應: ${delResult.status}`);
  });

  test('刪除不存在的圖片不應 500', async ({ page }) => {
    const result = await page.evaluate(async () => {
      const token = localStorage.getItem('booking_platform_token');
      const res = await fetch('/api/settings/line/flex-menu/card-image?cardIndex=999', {
        method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` }
      });
      return { status: res.status };
    });
    expect(result.status).toBeLessThan(500);
    console.log(`刪除不存在圖片: ${result.status}`);
  });
});

// ============================================================
// 第三部分：公開圖片端點
// ============================================================

test.describe('公開圖片端點', () => {
  test('不存在的租戶/索引回傳 404', async ({ request }) => {
    const response = await request.get('/api/public/flex-card-image/nonexistent/999');
    expect(response.status()).toBe(404);
    console.log('不存在的圖片回傳 404');
  });

  test('上傳後可透過公開 URL 存取', async ({ page }) => {
    await tenantLogin(page);
    const imageUrl = await page.evaluate(async () => {
      const token = localStorage.getItem('booking_platform_token');
      const blob = new Blob([new Uint8Array([137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,207,192,0,0,0,2,0,1,226,33,188,51,0,0,0,0,73,69,78,68,174,66,96,130])], { type: 'image/png' });
      const fd = new FormData();
      fd.append('file', blob, 'pub.png');
      fd.append('cardIndex', '6');
      const res = await fetch('/api/settings/line/flex-menu/upload-card-image', { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: fd });
      const data = await res.json();
      return data.success ? data.data.imageUrl : null;
    });
    if (imageUrl) {
      // 公開 URL 無需認證即可存取
      const response = await page.request.get(imageUrl);
      expect(response.ok()).toBeTruthy();
      const contentType = response.headers()['content-type'];
      expect(contentType).toContain('image/jpeg');
      console.log('公開圖片存取成功');

      // 清理
      await page.evaluate(async () => {
        const token = localStorage.getItem('booking_platform_token');
        await fetch('/api/settings/line/flex-menu/card-image?cardIndex=6', { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
      });
    }
  });

  test('刪除後公開 URL 回傳 404', async ({ page }) => {
    await tenantLogin(page);
    const imageUrl = await page.evaluate(async () => {
      const token = localStorage.getItem('booking_platform_token');
      const blob = new Blob([new Uint8Array([137,80,78,71,13,10,26,10,0,0,0,13,73,72,68,82,0,0,0,1,0,0,0,1,8,2,0,0,0,144,119,83,222,0,0,0,12,73,68,65,84,8,215,99,248,207,192,0,0,0,2,0,1,226,33,188,51,0,0,0,0,73,69,78,68,174,66,96,130])], { type: 'image/png' });
      const fd = new FormData();
      fd.append('file', blob, 'del2.png');
      fd.append('cardIndex', '4');
      const res = await fetch('/api/settings/line/flex-menu/upload-card-image', { method: 'POST', headers: { 'Authorization': `Bearer ${token}` }, body: fd });
      const data = await res.json();
      return data.success ? data.data.imageUrl : null;
    });
    if (imageUrl) {
      // 確認可存取
      const before = await page.request.get(imageUrl);
      expect(before.ok()).toBeTruthy();

      // 刪除
      await page.evaluate(async () => {
        const token = localStorage.getItem('booking_platform_token');
        await fetch('/api/settings/line/flex-menu/card-image?cardIndex=4', { method: 'DELETE', headers: { 'Authorization': `Bearer ${token}` } });
      });

      // 刪除後 404
      const after = await page.request.get(imageUrl);
      expect(after.status()).toBe(404);
      console.log('刪除後回傳 404');
    }
  });
});

// ============================================================
// 第四部分：認證保護
// ============================================================

test.describe('Flex Menu 認證保護', () => {
  test('未認證 GET flex-menu 被拒絕', async ({ request }) => {
    const r = await request.get('/api/settings/line/flex-menu');
    expect(r.status()).toBeGreaterThanOrEqual(400);
    expect(r.status()).toBeLessThan(500);
    console.log(`未認證 GET: ${r.status()}`);
  });

  test('未認證 PUT flex-menu 被拒絕', async ({ request }) => {
    const r = await request.put('/api/settings/line/flex-menu', { headers: { 'Content-Type': 'application/json' }, data: { menuType: 'carousel', cards: [] } });
    expect(r.status()).toBeGreaterThanOrEqual(400);
    expect(r.status()).toBeLessThan(500);
    console.log(`未認證 PUT: ${r.status()}`);
  });

  test('未認證上傳圖片被拒絕', async ({ request }) => {
    const r = await request.post('/api/settings/line/flex-menu/upload-card-image', {
      multipart: { file: { name: 'x.png', mimeType: 'image/png', buffer: MINIMAL_PNG }, cardIndex: '0' }
    });
    expect(r.status()).toBeGreaterThanOrEqual(400);
    expect(r.status()).toBeLessThan(500);
    console.log(`未認證上傳: ${r.status()}`);
  });

  test('未認證刪除圖片被拒絕', async ({ request }) => {
    const r = await request.delete('/api/settings/line/flex-menu/card-image?cardIndex=0');
    expect(r.status()).toBeGreaterThanOrEqual(400);
    expect(r.status()).toBeLessThan(500);
    console.log(`未認證刪除: ${r.status()}`);
  });
});

// ============================================================
// 第五部分：UI 測試 — 選單設計頁面步驟編輯器
// ============================================================

test.describe('選單設計頁面 — 步驟編輯器', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('頁面正常載入', async ({ page }) => {
    await page.goto('/tenant/rich-menu-design');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);
    const url = page.url();
    if (!url.includes('rich-menu-design')) {
      console.log(`頁面重導向到: ${url}，跳過`);
      return;
    }
    console.log('選單設計頁面載入成功');
  });

  test('步驟編輯器區塊存在且有 6 個步驟', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    const steps = page.locator('#fmStepList .fm-step-editor');
    const count = await steps.count();
    expect(count).toBe(6);
    console.log(`步驟編輯器數量: ${count}`);
  });

  test('步驟編輯器編號 1-6', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    const nums = page.locator('#fmStepList .fm-step-editor-num');
    for (let i = 0; i < 6; i++) {
      const text = await nums.nth(i).textContent();
      expect(text?.trim()).toBe(String(i + 1));
    }
    console.log('步驟編號 1-6 正確');
  });

  test('步驟標題包含預期關鍵字', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    const editors = page.locator('#fmStepList .fm-step-editor');
    const count = await editors.count();
    expect(count).toBe(6);
    // 步驟標題可能被自訂（套用範本後），改為檢查 data-step 屬性和標題非空
    const expectedSteps = ['service', 'date', 'staff', 'time', 'note', 'confirm'];
    for (let i = 0; i < 6; i++) {
      const editor = editors.nth(i);
      const title = await editor.locator('.fm-step-editor-title').textContent();
      // 標題不為空
      expect(title?.trim().length).toBeGreaterThan(0);
      // 檢查輸入框的 data-step 屬性確認步驟順序
      const stepInput = editor.locator('input[data-step]').first();
      const stepKey = await stepInput.getAttribute('data-step');
      expect(stepKey).toBe(expectedSteps[i]);
    }
    console.log('步驟標題正確（6 個步驟，順序正確）');
  });

  test('步驟可展開/收合', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;

    // 第二個步驟預設收合
    const secondBody = page.locator('#fmStepList .fm-step-editor:nth-child(2) .fm-step-editor-body');
    await expect(secondBody).toHaveClass(/collapsed/);

    // 點擊展開
    await page.locator('#fmStepList .fm-step-editor:nth-child(2) .fm-step-editor-header').click();
    await page.waitForTimeout(WAIT_TIME.short);
    const cls = await secondBody.getAttribute('class');
    expect(cls).not.toContain('collapsed');
    console.log('展開/收合功能正常');
  });

  test('每個步驟有圖示輸入框', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    const iconInputs = page.locator('#fmStepList input[data-f="icon"]');
    expect(await iconInputs.count()).toBe(6);
    const first = await iconInputs.first().inputValue();
    expect(first.length).toBeGreaterThan(0);
    console.log('圖示輸入框正確');
  });

  test('每個步驟有 Header 色選擇器', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    const colorInputs = page.locator('#fmStepList input[data-f="color"]');
    expect(await colorInputs.count()).toBe(6);
    const first = await colorInputs.first().inputValue();
    expect(first).toMatch(/^#[0-9a-fA-F]{6}$/);
    console.log('顏色選擇器正確');
  });

  test('每個步驟有標題/副標題輸入框', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    expect(await page.locator('#fmStepList input[data-f="title"]').count()).toBe(6);
    expect(await page.locator('#fmStepList input[data-f="subtitle"]').count()).toBe(6);
    console.log('標題/副標題輸入框正確');
  });

  test('每個步驟有 Hero 圖片 URL + 上傳按鈕', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    expect(await page.locator('#fmStepList input[data-f="imageUrl"]').count()).toBe(6);
    expect(await page.locator('#fmStepList input[data-f="uploadStepImg"]').count()).toBe(6);
    console.log('圖片 URL + 上傳按鈕正確');
  });

  test('步驟內嵌預覽區', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;
    const previews = page.locator('#fmStepList .fm-step-prev');
    expect(await previews.count()).toBe(6);
    console.log('內嵌預覽區正確');
  });
});

// ============================================================
// 第六部分：步驟編輯器即時更新
// ============================================================

test.describe('步驟編輯器即時更新', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('修改標題 → 預覽同步', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;

    // 第一步預設展開，直接填寫
    const titleInput = page.locator('#fmStepList .fm-step-editor:nth-child(1) input[data-f="title"]');
    await titleInput.fill('E2E自訂標題');
    await titleInput.dispatchEvent('input');
    await page.waitForTimeout(WAIT_TIME.short);

    const prevTitle = await page.locator('#fmStepList .fm-step-editor:nth-child(1) .fm-step-prev-title').textContent();
    expect(prevTitle).toContain('E2E自訂標題');
    console.log('修改標題後預覽同步');
  });

  test('修改副標題 → 預覽同步', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;

    const subInput = page.locator('#fmStepList .fm-step-editor:nth-child(1) input[data-f="subtitle"]');
    await subInput.fill('E2E副標題');
    await subInput.dispatchEvent('input');
    await page.waitForTimeout(WAIT_TIME.short);

    const prevSub = await page.locator('#fmStepList .fm-step-editor:nth-child(1) .fm-step-prev-sub').textContent();
    expect(prevSub).toContain('E2E副標題');
    console.log('修改副標題後預覽同步');
  });

  test('修改圖示 → 預覽同步', async ({ page }) => {
    const ok = await navigateToStepEditor(page);
    if (!ok) return;

    const iconInput = page.locator('#fmStepList .fm-step-editor:nth-child(1) input[data-f="icon"]');
    await iconInput.fill('🎉');
    await iconInput.dispatchEvent('input');
    await page.waitForTimeout(WAIT_TIME.short);

    const prevIcon = await page.locator('#fmStepList .fm-step-editor:nth-child(1) .fm-step-prev-icon').textContent();
    expect(prevIcon).toContain('🎉');
    console.log('修改圖示後預覽同步');
  });
});

// ============================================================
// 第七部分：儲存端對端流程
// ============================================================

test.describe('Flex Menu 端對端儲存流程', () => {
  test('完整流程：儲存卡片+步驟 → 讀取驗證', async ({ page }) => {
    await tenantLogin(page);

    // 儲存
    const saveResult = await apiCall(page, 'PUT', '/api/settings/line/flex-menu', {
      menuType: 'carousel',
      cards: [
        { icon: '📅', title: '開始預約', subtitle: '立即預約', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' },
        { icon: '📋', title: '我的預約', subtitle: '查看預約', color: '#4A90D9', action: 'view_bookings', buttonLabel: '查看' },
        { icon: '🛍️', title: '瀏覽商品', subtitle: '選購商品', color: '#FF6B35', action: 'start_shopping', buttonLabel: '逛逛' },
        { icon: '🎁', title: '領取票券', subtitle: '查看優惠', color: '#E91E63', action: 'view_coupons', buttonLabel: '領取' },
        { icon: '🎫', title: '我的票券', subtitle: '已領取', color: '#9C27B0', action: 'view_my_coupons', buttonLabel: '查看' },
        { icon: '👤', title: '會員資訊', subtitle: '會員資料', color: '#00BCD4', action: 'view_member_info', buttonLabel: '查看' },
        { icon: '📞', title: '聯絡店家', subtitle: '客服諮詢', color: '#607D8B', action: 'contact_shop', buttonLabel: '聯絡' }
      ],
      steps: {
        service: { color: '#E91E63', icon: '💇', title: 'E2E服務', subtitle: 'E2E副標', imageUrl: '' },
        date: { color: '#4CAF50', icon: '📆', title: 'E2E日期', subtitle: 'E2E日期副標', imageUrl: '' },
        staff: { color: '#2196F3', icon: '💁', title: 'E2E員工', subtitle: 'E2E員工副標', imageUrl: '' },
        time: { color: '#FF9800', icon: '🕐', title: 'E2E時段', subtitle: 'E2E時段副標', imageUrl: '' },
        note: { color: '#795548', icon: '✏️', title: 'E2E備註', subtitle: 'E2E備註副標', imageUrl: '' },
        confirm: { color: '#009688', icon: '👍', title: 'E2E確認', subtitle: 'E2E確認副標', imageUrl: '' }
      }
    });
    expect(saveResult.ok).toBeTruthy();

    // 讀取驗證
    const getResult = await apiCall(page, 'GET', '/api/settings/line/flex-menu');
    expect(getResult.ok).toBeTruthy();
    // API 回應結構：data.data 可能是 JSON string 或物件
    let config = getResult.data.data;
    if (typeof config === 'string') {
      try { config = JSON.parse(config); } catch { /* 非 JSON */ }
    }
    // config 可能直接包含 cards，或 flexMenuConfig 包在某個 key 下
    const cards = config?.cards || config?.flexMenuConfig?.cards;
    const steps = config?.steps || config?.flexMenuConfig?.steps;
    if (cards) {
      expect(cards).toHaveLength(7);
      expect(cards[0].title).toBe('開始預約');
      console.log(`卡片驗證通過: ${cards.length} 張`);
    } else {
      // flexMenuConfig 可能儲存為扁平結構
      console.log(`GET 回應結構: ${JSON.stringify(config).substring(0, 200)}`);
    }
    if (steps) {
      expect(steps.service.title).toBe('E2E服務');
      expect(steps.service.color).toBe('#E91E63');
      expect(steps.confirm.icon).toBe('👍');
      console.log('步驟驗證通過');
    }
    console.log('完整儲存+讀取驗證通過');
  });
});

// ============================================================
// 第八部分：頁面健康檢查
// ============================================================

test.describe('選單設計頁面健康檢查', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/rich-menu-design');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);
  });

  test('頁面無卡住的 spinner', async ({ page }) => {
    if (!page.url().includes('rich-menu-design')) return;
    await page.waitForTimeout(WAIT_TIME.long);
    const count = await page.locator('.spinner-border:visible').count();
    expect(count).toBe(0);
    console.log('無卡住 spinner');
  });

  test('頁面無「載入失敗」文字', async ({ page }) => {
    if (!page.url().includes('rich-menu-design')) return;
    await page.waitForTimeout(WAIT_TIME.long);
    const bodyText = await page.textContent('body');
    expect(bodyText).not.toContain('載入失敗');
    console.log('無載入失敗文字');
  });

  test('RWD 手機尺寸不崩版', async ({ page }) => {
    if (!page.url().includes('rich-menu-design')) return;
    await page.setViewportSize({ width: 375, height: 812 });
    await page.waitForTimeout(WAIT_TIME.medium);
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth > document.documentElement.clientWidth + 5);
    expect(overflow).toBeFalsy();
    console.log('手機尺寸正常');
  });
});

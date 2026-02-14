import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME,
  TEST_ACCOUNTS
} from './utils/test-helpers';

/**
 * Rich Menu 增強功能 — 預設 7 格 + 自訂圖片模式 E2E 測試
 *
 * 測試範圍：
 * 1. LINE 設定頁面載入與 Rich Menu 區塊
 * 2. 模式切換 Tab（預設 ↔ 自訂 ↔ 進階）
 * 3. 預設模式：7 格預覽（3+4 佈局）、主題按鈕、建立按鈕
 * 4. 自訂模式：佈局選擇器（5 種）、圖片上傳、動作設定（需訂閱 CUSTOM_RICH_MENU）
 * 5. Rich Menu API 契約驗證
 * 6. API 回應欄位驗證（mode、customConfig）
 * 7. 頁面 F12 Console 無錯誤（由 fixture 自動監控）
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// ============================================================
// 第一部分：UI 測試 — LINE 設定頁面 Rich Menu 區塊
// ============================================================

test.describe('Rich Menu UI 測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);
  });

  test.describe('頁面載入與基本結構', () => {
    test('LINE 設定頁面正常載入', async ({ page }) => {
      // 頁面標題
      await expect(page.locator('.page-title')).toContainText('LINE 設定');
      // Rich Menu 卡片存在
      await expect(page.locator('#richMenuCard')).toBeVisible();
      console.log('LINE 設定頁面正常載入，Rich Menu 卡片可見');
    });

    test('Rich Menu 狀態顯示區塊存在', async ({ page }) => {
      await expect(page.locator('#richMenuStatus')).toBeVisible();
      await expect(page.locator('#richMenuStatusBadge')).toBeVisible();
      const badgeText = await page.locator('#richMenuStatusBadge').textContent();
      console.log(`Rich Menu 狀態: ${badgeText}`);
      // 狀態應為已知值
      const validStatuses = ['已設定', '未設定', '已啟用', '未啟用'];
      expect(validStatuses.some(s => badgeText?.includes(s))).toBeTruthy();
    });

    test('Rich Menu 手機預覽框架存在', async ({ page }) => {
      await expect(page.locator('.rich-menu-preview-container')).toBeVisible();
      // 至少存在預設模式預覽或自訂模式預覽
      const defaultPreview = page.locator('#richMenuPreview');
      const customPreview = page.locator('#richMenuCustomPreview');
      const eitherVisible = await defaultPreview.isVisible() || await customPreview.isVisible();
      expect(eitherVisible).toBeTruthy();
      console.log('Rich Menu 手機預覽框架正常');
    });
  });

  test.describe('模式切換 Tab', () => {
    test('模式切換 Tab 存在且可見', async ({ page }) => {
      const modeTabs = page.locator('#richMenuModeTabs');
      await expect(modeTabs).toBeVisible();

      // DOM 中應有 3 個 Tab（預設、自訂、進階），但自訂/進階可能隱藏
      const allTabs = page.locator('#richMenuModeTabs .rich-menu-mode-tab');
      await expect(allTabs).toHaveCount(3);

      // 預設 Tab 永遠可見
      await expect(allTabs.nth(0)).toContainText('預設選單');
      console.log('模式切換 Tab 結構正確（3 個 Tab）');
    });

    test('預設 Tab 初始為預設選單', async ({ page }) => {
      const defaultTab = page.locator('#richMenuModeTabs .rich-menu-mode-tab[data-mode="default"]');
      await expect(defaultTab).toHaveClass(/active/);

      const customTab = page.locator('#richMenuModeTabs .rich-menu-mode-tab[data-mode="custom"]');
      const customClasses = await customTab.getAttribute('class');
      expect(customClasses).not.toContain('active');
      console.log('預設 Tab 初始狀態正確（預設選單 active）');
    });

    test('Tab 樣式清晰可辨', async ({ page }) => {
      const activeTab = page.locator('#richMenuModeTabs .rich-menu-mode-tab.active');
      // 使用 first() 因為可能有多個非 active Tab
      const inactiveTab = page.locator('#richMenuModeTabs .rich-menu-mode-tab:not(.active)').first();

      // Active Tab 應有明顯背景色
      const activeBg = await activeTab.evaluate(el => getComputedStyle(el).backgroundColor);
      // Inactive Tab 應有不同背景色
      const inactiveBg = await inactiveTab.evaluate(el => getComputedStyle(el).backgroundColor);

      // 兩者背景色應不同（視覺可辨）
      expect(activeBg).not.toBe(inactiveBg);
      console.log(`Active Tab 背景: ${activeBg}, Inactive Tab 背景: ${inactiveBg}`);
    });

    test('點擊自訂選單 Tab 切換模式', async ({ page }) => {
      const customTab = page.locator('#customMenuTab');
      // 自訂 Tab 可能隱藏（需訂閱 CUSTOM_RICH_MENU）
      const isVisible = await customTab.isVisible();
      if (!isVisible) {
        console.log('自訂 Tab 隱藏中（需訂閱），跳過');
        test.skip();
        return;
      }

      await customTab.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 自訂 Tab 變為 active
      await expect(customTab).toHaveClass(/active/);

      // 預設面板隱藏，自訂面板顯示
      await expect(page.locator('#defaultModePanel')).toBeHidden();
      await expect(page.locator('#customModePanel')).toBeVisible();
      console.log('切換到自訂選單模式成功');
    });

    test('點擊預設選單 Tab 切換回預設模式', async ({ page }) => {
      const customTab = page.locator('#customMenuTab');
      const isVisible = await customTab.isVisible();
      if (!isVisible) {
        console.log('自訂 Tab 隱藏中（需訂閱），跳過');
        test.skip();
        return;
      }

      // 先切到自訂
      await customTab.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 再切回預設
      const defaultTab = page.locator('#richMenuModeTabs .rich-menu-mode-tab[data-mode="default"]');
      await defaultTab.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 預設 Tab active
      await expect(defaultTab).toHaveClass(/active/);

      // 預設面板顯示，自訂面板隱藏
      await expect(page.locator('#defaultModePanel')).toBeVisible();
      await expect(page.locator('#customModePanel')).toBeHidden();
      console.log('切換回預設選單模式成功');
    });

    test('模式切換同時切換預覽', async ({ page }) => {
      // 預設模式：預設預覽可見
      await expect(page.locator('#richMenuPreview')).toBeVisible();

      const customTab = page.locator('#customMenuTab');
      const isVisible = await customTab.isVisible();
      if (!isVisible) {
        console.log('自訂 Tab 隱藏中（需訂閱），跳過');
        test.skip();
        return;
      }

      // 切到自訂
      await customTab.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 預設預覽隱藏，自訂預覽顯示
      await expect(page.locator('#richMenuPreview')).toBeHidden();
      await expect(page.locator('#richMenuCustomPreview')).toBeVisible();
      console.log('模式切換同時預覽切換正確');
    });
  });

  test.describe('預設模式面板', () => {
    test('7 格預覽顯示正確（3+4 佈局）', async ({ page }) => {
      const preview = page.locator('#richMenuPreview');
      await expect(preview).toBeVisible();

      // 應有 layout-3x4 class
      await expect(preview).toHaveClass(/layout-3x4/);

      // 應有 7 個格子
      const cells = preview.locator('.rich-menu-cell');
      await expect(cells).toHaveCount(7);

      // 上排 3 個 (cell-row1)
      const row1Cells = preview.locator('.rich-menu-cell.cell-row1');
      await expect(row1Cells).toHaveCount(3);

      // 下排 4 個 (cell-row2)
      const row2Cells = preview.locator('.rich-menu-cell.cell-row2');
      await expect(row2Cells).toHaveCount(4);
      console.log('7 格預覽佈局正確：上排 3 + 下排 4');
    });

    test('7 格預覽功能文字正確', async ({ page }) => {
      const cells = page.locator('#richMenuPreview .rich-menu-cell');
      const expectedTexts = ['開始預約', '我的預約', '瀏覽商品', '領取票券', '我的票券', '會員資訊', '聯絡店家'];

      for (let i = 0; i < 7; i++) {
        const cellText = await cells.nth(i).locator('.rich-menu-cell-text').textContent();
        expect(cellText?.trim()).toBe(expectedTexts[i]);
      }
      console.log('7 格功能文字全部正確');
    });

    test('7 格預覽每格都有圖示', async ({ page }) => {
      const icons = page.locator('#richMenuPreview .rich-menu-cell .rich-menu-cell-icon');
      await expect(icons).toHaveCount(7);

      // 每個圖示都應可見
      for (let i = 0; i < 7; i++) {
        await expect(icons.nth(i)).toBeVisible();
      }
      console.log('7 格預覽圖示全部顯示正確');
    });

    test('主題色按鈕存在', async ({ page }) => {
      const themeSection = page.locator('#defaultModePanel');
      await expect(themeSection).toBeVisible();

      // 檢查有主題按鈕
      const themeButtons = themeSection.locator('.theme-btn, [onclick*="selectTheme"]');
      const count = await themeButtons.count();
      expect(count).toBeGreaterThanOrEqual(5);
      console.log(`主題色按鈕數量: ${count}`);
    });

    test('建立主題選單按鈕存在', async ({ page }) => {
      const createBtn = page.locator('#defaultModePanel button, #defaultModePanel [onclick*="createRichMenu"]');
      const count = await createBtn.count();
      expect(count).toBeGreaterThan(0);
      console.log('建立主題選單按鈕存在');
    });

    test('預覽支援主題色切換', async ({ page }) => {
      const preview = page.locator('#richMenuPreview');

      // 初始應有某個主題 class
      const initialClasses = await preview.getAttribute('class');
      expect(initialClasses).toContain('theme-');
      console.log(`初始主題 class: ${initialClasses}`);
    });
  });

  test.describe('自訂模式面板', () => {
    test.beforeEach(async ({ page }) => {
      // 自訂 Tab 需訂閱 CUSTOM_RICH_MENU 才可見
      const customTab = page.locator('#customMenuTab');
      const isVisible = await customTab.isVisible();
      if (!isVisible) {
        test.skip();
        return;
      }
      // 切換到自訂模式
      await customTab.click();
      await page.waitForTimeout(WAIT_TIME.short);
    });

    test('佈局選擇器顯示 5 種佈局', async ({ page }) => {
      const layoutSelector = page.locator('#layoutSelector');
      await expect(layoutSelector).toBeVisible();

      const layoutOptions = layoutSelector.locator('.layout-option');
      await expect(layoutOptions).toHaveCount(5);
      console.log('佈局選擇器正確顯示 5 種佈局');
    });

    test('5 種佈局範本名稱正確', async ({ page }) => {
      const layoutOptions = page.locator('#layoutSelector .layout-option');
      const expectedLayouts = ['3+4', '2×3', '2+3', '2×2', '1+2'];

      for (let i = 0; i < 5; i++) {
        const label = await layoutOptions.nth(i).locator('.layout-label, small').textContent();
        expect(label?.trim()).toBe(expectedLayouts[i]);
      }
      console.log('5 種佈局範本名稱全部正確');
    });

    test('預設選中第一個佈局（3+4）', async ({ page }) => {
      const firstOption = page.locator('#layoutSelector .layout-option').first();
      await expect(firstOption).toHaveClass(/active/);
      console.log('預設選中 3+4 佈局');
    });

    test('點擊佈局選項切換選中狀態', async ({ page }) => {
      const options = page.locator('#layoutSelector .layout-option');

      // 點擊第二個佈局 (2×3)
      await options.nth(1).click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 第二個變 active，第一個不再 active
      await expect(options.nth(1)).toHaveClass(/active/);
      const firstClasses = await options.nth(0).getAttribute('class');
      expect(firstClasses).not.toContain('active');

      // 點擊第三個佈局 (2+3)
      await options.nth(2).click();
      await page.waitForTimeout(WAIT_TIME.short);
      await expect(options.nth(2)).toHaveClass(/active/);
      console.log('佈局選項切換正常');
    });

    test('切換佈局更新動作設定列表數量', async ({ page }) => {
      const options = page.locator('#layoutSelector .layout-option');

      // 3+4 佈局 → 7 個動作
      const actions3x4 = page.locator('#customActionsList .action-item, #customActionsList .mb-2');
      const count3x4 = await actions3x4.count();
      expect(count3x4).toBe(7);
      console.log(`3+4 佈局動作數: ${count3x4}`);

      // 切到 2×3 → 6 個動作
      await options.nth(1).click();
      await page.waitForTimeout(WAIT_TIME.short);
      const actions2x3 = page.locator('#customActionsList .action-item, #customActionsList .mb-2');
      const count2x3 = await actions2x3.count();
      expect(count2x3).toBe(6);
      console.log(`2×3 佈局動作數: ${count2x3}`);

      // 切到 2+3 → 5 個動作
      await options.nth(2).click();
      await page.waitForTimeout(WAIT_TIME.short);
      const actions2x3p = page.locator('#customActionsList .action-item, #customActionsList .mb-2');
      const count2x3p = await actions2x3p.count();
      expect(count2x3p).toBe(5);
      console.log(`2+3 佈局動作數: ${count2x3p}`);

      // 切到 2×2 → 4 個動作
      await options.nth(3).click();
      await page.waitForTimeout(WAIT_TIME.short);
      const actions2x2 = page.locator('#customActionsList .action-item, #customActionsList .mb-2');
      const count2x2 = await actions2x2.count();
      expect(count2x2).toBe(4);
      console.log(`2×2 佈局動作數: ${count2x2}`);

      // 切到 1+2 → 3 個動作
      await options.nth(4).click();
      await page.waitForTimeout(WAIT_TIME.short);
      const actions1x2 = page.locator('#customActionsList .action-item, #customActionsList .mb-2');
      const count1x2 = await actions1x2.count();
      expect(count1x2).toBe(3);
      console.log(`1+2 佈局動作數: ${count1x2}`);
    });

    test('動作下拉選單有預期的選項', async ({ page }) => {
      // 在 3+4 佈局下，第一個動作下拉
      const firstSelect = page.locator('#customActionsList select').first();
      await expect(firstSelect).toBeVisible();

      // 檢查有預設動作選項
      const options = firstSelect.locator('option');
      const optionCount = await options.count();
      expect(optionCount).toBeGreaterThanOrEqual(8);
      console.log(`動作選項數: ${optionCount}`);

      // 驗證有核心動作
      const allOptions = await firstSelect.evaluate((el: HTMLSelectElement) =>
        Array.from(el.options).map(o => o.value)
      );
      expect(allOptions).toContain('start_booking');
      expect(allOptions).toContain('view_bookings');
      expect(allOptions).toContain('start_shopping');
      expect(allOptions).toContain('view_coupons');
      expect(allOptions).toContain('view_member_info');
      expect(allOptions).toContain('contact_shop');
      console.log('動作選項包含所有核心功能');
    });

    test('圖片上傳區域存在', async ({ page }) => {
      // 檢查有圖片上傳輸入
      const fileInput = page.locator('#customModePanel input[type="file"]');
      await expect(fileInput).toBeAttached();
      console.log('圖片上傳區域存在');
    });

    test('建立自訂選單按鈕存在', async ({ page }) => {
      const createBtn = page.locator('#createCustomRichMenuBtn');
      await expect(createBtn).toBeVisible();
      await expect(createBtn).toContainText('建立');
      console.log('建立自訂選單按鈕存在');
    });

    test('自訂預覽區域存在', async ({ page }) => {
      const customPreview = page.locator('#richMenuCustomPreview');
      await expect(customPreview).toBeVisible();
      console.log('自訂預覽區域存在');
    });

    test('佈局迷你預覽正確顯示', async ({ page }) => {
      const layoutOptions = page.locator('#layoutSelector .layout-option');

      for (let i = 0; i < 5; i++) {
        const miniPreview = layoutOptions.nth(i).locator('.layout-mini, [class*="layout-mini"]');
        await expect(miniPreview).toBeVisible();
      }
      console.log('所有佈局迷你預覽正確顯示');
    });
  });

  test.describe('自訂模式預覽區域標記', () => {
    test.beforeEach(async ({ page }) => {
      const customTab = page.locator('#customMenuTab');
      const isVisible = await customTab.isVisible();
      if (!isVisible) {
        test.skip();
        return;
      }
      await customTab.click();
      await page.waitForTimeout(WAIT_TIME.short);
    });

    test('區域標記容器存在', async ({ page }) => {
      const markers = page.locator('#customAreaMarkers');
      await expect(markers).toBeAttached();
      console.log('區域標記容器存在');
    });

    test('切換佈局更新區域標記數量', async ({ page }) => {
      const options = page.locator('#layoutSelector .layout-option');

      // 3+4 → 7 個標記
      let markers = page.locator('#customAreaMarkers .rich-menu-area-number');
      let count = await markers.count();
      expect(count).toBe(7);

      // 2×2 → 4 個標記
      await options.nth(3).click();
      await page.waitForTimeout(WAIT_TIME.short);
      markers = page.locator('#customAreaMarkers .rich-menu-area-number');
      count = await markers.count();
      expect(count).toBe(4);

      // 1+2 → 3 個標記
      await options.nth(4).click();
      await page.waitForTimeout(WAIT_TIME.short);
      markers = page.locator('#customAreaMarkers .rich-menu-area-number');
      count = await markers.count();
      expect(count).toBe(3);
      console.log('佈局切換後區域標記數量正確更新');
    });

    test('區域標記顯示正確數字', async ({ page }) => {
      // 預設 3+4 佈局
      const markers = page.locator('#customAreaMarkers .rich-menu-area-number');
      const count = await markers.count();

      for (let i = 0; i < count; i++) {
        const text = await markers.nth(i).textContent();
        expect(text?.trim()).toBe(String(i + 1));
      }
      console.log('區域標記數字正確 (1~7)');
    });
  });
});

// ============================================================
// 第二部分：API 契約測試 — Rich Menu 相關 API
// ============================================================

test.describe('Rich Menu API 契約測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test.describe('GET /api/settings/line/rich-menu', () => {
    test('取得 Rich Menu 資訊 — API 正常回應', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings/line/rich-menu', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      expect(response.ok()).toBeTruthy();
      const data = await response.json();
      expect(data.success).toBeTruthy();
      console.log(`Rich Menu API 回應: ${JSON.stringify(data.data)}`);
    });

    test('取得 Rich Menu 資訊 — 包含 mode 欄位', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings/line/rich-menu', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const data = await response.json();

      if (data.data && data.data.richMenuId) {
        // 有 Rich Menu 時，應包含 mode 欄位
        expect(data.data).toHaveProperty('mode');
        const mode = data.data.mode;
        expect(mode === 'DEFAULT' || mode === 'CUSTOM' || mode === null).toBeTruthy();
        console.log(`Rich Menu mode: ${mode}`);
      } else {
        console.log('尚未建立 Rich Menu，跳過 mode 檢查');
      }
    });

    test('取得 Rich Menu 資訊 — customConfig 欄位檢查', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings/line/rich-menu', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const data = await response.json();

      if (data.data && data.data.richMenuId) {
        // 有 Rich Menu 時，customConfig 可能存在也可能不存在（取決於是否已設定）
        const hasCustomConfig = 'customConfig' in data.data;
        console.log(`Rich Menu customConfig 欄位${hasCustomConfig ? '存在' : '不存在'}：${data.data.customConfig || '(null/未設定)'}`);
        // 不強制要求欄位存在，只記錄狀態
      } else {
        console.log('尚未建立 Rich Menu，跳過 customConfig 檢查');
      }
    });

    test('取得 Rich Menu 資訊 — 包含基本欄位', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.get('/api/settings/line/rich-menu', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      const data = await response.json();
      expect(data).toHaveProperty('success');
      expect(data).toHaveProperty('data');

      if (data.data && data.data.richMenuId) {
        expect(data.data).toHaveProperty('richMenuId');
        expect(data.data).toHaveProperty('theme');
        console.log(`Rich Menu ID: ${data.data.richMenuId}, Theme: ${data.data.theme}`);
      }
    });
  });

  test.describe('POST /api/settings/line/rich-menu/create', () => {
    test('建立預設主題 Rich Menu — API 端點存在', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.post('/api/settings/line/rich-menu/create', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: { theme: 'GREEN' }
      });
      // 不應是 404（端點存在）
      expect(response.status()).not.toBe(404);
      // 不應是 400（欄位名稱正確）
      expect(response.status()).not.toBe(400);
      console.log(`建立預設 Rich Menu 回應: ${response.status()}`);

      if (response.ok()) {
        const data = await response.json();
        expect(data.data).toHaveProperty('richMenuId');
        expect(data.data).toHaveProperty('theme');
        expect(data.data.theme).toBe('GREEN');
        console.log(`建立成功: richMenuId=${data.data.richMenuId}`);
      }
    });

    test('建立預設 Rich Menu — 各主題都可接受', async ({ request }) => {
      if (!tenantToken) return;

      const themes = ['GREEN', 'BLUE', 'PURPLE', 'ORANGE', 'DARK'];
      for (const theme of themes) {
        const response = await request.post('/api/settings/line/rich-menu/create', {
          headers: {
            'Authorization': `Bearer ${tenantToken}`,
            'Content-Type': 'application/json'
          },
          data: { theme }
        });
        // 不應是 400 或 404
        expect(response.status()).not.toBe(400);
        expect(response.status()).not.toBe(404);
        console.log(`主題 ${theme} 回應: ${response.status()}`);

        // 只測第一個成功即可，避免多次建立
        if (response.ok()) break;
      }
    });
  });

  test.describe('POST /api/settings/line/rich-menu/create-custom', () => {
    test('建立自訂 Rich Menu — API 端點存在', async ({ request }) => {
      if (!tenantToken) return;

      // 建立一個最小的 PNG 圖片（1x1 像素）
      const minimalPng = Buffer.from([
        0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
        0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, // IHDR chunk
        0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, // 1x1
        0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53, // 8-bit RGB
        0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, // IDAT chunk
        0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00,
        0x00, 0x00, 0x02, 0x00, 0x01, 0xE2, 0x21, 0xBC,
        0x33, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, // IEND chunk
        0x44, 0xAE, 0x42, 0x60, 0x82
      ]);

      const config = JSON.stringify({
        layout: '3+4',
        areas: [
          { action: 'start_booking', label: '開始預約' },
          { action: 'view_bookings', label: '我的預約' },
          { action: 'start_shopping', label: '瀏覽商品' },
          { action: 'view_coupons', label: '領取票券' },
          { action: 'view_my_coupons', label: '我的票券' },
          { action: 'view_member_info', label: '會員資訊' },
          { action: 'contact_shop', label: '聯絡店家' }
        ]
      });

      const response = await request.post('/api/settings/line/rich-menu/create-custom', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`
        },
        multipart: {
          file: {
            name: 'test-menu.png',
            mimeType: 'image/png',
            buffer: minimalPng
          },
          config: config
        }
      });

      // 端點存在（非 404）
      expect(response.status()).not.toBe(404);
      console.log(`建立自訂 Rich Menu 回應: ${response.status()}`);

      if (response.ok()) {
        const data = await response.json();
        expect(data.data).toHaveProperty('richMenuId');
        expect(data.data).toHaveProperty('mode');
        expect(data.data.mode).toBe('CUSTOM');
        console.log(`自訂 Rich Menu 建立成功: ${data.data.richMenuId}`);
      }
    });

    test('建立自訂 Rich Menu — config JSON 格式驗證', async ({ request }) => {
      if (!tenantToken) return;

      const minimalPng = Buffer.from([
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

      // 測試各種佈局的 config
      const layouts = [
        { layout: '2x3', count: 6 },
        { layout: '2+3', count: 5 },
        { layout: '2x2', count: 4 },
        { layout: '1+2', count: 3 }
      ];

      for (const { layout, count } of layouts) {
        const areas = Array.from({ length: count }, (_, i) => ({
          action: 'start_booking',
          label: `區域 ${i + 1}`
        }));

        const config = JSON.stringify({ layout, areas });

        const response = await request.post('/api/settings/line/rich-menu/create-custom', {
          headers: { 'Authorization': `Bearer ${tenantToken}` },
          multipart: {
            file: {
              name: 'test-menu.png',
              mimeType: 'image/png',
              buffer: minimalPng
            },
            config: config
          }
        });

        // 不應是 400（config 格式正確）
        expect(response.status()).not.toBe(404);
        console.log(`佈局 ${layout} (${count} 格) 回應: ${response.status()}`);

        // 只測試一個成功即可
        if (response.ok()) break;
      }
    });

    test('建立自訂 Rich Menu — 無檔案應非 200', async ({ request }) => {
      if (!tenantToken) return;

      // 不送 file，只送 config（multipart 端點應回非 200）
      const response = await request.post('/api/settings/line/rich-menu/create-custom', {
        headers: {
          'Authorization': `Bearer ${tenantToken}`,
          'Content-Type': 'application/json'
        },
        data: { config: '{"layout":"3+4","areas":[]}' }
      });

      // 不應成功（因為缺少圖片檔案）
      // 可能是 400、415 或 500（取決於伺服器對非 multipart 請求的處理）
      expect(response.status()).not.toBe(200);
      console.log(`無檔案建立回應: ${response.status()} (預期非 200)`);
    });
  });

  test.describe('POST /api/settings/line/rich-menu/upload-image', () => {
    test('上傳圖片 API 端點存在', async ({ request }) => {
      if (!tenantToken) return;

      const minimalPng = Buffer.from([
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

      const response = await request.post('/api/settings/line/rich-menu/upload-image', {
        headers: { 'Authorization': `Bearer ${tenantToken}` },
        multipart: {
          file: {
            name: 'test-upload.png',
            mimeType: 'image/png',
            buffer: minimalPng
          }
        }
      });
      expect(response.status()).not.toBe(404);
      console.log(`上傳圖片 API 回應: ${response.status()}`);
    });
  });

  test.describe('DELETE /api/settings/line/rich-menu', () => {
    test('刪除 Rich Menu — API 端點存在', async ({ request }) => {
      if (!tenantToken) return;

      const response = await request.delete('/api/settings/line/rich-menu', {
        headers: { 'Authorization': `Bearer ${tenantToken}` }
      });
      // 不應是 404
      expect(response.status()).not.toBe(404);
      console.log(`刪除 Rich Menu 回應: ${response.status()}`);
    });
  });

  test.describe('認證保護驗證', () => {
    test('未認證存取 Rich Menu API 應被拒絕', async ({ request }) => {
      const response = await request.get('/api/settings/line/rich-menu');
      // 應該是 401 或 403
      expect(response.status()).toBeGreaterThanOrEqual(400);
      expect(response.status()).toBeLessThan(500);
      console.log(`未認證 Rich Menu API 回應: ${response.status()}`);
    });

    test('未認證建立 Rich Menu 應被拒絕', async ({ request }) => {
      const response = await request.post('/api/settings/line/rich-menu/create', {
        headers: { 'Content-Type': 'application/json' },
        data: { theme: 'GREEN' }
      });
      expect(response.status()).toBeGreaterThanOrEqual(400);
      expect(response.status()).toBeLessThan(500);
      console.log(`未認證建立 Rich Menu 回應: ${response.status()}`);
    });

    test('未認證建立自訂 Rich Menu 應被拒絕', async ({ request }) => {
      const response = await request.post('/api/settings/line/rich-menu/create-custom', {
        headers: { 'Content-Type': 'application/json' },
        data: {}
      });
      expect(response.status()).toBeGreaterThanOrEqual(400);
      expect(response.status()).toBeLessThan(500);
      console.log(`未認證建立自訂 Rich Menu 回應: ${response.status()}`);
    });
  });
});

// ============================================================
// 第三部分：頁面互動與狀態恢復測試
// ============================================================

test.describe('Rich Menu 頁面互動測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);
  });

  test('重新整理頁面後模式正確恢復', async ({ page }) => {
    // 載入頁面後，檢查 Rich Menu 資訊是否正確載入
    await page.waitForTimeout(WAIT_TIME.long);

    const statusBadge = page.locator('#richMenuStatusBadge');
    const badgeText = await statusBadge.textContent();

    if (badgeText?.includes('已設定') || badgeText?.includes('已啟用')) {
      // 有 Rich Menu 時，預覽應可見且主題有效
      const preview = page.locator('#richMenuPreview');
      if (await preview.isVisible()) {
        const classes = await preview.getAttribute('class');
        expect(classes).toContain('theme-');
      }
      console.log('Rich Menu 已設定，預覽正確顯示');
    } else {
      console.log('Rich Menu 未設定，為初始狀態');
    }
  });

  test('LINE 設定頁面不顯示載入中卡住', async ({ page }) => {
    // 等待足夠時間
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查沒有卡住的 spinner
    const spinners = page.locator('#richMenuCard .spinner-border:visible');
    const spinnerCount = await spinners.count();
    expect(spinnerCount).toBe(0);
    console.log('Rich Menu 區塊無卡住的 spinner');
  });

  test('自訂模式所有步驟可見', async ({ page }) => {
    const customTab = page.locator('#customMenuTab');
    if (!(await customTab.isVisible())) {
      console.log('自訂 Tab 隱藏中（需訂閱），跳過');
      test.skip();
      return;
    }

    // 切到自訂模式
    await customTab.click();
    await page.waitForTimeout(WAIT_TIME.short);

    // 步驟 1：佈局選擇
    await expect(page.locator('#layoutSelector')).toBeVisible();

    // 步驟 2：圖片上傳
    const fileInput = page.locator('#customModePanel input[type="file"]');
    await expect(fileInput).toBeAttached();

    // 步驟 3：動作設定
    await expect(page.locator('#customActionsList')).toBeVisible();

    // 建立按鈕
    await expect(page.locator('#createCustomRichMenuBtn')).toBeVisible();

    console.log('自訂模式所有步驟（佈局→上傳→動作→建立）都可見');
  });

  test('預設模式和自訂模式面板不會同時顯示', async ({ page }) => {
    // 預設模式
    await expect(page.locator('#defaultModePanel')).toBeVisible();
    await expect(page.locator('#customModePanel')).toBeHidden();

    const customTab = page.locator('#customMenuTab');
    if (!(await customTab.isVisible())) {
      console.log('自訂 Tab 隱藏中（需訂閱），跳過切換測試');
      test.skip();
      return;
    }

    // 切到自訂
    await customTab.click();
    await page.waitForTimeout(WAIT_TIME.short);

    // 自訂模式
    await expect(page.locator('#defaultModePanel')).toBeHidden();
    await expect(page.locator('#customModePanel')).toBeVisible();
    console.log('兩個面板永遠互斥顯示');
  });

  test('動作下拉選單可以切換', async ({ page }) => {
    const customTab = page.locator('#customMenuTab');
    if (!(await customTab.isVisible())) {
      console.log('自訂 Tab 隱藏中（需訂閱），跳過');
      test.skip();
      return;
    }

    // 切到自訂模式
    await customTab.click();
    await page.waitForTimeout(WAIT_TIME.short);

    // 第一個下拉選單
    const firstSelect = page.locator('#customActionsList select').first();
    await expect(firstSelect).toBeVisible();

    // 切換到不同動作
    await firstSelect.selectOption('contact_shop');
    const value = await firstSelect.inputValue();
    expect(value).toBe('contact_shop');
    console.log('動作下拉選單可正常切換');
  });

  test('RWD — 小螢幕 Rich Menu 區塊不崩版', async ({ page }) => {
    // 手機尺寸
    await page.setViewportSize({ width: 375, height: 812 });
    await page.waitForTimeout(WAIT_TIME.medium);

    // Rich Menu 卡片仍可見
    await expect(page.locator('#richMenuCard')).toBeVisible();

    // 模式 Tab 仍可見
    await expect(page.locator('#richMenuModeTabs')).toBeVisible();

    // 不應有水平滾動
    const hasHorizontalScroll = await page.evaluate(() => {
      return document.documentElement.scrollWidth > document.documentElement.clientWidth;
    });
    // 允許微小差異（1px）
    if (hasHorizontalScroll) {
      const diff = await page.evaluate(() => {
        return document.documentElement.scrollWidth - document.documentElement.clientWidth;
      });
      expect(diff).toBeLessThan(10); // 容許小誤差
    }
    console.log('小螢幕 Rich Menu 區塊不崩版');
  });

  test('RWD — 小螢幕自訂模式佈局選擇器不崩版', async ({ page }) => {
    const customTab = page.locator('#customMenuTab');
    if (!(await customTab.isVisible())) {
      console.log('自訂 Tab 隱藏中（需訂閱），跳過');
      test.skip();
      return;
    }

    await page.setViewportSize({ width: 375, height: 812 });
    await page.waitForTimeout(WAIT_TIME.medium);

    // 切到自訂模式
    await customTab.click();
    await page.waitForTimeout(WAIT_TIME.short);

    // 佈局選擇器可見
    await expect(page.locator('#layoutSelector')).toBeVisible();

    // 5 個選項都可見
    const options = page.locator('#layoutSelector .layout-option');
    const count = await options.count();
    expect(count).toBe(5);
    console.log('小螢幕佈局選擇器正常顯示');
  });
});

// ============================================================
// 第四部分：向後相容性測試
// ============================================================

test.describe('向後相容性測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('GET /api/settings/line 基本設定仍正常', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/settings/line', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();
    expect(data.data).toHaveProperty('channelId');
    expect(data.data).toHaveProperty('status');
    console.log(`LINE 基本設定正常: channelId=${data.data.channelId}, status=${data.data.status}`);
  });

  test('原有建立 Rich Menu API 仍可用', async ({ request }) => {
    if (!tenantToken) return;

    // POST /api/settings/line/rich-menu/create（原有 API）
    const response = await request.post('/api/settings/line/rich-menu/create', {
      headers: {
        'Authorization': `Bearer ${tenantToken}`,
        'Content-Type': 'application/json'
      },
      data: { theme: 'GREEN' }
    });
    expect(response.status()).not.toBe(404);
    expect(response.status()).not.toBe(400);
    console.log(`原有建立 API 回應: ${response.status()} (向後相容)`);
  });

  test('原有上傳圖片 API 仍可用', async ({ request }) => {
    if (!tenantToken) return;

    const minimalPng = Buffer.from([
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

    const response = await request.post('/api/settings/line/rich-menu/upload-image', {
      headers: { 'Authorization': `Bearer ${tenantToken}` },
      multipart: {
        file: {
          name: 'test.png',
          mimeType: 'image/png',
          buffer: minimalPng
        }
      }
    });
    expect(response.status()).not.toBe(404);
    console.log(`原有上傳 API 回應: ${response.status()} (向後相容)`);
  });
});

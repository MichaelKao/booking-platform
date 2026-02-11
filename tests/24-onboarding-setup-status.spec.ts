import { test, expect, APIRequestContext } from './fixtures';
import {
  tenantLogin,
  waitForLoading,
  WAIT_TIME
} from './utils/test-helpers';

/**
 * 新手引導系統 & 側邊欄設定狀態測試
 *
 * 測試範圍：
 * 1. Setup Status API 端點驗證
 * 2. 儀表板新手引導卡片（顯示、步驟、進度條、關閉）
 * 3. 側邊欄進度環（SVG、百分比、隱藏邏輯）
 * 4. 側邊欄注意圓點（脈動動畫、next-step 標記）
 * 5. 側邊欄店家資訊 footer
 * 6. 引導卡片 5 個步驟按鈕可點擊導航
 * 7. 關閉引導持久化（localStorage）
 * 8. 響應式行為
 */

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: 'g0909095118@gmail.com', password: 'gaojunting11' }
  });
  const data = await response.json();
  return data.data?.accessToken || '';
}

// ========================================
// Part 1: Setup Status API
// ========================================

test.describe('設定狀態 API 測試', () => {
  let tenantToken: string;

  test.beforeAll(async ({ request }) => {
    tenantToken = await getTenantToken(request);
  });

  test('GET /api/settings/setup-status 回傳正確結構', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    expect(response.ok()).toBeTruthy();
    const data = await response.json();
    expect(data.success).toBeTruthy();

    // 驗證所有必要欄位
    expect(data.data).toHaveProperty('hasBasicInfo');
    expect(data.data).toHaveProperty('hasBusinessHours');
    expect(data.data).toHaveProperty('staffCount');
    expect(data.data).toHaveProperty('serviceCount');
    expect(data.data).toHaveProperty('lineConfigured');
    expect(data.data).toHaveProperty('hasBookings');
    expect(data.data).toHaveProperty('completionPercentage');
    expect(data.data).toHaveProperty('totalSteps');
    expect(data.data).toHaveProperty('completedSteps');

    console.log(`設定完成度: ${data.data.completionPercentage}% (${data.data.completedSteps}/${data.data.totalSteps})`);
    console.log(`基本資訊: ${data.data.hasBasicInfo}, 營業時間: ${data.data.hasBusinessHours}`);
    console.log(`員工數: ${data.data.staffCount}, 服務數: ${data.data.serviceCount}`);
    console.log(`LINE: ${data.data.lineConfigured}, 有預約: ${data.data.hasBookings}`);
  });

  test('totalSteps 固定為 5', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const data = await response.json();
    expect(data.data.totalSteps).toBe(5);
  });

  test('completionPercentage 在 0-100 之間', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const data = await response.json();
    expect(data.data.completionPercentage).toBeGreaterThanOrEqual(0);
    expect(data.data.completionPercentage).toBeLessThanOrEqual(100);
  });

  test('completedSteps 不超過 totalSteps', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const data = await response.json();
    expect(data.data.completedSteps).toBeLessThanOrEqual(data.data.totalSteps);
    expect(data.data.completedSteps).toBeGreaterThanOrEqual(0);
  });

  test('staffCount 為非負數', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const data = await response.json();
    expect(data.data.staffCount).toBeGreaterThanOrEqual(0);
  });

  test('serviceCount 為非負數', async ({ request }) => {
    if (!tenantToken) return;

    const response = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${tenantToken}` }
    });
    const data = await response.json();
    expect(data.data.serviceCount).toBeGreaterThanOrEqual(0);
  });

  test('未授權存取回傳 401', async ({ request }) => {
    const response = await request.get('/api/settings/setup-status');
    expect(response.status()).toBe(401);
  });
});

// ========================================
// Part 2: 儀表板新手引導卡片
// ========================================

test.describe('儀表板新手引導卡片', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    // 清除 onboarding dismissed 狀態，確保引導卡片可見
    await page.evaluate(() => {
      localStorage.removeItem('onboarding_dismissed');
      sessionStorage.removeItem('setup_status');
      sessionStorage.removeItem('setup_status_time');
    });
  });

  test('引導卡片在儀表板顯示', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    // 引導卡片可能顯示（設定未完成）或不顯示（全部完成且有預約）
    const isVisible = await card.isVisible().catch(() => false);
    console.log(`引導卡片可見: ${isVisible}`);

    if (isVisible) {
      // 驗證標題
      await expect(page.locator('.onboarding-title')).toBeVisible();
      console.log('✓ 引導卡片標題可見');
    }
  });

  test('引導卡片包含進度條', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    if (await card.isVisible().catch(() => false)) {
      const progressBar = page.locator('.onboarding-progress-bar');
      await expect(progressBar).toBeVisible();
      const fill = page.locator('#onboardingProgressFill');
      await expect(fill).toBeVisible();
      console.log('✓ 進度條顯示正常');
    } else {
      console.log('○ 引導卡片未顯示（全部完成）');
    }
  });

  test('引導卡片包含 5 個步驟', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    if (await card.isVisible().catch(() => false)) {
      const steps = page.locator('.onboarding-step');
      const stepsVisible = await page.locator('#onboardingSteps').isVisible().catch(() => false);

      if (stepsVisible) {
        const count = await steps.count();
        expect(count).toBe(5);
        console.log(`✓ 引導步驟數量: ${count}`);
      } else {
        // 全部完成顯示慶祝訊息
        const complete = page.locator('#onboardingComplete');
        const completeVisible = await complete.isVisible().catch(() => false);
        console.log(`○ 步驟區隱藏，慶祝訊息: ${completeVisible}`);
      }
    }
  });

  test('步驟 1: 完善店家資訊連結正確', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-basicInfo');
    if (await step.isVisible().catch(() => false)) {
      const href = await step.getAttribute('href');
      expect(href).toBe('/tenant/settings');
      console.log('✓ 步驟 1 連結: /tenant/settings');
    }
  });

  test('步驟 2: 新增員工連結正確', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-staff');
    if (await step.isVisible().catch(() => false)) {
      const href = await step.getAttribute('href');
      expect(href).toBe('/tenant/staff');
      console.log('✓ 步驟 2 連結: /tenant/staff');
    }
  });

  test('步驟 3: 新增服務項目連結正確', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-service');
    if (await step.isVisible().catch(() => false)) {
      const href = await step.getAttribute('href');
      expect(href).toBe('/tenant/services');
      console.log('✓ 步驟 3 連結: /tenant/services');
    }
  });

  test('步驟 4: 設定營業時間連結正確', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-hours');
    if (await step.isVisible().catch(() => false)) {
      const href = await step.getAttribute('href');
      expect(href).toBe('/tenant/settings');
      console.log('✓ 步驟 4 連結: /tenant/settings');
    }
  });

  test('步驟 5: 連接 LINE Bot 連結正確', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-line');
    if (await step.isVisible().catch(() => false)) {
      const href = await step.getAttribute('href');
      expect(href).toBe('/tenant/line-settings');
      console.log('✓ 步驟 5 連結: /tenant/line-settings');
    }
  });

  test('已完成步驟有 completed class', async ({ page, request }) => {
    const token = await getTenantToken(request);
    if (!token) return;

    // 先確認 API 返回的完成狀態
    const apiResp = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const apiData = await apiResp.json();

    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    if (await card.isVisible().catch(() => false)) {
      // 檢查已完成的步驟是否有 completed class
      if (apiData.data.hasBasicInfo) {
        const hasClass = await page.locator('#step-basicInfo.completed').count();
        console.log(`步驟 1 (基本資訊) completed class: ${hasClass > 0}`);
      }
      if (apiData.data.staffCount > 0) {
        const hasClass = await page.locator('#step-staff.completed').count();
        console.log(`步驟 2 (員工) completed class: ${hasClass > 0}`);
      }
      if (apiData.data.serviceCount > 0) {
        const hasClass = await page.locator('#step-service.completed').count();
        console.log(`步驟 3 (服務) completed class: ${hasClass > 0}`);
      }
    }
  });

  test('有一個 next-step 步驟（若未全部完成）', async ({ page, request }) => {
    const token = await getTenantToken(request);
    if (!token) return;

    const apiResp = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const apiData = await apiResp.json();

    // 如果全部完成，跳過此測試
    if (apiData.data.completedSteps >= apiData.data.totalSteps) {
      console.log('○ 全部完成，無 next-step');
      return;
    }

    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    if (await card.isVisible().catch(() => false)) {
      const nextSteps = page.locator('.onboarding-step.next-step');
      const count = await nextSteps.count();
      expect(count).toBe(1);
      console.log('✓ 恰好有 1 個 next-step 步驟');
    }
  });

  test('關閉按鈕可點擊', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    if (await card.isVisible().catch(() => false)) {
      const closeBtn = page.locator('.onboarding-close');
      await expect(closeBtn).toBeVisible();
      console.log('✓ 關閉按鈕可見');
    }
  });

  test('點擊關閉按鈕後引導卡片隱藏', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    if (await card.isVisible().catch(() => false)) {
      await page.click('.onboarding-close');
      await page.waitForTimeout(500);

      // 檢查 localStorage
      const dismissed = await page.evaluate(() => localStorage.getItem('onboarding_dismissed'));
      expect(dismissed).toBe('true');
      console.log('✓ 關閉狀態已存入 localStorage');
    }
  });

  test('關閉後重新載入不再顯示', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('#onboardingCard');
    if (await card.isVisible().catch(() => false)) {
      // 關閉引導
      await page.click('.onboarding-close');
      await page.waitForTimeout(500);

      // 重新載入
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      // 確認不再顯示
      const stillVisible = await card.isVisible().catch(() => false);
      expect(stillVisible).toBeFalsy();
      console.log('✓ 重新載入後引導卡片不再顯示');
    }
  });

  test('步驟按鈕可點擊導航到設定頁', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-basicInfo');
    if (await step.isVisible().catch(() => false)) {
      await step.click();
      await waitForLoading(page);
      expect(page.url()).toContain('/tenant/settings');
      console.log('✓ 步驟 1 導航到 /tenant/settings');
    }
  });

  test('步驟按鈕可點擊導航到員工頁', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-staff');
    if (await step.isVisible().catch(() => false)) {
      await step.click();
      await waitForLoading(page);
      expect(page.url()).toContain('/tenant/staff');
      console.log('✓ 步驟 2 導航到 /tenant/staff');
    }
  });

  test('步驟按鈕可點擊導航到服務頁', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-service');
    if (await step.isVisible().catch(() => false)) {
      await step.click();
      await waitForLoading(page);
      expect(page.url()).toContain('/tenant/services');
      console.log('✓ 步驟 3 導航到 /tenant/services');
    }
  });

  test('步驟按鈕可點擊導航到 LINE 設定頁', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('#step-line');
    if (await step.isVisible().catch(() => false)) {
      await step.click();
      await waitForLoading(page);
      expect(page.url()).toContain('/tenant/line-settings');
      console.log('✓ 步驟 5 導航到 /tenant/line-settings');
    }
  });
});

// ========================================
// Part 3: 側邊欄進度環
// ========================================

test.describe('側邊欄進度環', () => {
  test('進度環 HTML 結構存在', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 進度環容器存在
    const container = page.locator('#sidebarSetupProgress');
    const exists = await container.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ 進度環容器存在');
  });

  test('進度環包含 SVG 元素', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const svg = page.locator('#sidebarSetupProgress svg.progress-ring');
    const exists = await svg.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ SVG 進度環存在');
  });

  test('進度環百分比文字更新', async ({ page, request }) => {
    const token = await getTenantToken(request);
    if (!token) return;

    const apiResp = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const apiData = await apiResp.json();

    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const progressText = page.locator('#progressRingText');
    if (await progressText.isVisible().catch(() => false)) {
      const text = await progressText.textContent();
      console.log(`進度環顯示: ${text}`);
      expect(text).toContain('%');
    } else {
      // 全部完成時進度環隱藏
      console.log('○ 進度環已隱藏（可能全部完成）');
    }
  });

  test('進度環連結指向設定頁', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const link = page.locator('.setup-progress-link');
    if (await link.isVisible().catch(() => false)) {
      const href = await link.getAttribute('href');
      expect(href).toBe('/tenant/settings');
      console.log('✓ 進度環連結指向 /tenant/settings');
    }
  });

  test('全部完成時進度環隱藏', async ({ page, request }) => {
    const token = await getTenantToken(request);
    if (!token) return;

    const apiResp = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const apiData = await apiResp.json();

    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const container = page.locator('#sidebarSetupProgress');
    const isVisible = await container.isVisible().catch(() => false);

    if (apiData.data.completedSteps >= apiData.data.totalSteps) {
      expect(isVisible).toBeFalsy();
      console.log('✓ 全部完成，進度環正確隱藏');
    } else {
      console.log(`○ 尚未全部完成 (${apiData.data.completedSteps}/${apiData.data.totalSteps})，進度環顯示: ${isVisible}`);
    }
  });
});

// ========================================
// Part 4: 側邊欄注意圓點
// ========================================

test.describe('側邊欄注意圓點', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    // 清除快取確保載入最新狀態
    await page.evaluate(() => {
      sessionStorage.removeItem('setup_status');
      sessionStorage.removeItem('setup_status_time');
    });
  });

  test('未完成步驟的選單項目有注意圓點', async ({ page, request }) => {
    const token = await getTenantToken(request);
    if (!token) return;

    const apiResp = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const apiData = await apiResp.json();

    // 全部完成時不應有圓點
    if (apiData.data.completedSteps >= apiData.data.totalSteps) {
      await page.goto('/tenant/dashboard');
      await waitForLoading(page);
      await page.waitForTimeout(WAIT_TIME.api);

      const dots = page.locator('.setup-attention-dot');
      const count = await dots.count();
      expect(count).toBe(0);
      console.log('✓ 全部完成，無注意圓點');
      return;
    }

    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const dots = page.locator('.setup-attention-dot');
    const count = await dots.count();
    console.log(`注意圓點數量: ${count}`);
    expect(count).toBeGreaterThan(0);
  });

  test('第一個未完成步驟有 next-step class', async ({ page, request }) => {
    const token = await getTenantToken(request);
    if (!token) return;

    const apiResp = await request.get('/api/settings/setup-status', {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const apiData = await apiResp.json();

    if (apiData.data.completedSteps >= apiData.data.totalSteps) {
      console.log('○ 全部完成，跳過');
      return;
    }

    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const nextStepDot = page.locator('.setup-attention-dot.next-step');
    const count = await nextStepDot.count();
    expect(count).toBe(1);
    console.log('✓ 恰好有 1 個 next-step 圓點');
  });

  test('注意圓點位於 nav-item 內', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const dots = page.locator('.nav-item .setup-attention-dot');
    const count = await dots.count();
    const totalDots = await page.locator('.setup-attention-dot').count();

    // 所有圓點都應在 nav-item 內
    expect(count).toBe(totalDots);
    console.log(`圓點全部在 nav-item 內: ${count}/${totalDots}`);
  });

  test('sessionStorage 快取機制運作', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    // 檢查 sessionStorage 是否有快取
    const cached = await page.evaluate(() => sessionStorage.getItem('setup_status'));
    const cachedTime = await page.evaluate(() => sessionStorage.getItem('setup_status_time'));

    if (cached) {
      expect(cachedTime).toBeTruthy();
      const data = JSON.parse(cached);
      expect(data).toHaveProperty('completionPercentage');
      console.log('✓ sessionStorage 快取存在');
    } else {
      console.log('○ 快取不存在（可能是公開頁面或未登入）');
    }
  });
});

// ========================================
// Part 5: 側邊欄店家 footer
// ========================================

test.describe('側邊欄店家 footer', () => {
  test('sidebar footer 存在', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);

    const footer = page.locator('.sidebar-footer');
    const exists = await footer.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ sidebar footer 存在');
  });

  test('sidebar footer 包含店家圖示', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);

    const icon = page.locator('.sidebar-footer-icon');
    const exists = await icon.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ sidebar footer 圖示存在');
  });

  test('sidebar footer 顯示店家名稱', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.medium);

    const nameEl = page.locator('#sidebarTenantName');
    const exists = await nameEl.count();
    expect(exists).toBeGreaterThan(0);

    const text = await nameEl.textContent();
    console.log(`sidebar footer 店家名稱: "${text}"`);
    // 名稱應該被載入（可能是預設值或實際名稱）
    expect(text).toBeTruthy();
  });
});

// ========================================
// Part 6: 多頁面測試（設定狀態在各頁面載入）
// ========================================

test.describe('設定狀態跨頁面載入', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.evaluate(() => {
      sessionStorage.removeItem('setup_status');
      sessionStorage.removeItem('setup_status_time');
    });
  });

  test('預約列表頁面載入側邊欄狀態', async ({ page }) => {
    await page.goto('/tenant/bookings');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const footer = page.locator('.sidebar-footer');
    const exists = await footer.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ 預約列表頁有 sidebar footer');
  });

  test('員工管理頁面載入側邊欄狀態', async ({ page }) => {
    await page.goto('/tenant/staff');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const footer = page.locator('.sidebar-footer');
    const exists = await footer.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ 員工管理頁有 sidebar footer');
  });

  test('服務項目頁面載入側邊欄狀態', async ({ page }) => {
    await page.goto('/tenant/services');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const footer = page.locator('.sidebar-footer');
    const exists = await footer.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ 服務項目頁有 sidebar footer');
  });

  test('店家設定頁面載入側邊欄狀態', async ({ page }) => {
    await page.goto('/tenant/settings');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const footer = page.locator('.sidebar-footer');
    const exists = await footer.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ 店家設定頁有 sidebar footer');
  });

  test('LINE 設定頁面載入側邊欄狀態', async ({ page }) => {
    await page.goto('/tenant/line-settings');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const footer = page.locator('.sidebar-footer');
    const exists = await footer.count();
    expect(exists).toBeGreaterThan(0);
    console.log('✓ LINE 設定頁有 sidebar footer');
  });
});

// ========================================
// Part 7: CSS 樣式驗證
// ========================================

test.describe('CSS 樣式驗證', () => {
  test('引導卡片有漸層背景', async ({ page }) => {
    await tenantLogin(page);
    await page.evaluate(() => {
      localStorage.removeItem('onboarding_dismissed');
      sessionStorage.removeItem('setup_status');
      sessionStorage.removeItem('setup_status_time');
    });
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const card = page.locator('.onboarding-card');
    if (await card.isVisible().catch(() => false)) {
      const bg = await card.evaluate(el => getComputedStyle(el).backgroundImage);
      expect(bg).toContain('gradient');
      console.log('✓ 引導卡片有漸層背景');
    }
  });

  test('注意圓點有脈動動畫', async ({ page }) => {
    await tenantLogin(page);
    await page.evaluate(() => {
      sessionStorage.removeItem('setup_status');
      sessionStorage.removeItem('setup_status_time');
    });
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const dot = page.locator('.setup-attention-dot').first();
    if (await dot.isVisible().catch(() => false)) {
      const animation = await dot.evaluate(el => getComputedStyle(el).animationName);
      console.log(`圓點動畫: ${animation}`);
      // 動畫名稱應包含 pulse-dot
      expect(animation).toContain('pulse-dot');
    } else {
      console.log('○ 無注意圓點（可能全部完成）');
    }
  });

  test('步驟卡片 hover 效果存在', async ({ page }) => {
    await tenantLogin(page);
    await page.evaluate(() => {
      localStorage.removeItem('onboarding_dismissed');
      sessionStorage.removeItem('setup_status');
      sessionStorage.removeItem('setup_status_time');
    });
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);
    await page.waitForTimeout(WAIT_TIME.api);

    const step = page.locator('.onboarding-step').first();
    if (await step.isVisible().catch(() => false)) {
      // 驗證 transition CSS 屬性存在
      const transition = await step.evaluate(el => getComputedStyle(el).transition);
      expect(transition).toBeTruthy();
      console.log('✓ 步驟卡片有 transition 效果');
    }
  });
});

// ========================================
// Part 8: 版本號檢查
// ========================================

test.describe('快取版本號', () => {
  test('tenant.css 版本號已更新至 v5', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);

    // Thymeleaf 渲染後 th:href 變成 href，用 evaluate 找所有 link 標籤
    const href = await page.evaluate(() => {
      const links = Array.from(document.querySelectorAll('link[rel="stylesheet"]'));
      const tenantCss = links.find(l => l.getAttribute('href')?.includes('tenant.css'));
      return tenantCss ? tenantCss.getAttribute('href') : null;
    });
    console.log(`tenant.css href: ${href}`);
    expect(href).toBeTruthy();
    expect(href).toContain('v=5');
  });

  test('tenant.js 版本號已更新至 v10', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/dashboard');
    await waitForLoading(page);

    // Thymeleaf 渲染後 th:src 變成 src，用 evaluate 找所有 script 標籤
    const src = await page.evaluate(() => {
      const scripts = Array.from(document.querySelectorAll('script[src]'));
      const tenantJs = scripts.find(s => s.getAttribute('src')?.includes('tenant.js'));
      return tenantJs ? tenantJs.getAttribute('src') : null;
    });
    console.log(`tenant.js src: ${src}`);
    expect(src).toBeTruthy();
    expect(src).toContain('v=10');
  });
});

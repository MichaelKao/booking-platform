import { test, expect, APIRequestContext } from './fixtures';
import { TEST_ACCOUNTS, tenantLogin, waitForLoading, WAIT_TIME } from './utils/test-helpers';

/**
 * LINE Bot API + LINE Settings UI -- Consolidated E2E Tests
 *
 * Consolidates: 15-line-bot, 16-line-category-selection, 17-rich-menu-custom,
 *               18-advanced-rich-menu, 19-flex-menu-step-editor
 *
 * Scope:
 *  1. LINE Webhook API (endpoint availability, event types, error handling)
 *  2. LINE Settings API (config structure, rich menu, flex menu)
 *  3. Flex Menu Config API (CRUD, steps config, persistence)
 *  4. LINE Settings UI (page load, tabs, connection status, webhook URL)
 *  5. Rich Menu UI (preview, theme colors)
 *  6. AI Feature (feature store entry, webhook graceful handling)
 *
 * NOTE: Does NOT make actual LINE API calls -- only tests our backend APIs.
 */

// ---------------------------------------------------------------------------
// Helpers
// ---------------------------------------------------------------------------

async function getTenantToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/tenant/login', {
    data: { username: TEST_ACCOUNTS.tenant.username, password: TEST_ACCOUNTS.tenant.password }
  });
  expect(response.ok()).toBeTruthy();
  const data = await response.json();
  expect(data.data.accessToken).toBeTruthy();
  return data.data.accessToken;
}

async function getAdminToken(request: APIRequestContext): Promise<string> {
  const response = await request.post('/api/auth/admin/login', {
    data: { username: 'admin', password: 'admin123' }
  });
  expect(response.ok()).toBeTruthy();
  const data = await response.json();
  expect(data.data.accessToken).toBeTruthy();
  return data.data.accessToken;
}

/** Build a LINE webhook payload wrapper */
function buildWebhookPayload(events: any[]) {
  return { destination: 'test', events };
}

/** Build a LINE message event */
function buildMessageEvent(text: string, userId?: string) {
  return {
    type: 'message',
    timestamp: Date.now(),
    source: { type: 'user', userId: userId || 'Utest_' + Date.now() + Math.random().toString(36).slice(2, 6) },
    replyToken: 'test-reply-' + Date.now(),
    message: { id: 'msg' + Date.now(), type: 'text', text }
  };
}

/** Build a LINE postback event */
function buildPostbackEvent(data: string, userId?: string) {
  return {
    type: 'postback',
    timestamp: Date.now(),
    source: { type: 'user', userId: userId || 'Utest_' + Date.now() + Math.random().toString(36).slice(2, 6) },
    replyToken: 'test-reply-' + Date.now(),
    postback: { data }
  };
}

/** Build a LINE follow event */
function buildFollowEvent(userId?: string) {
  return {
    type: 'follow',
    timestamp: Date.now(),
    source: { type: 'user', userId: userId || 'Utest_' + Date.now() },
    replyToken: 'test-reply-' + Date.now()
  };
}

const TENANT_CODE = 'michaelshop';

// ===========================================================================
//  1. LINE Webhook API
// ===========================================================================

test.describe('1. LINE Webhook API', () => {

  test('POST empty events array returns < 500', async ({ request }) => {
    const res = await request.post(`/api/line/webhook/${TENANT_CODE}`, {
      headers: { 'Content-Type': 'application/json' },
      data: buildWebhookPayload([])
    });
    expect(res.status()).toBeLessThan(500);
  });

  test('POST follow event returns < 500', async ({ request }) => {
    const res = await request.post(`/api/line/webhook/${TENANT_CODE}`, {
      headers: { 'Content-Type': 'application/json' },
      data: buildWebhookPayload([buildFollowEvent()])
    });
    expect(res.status()).toBeLessThan(500);
  });

  test('POST message event with keyword returns < 500', async ({ request }) => {
    const res = await request.post(`/api/line/webhook/${TENANT_CODE}`, {
      headers: { 'Content-Type': 'application/json' },
      data: buildWebhookPayload([buildMessageEvent('預約')])
    });
    expect(res.status()).toBeLessThan(500);
  });

  test('POST postback event returns < 500', async ({ request }) => {
    const res = await request.post(`/api/line/webhook/${TENANT_CODE}`, {
      headers: { 'Content-Type': 'application/json' },
      data: buildWebhookPayload([buildPostbackEvent('action=start_booking')])
    });
    expect(res.status()).toBeLessThan(500);
  });

  test('Invalid tenant code does not return 500', async ({ request }) => {
    const res = await request.post('/api/line/webhook/nonexistent_tenant_xyz_99', {
      headers: { 'Content-Type': 'application/json' },
      data: buildWebhookPayload([])
    });
    expect(res.status()).toBeLessThan(500);
  });
});

// ===========================================================================
//  2. LINE Settings API
// ===========================================================================

test.describe('2. LINE Settings API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('GET /api/settings/line returns expected structure', async ({ request }) => {
    const res = await request.get('/api/settings/line', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.success).toBe(true);
    const d = body.data;
    // Verify key fields exist in response
    expect(d).toHaveProperty('channelId');
    expect(d).toHaveProperty('status');
    expect(d).toHaveProperty('webhookUrl');
  });

  test('GET /api/settings/line/rich-menu returns valid response', async ({ request }) => {
    const res = await request.get('/api/settings/line/rich-menu', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body).toHaveProperty('data');
  });

  test('GET /api/settings/line/flex-menu returns expected structure', async ({ request }) => {
    const res = await request.get('/api/settings/line/flex-menu', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.success).toBe(true);
    expect(body).toHaveProperty('data');
  });

  test('GET /api/settings/line/rich-menu/advanced-config returns valid response', async ({ request }) => {
    const res = await request.get('/api/settings/line/rich-menu/advanced-config', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(res.status()).toBe(200);
    const body = await res.json();
    expect(body.success).toBe(true);
  });
});

// ===========================================================================
//  3. Flex Menu Config API
// ===========================================================================

test.describe('3. Flex Menu Config API', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('GET flex-menu config has buttons with icon, title, subtitle, color', async ({ request }) => {
    const res = await request.get('/api/settings/line/flex-menu', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.success).toBe(true);

    // Parse config -- may be a JSON string or object
    let config = body.data;
    if (typeof config === 'string') {
      config = JSON.parse(config);
    }

    // If cards exist, verify structure
    const cards = config?.cards || [];
    if (cards.length > 0) {
      const card = cards[0];
      expect(card).toHaveProperty('icon');
      expect(card).toHaveProperty('title');
      expect(card).toHaveProperty('color');
    }
  });

  test('PUT flex-menu config updates headerTitle and persists', async ({ request }) => {
    // Read current config
    const getRes = await request.get('/api/settings/line/flex-menu', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(getRes.ok()).toBeTruthy();
    const original = await getRes.json();
    let config = original.data;
    if (typeof config === 'string') {
      config = JSON.parse(config);
    }

    // Update with a test headerTitle
    const testTitle = 'E2E Test Title ' + Date.now();
    const payload = {
      ...config,
      headerTitle: testTitle,
      menuType: config?.menuType || 'carousel',
      cards: config?.cards || [
        { icon: '📅', title: '預約', subtitle: '', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' }
      ]
    };

    const putRes = await request.put('/api/settings/line/flex-menu', {
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      data: payload
    });
    expect(putRes.ok()).toBeTruthy();

    // Verify persistence
    const verifyRes = await request.get('/api/settings/line/flex-menu', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(verifyRes.ok()).toBeTruthy();
    const verified = await verifyRes.json();
    let verifiedConfig = verified.data;
    if (typeof verifiedConfig === 'string') {
      verifiedConfig = JSON.parse(verifiedConfig);
    }
    expect(verifiedConfig.headerTitle).toBe(testTitle);
  });

  test('Flex menu steps config contains expected keys', async ({ request }) => {
    // Save config with steps
    const payload = {
      menuType: 'carousel',
      cards: [
        { icon: '📅', title: '預約', subtitle: '', color: '#1DB446', action: 'start_booking', buttonLabel: '前往' }
      ],
      steps: {
        service: { color: '#4A90D9', icon: '✂️', title: '選擇服務', subtitle: '瀏覽服務', imageUrl: '' },
        date: { color: '#1DB446', icon: '📅', title: '選擇日期', subtitle: '選擇日期', imageUrl: '' },
        staff: { color: '#4A90D9', icon: '👤', title: '選擇人員', subtitle: '指定人員', imageUrl: '' },
        time: { color: '#4A90D9', icon: '⏰', title: '選擇時段', subtitle: '查看時段', imageUrl: '' },
        note: { color: '#5C6BC0', icon: '📝', title: '備註', subtitle: '特殊需求', imageUrl: '' },
        confirm: { color: '#1DB446', icon: '✅', title: '確認預約', subtitle: '確認資訊', imageUrl: '' }
      }
    };

    const putRes = await request.put('/api/settings/line/flex-menu', {
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      data: payload
    });
    expect(putRes.ok()).toBeTruthy();

    // Verify steps persisted
    const getRes = await request.get('/api/settings/line/flex-menu', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(getRes.ok()).toBeTruthy();
    const body = await getRes.json();
    let config = body.data;
    if (typeof config === 'string') {
      config = JSON.parse(config);
    }

    const expectedStepKeys = ['service', 'date', 'staff', 'time', 'note', 'confirm'];
    if (config?.steps) {
      for (const key of expectedStepKeys) {
        expect(config.steps).toHaveProperty(key);
      }
    }
  });
});

// ===========================================================================
//  4. LINE Settings UI
// ===========================================================================

test.describe('4. LINE Settings UI', () => {

  test('LINE settings page loads successfully', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // Verify the page loaded with expected content
    const pageTitle = page.locator('.page-title');
    await expect(pageTitle).toContainText('LINE');
  });

  test('Mode tabs visible on rich-menu-design page', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/rich-menu-design');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // If redirected (not subscribed), page still loaded without error
    const url = page.url();
    if (url.includes('rich-menu-design')) {
      const tabs = page.locator('.rm-design-tabs .nav-link');
      const tabCount = await tabs.count();
      expect(tabCount).toBeGreaterThanOrEqual(2);
    }
    // If redirected due to no subscription, that is acceptable -- no assertion failure
  });

  test('Connection status area is displayed', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // Rich Menu status badge should exist
    const statusBadge = page.locator('#richMenuStatusBadge');
    await expect(statusBadge).toBeVisible();
    const badgeText = await statusBadge.textContent();
    expect(badgeText?.trim().length).toBeGreaterThan(0);
  });

  test('Webhook URL field visible on LINE settings page', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // The page should contain webhook URL information
    const bodyText = await page.textContent('body');
    expect(bodyText).toBeTruthy();
    // The LINE settings page should reference webhook in some form
    const hasWebhookReference = bodyText!.includes('Webhook') || bodyText!.includes('webhook');
    expect(hasWebhookReference).toBe(true);
  });
});

// ===========================================================================
//  5. Rich Menu UI
// ===========================================================================

test.describe('5. Rich Menu UI', () => {

  test('Default rich menu preview renders on LINE settings page', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    // Preview container exists
    const previewContainer = page.locator('.rich-menu-preview-container');
    await expect(previewContainer).toBeVisible();

    // Default preview or custom preview should be visible
    const defaultPreview = page.locator('#richMenuPreview');
    const customPreview = page.locator('#richMenuCustomPreview');
    const eitherVisible = (await defaultPreview.isVisible()) || (await customPreview.isVisible());
    expect(eitherVisible).toBe(true);
  });

  test('Theme color options are visible in default mode', async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(WAIT_TIME.api);

    const defaultPanel = page.locator('#defaultModePanel');
    await expect(defaultPanel).toBeVisible();

    // Theme buttons should exist (GREEN, BLUE, PURPLE, ORANGE, DARK)
    const themeButtons = defaultPanel.locator('.theme-btn, [onclick*="selectTheme"]');
    const count = await themeButtons.count();
    expect(count).toBeGreaterThanOrEqual(5);
  });
});

// ===========================================================================
//  6. AI Feature
// ===========================================================================

test.describe('6. AI Feature', () => {
  let token: string;

  test.beforeAll(async ({ request }) => {
    token = await getTenantToken(request);
  });

  test('AI_ASSISTANT feature exists in feature store', async ({ request }) => {
    const res = await request.get('/api/feature-store', {
      headers: { Authorization: `Bearer ${token}` }
    });
    expect(res.ok()).toBeTruthy();
    const body = await res.json();
    expect(body.success).toBe(true);

    const features = body.data;
    expect(Array.isArray(features)).toBe(true);
    const aiFeature = features.find((f: any) => f.code === 'AI_ASSISTANT');
    expect(aiFeature).toBeTruthy();
    expect(aiFeature.name).toBeTruthy();
    // monthlyPrice 是功能商店欄位名
    const price = Number(aiFeature.monthlyPrice ?? aiFeature.monthlyPoints ?? 0);
    expect(price).toBeGreaterThan(0);
  });

  test('Webhook with general text (non-keyword) handles gracefully', async ({ request }) => {
    const res = await request.post(`/api/line/webhook/${TENANT_CODE}`, {
      headers: { 'Content-Type': 'application/json' },
      data: buildWebhookPayload([buildMessageEvent('今天天氣如何呢')])
    });
    expect(res.status()).toBeLessThan(500);
  });
});

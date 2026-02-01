import { defineConfig, devices } from '@playwright/test';

/**
 * Booking Platform E2E 測試配置
 *
 * 測試目標：https://booking-platform-production-1e08.up.railway.app
 */
export default defineConfig({
  testDir: './tests',
  fullyParallel: false, // 依序執行避免衝突
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: 1, // 單一 worker 避免併發問題
  reporter: [
    ['html', { open: 'never' }],
    ['list']
  ],

  use: {
    baseURL: 'https://booking-platform-production-1e08.up.railway.app',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    actionTimeout: 15000,
    navigationTimeout: 30000,
  },

  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
  ],

  // 測試超時
  timeout: 60000,
  expect: {
    timeout: 10000,
  },
});

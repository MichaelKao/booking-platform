import { test, expect } from '@playwright/test';
import { tenantLogin, WAIT_TIME } from './utils/test-helpers';

test.describe('行銷推播 UI 測試', () => {
  test('建立推播表單測試', async ({ page }) => {
    // 登入
    await tenantLogin(page);
    
    // 前往行銷推播頁面
    await page.goto('/tenant/marketing');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
    
    // 檢查頁面是否有 JavaScript 錯誤
    const errors: string[] = [];
    page.on('pageerror', (error) => {
      errors.push(error.message);
      console.log('頁面錯誤:', error.message);
    });
    
    // 點擊建立推播按鈕
    const createBtn = page.locator('button:has-text("建立推播")');
    await expect(createBtn).toBeVisible();
    await createBtn.click();
    await page.waitForTimeout(1000);
    
    // 確認 Modal 顯示
    const modal = page.locator('#formModal');
    await expect(modal).toBeVisible();
    
    // 檢查表單元素（使用 modal 內的選擇器）
    const titleInput = page.locator('#formModal #title');
    const contentInput = page.locator('#formModal textarea#pushContent, #formModal textarea#content').first();
    const targetTypeSelect = page.locator('#formModal #targetType');
    const saveBtn = page.locator('#formModal #saveBtn');

    console.log('標題輸入框存在:', await titleInput.isVisible());
    console.log('內容輸入框存在:', await contentInput.isVisible());
    console.log('目標類型選擇存在:', await targetTypeSelect.isVisible());
    console.log('儲存按鈕存在:', await saveBtn.isVisible());

    // 填寫表單
    if (await titleInput.isVisible()) {
      await titleInput.fill('Playwright 測試推播');
    }
    if (await contentInput.isVisible()) {
      await contentInput.fill('這是 Playwright 自動化測試建立的推播內容');
    }
    await targetTypeSelect.selectOption('ALL');
    
    console.log('表單已填寫完成');
    
    // 監聽 console.log
    page.on('console', (msg) => {
      if (msg.type() === 'log' || msg.type() === 'error') {
        console.log('瀏覽器 console:', msg.text());
      }
    });
    
    // 點擊儲存按鈕
    await saveBtn.click();
    console.log('已點擊儲存按鈕');
    
    // 等待回應
    await page.waitForTimeout(3000);
    
    // 檢查是否有錯誤
    if (errors.length > 0) {
      console.log('發現頁面錯誤:', errors);
    }
    
    // 檢查 Modal 是否關閉（成功的話會關閉）
    const modalStillVisible = await modal.isVisible();
    console.log('Modal 仍然可見:', modalStillVisible);
    
    // 檢查是否有 toast 訊息
    const toast = page.locator('.toast');
    const toastVisible = await toast.isVisible().catch(() => false);
    console.log('Toast 訊息可見:', toastVisible);
  });
});

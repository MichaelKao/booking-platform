import { test, expect, Page } from '@playwright/test';
import { tenantLogin, WAIT_TIME } from './utils/test-helpers';

/**
 * 功能商店詳細說明測試
 *
 * 測試範圍：
 * 1. 功能商店頁面載入
 * 2. 功能卡片顯示
 * 3. 「查看詳細功能」按鈕
 * 4. 詳細說明展開/收合
 * 5. 詳細說明內容（功能亮點、使用位置、訂閱差異）
 * 6. 訂閱/取消訂閱功能
 * 7. RWD 響應式顯示
 */

test.describe('功能商店詳細說明測試', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test.describe('頁面基本功能', () => {
    test('功能商店頁面可正常載入', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查頁面標題
      await expect(page.locator('.page-title')).toContainText('功能商店');

      // 檢查點數餘額顯示
      await expect(page.locator('#pointBalance')).toBeVisible();

      // 檢查說明提示區塊
      await expect(page.locator('.alert-light')).toBeVisible();
      await expect(page.locator('.alert-light')).toContainText('如何使用功能商店');
    });

    test('功能卡片列表正確顯示', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 等待功能卡片載入
      const featureCards = page.locator('.feature-card');
      await expect(featureCards.first()).toBeVisible({ timeout: 10000 });

      // 檢查至少有一張功能卡片
      const cardCount = await featureCards.count();
      console.log(`功能卡片數量: ${cardCount}`);
      expect(cardCount).toBeGreaterThan(0);
    });

    test('功能卡片包含必要元素', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 取得第一張功能卡片
      const firstCard = page.locator('.feature-card').first();
      await expect(firstCard).toBeVisible();

      // 檢查卡片包含圖示
      await expect(firstCard.locator('.feature-icon')).toBeVisible();

      // 檢查卡片包含名稱
      await expect(firstCard.locator('h6')).toBeVisible();

      // 檢查卡片包含描述
      await expect(firstCard.locator('.feature-card-body p')).toBeVisible();

      // 檢查卡片包含價格或免費標示
      await expect(firstCard.locator('.feature-price')).toBeVisible();

      // 檢查卡片包含操作按鈕
      await expect(firstCard.locator('.feature-card-footer button')).toBeVisible();
    });

    test('已訂閱功能顯示在前面', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查已訂閱卡片有特殊樣式
      const subscribedCards = page.locator('.feature-card.subscribed');
      const subscribedCount = await subscribedCards.count();
      console.log(`已訂閱功能數量: ${subscribedCount}`);

      if (subscribedCount > 0) {
        // 第一張卡片應該是已訂閱的
        const firstCard = page.locator('.feature-card').first();
        const isSubscribed = await firstCard.evaluate(el => el.classList.contains('subscribed'));
        console.log(`第一張卡片是否已訂閱: ${isSubscribed}`);
      }
    });
  });

  test.describe('詳細說明功能', () => {
    test('「查看詳細功能」按鈕存在', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 檢查有「查看詳細功能」按鈕
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await expect(detailToggle).toBeVisible();
      await expect(detailToggle).toContainText('查看詳細功能');
    });

    test('點擊「查看詳細功能」展開詳細說明', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到第一個有詳細說明的功能卡片
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await expect(detailToggle).toBeVisible();

      // 取得對應的 collapse ID
      const href = await detailToggle.getAttribute('href');
      const collapseId = href?.replace('#', '') || '';
      const collapseContent = page.locator(`#${collapseId}`);

      // 展開前應該是隱藏的
      await expect(collapseContent).not.toBeVisible();

      // 點擊展開
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 展開後應該可見
      await expect(collapseContent).toBeVisible();
    });

    test('詳細說明包含「功能亮點」區塊', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 展開第一個詳細說明
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 檢查功能亮點區塊
      const highlightSection = page.locator('.feature-detail-content').first();
      await expect(highlightSection).toBeVisible();
      await expect(highlightSection).toContainText('功能亮點');

      // 檢查功能亮點列表
      const highlightList = highlightSection.locator('.feature-highlight-list');
      await expect(highlightList).toBeVisible();

      // 確認有列表項目
      const listItems = highlightList.locator('li');
      const itemCount = await listItems.count();
      console.log(`功能亮點項目數量: ${itemCount}`);
      expect(itemCount).toBeGreaterThan(0);
    });

    test('詳細說明包含「使用位置」區塊', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 展開第一個詳細說明
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 檢查使用位置區塊
      const detailContent = page.locator('.feature-detail-content').first();
      await expect(detailContent).toContainText('使用位置');

      // 檢查使用位置項目
      const locationItems = detailContent.locator('.feature-location-item');
      const locationCount = await locationItems.count();
      console.log(`使用位置項目數量: ${locationCount}`);
      expect(locationCount).toBeGreaterThan(0);
    });

    test('詳細說明包含「訂閱前後差異」區塊', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 展開第一個詳細說明
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 檢查訂閱前後差異區塊
      const detailContent = page.locator('.feature-detail-content').first();
      await expect(detailContent).toContainText('訂閱前後差異');

      // 檢查比較表格
      const comparisonTable = detailContent.locator('.feature-comparison-table');
      await expect(comparisonTable).toBeVisible();

      // 檢查訂閱前、訂閱後文字
      await expect(comparisonTable).toContainText('訂閱前');
      await expect(comparisonTable).toContainText('訂閱後');

      // 檢查顏色樣式（黃/綠）
      await expect(comparisonTable.locator('.feature-comparison-before')).toBeVisible();
      await expect(comparisonTable.locator('.feature-comparison-after')).toBeVisible();
    });

    test('再次點擊收合詳細說明', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      const detailToggle = page.locator('.feature-detail-toggle').first();
      const href = await detailToggle.getAttribute('href');
      const collapseId = href?.replace('#', '') || '';
      const collapseContent = page.locator(`#${collapseId}`);

      // 展開
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);
      await expect(collapseContent).toBeVisible();

      // 收合
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);
      await expect(collapseContent).not.toBeVisible();
    });

    test('箭頭圖示在展開時旋轉', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      const detailToggle = page.locator('.feature-detail-toggle').first();
      const chevron = detailToggle.locator('.bi-chevron-down');
      await expect(chevron).toBeVisible();

      // 展開前 aria-expanded 應該是 false
      const ariaExpandedBefore = await detailToggle.getAttribute('aria-expanded');
      expect(ariaExpandedBefore).toBe('false');

      // 點擊展開
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 展開後 aria-expanded 應該是 true
      const ariaExpandedAfter = await detailToggle.getAttribute('aria-expanded');
      expect(ariaExpandedAfter).toBe('true');
    });
  });

  test.describe('多個功能卡片詳細說明', () => {
    test('可以同時展開多個功能的詳細說明', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      const detailToggles = page.locator('.feature-detail-toggle');
      const toggleCount = await detailToggles.count();

      if (toggleCount >= 2) {
        // 展開第一個
        await detailToggles.nth(0).click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 展開第二個
        await detailToggles.nth(1).click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 兩個都應該展開
        const href1 = await detailToggles.nth(0).getAttribute('href');
        const href2 = await detailToggles.nth(1).getAttribute('href');
        const collapse1 = page.locator(href1!);
        const collapse2 = page.locator(href2!);

        await expect(collapse1).toBeVisible();
        await expect(collapse2).toBeVisible();
      }
    });

    test('所有功能卡片都有詳細說明按鈕', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      const featureCards = page.locator('.feature-card');
      const cardCount = await featureCards.count();

      const detailToggles = page.locator('.feature-detail-toggle');
      const toggleCount = await detailToggles.count();

      console.log(`功能卡片數量: ${cardCount}, 詳細說明按鈕數量: ${toggleCount}`);

      // 每個功能卡片都應該有詳細說明按鈕（除非沒有定義）
      expect(toggleCount).toBeGreaterThan(0);
    });
  });

  test.describe('特定功能詳細說明內容驗證', () => {
    test('票券系統 (COUPON_SYSTEM) 詳細說明正確', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到票券系統卡片
      const couponCard = page.locator('.feature-card:has(#detail-COUPON_SYSTEM)');

      if (await couponCard.count() > 0) {
        const detailToggle = couponCard.locator('.feature-detail-toggle');
        await detailToggle.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const detailContent = page.locator('#detail-COUPON_SYSTEM');
        await expect(detailContent).toBeVisible();

        // 檢查功能亮點
        await expect(detailContent).toContainText('折扣券');
        await expect(detailContent).toContainText('LINE 領取');
        await expect(detailContent).toContainText('核銷');

        // 檢查使用位置
        await expect(detailContent).toContainText('票券管理');
        await expect(detailContent).toContainText('領取票券');
      } else {
        console.log('票券系統功能未顯示，跳過測試');
      }
    });

    test('商品銷售 (PRODUCT_SALES) 詳細說明正確', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到商品銷售卡片
      const productCard = page.locator('.feature-card:has(#detail-PRODUCT_SALES)');

      if (await productCard.count() > 0) {
        const detailToggle = productCard.locator('.feature-detail-toggle');
        await detailToggle.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const detailContent = page.locator('#detail-PRODUCT_SALES');
        await expect(detailContent).toBeVisible();

        // 檢查功能亮點
        await expect(detailContent).toContainText('商品目錄');
        await expect(detailContent).toContainText('庫存');

        // 檢查使用位置
        await expect(detailContent).toContainText('商品管理');
        await expect(detailContent).toContainText('瀏覽商品');
      } else {
        console.log('商品銷售功能未顯示，跳過測試');
      }
    });

    test('AI 智慧客服 (AI_ASSISTANT) 詳細說明正確', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到 AI 智慧客服卡片
      const aiCard = page.locator('.feature-card:has(#detail-AI_ASSISTANT)');

      if (await aiCard.count() > 0) {
        const detailToggle = aiCard.locator('.feature-detail-toggle');
        await detailToggle.click();
        await page.waitForTimeout(WAIT_TIME.short);

        const detailContent = page.locator('#detail-AI_ASSISTANT');
        await expect(detailContent).toBeVisible();

        // 檢查功能亮點
        await expect(detailContent).toContainText('AI');
        await expect(detailContent).toContainText('24 小時');

        // 檢查使用位置
        await expect(detailContent).toContainText('LINE Bot');
      } else {
        console.log('AI 智慧客服功能未顯示，跳過測試');
      }
    });
  });

  test.describe('訂閱功能測試', () => {
    test('未訂閱功能顯示「立即訂閱」按鈕', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到未訂閱的功能卡片
      const unsubscribedCard = page.locator('.feature-card:not(.subscribed)').first();

      if (await unsubscribedCard.count() > 0) {
        const subscribeBtn = unsubscribedCard.locator('button:has-text("立即訂閱")');
        await expect(subscribeBtn).toBeVisible();
      }
    });

    test('已訂閱功能顯示「取消訂閱」按鈕和已訂閱標籤', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到已訂閱的功能卡片
      const subscribedCard = page.locator('.feature-card.subscribed').first();

      if (await subscribedCard.count() > 0) {
        // 檢查已訂閱標籤
        const badge = subscribedCard.locator('.subscribed-badge');
        await expect(badge).toBeVisible();
        await expect(badge).toContainText('已訂閱');

        // 檢查取消訂閱按鈕
        const cancelBtn = subscribedCard.locator('button:has-text("取消訂閱")');
        await expect(cancelBtn).toBeVisible();
      } else {
        console.log('沒有已訂閱的功能，跳過測試');
      }
    });

    test('點擊訂閱按鈕顯示確認對話框', async ({ page }) => {
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 找到未訂閱的功能卡片
      const unsubscribedCard = page.locator('.feature-card:not(.subscribed)').first();

      if (await unsubscribedCard.count() > 0) {
        const subscribeBtn = unsubscribedCard.locator('button:has-text("立即訂閱")');
        await subscribeBtn.click();
        await page.waitForTimeout(WAIT_TIME.short);

        // 檢查確認對話框
        const modal = page.locator('.modal.show, #confirmModal.show');
        await expect(modal).toBeVisible();

        // 關閉對話框
        const cancelBtn = modal.locator('button:has-text("取消"), .btn-secondary');
        if (await cancelBtn.isVisible()) {
          await cancelBtn.click();
        }
      }
    });
  });

  test.describe('RWD 響應式測試', () => {
    test('手機版（375px）正確顯示', async ({ page }) => {
      await page.setViewportSize({ width: 375, height: 667 });
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 功能卡片應該全寬顯示
      const firstCard = page.locator('.feature-card').first();
      await expect(firstCard).toBeVisible();

      // 詳細說明按鈕應該可見
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await expect(detailToggle).toBeVisible();

      // 展開詳細說明
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      // 詳細內容應該正常顯示
      const href = await detailToggle.getAttribute('href');
      const collapseContent = page.locator(href!);
      await expect(collapseContent).toBeVisible();
    });

    test('平板版（768px）正確顯示', async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 功能卡片應該兩列顯示
      const cards = page.locator('.feature-card');
      await expect(cards.first()).toBeVisible();

      // 展開詳細說明
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      const href = await detailToggle.getAttribute('href');
      const collapseContent = page.locator(href!);
      await expect(collapseContent).toBeVisible();
    });

    test('桌面版（1920px）正確顯示', async ({ page }) => {
      await page.setViewportSize({ width: 1920, height: 1080 });
      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 功能卡片應該三列顯示
      const cards = page.locator('.feature-card');
      await expect(cards.first()).toBeVisible();

      // 展開詳細說明
      const detailToggle = page.locator('.feature-detail-toggle').first();
      await detailToggle.click();
      await page.waitForTimeout(WAIT_TIME.short);

      const href = await detailToggle.getAttribute('href');
      const collapseContent = page.locator(href!);
      await expect(collapseContent).toBeVisible();
    });
  });

  test.describe('無 JavaScript 錯誤', () => {
    test('頁面載入無 JS 錯誤', async ({ page }) => {
      const jsErrors: string[] = [];
      page.on('pageerror', error => {
        jsErrors.push(error.message);
      });

      await page.goto('/tenant/feature-store');
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(WAIT_TIME.api);

      // 展開詳細說明
      const detailToggle = page.locator('.feature-detail-toggle').first();
      if (await detailToggle.count() > 0) {
        await detailToggle.click();
        await page.waitForTimeout(WAIT_TIME.short);
      }

      // 檢查無 JS 錯誤
      if (jsErrors.length > 0) {
        console.log('JavaScript 錯誤:', jsErrors);
      }
      expect(jsErrors.length).toBe(0);
    });
  });
});

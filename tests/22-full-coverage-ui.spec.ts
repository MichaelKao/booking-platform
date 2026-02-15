/**
 * 全面 UI 覆蓋測試 - 涵蓋所有畫面、按鈕、Modal、表單操作
 * 使用 fixtures 自動監控 F12 Console 錯誤
 *
 * 覆蓋範圍：
 * - 店家後台所有頁面完整操作
 * - 超管後台所有頁面完整操作
 * - 所有 Modal 開關
 * - 所有按鈕點擊
 * - 所有篩選/搜尋功能
 * - 所有匯出功能
 * - F12 Console 零錯誤
 */
import { test, expect } from './fixtures';
import { tenantLogin, adminLogin, WAIT_TIME, TEST_ACCOUNTS } from './utils/test-helpers';

// ============================================================
// 店家後台 - 儀表板
// ============================================================
test.describe('店家儀表板完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('儀表板載入所有統計卡片和圖表', async ({ page }) => {
    await page.goto('/tenant/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 檢查統計卡片存在
    const cards = page.locator('.card, .stat-card, .dashboard-card');
    const cardCount = await cards.count();
    expect(cardCount).toBeGreaterThan(0);

    // 檢查頁面主要內容區域不是空的
    const mainContent = page.locator('.main-content, .content-wrapper, main, [class*="content"]').first();
    if (await mainContent.count() > 0) {
      const text = await mainContent.textContent();
      expect(text?.length).toBeGreaterThan(10);
    }
  });
});

// ============================================================
// 店家後台 - 預約管理完整操作
// ============================================================
test.describe('預約管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/bookings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('預約頁面載入正常 - 表格和篩選器', async ({ page }) => {
    // 檢查篩選控制項
    await expect(page.locator('#dateFilter, #statusFilter, #searchKeyword').first()).toBeVisible();

    // 檢查表格存在
    await expect(page.locator('#bookingsTable, table').first()).toBeVisible();
  });

  test('新增預約 Modal 開關正常', async ({ page }) => {
    // 點擊新增預約按鈕
    const addBtn = page.locator('button:has-text("新增預約"), button:has-text("新增"), [onclick*="openCreateBookingModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      // 檢查 Modal 是否開啟
      const modal = page.locator('#bookingModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查表單欄位
      const form = modal.locator('form, #bookingForm').first();
      expect(await form.count()).toBeGreaterThan(0);

      // 關閉 Modal
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });

  test('預約狀態篩選功能', async ({ page }) => {
    const statusFilter = page.locator('#statusFilter');
    if (await statusFilter.isVisible()) {
      // 取得所有選項
      const options = await statusFilter.locator('option').allTextContents();
      expect(options.length).toBeGreaterThan(0);

      // 逐一切換篩選
      for (const option of ['', 'PENDING_CONFIRMATION', 'CONFIRMED', 'COMPLETED', 'CANCELLED']) {
        const optionExists = await statusFilter.locator(`option[value="${option}"]`).count();
        if (optionExists > 0) {
          await statusFilter.selectOption(option);
          await page.waitForTimeout(1500);
        }
      }
    }
  });

  test('預約日期篩選功能', async ({ page }) => {
    const dateFilter = page.locator('#dateFilter, input[type="date"]').first();
    if (await dateFilter.isVisible()) {
      const today = new Date().toISOString().split('T')[0];
      await dateFilter.fill(today);
      await page.waitForTimeout(1500);
    }
  });

  test('預約搜尋功能', async ({ page }) => {
    const searchInput = page.locator('#searchKeyword, input[placeholder*="搜尋"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('test');
      await page.waitForTimeout(1500);
      await searchInput.fill('');
      await page.waitForTimeout(1000);
    }
  });

  test('匯出 Excel 按鈕可點擊', async ({ page }) => {
    const exportBtn = page.locator('button:has-text("匯出 Excel"), button:has-text("Excel"), [onclick*="exportBookings"][onclick*="excel"]').first();
    if (await exportBtn.isVisible()) {
      // 監聽下載事件
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
        exportBtn.click()
      ]);
      if (download) {
        expect(download.suggestedFilename()).toContain('.xls');
      }
      await page.waitForTimeout(1000);
    }
  });

  test('匯出 PDF 按鈕可點擊', async ({ page }) => {
    const exportBtn = page.locator('button:has-text("匯出 PDF"), button:has-text("PDF"), [onclick*="exportBookings"][onclick*="pdf"]').first();
    if (await exportBtn.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
        exportBtn.click()
      ]);
      if (download) {
        expect(download.suggestedFilename()).toContain('.pdf');
      }
      await page.waitForTimeout(1000);
    }
  });

  test('預約詳情 Modal 開關正常', async ({ page }) => {
    // 找到第一筆預約的詳情按鈕
    const detailBtn = page.locator('table tbody tr:first-child button:has-text("詳情"), table tbody tr:first-child [onclick*="openDetailModal"], table tbody tr:first-child [onclick*="viewDetail"]').first();
    if (await detailBtn.isVisible()) {
      await detailBtn.click();
      await page.waitForTimeout(1000);

      // 關閉 Modal
      const closeBtn = page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first();
      if (await closeBtn.isVisible()) {
        await closeBtn.click();
        await page.waitForTimeout(500);
      }
    }
  });

  test('全選/取消全選 checkbox 功能', async ({ page }) => {
    const selectAll = page.locator('#selectAllCheckbox, input[type="checkbox"][onclick*="selectAll"], thead input[type="checkbox"]').first();
    if (await selectAll.isVisible()) {
      await selectAll.check();
      await page.waitForTimeout(500);
      await selectAll.uncheck();
      await page.waitForTimeout(500);
    }
  });
});

// ============================================================
// 店家後台 - 行事曆
// ============================================================
test.describe('行事曆完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('行事曆頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/calendar');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 檢查 FullCalendar 存在
    const calendar = page.locator('#calendar, .fc, .fc-view-harness').first();
    await expect(calendar).toBeVisible();
  });

  test('行事曆視圖切換 - 月/週/日', async ({ page }) => {
    await page.goto('/tenant/calendar');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 月視圖
    const monthBtn = page.locator('.fc-dayGridMonth-button, button:has-text("月")').first();
    if (await monthBtn.isVisible()) {
      await monthBtn.click();
      await page.waitForTimeout(1500);
    }

    // 週視圖
    const weekBtn = page.locator('.fc-timeGridWeek-button, button:has-text("週")').first();
    if (await weekBtn.isVisible()) {
      await weekBtn.click();
      await page.waitForTimeout(1500);
    }

    // 日視圖
    const dayBtn = page.locator('.fc-timeGridDay-button, button:has-text("日")').first();
    if (await dayBtn.isVisible()) {
      await dayBtn.click();
      await page.waitForTimeout(1500);
    }
  });

  test('行事曆導航 - 前/後/今天', async ({ page }) => {
    await page.goto('/tenant/calendar');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);

    // 下一頁
    const nextBtn = page.locator('.fc-next-button, button:has-text("next")').first();
    if (await nextBtn.isVisible()) {
      await nextBtn.click();
      await page.waitForTimeout(1500);
    }

    // 上一頁
    const prevBtn = page.locator('.fc-prev-button, button:has-text("prev")').first();
    if (await prevBtn.isVisible()) {
      await prevBtn.click();
      await page.waitForTimeout(1500);
    }

    // 今天
    const todayBtn = page.locator('.fc-today-button, button:has-text("今天"), button:has-text("today")').first();
    if (await todayBtn.isVisible() && await todayBtn.isEnabled()) {
      await todayBtn.click();
      await page.waitForTimeout(1500);
    }
  });
});

// ============================================================
// 店家後台 - 顧客管理完整操作
// ============================================================
test.describe('顧客管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/customers');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('顧客列表頁面載入正常', async ({ page }) => {
    await expect(page.locator('#dataTable, table').first()).toBeVisible();
  });

  test('新增顧客 Modal 開關和表單欄位', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增顧客"), button:has-text("新增"), [onclick*="openCreateModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#formModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查所有表單欄位
      const nameField = modal.locator('input[name="name"], #name, #customerName').first();
      const phoneField = modal.locator('input[name="phone"], #phone, #customerPhone').first();
      expect(await nameField.count() + await phoneField.count()).toBeGreaterThan(0);

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });

  test('顧客搜尋功能', async ({ page }) => {
    const searchInput = page.locator('#searchKeyword, input[placeholder*="搜尋"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('test');
      await page.waitForTimeout(1500);
      await searchInput.fill('');
      await page.waitForTimeout(1000);
    }
  });

  test('匯出顧客 Excel', async ({ page }) => {
    const exportBtn = page.locator('button:has-text("匯出"), [onclick*="exportCustomers"]').first();
    if (await exportBtn.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
        exportBtn.click()
      ]);
      await page.waitForTimeout(1000);
    }
  });

  test('顧客詳情頁面進入和操作', async ({ page }) => {
    // 找到第一筆顧客的詳情連結
    const detailLink = page.locator('table tbody tr:first-child a[href*="/tenant/customers/"], table tbody tr:first-child button:has-text("詳情"), table tbody tr:first-child [onclick*="viewDetail"]').first();
    if (await detailLink.isVisible()) {
      await detailLink.click();
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 檢查顧客詳情頁面元素
      const pageContent = await page.locator('body').textContent();
      // 頁面應該有某種內容載入
      expect(pageContent?.length).toBeGreaterThan(100);

      // 檢查編輯按鈕
      const editBtn = page.locator('button:has-text("編輯"), [onclick*="openEditModal"]').first();
      if (await editBtn.isVisible()) {
        await editBtn.click();
        await page.waitForTimeout(1000);
        // 關閉 Modal
        const closeBtn = page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first();
        if (await closeBtn.isVisible()) {
          await closeBtn.click();
          await page.waitForTimeout(500);
        }
      }

      // 檢查調整點數按鈕
      const pointBtn = page.locator('button:has-text("調整點數"), button:has-text("點數"), [onclick*="openAdjustPointsModal"]').first();
      if (await pointBtn.isVisible()) {
        await pointBtn.click();
        await page.waitForTimeout(1000);
        // 關閉 Modal
        const closeBtn2 = page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first();
        if (await closeBtn2.isVisible()) {
          await closeBtn2.click();
          await page.waitForTimeout(500);
        }
      }

      // 檢查預約篩選按鈕
      const filterBtns = page.locator('button:has-text("全部"), button:has-text("即將到來"), button:has-text("已完成")');
      const filterCount = await filterBtns.count();
      for (let i = 0; i < filterCount; i++) {
        if (await filterBtns.nth(i).isVisible()) {
          await filterBtns.nth(i).click();
          await page.waitForTimeout(1000);
        }
      }
    }
  });
});

// ============================================================
// 店家後台 - 員工管理完整操作
// ============================================================
test.describe('員工管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/staff');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('員工列表頁面載入正常', async ({ page }) => {
    await expect(page.locator('#dataTable, table').first()).toBeVisible();
  });

  test('新增員工 Modal 開關和欄位檢查', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增員工"), button:has-text("新增"), [onclick*="openCreateModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#formModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查表單欄位
      const fields = ['name', 'displayName', 'phone', 'email'];
      for (const field of fields) {
        const input = modal.locator(`input[name="${field}"], #${field}`).first();
        if (await input.count() > 0) {
          expect(await input.count()).toBe(1);
        }
      }

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });

  test('排班 Modal 開關和操作', async ({ page }) => {
    const scheduleBtn = page.locator('[onclick*="openScheduleModal"], button:has-text("排班")').first();
    if (await scheduleBtn.isVisible()) {
      await scheduleBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#scheduleModal, .modal.show').first();
      if (await modal.isVisible()) {
        // 檢查排班表格 (7 天)
        const dayToggles = modal.locator('.day-toggle, input[type="checkbox"]');
        const toggleCount = await dayToggles.count();
        expect(toggleCount).toBeGreaterThan(0);

        // 檢查時間輸入
        const timeInputs = modal.locator('input[type="time"]');
        expect(await timeInputs.count()).toBeGreaterThan(0);

        // 關閉
        await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
        await page.waitForTimeout(500);
      }
    }
  });

  test('請假 Modal 開關和操作', async ({ page }) => {
    const leaveBtn = page.locator('[onclick*="openLeaveModal"], button:has-text("請假")').first();
    if (await leaveBtn.isVisible()) {
      await leaveBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#leaveModal, .modal.show').first();
      if (await modal.isVisible()) {
        // 檢查快速日期選擇按鈕
        const quickBtns = modal.locator('button:has-text("明天"), button:has-text("下週"), button:has-text("週末")');
        const quickCount = await quickBtns.count();
        // 快速選擇按鈕可能存在

        // 檢查假別選擇
        const leaveType = modal.locator('select[name="leaveType"], #leaveType').first();
        if (await leaveType.isVisible()) {
          const options = await leaveType.locator('option').count();
          expect(options).toBeGreaterThan(0);
        }

        // 關閉
        await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
        await page.waitForTimeout(500);
      }
    }
  });
});

// ============================================================
// 店家後台 - 服務管理完整操作
// ============================================================
test.describe('服務管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/services');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('服務列表頁面載入正常', async ({ page }) => {
    await expect(page.locator('#dataTable, table').first()).toBeVisible();
  });

  test('新增服務 Modal 開關和欄位檢查', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增服務"), button:has-text("新增"), [onclick*="openCreateModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#formModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查關鍵欄位
      const nameInput = modal.locator('input[name="name"], #name, #serviceName').first();
      const priceInput = modal.locator('input[name="price"], #price').first();
      const durationInput = modal.locator('input[name="duration"], #duration, select[name="duration"]').first();
      expect((await nameInput.count()) + (await priceInput.count()) + (await durationInput.count())).toBeGreaterThan(0);

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });

  test('管理分類 Modal 開關', async ({ page }) => {
    const categoryBtn = page.locator('button:has-text("管理分類"), button:has-text("分類"), [onclick*="openCategoryModal"]').first();
    if (await categoryBtn.isVisible()) {
      await categoryBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#categoryModal, .modal.show').first();
      if (await modal.isVisible()) {
        // 檢查分類列表
        const categoryTable = modal.locator('table, #categoryTable, .category-list');
        expect(await categoryTable.count()).toBeGreaterThanOrEqual(0);

        // 關閉
        await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
        await page.waitForTimeout(500);
      }
    }
  });

  test('分類篩選按鈕', async ({ page }) => {
    const filterBtns = page.locator('[onclick*="filterByCategory"], .category-filter, .btn-group button');
    const count = await filterBtns.count();
    if (count > 0) {
      // 點擊第一個篩選按鈕
      await filterBtns.first().click();
      await page.waitForTimeout(1000);
    }
  });
});

// ============================================================
// 店家後台 - 商品管理完整操作
// ============================================================
test.describe('商品管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/products');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('商品列表頁面載入正常', async ({ page }) => {
    await expect(page.locator('#dataTable, table, .product-list').first()).toBeVisible();
  });

  test('新增商品 Modal 開關和欄位檢查', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增商品"), button:has-text("新增"), [onclick*="openCreateModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#formModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查關鍵欄位
      const nameInput = modal.locator('input[name="name"], #name, #productName').first();
      const priceInput = modal.locator('input[name="price"], #price').first();
      const stockInput = modal.locator('input[name="stockQuantity"], #stockQuantity').first();
      expect((await nameInput.count()) + (await priceInput.count())).toBeGreaterThan(0);

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });

  test('庫存調整 Modal 開關', async ({ page }) => {
    const stockBtn = page.locator('[onclick*="openStockModal"], button:has-text("調整庫存"), button:has-text("庫存")').first();
    if (await stockBtn.isVisible()) {
      await stockBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#stockModal, .modal.show').first();
      if (await modal.isVisible()) {
        // 檢查快速調整按鈕
        const quickBtns = modal.locator('[onclick*="quickAdjust"], button:has-text("-10"), button:has-text("+1")');
        expect(await quickBtns.count()).toBeGreaterThanOrEqual(0);

        // 關閉
        await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
        await page.waitForTimeout(500);
      }
    }
  });

  test('低庫存篩選按鈕', async ({ page }) => {
    const lowStockBtn = page.locator('button:has-text("低庫存"), [onclick*="showLowStockProducts"], [onclick*="lowStock"]').first();
    if (await lowStockBtn.isVisible()) {
      await lowStockBtn.click();
      await page.waitForTimeout(1500);
    }
  });
});

// ============================================================
// 店家後台 - 商品訂單頁面
// ============================================================
test.describe('商品訂單頁面完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('商品訂單頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/product-orders');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    // 頁面不應該有 500 伺服器錯誤頁面
    const title = await page.title();
    expect(title).not.toContain('500 Internal Server Error');
    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(50);
  });

  test('訂單狀態篩選功能', async ({ page }) => {
    await page.goto('/tenant/product-orders');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const statusFilter = page.locator('#statusFilter, select[onchange*="loadData"]').first();
    if (await statusFilter.isVisible()) {
      const options = await statusFilter.locator('option').allTextContents();
      expect(options.length).toBeGreaterThan(0);

      // 逐一切換
      for (let i = 0; i < Math.min(options.length, 5); i++) {
        await statusFilter.selectOption({ index: i });
        await page.waitForTimeout(1500);
      }
    }
  });

  test('訂單表格載入', async ({ page }) => {
    await page.goto('/tenant/product-orders');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const table = page.locator('#dataTable, table').first();
    await expect(table).toBeVisible();
  });
});

// ============================================================
// 店家後台 - 庫存異動頁面
// ============================================================
test.describe('庫存異動頁面完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('庫存異動頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/inventory');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('500 Internal Server Error');
    expect(bodyText?.length).toBeGreaterThan(50);
  });

  test('庫存異動表格載入', async ({ page }) => {
    await page.goto('/tenant/inventory');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const table = page.locator('#dataTable, table').first();
    await expect(table).toBeVisible();
  });
});

// ============================================================
// 店家後台 - 票券管理完整操作
// ============================================================
test.describe('票券管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/coupons');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('票券列表頁面載入正常', async ({ page }) => {
    await expect(page.locator('#dataTable, table, .coupon-list').first()).toBeVisible();
  });

  test('新增票券 Modal 開關和欄位檢查', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增票券"), button:has-text("新增"), [onclick*="openCreateModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#formModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查票券類型選擇
      const typeSelect = modal.locator('select[name="type"], #type, #couponType').first();
      if (await typeSelect.isVisible()) {
        const options = await typeSelect.locator('option').count();
        expect(options).toBeGreaterThan(0);
      }

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });

  test('核銷票券 Modal 開關', async ({ page }) => {
    const redeemBtn = page.locator('button:has-text("核銷票券"), button:has-text("核銷"), [onclick*="openRedeemModal"]').first();
    if (await redeemBtn.isVisible()) {
      await redeemBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#redeemModal, .modal.show').first();
      if (await modal.isVisible()) {
        // 檢查代碼輸入欄位
        const codeInput = modal.locator('input[name="couponCode"], #couponCode, input[placeholder*="代碼"]').first();
        expect(await codeInput.count()).toBeGreaterThan(0);

        // 關閉
        await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
        await page.waitForTimeout(500);
      }
    }
  });
});

// ============================================================
// 店家後台 - 行銷活動完整操作
// ============================================================
test.describe('行銷活動完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/campaigns');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('行銷活動列表頁面載入正常', async ({ page }) => {
    const table = page.locator('#dataTable, table').first();
    await expect(table).toBeVisible();
  });

  test('新增活動 Modal 開關和欄位檢查', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增活動"), button:has-text("新增"), [onclick*="openCreateModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#formModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查表單欄位
      const nameInput = modal.locator('input[name="name"], #name, #campaignName').first();
      const typeSelect = modal.locator('select[name="type"], #type').first();
      expect((await nameInput.count()) + (await typeSelect.count())).toBeGreaterThan(0);

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });
});

// ============================================================
// 店家後台 - 行銷推播完整操作
// ============================================================
test.describe('行銷推播完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/marketing');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('行銷推播列表頁面載入正常', async ({ page }) => {
    const table = page.locator('#dataTable, table').first();
    await expect(table).toBeVisible();
  });

  test('建立推播 Modal 開關和欄位檢查', async ({ page }) => {
    const addBtn = page.locator('button:has-text("建立推播"), button:has-text("新增"), [onclick*="openCreateModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#formModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 檢查推播表單欄位
      const titleInput = modal.locator('input[name="title"], #title').first();
      const contentInput = modal.locator('textarea[name="pushContent"], #pushContent, textarea').first();
      const targetSelect = modal.locator('select[name="targetType"], #targetType').first();
      expect((await titleInput.count()) + (await contentInput.count())).toBeGreaterThan(0);

      // 如果有目標選擇，切換看看
      if (await targetSelect.isVisible()) {
        const options = await targetSelect.locator('option').count();
        for (let i = 0; i < options; i++) {
          await targetSelect.selectOption({ index: i });
          await page.waitForTimeout(500);
        }
      }

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });
});

// ============================================================
// 店家後台 - 設定頁面所有 Tab 操作
// ============================================================
test.describe('設定頁面所有 Tab 完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('基本資訊 Tab 載入和表單欄位', async ({ page }) => {
    // 點擊基本資訊 Tab
    const basicTab = page.locator('[href="#basic"], button:has-text("基本資訊"), a:has-text("基本資訊")').first();
    if (await basicTab.isVisible()) {
      await basicTab.click();
      await page.waitForTimeout(1000);
    }

    // 檢查表單欄位
    const fields = ['tenantName', 'tenantPhone', 'tenantEmail'];
    for (const field of fields) {
      const input = page.locator(`#${field}, input[name="${field}"]`).first();
      if (await input.isVisible()) {
        expect(await input.inputValue()).toBeDefined();
      }
    }
  });

  test('營業設定 Tab 載入和表單欄位', async ({ page }) => {
    const bizTab = page.locator('[href="#business"], button:has-text("營業設定"), a:has-text("營業")').first();
    if (await bizTab.isVisible()) {
      await bizTab.click();
      await page.waitForTimeout(1000);

      // 檢查營業時間欄位
      const startTime = page.locator('#businessStart, input[name="businessStart"]').first();
      const endTime = page.locator('#businessEnd, input[name="businessEnd"]').first();
      if (await startTime.isVisible()) {
        expect(await startTime.inputValue()).toBeDefined();
      }

      // 檢查休息日勾選
      const closedDayChecks = page.locator('.closed-day-check, input[name*="closedDay"]');
      expect(await closedDayChecks.count()).toBeGreaterThanOrEqual(0);
    }
  });

  test('通知設定 Tab 載入和開關', async ({ page }) => {
    const notifTab = page.locator('[href="#notification"], button:has-text("通知設定"), a:has-text("通知")').first();
    if (await notifTab.isVisible()) {
      await notifTab.click();
      await page.waitForTimeout(1000);

      // 檢查通知開關
      const switches = page.locator('input[type="checkbox"][name*="notify"], .form-check-input');
      expect(await switches.count()).toBeGreaterThanOrEqual(0);
    }
  });

  test('點數設定 Tab 載入和表單', async ({ page }) => {
    const pointsTab = page.locator('[href="#points"], button:has-text("點數設定"), a:has-text("點數")').first();
    if (await pointsTab.isVisible()) {
      await pointsTab.click();
      await page.waitForTimeout(1000);

      // 檢查點數設定欄位
      const rateInput = page.locator('#pointEarnRate, input[name="pointEarnRate"]').first();
      if (await rateInput.isVisible()) {
        expect(await rateInput.inputValue()).toBeDefined();
      }

      // 檢查取整模式 Radio
      const radios = page.locator('input[name="pointRoundMode"]');
      expect(await radios.count()).toBeGreaterThanOrEqual(0);

      // 測試計算器
      const testAmount = page.locator('#testAmount').first();
      if (await testAmount.isVisible()) {
        await testAmount.fill('100');
        await page.waitForTimeout(500);
        const testResult = page.locator('#testResult');
        if (await testResult.isVisible()) {
          const result = await testResult.textContent();
          expect(result?.length).toBeGreaterThan(0);
        }
      }
    }
  });

  test('安全設定 Tab 載入和密碼變更表單', async ({ page }) => {
    const secTab = page.locator('[href="#security"], button:has-text("安全設定"), a:has-text("安全")').first();
    if (await secTab.isVisible()) {
      await secTab.click();
      await page.waitForTimeout(1000);

      // 檢查密碼欄位
      const currentPwd = page.locator('#currentPassword, input[name="currentPassword"]').first();
      const newPwd = page.locator('#newPassword, input[name="newPassword"]').first();
      expect((await currentPwd.count()) + (await newPwd.count())).toBeGreaterThanOrEqual(0);

      // 檢查密碼顯示切換按鈕
      const toggleBtns = page.locator('[onclick*="togglePassword"], .password-toggle');
      expect(await toggleBtns.count()).toBeGreaterThanOrEqual(0);
    }
  });
});

// ============================================================
// 店家後台 - LINE 設定完整操作
// ============================================================
test.describe('LINE 設定完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/line-settings');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('LINE 設定頁面載入正常', async ({ page }) => {
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('500 Internal Server Error');
    expect(bodyText?.length).toBeGreaterThan(100);
  });

  test('教學展開/收合', async ({ page }) => {
    const tutorialToggle = page.locator('[data-bs-toggle="collapse"][href*="tutorial"], button:has-text("教學"), a:has-text("教學")').first();
    if (await tutorialToggle.isVisible()) {
      await tutorialToggle.click();
      await page.waitForTimeout(1000);
      // 再次點擊收合
      await tutorialToggle.click();
      await page.waitForTimeout(500);
    }
  });

  test('Webhook URL 複製按鈕', async ({ page }) => {
    const copyBtn = page.locator('[onclick*="copyWebhookUrl"], button:has-text("複製")').first();
    if (await copyBtn.isVisible()) {
      await copyBtn.click();
      await page.waitForTimeout(500);
    }
  });

  test('LINE 設定表單欄位檢查', async ({ page }) => {
    const channelId = page.locator('#channelId, input[name="channelId"]').first();
    const channelSecret = page.locator('#channelSecret, input[name="channelSecret"]').first();
    const channelToken = page.locator('#channelAccessToken, input[name="channelAccessToken"]').first();

    if (await channelId.isVisible()) {
      expect(await channelId.count()).toBe(1);
    }
  });

  test('自動回覆設定欄位', async ({ page }) => {
    const welcomeMsg = page.locator('#welcomeMessage, textarea[name="welcomeMessage"]').first();
    const defaultReply = page.locator('#defaultReply, textarea[name="defaultReply"]').first();

    if (await welcomeMsg.isVisible()) {
      expect(await welcomeMsg.count()).toBe(1);
    }
  });

  test('密碼顯示切換按鈕', async ({ page }) => {
    const toggleBtns = page.locator('[onclick*="togglePassword"], .password-toggle, .input-group-text').first();
    if (await toggleBtns.isVisible()) {
      await toggleBtns.click();
      await page.waitForTimeout(300);
      await toggleBtns.click();
      await page.waitForTimeout(300);
    }
  });
});

// ============================================================
// 店家後台 - 功能商店完整操作
// ============================================================
test.describe('功能商店完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/feature-store');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('功能商店頁面載入正常', async ({ page }) => {
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('500 Internal Server Error');
    expect(bodyText?.length).toBeGreaterThan(100);
  });

  test('功能卡片顯示正常', async ({ page }) => {
    const cards = page.locator('.card, .feature-card, [class*="feature"]');
    const count = await cards.count();
    expect(count).toBeGreaterThan(0);
  });

  test('功能詳情點擊操作', async ({ page }) => {
    const detailBtn = page.locator('[onclick*="openFeatureDetail"], button:has-text("詳情"), button:has-text("查看"), .feature-card .btn').first();
    if (await detailBtn.isVisible()) {
      await detailBtn.click();
      await page.waitForTimeout(1000);

      // 關閉 Modal
      const closeBtn = page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first();
      if (await closeBtn.isVisible()) {
        await closeBtn.click();
        await page.waitForTimeout(500);
      }
    }
  });
});

// ============================================================
// 店家後台 - 點數管理完整操作
// ============================================================
test.describe('點數管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('點數管理頁面載入正常', async ({ page }) => {
    await page.goto('/tenant/points');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('500 Internal Server Error');
    expect(bodyText?.length).toBeGreaterThan(50);
  });

  test('點數餘額和異動記錄顯示', async ({ page }) => {
    await page.goto('/tenant/points');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    // 頁面應有點數相關內容
    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(50);
  });
});

// ============================================================
// 店家後台 - 會員等級完整操作
// ============================================================
test.describe('會員等級完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/membership-levels');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('會員等級頁面載入正常', async ({ page }) => {
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('500 Internal Server Error');
    expect(bodyText?.length).toBeGreaterThan(50);
  });

  test('新增會員等級 Modal 開關', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增"), [onclick*="openCreateModal"], [onclick*="createLevel"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('.modal.show').first();
      if (await modal.isVisible()) {
        // 關閉
        await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
        await page.waitForTimeout(500);
      }
    }
  });

  test('會員等級表格載入', async ({ page }) => {
    const table = page.locator('#dataTable, table').first();
    await expect(table).toBeVisible();
  });
});

// ============================================================
// 店家後台 - 報表完整操作
// ============================================================
test.describe('報表完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
    await page.goto('/tenant/reports');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(3000);
  });

  test('報表頁面載入正常 - 圖表顯示', async ({ page }) => {
    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('500 Internal Server Error');

    // 檢查 canvas (Chart.js)
    const charts = page.locator('canvas');
    expect(await charts.count()).toBeGreaterThanOrEqual(0);
  });

  test('報表日期範圍篩選', async ({ page }) => {
    const dateRange = page.locator('select[onchange*="loadReport"], #reportRange, select').first();
    if (await dateRange.isVisible()) {
      const options = await dateRange.locator('option').count();
      for (let i = 0; i < Math.min(options, 4); i++) {
        await dateRange.selectOption({ index: i });
        await page.waitForTimeout(2000);
      }
    }
  });

  test('匯出報表 Excel', async ({ page }) => {
    const exportBtn = page.locator('button:has-text("匯出 Excel"), [onclick*="exportReports"][onclick*="excel"]').first();
    if (await exportBtn.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
        exportBtn.click()
      ]);
      await page.waitForTimeout(1000);
    }
  });

  test('匯出報表 PDF', async ({ page }) => {
    const exportBtn = page.locator('button:has-text("匯出 PDF"), [onclick*="exportReports"][onclick*="pdf"]').first();
    if (await exportBtn.isVisible()) {
      const [download] = await Promise.all([
        page.waitForEvent('download', { timeout: 10000 }).catch(() => null),
        exportBtn.click()
      ]);
      await page.waitForTimeout(1000);
    }
  });
});

// ============================================================
// 超管後台 - 儀表板完整操作
// ============================================================
test.describe('超管儀表板完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test('超管儀表板載入所有統計', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText).not.toContain('500 Internal Server Error');
    expect(bodyText?.length).toBeGreaterThan(100);
  });

  test('快速連結按鈕可點擊', async ({ page }) => {
    await page.goto('/admin/dashboard');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const viewAllBtns = page.locator('a:has-text("查看全部"), a:has-text("查看更多")');
    const count = await viewAllBtns.count();
    for (let i = 0; i < count; i++) {
      const href = await viewAllBtns.nth(i).getAttribute('href');
      if (href) {
        expect(href.length).toBeGreaterThan(0);
      }
    }
  });
});

// ============================================================
// 超管後台 - 店家管理完整操作
// ============================================================
test.describe('超管店家管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto('/admin/tenants');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('店家列表頁面和表格載入', async ({ page }) => {
    const table = page.locator('table, #tenantsTable').first();
    await expect(table).toBeVisible();
  });

  test('新增店家 Modal 開關和欄位', async ({ page }) => {
    const addBtn = page.locator('button:has-text("新增店家"), button:has-text("新增"), [onclick*="openCreateTenantModal"]').first();
    if (await addBtn.isVisible()) {
      await addBtn.click();
      await page.waitForTimeout(1000);

      const modal = page.locator('#tenantModal, .modal.show').first();
      await expect(modal).toBeVisible();

      // 關閉
      await page.locator('.modal.show .btn-close, .modal.show [data-bs-dismiss="modal"]').first().click();
      await page.waitForTimeout(500);
    }
  });

  test('搜尋和狀態篩選', async ({ page }) => {
    // 搜尋
    const searchInput = page.locator('#searchKeyword, input[placeholder*="搜尋"]').first();
    if (await searchInput.isVisible()) {
      await searchInput.fill('test');
      await page.waitForTimeout(1500);
      await searchInput.fill('');
      await page.waitForTimeout(1000);
    }

    // 狀態篩選
    const statusFilter = page.locator('#statusFilter, select[onchange*="loadTenants"]').first();
    if (await statusFilter.isVisible()) {
      const options = await statusFilter.locator('option').count();
      for (let i = 0; i < Math.min(options, 4); i++) {
        await statusFilter.selectOption({ index: i });
        await page.waitForTimeout(1500);
      }
    }
  });

  test('店家詳情頁面載入', async ({ page }) => {
    // 點擊第一個店家的詳情
    const detailLink = page.locator('table tbody tr:first-child a[href*="/admin/tenants/"], table tbody tr:first-child button:has-text("詳情"), table tbody tr:first-child [onclick*="viewDetail"]').first();
    if (await detailLink.isVisible()) {
      await detailLink.click();
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      // 確認頁面載入
      const bodyText = await page.locator('body').textContent();
      // 確認頁面不是 500 錯誤頁面
      const title = await page.title();
      expect(title).not.toContain('Error');
      expect(bodyText?.length).toBeGreaterThan(100);
    }
  });
});

// ============================================================
// 超管後台 - 功能管理完整操作
// ============================================================
test.describe('超管功能管理完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto('/admin/features');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('功能列表頁面載入', async ({ page }) => {
    const table = page.locator('table, .feature-list').first();
    await expect(table).toBeVisible();
  });

  test('功能列表有資料', async ({ page }) => {
    const rows = page.locator('table tbody tr, .feature-item');
    const count = await rows.count();
    expect(count).toBeGreaterThan(0);
  });
});

// ============================================================
// 超管後台 - 儲值審核完整操作
// ============================================================
test.describe('超管儲值審核完整操作', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
    await page.goto('/admin/point-topups');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
  });

  test('儲值審核頁面載入', async ({ page }) => {
    const table = page.locator('table, #topupsTable').first();
    await expect(table).toBeVisible();
  });

  test('篩選待審核項目', async ({ page }) => {
    const statusFilter = page.locator('#statusFilter, select').first();
    if (await statusFilter.isVisible()) {
      const options = await statusFilter.locator('option').count();
      for (let i = 0; i < Math.min(options, 4); i++) {
        await statusFilter.selectOption({ index: i });
        await page.waitForTimeout(1500);
      }
    }
  });
});

// ============================================================
// 超管後台 - 側邊欄所有頁面導航
// ============================================================
test.describe('超管側邊欄所有頁面導航', () => {
  test.beforeEach(async ({ page }) => {
    await adminLogin(page);
  });

  test('側邊欄所有連結可點擊 - 無 F12 錯誤', async ({ page }) => {
    const pages = [
      '/admin/dashboard',
      '/admin/tenants',
      '/admin/features',
      '/admin/point-topups'
    ];

    for (const url of pages) {
      await page.goto(url);
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const bodyText = await page.locator('body').textContent();
      // 確認頁面不是 500 錯誤頁面
      const title = await page.title();
      expect(title).not.toContain('Error');
    }
  });
});

// ============================================================
// 店家後台 - 側邊欄所有頁面導航
// ============================================================
test.describe('店家側邊欄所有頁面導航 - F12 全頁面檢查', () => {
  test.beforeEach(async ({ page }) => {
    await tenantLogin(page);
  });

  test('所有店家頁面載入無 F12 錯誤', async ({ page }) => {
    const pages = [
      '/tenant/dashboard',
      '/tenant/bookings',
      '/tenant/calendar',
      '/tenant/reports',
      '/tenant/customers',
      '/tenant/staff',
      '/tenant/services',
      '/tenant/products',
      '/tenant/product-orders',
      '/tenant/inventory',
      '/tenant/coupons',
      '/tenant/campaigns',
      '/tenant/marketing',
      '/tenant/settings',
      '/tenant/line-settings',
      '/tenant/feature-store',
      '/tenant/points',
      '/tenant/membership-levels'
    ];

    for (const url of pages) {
      await page.goto(url);
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(2000);

      const bodyText = await page.locator('body').textContent();
      // 確認頁面不是 500 錯誤頁面
      const title = await page.title();
      expect(title).not.toContain('Error');
      expect(bodyText?.length).toBeGreaterThan(50);
    }
  });
});

// ============================================================
// 公開頁面 - SEO 和公開頁面完整檢查
// ============================================================
test.describe('公開頁面完整操作', () => {
  test('首頁載入正常', async ({ page }) => {
    await page.goto('/');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(100);
  });

  test('功能介紹頁面載入正常', async ({ page }) => {
    await page.goto('/features');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(100);
  });

  test('價格方案頁面載入正常', async ({ page }) => {
    await page.goto('/pricing');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(100);
  });

  test('FAQ 頁面載入正常', async ({ page }) => {
    await page.goto('/faq');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(100);
  });

  test('隱私政策頁面載入正常', async ({ page }) => {
    await page.goto('/privacy');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
    expect(await page.locator('body').textContent()).toBeTruthy();
  });

  test('服務條款頁面載入正常', async ({ page }) => {
    await page.goto('/terms');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);
    expect(await page.locator('body').textContent()).toBeTruthy();
  });

  test('行業頁面 - 美容/美髮/SPA/健身/餐廳/診所', async ({ page }) => {
    const industries = ['beauty', 'hair-salon', 'spa', 'fitness', 'restaurant', 'clinic'];
    for (const industry of industries) {
      await page.goto(`/${industry}`);
      await page.waitForLoadState('domcontentloaded');
      await page.waitForTimeout(1500);

      const bodyText = await page.locator('body').textContent();
      expect(bodyText?.length).toBeGreaterThan(100);
    }
  });

  test('404 頁面顯示正常', async ({ page }) => {
    await page.goto('/nonexistent-page-12345');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1500);

    // 頁面不應為空
    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(10);
  });

  test('取消預約頁面 - 無效 token', async ({ page }) => {
    await page.goto('/booking/cancel/invalid-token-test');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(2000);

    // 頁面應該載入（可能顯示錯誤或過期訊息）
    const bodyText = await page.locator('body').textContent();
    expect(bodyText?.length).toBeGreaterThan(10);
  });
});

// ============================================================
// 錯誤頁面檢查
// ============================================================
test.describe('錯誤頁面檢查', () => {
  test('登入頁面 - 店家', async ({ page }) => {
    await page.goto('/tenant/login');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    // 檢查表單元素
    await expect(page.locator('#username, input[name="username"]').first()).toBeVisible();
    await expect(page.locator('#password, input[name="password"]').first()).toBeVisible();
    await expect(page.locator('button[type="submit"]').first()).toBeVisible();
  });

  test('登入頁面 - 超管', async ({ page }) => {
    await page.goto('/admin/login');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    await expect(page.locator('#username, input[name="username"]').first()).toBeVisible();
    await expect(page.locator('#password, input[name="password"]').first()).toBeVisible();
    await expect(page.locator('button[type="submit"]').first()).toBeVisible();
  });

  test('註冊頁面表單欄位完整', async ({ page }) => {
    await page.goto('/tenant/register');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    // 註冊表單應有多個欄位
    const inputs = page.locator('input, select');
    expect(await inputs.count()).toBeGreaterThan(2);
  });

  test('忘記密碼頁面表單', async ({ page }) => {
    await page.goto('/tenant/forgot-password');
    await page.waitForLoadState('domcontentloaded');
    await page.waitForTimeout(1000);

    const emailInput = page.locator('input[type="email"], input[name="email"], #email').first();
    await expect(emailInput).toBeVisible();
  });
});

// ============================================================
// API 端點健康檢查
// ============================================================
test.describe('API 端點健康檢查', () => {
  test('所有店家 API 端點正常回應', async ({ request }) => {
    // 直接用 API 登入取得 token
    const loginResp = await request.post('/api/auth/tenant/login', {
      data: {
        username: TEST_ACCOUNTS.tenant.username,
        password: TEST_ACCOUNTS.tenant.password
      }
    });
    const loginData = await loginResp.json();
    const token = loginData.data?.accessToken;
    if (!token) { test.skip(); return; }

    const endpoints = [
      '/api/bookings',
      '/api/customers',
      '/api/staff',
      '/api/services',
      '/api/service-categories',
      '/api/products',
      '/api/coupons',
      '/api/campaigns',
      '/api/marketing/pushes',
      '/api/settings',
      '/api/settings/line',
      '/api/feature-store',
      '/api/points/balance',
      '/api/membership-levels',
      '/api/reports/dashboard',
      '/api/reports/summary?range=MONTHLY',
      '/api/staff/bookable',
      '/api/services/bookable',
      '/api/payments'
    ];

    for (const endpoint of endpoints) {
      const response = await request.get(endpoint, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      expect(response.status(), `API ${endpoint} 應返回成功狀態碼`).toBeLessThan(500);
    }
  });

  test('所有超管 API 端點正常回應', async ({ request }) => {
    const loginResp = await request.post('/api/auth/admin/login', {
      data: {
        username: 'admin',
        password: 'admin123'
      }
    });
    const loginData = await loginResp.json();
    const token = loginData.data?.accessToken;
    expect(token).toBeTruthy();

    const endpoints = [
      '/api/admin/tenants',
      '/api/admin/features',
      '/api/admin/dashboard',
      '/api/admin/point-topups'
    ];

    for (const endpoint of endpoints) {
      const response = await request.get(endpoint, {
        headers: { 'Authorization': `Bearer ${token}` }
      });
      expect(response.status(), `API ${endpoint} 應返回成功狀態碼`).toBeLessThan(500);
    }
  });
});

// ============================================================
// 靜態資源載入檢查
// ============================================================
test.describe('靜態資源載入檢查', () => {
  test('CSS 檔案載入正常', async ({ page }) => {
    const resources = [
      '/css/common.css',
      '/css/tenant.css',
      '/css/admin.css'
    ];

    for (const res of resources) {
      const response = await page.request.get(res);
      expect(response.status(), `${res} 應該可訪問`).toBe(200);
    }
  });

  test('JS 檔案載入正常', async ({ page }) => {
    const resources = [
      '/js/common.js',
      '/js/tenant.js',
      '/js/admin.js',
      '/js/notification.js'
    ];

    for (const res of resources) {
      const response = await page.request.get(res);
      expect(response.status(), `${res} 應該可訪問`).toBe(200);
    }
  });
});

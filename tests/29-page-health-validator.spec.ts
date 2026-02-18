/**
 * 頁面健康驗證器
 *
 * 核心思路和現有測試完全不同：
 * - 現有測試問：「頁面有沒有報錯？」
 * - 這個測試問：「頁面有沒有完成載入？」
 *
 * 三大檢查：
 * 1. Stale Loading Detector — 卡住的「載入中」文字
 * 2. Orphan Spinner Detector — 5 秒後仍在轉的 spinner
 * 3. Loading Overlay Detector — 仍可見的全頁載入遮罩
 */
import { test, expect, Page } from './fixtures';
import { tenantLogin, adminLogin } from './utils/test-helpers';

// ========== 頁面清單 ==========

const TENANT_PAGES = [
    { name: '儀表板', url: '/tenant/dashboard' },
    { name: '預約管理', url: '/tenant/bookings' },
    { name: '行事曆', url: '/tenant/calendar' },
    { name: '顧客管理', url: '/tenant/customers' },
    { name: '員工管理', url: '/tenant/staff' },
    { name: '服務管理', url: '/tenant/services' },
    { name: '商品管理', url: '/tenant/products' },
    { name: '庫存異動', url: '/tenant/inventory' },
    { name: '商品訂單', url: '/tenant/product-orders' },
    { name: '票券管理', url: '/tenant/coupons' },
    { name: '行銷活動', url: '/tenant/campaigns' },
    { name: '行銷推播', url: '/tenant/marketing' },
    { name: '營運報表', url: '/tenant/reports' },
    { name: '店家設定', url: '/tenant/settings' },
    { name: 'LINE 設定', url: '/tenant/line-settings' },
    { name: '功能商店', url: '/tenant/feature-store' },
    { name: '點數管理', url: '/tenant/points' },
    { name: '會員等級', url: '/tenant/membership-levels' },
    { name: '推薦好友', url: '/tenant/referrals' },
];

const ADMIN_PAGES = [
    { name: '儀表板', url: '/admin/dashboard' },
    { name: '店家列表', url: '/admin/tenants' },
    { name: '功能管理', url: '/admin/features' },
    { name: '儲值審核', url: '/admin/point-topups' },
];

// ========== 核心驗證函式 ==========

/**
 * 等待所有 /api/ 請求完成（排除 SSE 的 /notifications/stream）
 * 不用 networkidle（SSE 永遠不會 idle），改用 request/response 計數
 */
async function waitForApiSettled(page: Page, timeoutMs = 8000): Promise<void> {
    let pending = 0;

    const onRequest = (req: { url: () => string }) => {
        const url = req.url();
        if (url.includes('/api/') && !url.includes('/notifications/stream')) {
            pending++;
        }
    };
    const onResponse = (res: { url: () => string }) => {
        const url = res.url();
        if (url.includes('/api/') && !url.includes('/notifications/stream')) {
            pending = Math.max(0, pending - 1);
        }
    };
    const onFailed = (req: { url: () => string }) => {
        const url = req.url();
        if (url.includes('/api/') && !url.includes('/notifications/stream')) {
            pending = Math.max(0, pending - 1);
        }
    };

    page.on('request', onRequest);
    page.on('response', onResponse);
    page.on('requestfailed', onFailed);

    // 等待 pending 歸零 + 500ms 穩定期
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
        await page.waitForTimeout(300);
        if (pending === 0) {
            // 穩定期：確認 500ms 內沒有新請求
            await page.waitForTimeout(500);
            if (pending === 0) break;
        }
    }

    page.off('request', onRequest);
    page.off('response', onResponse);
    page.off('requestfailed', onFailed);
}

/**
 * 驗證頁面健康狀態
 * 回傳發現的問題清單，空陣列 = 健康
 */
async function validatePageHealth(page: Page, url: string, pageName: string): Promise<string[]> {
    // 導航到頁面
    await page.goto(url);
    await page.waitForLoadState('domcontentloaded');

    // 等待所有 API 請求完成
    await waitForApiSettled(page, 5000);

    const issues: string[] = [];

    // ===== 檢查 1: 卡住的「載入中」文字 =====
    // 排除：btn-loading 裡的「處理中/儲存中/登入中」（按鈕初始隱藏狀態）
    // 排除：modal 裡的載入指標（modal 未開啟時不算卡住）
    // 排除：「載入失敗」（這是錯誤處理結果，fixtures.ts 會抓）
    const staleLoadingElements = await page.evaluate(() => {
        const results: string[] = [];
        // 建立 TreeWalker 找所有文字節點包含「載入中」的元素
        const walker = document.createTreeWalker(
            document.body,
            NodeFilter.SHOW_TEXT,
            {
                acceptNode: (node) => {
                    if (node.textContent && node.textContent.includes('載入中')) {
                        return NodeFilter.FILTER_ACCEPT;
                    }
                    return NodeFilter.FILTER_REJECT;
                }
            }
        );

        while (walker.nextNode()) {
            const textNode = walker.currentNode;
            const el = textNode.parentElement;
            if (!el) continue;

            // 排除：按鈕載入狀態（btn-loading 或其子元素，這些本來就用 d-none 隱藏）
            if (el.closest('.btn-loading')) continue;

            // 排除：不可見元素（display:none 或 visibility:hidden 的祖先）
            const style = window.getComputedStyle(el);
            if (style.display === 'none' || style.visibility === 'hidden') continue;

            // 檢查所有祖先是否可見
            let ancestor: HTMLElement | null = el.parentElement;
            let hidden = false;
            while (ancestor && ancestor !== document.body) {
                const ancestorStyle = window.getComputedStyle(ancestor);
                if (ancestorStyle.display === 'none' || ancestorStyle.visibility === 'hidden') {
                    hidden = true;
                    break;
                }
                ancestor = ancestor.parentElement;
            }
            if (hidden) continue;

            // 排除：在未開啟的 modal 裡
            const modal = el.closest('.modal');
            if (modal && !modal.classList.contains('show')) continue;

            // 取得元素辨識資訊
            const closestId = el.closest('[id]');
            const identifier = closestId
                ? `#${closestId.id}`
                : el.tagName.toLowerCase() + (el.className ? `.${el.className.split(' ')[0]}` : '');
            results.push(identifier);
        }

        return results;
    });

    for (const id of staleLoadingElements) {
        issues.push(`卡住的載入指標: 「載入中」仍可見於 ${id}`);
    }

    // ===== 檢查 2: 仍在轉的 spinner =====
    // 排除：btn-loading 裡的 spinner（按鈕隱藏狀態）
    // 排除：未開啟的 modal 裡的 spinner
    const orphanSpinners = await page.evaluate(() => {
        const spinners = document.querySelectorAll('.spinner-border');
        let visibleCount = 0;
        const locations: string[] = [];

        for (const spinner of spinners) {
            const el = spinner as HTMLElement;

            // 排除：在 btn-loading 容器裡
            if (el.closest('.btn-loading')) continue;

            // 排除：在未開啟的 modal 裡
            const modal = el.closest('.modal');
            if (modal && !modal.classList.contains('show')) continue;

            // 檢查是否真的可見
            const style = window.getComputedStyle(el);
            if (style.display === 'none' || style.visibility === 'hidden') continue;

            // 檢查祖先可見性
            let ancestor: HTMLElement | null = el.parentElement;
            let hidden = false;
            while (ancestor && ancestor !== document.body) {
                const ancestorStyle = window.getComputedStyle(ancestor);
                if (ancestorStyle.display === 'none' || ancestorStyle.visibility === 'hidden') {
                    hidden = true;
                    break;
                }
                ancestor = ancestor.parentElement;
            }
            if (hidden) continue;

            visibleCount++;
            const closestId = el.closest('[id]');
            const loc = closestId ? `#${closestId.id}` : el.parentElement?.tagName || 'unknown';
            locations.push(loc);
        }

        return { count: visibleCount, locations };
    });

    if (orphanSpinners.count > 0) {
        issues.push(`${orphanSpinners.count} 個 spinner 仍在轉動 (${orphanSpinners.locations.join(', ')})`);
    }

    // ===== 檢查 3: 仍可見的全頁載入遮罩 =====
    const overlayVisible = await page.evaluate(() => {
        const overlays = document.querySelectorAll('.loading-overlay');
        for (const overlay of overlays) {
            const style = window.getComputedStyle(overlay as HTMLElement);
            if (style.display !== 'none' && style.visibility !== 'hidden' && style.opacity !== '0') {
                return true;
            }
        }
        return false;
    });

    if (overlayVisible) {
        issues.push('全頁載入遮罩 (.loading-overlay) 仍可見');
    }

    return issues;
}

// ========== 測試 ==========

test.describe('頁面健康驗證 - 店家後台', () => {
    test.beforeEach(async ({ page }) => {
        await tenantLogin(page);
    });

    for (const p of TENANT_PAGES) {
        test(`[${p.name}] 載入完成，無卡住指標`, async ({ page }) => {
            const issues = await validatePageHealth(page, p.url, p.name);
            if (issues.length > 0) {
                console.log(`\n❌ ${p.name} (${p.url}) 發現 ${issues.length} 個健康問題:`);
                issues.forEach(i => console.log(`  - ${i}`));
            }
            expect(issues, `${p.name} 頁面有未完成的載入指標`).toEqual([]);
        });
    }
});

test.describe('頁面健康驗證 - 超管後台', () => {
    test.beforeEach(async ({ page }) => {
        await adminLogin(page);
    });

    for (const p of ADMIN_PAGES) {
        test(`[${p.name}] 載入完成，無卡住指標`, async ({ page }) => {
            const issues = await validatePageHealth(page, p.url, p.name);
            if (issues.length > 0) {
                console.log(`\n❌ ${p.name} (${p.url}) 發現 ${issues.length} 個健康問題:`);
                issues.forEach(i => console.log(`  - ${i}`));
            }
            expect(issues, `${p.name} 頁面有未完成的載入指標`).toEqual([]);
        });
    }
});

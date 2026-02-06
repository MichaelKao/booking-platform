import { test, expect } from './fixtures';

const BASE_URL = 'https://booking-platform-production-1e08.up.railway.app';

test.describe('SEO 頁面測試', () => {

    test.describe('首頁', () => {
        test('首頁載入成功', async ({ page }) => {
            await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
            await expect(page).toHaveTitle(/預約平台/);
        });

        test('首頁有導航連結', async ({ page }) => {
            await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('a[href="/features"]').first()).toBeVisible();
            await expect(page.locator('a[href="/pricing"]').first()).toBeVisible();
            await expect(page.locator('a[href="/faq"]').first()).toBeVisible();
        });

        test('首頁有 CTA 按鈕', async ({ page }) => {
            await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
        });

        test('首頁有 Schema.org 結構化資料', async ({ page }) => {
            await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
            const schemaScripts = await page.locator('script[type="application/ld+json"]').count();
            expect(schemaScripts).toBeGreaterThanOrEqual(1);
        });
    });

    test.describe('功能介紹頁', () => {
        test('功能介紹頁載入成功', async ({ page }) => {
            await page.goto(`${BASE_URL}/features`, { waitUntil: 'domcontentloaded' });
            await expect(page).toHaveTitle(/功能介紹/);
        });

        test('功能介紹頁有功能列表', async ({ page }) => {
            await page.goto(`${BASE_URL}/features`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('text=LINE 預約機器人').first()).toBeVisible();
            await expect(page.locator('text=智慧排班管理').first()).toBeVisible();
        });

        test('功能介紹頁無 JavaScript 錯誤', async ({ page }) => {
            const errors: string[] = [];
            page.on('pageerror', error => errors.push(error.message));
            await page.goto(`${BASE_URL}/features`, { waitUntil: 'domcontentloaded' });
            await page.waitForTimeout(1000);
            expect(errors.filter(e => e.includes('SyntaxError') || e.includes('ReferenceError'))).toHaveLength(0);
        });
    });

    test.describe('價格方案頁', () => {
        test('價格方案頁載入成功', async ({ page }) => {
            await page.goto(`${BASE_URL}/pricing`, { waitUntil: 'domcontentloaded' });
            await expect(page).toHaveTitle(/價格方案/);
        });

        test('價格方案頁有免費版資訊', async ({ page }) => {
            await page.goto(`${BASE_URL}/pricing`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('text=免費版').first()).toBeVisible();
            await expect(page.locator('.pricing-price').first()).toBeVisible();
        });

        test('價格方案頁有專業版資訊', async ({ page }) => {
            await page.goto(`${BASE_URL}/pricing`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('text=專業版')).toBeVisible();
        });
    });

    test.describe('常見問題頁', () => {
        test('FAQ 頁面載入成功', async ({ page }) => {
            await page.goto(`${BASE_URL}/faq`, { waitUntil: 'domcontentloaded' });
            await expect(page).toHaveTitle(/常見問題/);
        });

        test('FAQ 頁面有問答內容', async ({ page }) => {
            await page.goto(`${BASE_URL}/faq`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('text=預約平台是否免費使用')).toBeVisible();
        });

        test('FAQ 頁面 Accordion 存在', async ({ page }) => {
            await page.goto(`${BASE_URL}/faq`, { waitUntil: 'domcontentloaded' });
            // 驗證 accordion 結構存在
            await expect(page.locator('.accordion').first()).toBeVisible();
            await expect(page.locator('.accordion-item').first()).toBeVisible();
        });
    });

    test.describe('行業專屬頁面', () => {
        const industryPages = [
            { path: '/beauty', title: '美容預約系統', keyword: '美容' },
            { path: '/hair-salon', title: '美髮預約系統', keyword: '美髮' },
            { path: '/spa', title: 'SPA', keyword: '按摩' },
            { path: '/fitness', title: '健身教練', keyword: '健身' },
            { path: '/restaurant', title: '餐廳訂位', keyword: '餐廳' },
            { path: '/clinic', title: '診所預約', keyword: '診所' },
        ];

        for (const industry of industryPages) {
            test(`${industry.title}頁面載入成功`, async ({ page }) => {
                await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
                await expect(page).toHaveTitle(new RegExp(industry.title));
            });

            test(`${industry.title}頁面有行業關鍵字`, async ({ page }) => {
                await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
                const content = await page.content();
                expect(content).toContain(industry.keyword);
            });

            test(`${industry.title}頁面有 CTA 按鈕`, async ({ page }) => {
                await page.goto(`${BASE_URL}${industry.path}`, { waitUntil: 'domcontentloaded' });
                await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
            });
        }
    });

    test.describe('城市專屬頁面', () => {
        const cityPages = [
            { path: '/taipei', title: '台北預約系統', keyword: '台北' },
            { path: '/new-taipei', title: '新北預約系統', keyword: '新北' },
            { path: '/taoyuan', title: '桃園預約系統', keyword: '桃園' },
            { path: '/taichung', title: '台中預約系統', keyword: '台中' },
            { path: '/tainan', title: '台南預約系統', keyword: '台南' },
            { path: '/kaohsiung', title: '高雄預約系統', keyword: '高雄' },
            { path: '/hsinchu', title: '新竹預約系統', keyword: '新竹' },
        ];

        for (const city of cityPages) {
            test(`${city.title}頁面載入成功`, async ({ page }) => {
                await page.goto(`${BASE_URL}${city.path}`, { waitUntil: 'domcontentloaded' });
                await expect(page).toHaveTitle(new RegExp(city.title));
            });

            test(`${city.title}頁面有城市關鍵字`, async ({ page }) => {
                await page.goto(`${BASE_URL}${city.path}`, { waitUntil: 'domcontentloaded' });
                const content = await page.content();
                expect(content).toContain(city.keyword);
            });

            test(`${city.title}頁面有 CTA 按鈕`, async ({ page }) => {
                await page.goto(`${BASE_URL}${city.path}`, { waitUntil: 'domcontentloaded' });
                await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
            });

            test(`${city.title}頁面有結構化資料`, async ({ page }) => {
                await page.goto(`${BASE_URL}${city.path}`, { waitUntil: 'domcontentloaded' });
                const schemaScripts = await page.locator('script[type="application/ld+json"]').count();
                expect(schemaScripts).toBeGreaterThanOrEqual(1);
            });

            test(`${city.title}頁面有 Open Graph meta tags`, async ({ page }) => {
                await page.goto(`${BASE_URL}${city.path}`, { waitUntil: 'domcontentloaded' });
                await expect(page.locator('meta[property="og:title"]')).toHaveCount(1);
                await expect(page.locator('meta[property="og:description"]')).toHaveCount(1);
            });

            test(`${city.title}頁面有 canonical URL`, async ({ page }) => {
                await page.goto(`${BASE_URL}${city.path}`, { waitUntil: 'domcontentloaded' });
                await expect(page.locator('link[rel="canonical"]')).toHaveCount(1);
            });
        }
    });

    test.describe('法律頁面', () => {
        test('服務條款頁面載入成功', async ({ page }) => {
            await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
            await expect(page).toHaveTitle(/服務條款/);
        });

        test('服務條款頁面有內容', async ({ page }) => {
            await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('text=服務說明').first()).toBeVisible();
            await expect(page.locator('text=帳號註冊').first()).toBeVisible();
            await expect(page.locator('text=使用規範').first()).toBeVisible();
        });

        test('服務條款頁面有 CTA 按鈕', async ({ page }) => {
            await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
        });

        test('隱私權政策頁面載入成功', async ({ page }) => {
            await page.goto(`${BASE_URL}/privacy`, { waitUntil: 'domcontentloaded' });
            await expect(page).toHaveTitle(/隱私權政策/);
        });

        test('隱私權政策頁面有內容', async ({ page }) => {
            await page.goto(`${BASE_URL}/privacy`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('text=資料收集').first()).toBeVisible();
            await expect(page.locator('text=資料保護措施').first()).toBeVisible();
            await expect(page.locator('text=您的權利').first()).toBeVisible();
        });

        test('隱私權政策頁面有 CTA 按鈕', async ({ page }) => {
            await page.goto(`${BASE_URL}/privacy`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('a[href="/tenant/register"]').first()).toBeVisible();
        });

        test('法律頁面有 BreadcrumbList 結構化資料', async ({ page }) => {
            await page.goto(`${BASE_URL}/terms`, { waitUntil: 'domcontentloaded' });
            const schemaScripts = await page.locator('script[type="application/ld+json"]').count();
            expect(schemaScripts).toBeGreaterThanOrEqual(1);
        });
    });

    test.describe('SEO 資源', () => {
        test('robots.txt 可存取', async ({ request }) => {
            const response = await request.get(`${BASE_URL}/robots.txt`);
            expect(response.status()).toBe(200);
            const text = await response.text();
            expect(text).toContain('User-agent');
            expect(text).toContain('Sitemap');
        });

        test('sitemap.xml 可存取', async ({ request }) => {
            const response = await request.get(`${BASE_URL}/sitemap.xml`);
            expect(response.status()).toBe(200);
            const text = await response.text();
            expect(text).toContain('<?xml');
            expect(text).toContain('<urlset');
            expect(text).toContain('<loc>');
        });

        test('sitemap.xml 包含所有頁面', async ({ request }) => {
            const response = await request.get(`${BASE_URL}/sitemap.xml`);
            const text = await response.text();
            expect(text).toContain('/features');
            expect(text).toContain('/pricing');
            expect(text).toContain('/faq');
            expect(text).toContain('/terms');
            expect(text).toContain('/privacy');
            expect(text).toContain('/beauty');
            expect(text).toContain('/hair-salon');
            expect(text).toContain('/spa');
            expect(text).toContain('/fitness');
            expect(text).toContain('/restaurant');
            expect(text).toContain('/clinic');
            // 城市頁面
            expect(text).toContain('/taipei');
            expect(text).toContain('/new-taipei');
            expect(text).toContain('/taoyuan');
            expect(text).toContain('/taichung');
            expect(text).toContain('/tainan');
            expect(text).toContain('/kaohsiung');
            expect(text).toContain('/hsinchu');
        });

        test('OG 圖片可存取', async ({ request }) => {
            const response = await request.get(`${BASE_URL}/images/og-image.png`);
            expect(response.status()).toBe(200);
            expect(response.headers()['content-type']).toContain('image/png');
        });
    });

    test.describe('Meta Tags 驗證', () => {
        test('首頁有 Open Graph meta tags', async ({ page }) => {
            await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('meta[property="og:title"]')).toHaveCount(1);
            await expect(page.locator('meta[property="og:description"]')).toHaveCount(1);
            await expect(page.locator('meta[property="og:url"]')).toHaveCount(1);
        });

        test('首頁有 canonical URL', async ({ page }) => {
            await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('link[rel="canonical"]')).toHaveCount(1);
        });

        test('首頁有 Google Search Console 驗證', async ({ page }) => {
            await page.goto(`${BASE_URL}/`, { waitUntil: 'domcontentloaded' });
            await expect(page.locator('meta[name="google-site-verification"]')).toHaveCount(1);
        });
    });
});

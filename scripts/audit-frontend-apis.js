/**
 * 前端靜態分析腳本
 *
 * 不需要啟動伺服器，直接掃描 HTML 原始碼找問題：
 *   node scripts/audit-frontend-apis.js
 *
 * 掃描規則：
 *   STALE_LOADING — HTML 有「載入中」文字的元素，但同頁 JS 沒有對應的 DOM 替換
 *   ORPHAN_SPINNER — HTML 有 spinner-border，但同頁 JS 沒有移除/隱藏它的邏輯
 */
const fs = require('fs');
const path = require('path');

const TEMPLATE_DIR = path.join(__dirname, '..', 'src', 'main', 'resources', 'templates');
const JS_DIR = path.join(__dirname, '..', 'src', 'main', 'resources', 'static', 'js');

// 排除的目錄（不含動態載入邏輯的模板）
const EXCLUDED_DIRS = ['email', 'error', 'fragments'];

// 排除的 ID（佈局/結構容器，不是載入目標）
const LAYOUT_IDS = new Set([
    'wrapper', 'content-wrapper', 'content', 'sidebar',
    'page-top', 'main-content', 'app',
]);

// ========== 工具函式 ==========

/**
 * 從 HTML 中提取所有 inline <script> 區塊的內容
 */
function extractInlineScripts(html) {
    const scripts = [];
    const regex = /<script[^>]*>([\s\S]*?)<\/script>/gi;
    let match;
    while ((match = regex.exec(html)) !== null) {
        // 排除 src="..." 的外部 script 標籤
        const tag = match[0];
        if (!tag.match(/<script[^>]+src\s*=/i)) {
            scripts.push(match[1]);
        }
    }
    return scripts.join('\n');
}

/**
 * 從 HTML 找出所有直接包含「載入中」文字的 id 元素
 * 只找最近的 id 容器（不找佈局層級的祖先）
 * 回傳 [{ id, type, context }]
 */
function findLoadingElements(html) {
    const results = [];
    const seen = new Set();

    // 模式 1：<tbody id="xxx"> 裡面有「載入中」
    const tbodyRegex = /<tbody\s+id="([^"]+)"[^>]*>[\s\S]*?<\/tbody>/gi;
    let match;
    while ((match = tbodyRegex.exec(html)) !== null) {
        const id = match[1];
        if (LAYOUT_IDS.has(id) || seen.has(id)) continue;
        if (match[0].includes('載入中')) {
            seen.add(id);
            results.push({ id, type: 'tbody', context: 'table body' });
        }
    }

    // 模式 2：<div/ul/h1 id="xxx"> 後面緊接的內容（到下一個同級 id 元素前）含「載入中」
    // 用短範圍搜索（300 字）避免抓到遠處的 loading
    const idElementRegex = /<(\w+)\s[^>]*id="([^"]+)"[^>]*>/gi;
    while ((match = idElementRegex.exec(html)) !== null) {
        const tagName = match[1].toLowerCase();
        const id = match[2];
        if (LAYOUT_IDS.has(id) || seen.has(id)) continue;
        if (tagName === 'tbody') continue; // 已在模式 1 處理

        // 往後看 300 字元的範圍
        const afterElement = html.substring(match.index + match[0].length, match.index + match[0].length + 300);

        // 在遇到下一個 id 元素之前搜索
        const nextIdPos = afterElement.search(/id="/);
        const searchRange = nextIdPos > 0 ? afterElement.substring(0, nextIdPos) : afterElement;

        if (searchRange.includes('載入中')) {
            seen.add(id);
            results.push({ id, type: tagName, context: 'container' });
        }
    }

    return results;
}

/**
 * 檢查 JS 代碼是否有存取指定 id 的元素
 */
function jsAccessesElement(jsCode, elementId) {
    if (jsCode.includes(`getElementById('${elementId}')`) || jsCode.includes(`getElementById("${elementId}")`)) {
        return true;
    }
    if (jsCode.includes(`querySelector('#${elementId}')`) || jsCode.includes(`querySelector("#${elementId}")`)) {
        return true;
    }
    if (jsCode.includes(`$('#${elementId}')`) || jsCode.includes(`$("#${elementId}")`)) {
        return true;
    }
    return false;
}

/**
 * 讀取外部 JS 檔案的內容
 */
function loadExternalJs() {
    const jsFiles = {};
    if (fs.existsSync(JS_DIR)) {
        const files = fs.readdirSync(JS_DIR).filter(f => f.endsWith('.js'));
        for (const file of files) {
            jsFiles[file] = fs.readFileSync(path.join(JS_DIR, file), 'utf-8');
        }
    }
    return jsFiles;
}

/**
 * 從 HTML 找出引用的外部 JS 檔案名
 */
function findReferencedJs(html) {
    const refs = [];
    const regex = /src="[^"]*\/js\/([^"]+\.js)"/g;
    let match;
    while ((match = regex.exec(html)) !== null) {
        refs.push(match[1]);
    }
    return refs;
}

// ========== 主程式 ==========

function main() {
    console.log('=== 前端靜態分析報告 ===\n');

    const htmlFiles = [];
    function scanDir(dir) {
        if (!fs.existsSync(dir)) return;
        const entries = fs.readdirSync(dir, { withFileTypes: true });
        for (const entry of entries) {
            const fullPath = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                if (!EXCLUDED_DIRS.includes(entry.name)) {
                    scanDir(fullPath);
                }
            } else if (entry.name.endsWith('.html')) {
                htmlFiles.push(fullPath);
            }
        }
    }
    scanDir(TEMPLATE_DIR);

    if (htmlFiles.length === 0) {
        console.log('找不到 HTML 模板檔案。');
        console.log(`搜尋路徑: ${TEMPLATE_DIR}`);
        process.exit(1);
    }

    console.log(`掃描 ${htmlFiles.length} 個 HTML 模板...\n`);

    const externalJs = loadExternalJs();

    const staleLoadingIssues = [];
    const orphanSpinnerIssues = [];
    let totalLoadingElements = 0;
    let totalSpinnerElements = 0;

    for (const filePath of htmlFiles) {
        const relativePath = path.relative(TEMPLATE_DIR, filePath).replace(/\\/g, '/');
        const html = fs.readFileSync(filePath, 'utf-8');

        // 提取該頁面的 JS 代碼（inline + 引用的外部 JS）
        const inlineJs = extractInlineScripts(html);
        const referencedJsFiles = findReferencedJs(html);
        let allJs = inlineJs;
        for (const jsFile of referencedJsFiles) {
            if (externalJs[jsFile]) {
                allJs += '\n' + externalJs[jsFile];
            }
        }

        // ===== 規則 1: STALE_LOADING =====
        const loadingElements = findLoadingElements(html);
        totalLoadingElements += loadingElements.length;

        for (const elem of loadingElements) {
            const hasJsAccess = jsAccessesElement(allJs, elem.id);
            if (!hasJsAccess) {
                staleLoadingIssues.push({
                    file: relativePath,
                    elementId: elem.id,
                    type: elem.type,
                    context: elem.context,
                });
            }
        }

        // ===== 規則 2: ORPHAN_SPINNER =====
        // 找有 spinner-border 的最近 id 容器
        const spinnerPositions = [];
        let sIdx = 0;
        while ((sIdx = html.indexOf('spinner-border', sIdx)) !== -1) {
            spinnerPositions.push(sIdx);
            sIdx++;
        }

        for (const pos of spinnerPositions) {
            // 往前找最近的 id="xxx"
            const before = html.substring(Math.max(0, pos - 500), pos);
            const idMatches = [...before.matchAll(/id="([^"]+)"/g)];
            if (idMatches.length === 0) continue;

            const containerId = idMatches[idMatches.length - 1][1];
            if (LAYOUT_IDS.has(containerId)) continue;

            // 排除 btn-loading（按鈕載入狀態有自己的 toggle 邏輯）
            const nearContext = html.substring(Math.max(0, pos - 200), pos + 50);
            if (nearContext.includes('btn-loading')) continue;

            // 避免重複報告同一容器
            if (orphanSpinnerIssues.some(i => i.file === relativePath && i.containerId === containerId)) continue;

            totalSpinnerElements++;
            const hasJsAccess = jsAccessesElement(allJs, containerId);
            if (!hasJsAccess) {
                orphanSpinnerIssues.push({
                    file: relativePath,
                    containerId,
                });
            }
        }
    }

    // ===== 輸出報告 =====
    let hasIssues = false;

    console.log(`--- STALE_LOADING: HTML 有「載入中」但 JS 沒有對應替換 ---`);
    if (staleLoadingIssues.length === 0) {
        console.log(`  ✅ 0 個問題 (共檢查 ${totalLoadingElements} 個載入元素)\n`);
    } else {
        hasIssues = true;
        console.log(`  ❌ ${staleLoadingIssues.length} 個問題:\n`);
        for (const issue of staleLoadingIssues) {
            console.log(`  [${issue.file}]`);
            console.log(`    元素: <${issue.type} id="${issue.elementId}"> (${issue.context})`);
            console.log(`    問題: JS 代碼中找不到 getElementById('${issue.elementId}') 或類似存取`);
            console.log('');
        }
    }

    console.log(`--- ORPHAN_SPINNER: spinner 容器沒有被 JS 操作 ---`);
    if (orphanSpinnerIssues.length === 0) {
        console.log(`  ✅ 0 個問題 (共檢查 ${totalSpinnerElements} 個 spinner 容器)\n`);
    } else {
        hasIssues = true;
        console.log(`  ❌ ${orphanSpinnerIssues.length} 個問題:\n`);
        for (const issue of orphanSpinnerIssues) {
            console.log(`  [${issue.file}]`);
            console.log(`    容器: #${issue.containerId}`);
            console.log(`    問題: JS 代碼中找不到對此容器的 DOM 操作`);
            console.log('');
        }
    }

    console.log('=== 摘要 ===');
    console.log(`掃描檔案數: ${htmlFiles.length}`);
    console.log(`載入元素數: ${totalLoadingElements}`);
    console.log(`STALE_LOADING: ${staleLoadingIssues.length}`);
    console.log(`ORPHAN_SPINNER: ${orphanSpinnerIssues.length}`);
    console.log(`總問題數: ${staleLoadingIssues.length + orphanSpinnerIssues.length}`);

    if (hasIssues) {
        console.log('\n⚠️  發現潛在問題，請檢查上述報告。');
        process.exit(1);
    } else {
        console.log('\n✅ 所有檢查通過！');
    }
}

main();

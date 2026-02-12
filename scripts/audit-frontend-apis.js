/**
 * 前端靜態分析腳本
 *
 * 不需要啟動伺服器，直接掃描 HTML 原始碼找問題：
 *   node scripts/audit-frontend-apis.js
 *
 * 掃描規則：
 *   STALE_LOADING — HTML 有「載入中」文字的元素，但同頁 JS 沒有對應的 DOM 替換
 *   ORPHAN_SPINNER — HTML 有 spinner-border，但同頁 JS 沒有移除/隱藏它的邏輯
 *   FIELD_MISMATCH — 前端 api.post/put 送出的欄位名與後端 DTO 不匹配
 */
const fs = require('fs');
const path = require('path');

const TEMPLATE_DIR = path.join(__dirname, '..', 'src', 'main', 'resources', 'templates');
const JS_DIR = path.join(__dirname, '..', 'src', 'main', 'resources', 'static', 'js');
const DTO_DIR = path.join(__dirname, '..', 'src', 'main', 'java', 'com', 'booking', 'platform', 'dto', 'request');
const CONTROLLER_DIR = path.join(__dirname, '..', 'src', 'main', 'java', 'com', 'booking', 'platform', 'controller');

// API URL → 後端 DTO 類別名對應表
// ServiceCategoryController 使用內嵌 DTO（CategoryRequest），需特殊處理
const API_TO_DTO = {
    'POST /api/bookings':               'CreateBookingRequest',
    'PUT /api/bookings/':               'UpdateBookingRequest',
    'POST /api/customers':              'CreateCustomerRequest',
    'PUT /api/customers/':              'UpdateCustomerRequest',
    'POST /api/staff':                  'CreateStaffRequest',
    'PUT /api/staff/':                  'UpdateStaffRequest',
    'POST /api/services':               'CreateServiceItemRequest',
    'PUT /api/services/':               'UpdateServiceItemRequest',
    'POST /api/products':               'CreateProductRequest',
    'PUT /api/products/':               'UpdateProductRequest',
    'POST /api/coupons':                'CreateCouponRequest',
    'PUT /api/coupons/':                'UpdateCouponRequest',
    'POST /api/campaigns':              'CreateCampaignRequest',
    'PUT /api/campaigns/':              'UpdateCampaignRequest',
    'POST /api/marketing/pushes':       'CreateMarketingPushRequest',
    'POST /api/membership-levels':      'CreateMembershipLevelRequest',
    'PUT /api/membership-levels/':      'UpdateMembershipLevelRequest',
    'PUT /api/settings':                'UpdateSettingsRequest',
    'POST /api/auth/change-password':   'ChangePasswordRequest',
    'POST /api/service-categories':     'ServiceCategoryController$CategoryRequest',
    'PUT /api/service-categories/':     'ServiceCategoryController$CategoryRequest',
};

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

// ========== 規則 3: FIELD_MISMATCH 工具函式 ==========

/**
 * 從 JS 代碼提取所有 api.post/put 呼叫及其 data 物件欄位
 * 回傳 [{ method: 'POST'|'PUT', url: '/api/xxx', fields: ['f1','f2'], line: 行號 }]
 */
function extractApiCalls(jsCode) {
    const results = [];

    // 匹配 api.post('/api/xxx', data) 或 api.put(`/api/xxx/${id}`, data)
    // 也匹配 api.post('/api/xxx', { ... })
    const callRegex = /api\.(post|put)\(\s*[`'"]([^`'"]+)[`'"]\s*,\s*(\w+|{[^}]*})/g;
    let match;

    while ((match = callRegex.exec(jsCode)) !== null) {
        const method = match[1].toUpperCase();
        let url = match[2];
        const dataArg = match[3];

        // 清理 URL 中的模板變數：/api/xxx/${id} → /api/xxx/
        url = url.replace(/\$\{[^}]+\}/g, '').replace(/\/+$/, '/');
        // 只處理 /api/ 開頭的 URL
        if (!url.startsWith('/api/')) continue;

        let fields = [];

        if (dataArg.startsWith('{')) {
            // 直接內聯物件：api.post('/api/xxx', { name: ..., phone: ... })
            fields = extractKeysFromObject(dataArg);
        } else {
            // 變數引用：api.post('/api/xxx', data)
            // 往上搜尋 "const data = {" 或 "let data = {" 或 "data = {"
            fields = findDataObjectFields(jsCode, dataArg, match.index);
        }

        if (fields.length > 0) {
            results.push({ method, url, fields });
        }
    }

    return results;
}

/**
 * 從物件字面量提取 key 名稱
 * 支援 ES6 shorthand: { name, phone } → ['name', 'phone']
 * 支援傳統寫法: { name: val, phone: val } → ['name', 'phone']
 */
function extractKeysFromObject(objStr) {
    const keys = [];
    // 移除外層 {} 和多餘空白
    let inner = objStr.replace(/^\{|\}$/g, '');

    // 先用粗略方式切割（處理巢狀物件、陣列、三元運算子等）
    // 追蹤括號深度，只在深度 0 時的逗號切割
    const tokens = [];
    let depth = 0;
    let current = '';
    for (const ch of inner) {
        if (ch === '{' || ch === '[' || ch === '(') depth++;
        else if (ch === '}' || ch === ']' || ch === ')') depth--;

        if (ch === ',' && depth === 0) {
            tokens.push(current.trim());
            current = '';
        } else {
            current += ch;
        }
    }
    if (current.trim()) tokens.push(current.trim());

    for (let token of tokens) {
        // 移除行內註解（// 到行尾），但保留多行內容
        token = token.split('\n').map(line => {
            const commentIdx = line.indexOf('//');
            return commentIdx >= 0 ? line.substring(0, commentIdx) : line;
        }).join('\n').trim();

        // 跳過空 token
        if (!token) continue;

        // 模式 1: key: value（搜尋第一個 key:）
        const kvMatch = token.match(/(?:^|\n)\s*(\w+)\s*:/);
        if (kvMatch) {
            keys.push(kvMatch[1]);
            continue;
        }

        // 模式 2: ES6 shorthand (standalone identifier)
        const shorthandMatch = token.match(/^\s*(\w+)\s*$/);
        if (shorthandMatch) {
            keys.push(shorthandMatch[1]);
            continue;
        }

        // 模式 3: ...spread 運算子（跳過）
        if (token.trim().startsWith('...')) continue;
    }

    return keys;
}

/**
 * 在 JS 代碼中往上搜尋 data 物件定義，提取所有欄位名
 */
function findDataObjectFields(jsCode, varName, callPosition) {
    // 往前最多搜尋 3000 字元的範圍
    const searchRange = jsCode.substring(Math.max(0, callPosition - 3000), callPosition);

    // 搜尋 "const data = {" 或 "let data = {" 或 "data = {"
    // 需考慮多行物件
    const patterns = [
        new RegExp(`(?:const|let|var)\\s+${varName}\\s*=\\s*\\{`, 'g'),
        new RegExp(`${varName}\\s*=\\s*\\{`, 'g'),
    ];

    let lastMatch = null;
    let lastIndex = -1;

    for (const pattern of patterns) {
        let m;
        while ((m = pattern.exec(searchRange)) !== null) {
            if (m.index > lastIndex) {
                lastMatch = m;
                lastIndex = m.index;
            }
        }
    }

    if (!lastMatch) return [];

    // 從 { 開始，找到對應的 }（處理巢狀）
    const startPos = lastIndex + lastMatch[0].length - 1; // 指向 {
    const objBody = extractBalancedBraces(searchRange.substring(startPos));
    if (!objBody) return [];

    return extractKeysFromObject(objBody);
}

/**
 * 提取平衡大括號內的內容
 */
function extractBalancedBraces(code) {
    if (code[0] !== '{') return null;
    let depth = 0;
    for (let i = 0; i < code.length && i < 2000; i++) {
        if (code[i] === '{') depth++;
        else if (code[i] === '}') {
            depth--;
            if (depth === 0) return code.substring(0, i + 1);
        }
    }
    return null; // 沒找到閉合
}

/**
 * 解析 Java DTO 的欄位
 * 回傳 { fields: ['f1','f2'], required: ['f1'] }
 */
function parseDtoFields(dtoName) {
    // 處理內嵌 DTO（如 ServiceCategoryController$CategoryRequest）
    if (dtoName.includes('$')) {
        const [controller, innerClass] = dtoName.split('$');
        return parseInnerClassDto(controller, innerClass);
    }

    const filePath = path.join(DTO_DIR, dtoName + '.java');
    if (!fs.existsSync(filePath)) {
        return null;
    }

    return parseJavaFields(fs.readFileSync(filePath, 'utf-8'));
}

/**
 * 解析 Controller 中的內嵌 DTO 類別
 */
function parseInnerClassDto(controllerName, innerClassName) {
    const filePath = path.join(CONTROLLER_DIR, controllerName + '.java');
    if (!fs.existsSync(filePath)) return null;

    const content = fs.readFileSync(filePath, 'utf-8');

    // 找到內嵌類別的起始位置
    const classRegex = new RegExp(`class\\s+${innerClassName}\\s*\\{`);
    const classMatch = classRegex.exec(content);
    if (!classMatch) return null;

    // 從類別開始位置提取平衡的 {}
    const classBody = extractBalancedBraces(content.substring(classMatch.index + classMatch[0].length - 1));
    if (!classBody) return null;

    return parseJavaFields(classBody);
}

/**
 * 從 Java 原始碼提取欄位和必填標記
 */
function parseJavaFields(javaCode) {
    const fields = [];
    const required = [];

    // 先按行處理，追蹤 annotation
    const lines = javaCode.split('\n');
    let pendingRequired = false;

    for (const line of lines) {
        const trimmed = line.trim();

        // 檢查必填標記
        if (trimmed.match(/@Not(Null|Blank|Empty)/)) {
            pendingRequired = true;
        }

        // 匹配欄位宣告: private TypeName fieldName;
        const fieldMatch = trimmed.match(/private\s+[\w<>,\s?]+\s+(\w+)\s*;/);
        if (fieldMatch) {
            const fieldName = fieldMatch[1];
            fields.push(fieldName);
            if (pendingRequired) {
                required.push(fieldName);
            }
            pendingRequired = false;
        }

        // 非欄位行重置（避免 annotation 串接到錯誤欄位）
        if (!trimmed.startsWith('@') && !trimmed.startsWith('private') && trimmed.length > 0) {
            pendingRequired = false;
        }
    }

    return { fields, required };
}

/**
 * 比對前端 API 呼叫與後端 DTO，找出欄位不匹配
 */
function validateFieldMismatch(apiCalls, fileName) {
    const issues = [];

    for (const call of apiCalls) {
        // 找到對應的 DTO
        const apiKey = `${call.method} ${call.url}`;

        let dtoName = null;
        // 精確匹配或前綴匹配（PUT /api/xxx/ 開頭）
        if (API_TO_DTO[apiKey]) {
            dtoName = API_TO_DTO[apiKey];
        } else {
            // 嘗試前綴匹配（PUT /api/bookings/123 → PUT /api/bookings/）
            // 但排除子資源路徑（PUT /api/staff/123/schedule 不應匹配 PUT /api/staff/）
            for (const [pattern, dto] of Object.entries(API_TO_DTO)) {
                if (pattern.endsWith('/') && apiKey.startsWith(pattern)) {
                    // 取出 ID 後面的部分，如果還有 / 開頭的路徑，代表是子資源
                    const remainder = apiKey.substring(pattern.length);
                    // remainder 應該是空的或只是 ID，不含 / 後更多路徑
                    if (!remainder.includes('/')) {
                        dtoName = dto;
                        break;
                    }
                }
            }
        }

        if (!dtoName) continue; // 沒有對應的 DTO 規則，跳過

        const dtoInfo = parseDtoFields(dtoName);
        if (!dtoInfo) continue; // DTO 檔案不存在，跳過

        const frontendFields = new Set(call.fields);
        const dtoFields = new Set(dtoInfo.fields);

        // 前端送了但 DTO 沒有的欄位
        const extraFields = call.fields.filter(f => !dtoFields.has(f));
        // DTO 必填但前端沒送的欄位
        const missingRequired = dtoInfo.required.filter(f => !frontendFields.has(f));

        if (extraFields.length > 0 || missingRequired.length > 0) {
            issues.push({
                file: fileName,
                method: call.method,
                url: call.url,
                dtoName,
                extraFields,
                missingRequired,
            });
        }
    }

    return issues;
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
    const fieldMismatchIssues = [];
    let totalLoadingElements = 0;
    let totalSpinnerElements = 0;
    let totalApiCalls = 0;

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

        // ===== 規則 3: FIELD_MISMATCH =====
        const apiCalls = extractApiCalls(inlineJs);
        totalApiCalls += apiCalls.length;
        const mismatches = validateFieldMismatch(apiCalls, relativePath);
        fieldMismatchIssues.push(...mismatches);
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

    console.log(`--- FIELD_MISMATCH: 前端欄位與後端 DTO 不匹配 ---`);
    if (fieldMismatchIssues.length === 0) {
        console.log(`  ✅ 0 個問題 (共檢查 ${totalApiCalls} 個 API 呼叫)\n`);
    } else {
        hasIssues = true;
        console.log(`  ❌ ${fieldMismatchIssues.length} 個問題:\n`);
        for (const issue of fieldMismatchIssues) {
            console.log(`  [${issue.file}] ${issue.method} ${issue.url} → ${issue.dtoName}`);
            if (issue.extraFields.length > 0) {
                console.log(`    前端多的欄位: ${issue.extraFields.join(', ')} (DTO 中不存在)`);
            }
            if (issue.missingRequired.length > 0) {
                console.log(`    前端缺少的必填欄位: ${issue.missingRequired.join(', ')}`);
            }
            console.log('');
        }
    }

    console.log('=== 摘要 ===');
    console.log(`掃描檔案數: ${htmlFiles.length}`);
    console.log(`載入元素數: ${totalLoadingElements}`);
    console.log(`API 呼叫數: ${totalApiCalls}`);
    console.log(`STALE_LOADING: ${staleLoadingIssues.length}`);
    console.log(`ORPHAN_SPINNER: ${orphanSpinnerIssues.length}`);
    console.log(`FIELD_MISMATCH: ${fieldMismatchIssues.length}`);
    console.log(`總問題數: ${staleLoadingIssues.length + orphanSpinnerIssues.length + fieldMismatchIssues.length}`);

    if (hasIssues) {
        console.log('\n⚠️  發現潛在問題，請檢查上述報告。');
        process.exit(1);
    } else {
        console.log('\n✅ 所有檢查通過！');
    }
}

main();

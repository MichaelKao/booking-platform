package com.booking.platform.common.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 租戶過濾器
 *
 * <p>從請求中提取租戶資訊並設定到 TenantContext
 *
 * <p>支援的租戶識別方式：
 * <ul>
 *   <li>Header: X-Tenant-Id</li>
 *   <li>URL Path: /api/line/webhook/{tenantCode}</li>
 *   <li>JWT Token 中的 tenantId claim</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@Order(1)
@Slf4j
public class TenantFilter extends OncePerRequestFilter {

    // ========================================
    // 常數
    // ========================================

    /**
     * 租戶 ID Header 名稱
     */
    private static final String TENANT_ID_HEADER = "X-Tenant-Id";

    /**
     * 租戶代碼 Header 名稱
     */
    private static final String TENANT_CODE_HEADER = "X-Tenant-Code";

    /**
     * LINE Webhook 路徑前綴
     */
    private static final String LINE_WEBHOOK_PATH = "/api/line/webhook/";

    // ========================================
    // 過濾邏輯
    // ========================================

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            // 嘗試從不同來源提取租戶資訊
            extractTenantFromRequest(request);

            // 繼續處理請求
            filterChain.doFilter(request, response);
        } finally {
            // 請求結束後清除上下文，避免記憶體洩漏
            TenantContext.clear();
        }
    }

    // ========================================
    // 私有方法
    // ========================================

    /**
     * 從請求中提取租戶資訊
     */
    private void extractTenantFromRequest(HttpServletRequest request) {
        // 1. 嘗試從 Header 取得租戶 ID
        String tenantId = request.getHeader(TENANT_ID_HEADER);
        if (tenantId != null && !tenantId.isEmpty()) {
            TenantContext.setTenantId(tenantId);
            log.debug("從 Header 取得租戶 ID：{}", tenantId);
        }

        // 2. 嘗試從 Header 取得租戶代碼
        String tenantCode = request.getHeader(TENANT_CODE_HEADER);
        if (tenantCode != null && !tenantCode.isEmpty()) {
            TenantContext.setTenantCode(tenantCode);
            log.debug("從 Header 取得租戶代碼：{}", tenantCode);
        }

        // 3. 嘗試從 LINE Webhook 路徑取得租戶代碼
        String requestPath = request.getRequestURI();
        if (requestPath.startsWith(LINE_WEBHOOK_PATH)) {
            String pathTenantCode = extractTenantCodeFromPath(requestPath);
            if (pathTenantCode != null) {
                TenantContext.setTenantCode(pathTenantCode);
                log.debug("從 URL 路徑取得租戶代碼：{}", pathTenantCode);
            }
        }
    }

    /**
     * 從 URL 路徑提取租戶代碼
     */
    private String extractTenantCodeFromPath(String path) {
        // /api/line/webhook/{tenantCode} -> tenantCode
        String remaining = path.substring(LINE_WEBHOOK_PATH.length());
        int slashIndex = remaining.indexOf('/');
        if (slashIndex > 0) {
            return remaining.substring(0, slashIndex);
        } else if (!remaining.isEmpty()) {
            return remaining;
        }
        return null;
    }

    // ========================================
    // 排除路徑
    // ========================================

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 排除靜態資源和健康檢查
        return path.startsWith("/actuator/") ||
               path.startsWith("/static/") ||
               path.startsWith("/favicon.ico") ||
               path.equals("/health") ||
               path.equals("/");
    }
}

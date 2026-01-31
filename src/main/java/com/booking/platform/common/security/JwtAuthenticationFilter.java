package com.booking.platform.common.security;

import com.booking.platform.common.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * JWT 認證過濾器
 *
 * <p>從 HTTP Header 中取得 JWT Token，驗證後設定 SecurityContext
 *
 * <p>同時設定 TenantContext，讓後續的 Service 可以取得當前租戶 ID
 *
 * @author Developer
 * @since 1.0.0
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // ========================================
    // 依賴注入
    // ========================================

    private final JwtTokenProvider jwtTokenProvider;

    // ========================================
    // 常數
    // ========================================

    /**
     * Authorization Header 名稱
     */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /**
     * Bearer Token 前綴
     */
    private static final String BEARER_PREFIX = "Bearer ";

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
            // ========================================
            // 1. 取得 Token
            // ========================================

            String token = extractToken(request);

            // ========================================
            // 2. 驗證 Token
            // ========================================

            if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {

                // 確認是 Access Token
                if (!jwtTokenProvider.isAccessToken(token)) {
                    log.debug("Token 類型錯誤，不是 Access Token");
                    filterChain.doFilter(request, response);
                    return;
                }

                // ========================================
                // 3. 取得使用者資訊
                // ========================================

                String userId = jwtTokenProvider.getUserId(token);
                String username = jwtTokenProvider.getUsername(token);
                String role = jwtTokenProvider.getRole(token);
                String tenantId = jwtTokenProvider.getTenantId(token);

                // ========================================
                // 4. 設定 SecurityContext
                // ========================================

                List<SimpleGrantedAuthority> authorities = Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                authorities
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);

                // ========================================
                // 5. 設定 TenantContext
                // ========================================

                if (tenantId != null) {
                    TenantContext.setTenantId(tenantId);
                }

                log.debug("認證成功，使用者：{}，角色：{}，租戶：{}",
                        username, role, tenantId);
            }
        } catch (Exception e) {
            log.error("JWT 認證失敗：{}", e.getMessage());
        } finally {
            // 繼續過濾鏈
            filterChain.doFilter(request, response);

            // 清除 TenantContext
            TenantContext.clear();
        }
    }

    // ========================================
    // 工具方法
    // ========================================

    /**
     * 從請求中提取 Token
     *
     * @param request HTTP 請求
     * @return JWT Token，若無則回傳 null
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }

        return null;
    }
}

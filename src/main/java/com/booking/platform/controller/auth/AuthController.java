package com.booking.platform.controller.auth;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.security.JwtTokenProvider;
import com.booking.platform.dto.request.ChangePasswordRequest;
import com.booking.platform.dto.request.ForgotPasswordRequest;
import com.booking.platform.dto.request.LoginRequest;
import com.booking.platform.dto.request.RefreshTokenRequest;
import com.booking.platform.dto.request.ResetPasswordRequest;
import com.booking.platform.dto.request.TenantRegisterRequest;
import com.booking.platform.dto.response.LoginResponse;
import com.booking.platform.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 認證 Controller
 *
 * <p>處理登入、登出、Token 刷新等認證相關 API
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AuthController {

    // ========================================
    // 依賴注入
    // ========================================

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    // ========================================
    // 超級管理員認證
    // ========================================

    /**
     * 超級管理員登入
     *
     * @param request 登入請求
     * @param httpRequest HTTP 請求（取得 IP）
     * @return 登入回應
     */
    @PostMapping("/admin/login")
    public ApiResponse<LoginResponse> adminLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("收到超級管理員登入請求，帳號：{}", request.getUsername());

        String ip = getClientIp(httpRequest);
        LoginResponse response = authService.adminLogin(request, ip);

        return ApiResponse.ok("登入成功", response);
    }

    // ========================================
    // 店家認證
    // ========================================

    /**
     * 店家登入
     *
     * @param request 登入請求
     * @param httpRequest HTTP 請求（取得 IP）
     * @return 登入回應
     */
    @PostMapping("/tenant/login")
    public ApiResponse<LoginResponse> tenantLogin(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("收到店家登入請求，帳號：{}", request.getUsername());

        String ip = getClientIp(httpRequest);
        LoginResponse response = authService.tenantLogin(request, ip);

        return ApiResponse.ok("登入成功", response);
    }

    // ========================================
    // 通用認證
    // ========================================

    /**
     * 統一登入入口
     *
     * <p>根據帳號格式自動判斷是超級管理員還是店家
     *
     * @param request 登入請求
     * @param httpRequest HTTP 請求
     * @return 登入回應
     */
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("收到統一登入請求，帳號：{}", request.getUsername());

        String ip = getClientIp(httpRequest);

        // 嘗試超級管理員登入
        try {
            LoginResponse response = authService.adminLogin(request, ip);
            return ApiResponse.ok("登入成功", response);
        } catch (Exception e) {
            log.debug("非超級管理員帳號，嘗試店家登入");
        }

        // 嘗試店家登入
        LoginResponse response = authService.tenantLogin(request, ip);
        return ApiResponse.ok("登入成功", response);
    }

    // ========================================
    // Token 管理
    // ========================================

    /**
     * 刷新 Token
     *
     * @param request 刷新請求
     * @return 新的登入回應
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("收到刷新 Token 請求");

        LoginResponse response = authService.refreshToken(request);

        return ApiResponse.ok("Token 刷新成功", response);
    }

    /**
     * 登出
     *
     * <p>目前使用無狀態 JWT，登出由前端處理（刪除 Token）
     *
     * @return 登出結果
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        log.info("收到登出請求");

        // JWT 是無狀態的，登出由前端刪除 Token
        // 如果需要強制登出，可以實作 Token 黑名單（使用 Redis）

        return ApiResponse.ok("登出成功", null);
    }

    // ========================================
    // 店家註冊
    // ========================================

    /**
     * 店家自助註冊
     *
     * @param request 註冊請求
     * @return 登入回應（註冊成功後自動登入）
     */
    @PostMapping("/tenant/register")
    public ApiResponse<LoginResponse> tenantRegister(
            @Valid @RequestBody TenantRegisterRequest request
    ) {
        log.info("收到店家註冊請求，代碼：{}", request.getCode());

        LoginResponse response = authService.tenantRegister(request);

        return ApiResponse.ok("註冊成功", response);
    }

    // ========================================
    // 密碼管理
    // ========================================

    /**
     * 忘記密碼
     *
     * @param request 忘記密碼請求
     * @return 處理結果
     */
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        log.info("收到忘記密碼請求");

        authService.forgotPassword(request);

        return ApiResponse.ok("如果帳號存在，重設連結將發送到您的電子郵件", null);
    }

    /**
     * 重設密碼
     *
     * @param request 重設密碼請求
     * @return 處理結果
     */
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        log.info("收到重設密碼請求");

        authService.resetPassword(request);

        return ApiResponse.ok("密碼已重設，請使用新密碼登入", null);
    }

    /**
     * 更改密碼（需登入）
     *
     * @param request 更改密碼請求
     * @param httpRequest HTTP 請求（取得 Token）
     * @return 處理結果
     */
    @PostMapping("/change-password")
    public ApiResponse<Void> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("收到更改密碼請求");

        // 從 Token 取得租戶 ID
        String token = extractToken(httpRequest);
        if (token == null || !jwtTokenProvider.validateToken(token)) {
            return ApiResponse.error("AUTH_001", "請先登入");
        }

        String tenantId = jwtTokenProvider.getTenantId(token);
        if (tenantId == null) {
            return ApiResponse.error("AUTH_001", "請先登入");
        }

        authService.changePassword(tenantId, request);

        return ApiResponse.ok("密碼已更改", null);
    }

    // ========================================
    // 工具方法
    // ========================================

    /**
     * 從請求中提取 Token
     *
     * @param request HTTP 請求
     * @return Token 字串
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 取得客戶端 IP
     *
     * @param request HTTP 請求
     * @return IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        // 依序檢查各種 Header
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For 可能包含多個 IP，取第一個
                int index = ip.indexOf(',');
                if (index > 0) {
                    ip = ip.substring(0, index).trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}

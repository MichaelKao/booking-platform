package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.common.tenant.TenantContext;
import com.booking.platform.dto.response.ReferralDashboardResponse;
import com.booking.platform.service.ReferralService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 推薦 API Controller
 *
 * <p>處理店家推薦相關 API
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/referrals")
@RequiredArgsConstructor
@Slf4j
public class ReferralController {

    private final ReferralService referralService;

    /**
     * 取得推薦儀表板
     */
    @GetMapping("/dashboard")
    public ApiResponse<ReferralDashboardResponse> getDashboard() {
        String tenantId = TenantContext.getTenantId();
        ReferralDashboardResponse dashboard = referralService.getDashboard(tenantId);
        return ApiResponse.ok(dashboard);
    }

    /**
     * 取得推薦碼
     */
    @GetMapping("/code")
    public ApiResponse<String> getReferralCode() {
        String tenantId = TenantContext.getTenantId();
        String code = referralService.getOrGenerateReferralCode(tenantId);
        return ApiResponse.ok(code);
    }
}

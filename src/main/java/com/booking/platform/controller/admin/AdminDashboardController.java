package com.booking.platform.controller.admin;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.dto.response.AdminDashboardResponse;
import com.booking.platform.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 超級管理儀表板 Controller
 *
 * <p>提供平台整體統計數據
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminDashboardController {

    // ========================================
    // 依賴注入
    // ========================================

    private final AdminDashboardService adminDashboardService;

    // ========================================
    // API
    // ========================================

    /**
     * 取得儀表板資料
     *
     * @return 儀表板資料
     */
    @GetMapping
    public ApiResponse<AdminDashboardResponse> getDashboard() {
        log.info("收到取得超級管理儀表板請求");
        return ApiResponse.ok(adminDashboardService.getDashboard());
    }
}

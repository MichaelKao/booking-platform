package com.booking.platform.controller;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.dto.request.UpdateSettingsRequest;
import com.booking.platform.dto.response.SettingsResponse;
import com.booking.platform.service.SettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 店家設定 Controller
 *
 * <p>管理店家的基本設定
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Validated
@Slf4j
public class SettingsController {

    // ========================================
    // 依賴注入
    // ========================================

    private final SettingsService settingsService;

    // ========================================
    // API
    // ========================================

    /**
     * 取得店家設定
     *
     * @return 設定資料
     */
    @GetMapping
    public ApiResponse<SettingsResponse> getSettings() {
        log.info("收到取得店家設定請求");
        return ApiResponse.ok(settingsService.getSettings());
    }

    /**
     * 更新店家設定
     *
     * @param request 更新請求
     * @return 更新後的設定
     */
    @PutMapping
    public ApiResponse<SettingsResponse> updateSettings(
            @Valid @RequestBody UpdateSettingsRequest request
    ) {
        log.info("收到更新店家設定請求");
        return ApiResponse.ok(settingsService.updateSettings(request));
    }
}

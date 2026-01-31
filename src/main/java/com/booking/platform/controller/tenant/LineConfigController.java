package com.booking.platform.controller.tenant;

import com.booking.platform.common.response.ApiResponse;
import com.booking.platform.dto.line.LineConfigResponse;
import com.booking.platform.dto.line.SaveLineConfigRequest;
import com.booking.platform.service.line.LineConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * LINE 設定控制器
 *
 * <p>提供店家管理 LINE Bot 設定的 API
 *
 * <p>API 端點：
 * <ul>
 *   <li>GET /api/settings/line - 取得 LINE 設定</li>
 *   <li>PUT /api/settings/line - 更新 LINE 設定</li>
 *   <li>POST /api/settings/line/activate - 啟用 LINE Bot</li>
 *   <li>POST /api/settings/line/deactivate - 停用 LINE Bot</li>
 * </ul>
 *
 * @author Developer
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/settings/line")
@RequiredArgsConstructor
@Slf4j
public class LineConfigController {

    // ========================================
    // 依賴注入
    // ========================================

    private final LineConfigService lineConfigService;

    // ========================================
    // 查詢 API
    // ========================================

    /**
     * 取得 LINE 設定
     *
     * @return LINE 設定
     */
    @GetMapping
    public ResponseEntity<ApiResponse<LineConfigResponse>> getConfig() {
        log.debug("取得 LINE 設定");

        LineConfigResponse response = lineConfigService.getConfig();

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    // ========================================
    // 寫入 API
    // ========================================

    /**
     * 更新 LINE 設定
     *
     * @param request 設定請求
     * @return 更新後的 LINE 設定
     */
    @PutMapping
    public ResponseEntity<ApiResponse<LineConfigResponse>> saveConfig(
            @Valid @RequestBody SaveLineConfigRequest request
    ) {
        log.debug("更新 LINE 設定，請求：{}", request);

        LineConfigResponse response = lineConfigService.saveConfig(request);

        return ResponseEntity.ok(ApiResponse.ok("LINE 設定更新成功", response));
    }

    /**
     * 啟用 LINE Bot
     *
     * @return 更新後的 LINE 設定
     */
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<LineConfigResponse>> activate() {
        log.debug("啟用 LINE Bot");

        LineConfigResponse response = lineConfigService.activate();

        return ResponseEntity.ok(ApiResponse.ok("LINE Bot 已啟用", response));
    }

    /**
     * 停用 LINE Bot
     *
     * @return 更新後的 LINE 設定
     */
    @PostMapping("/deactivate")
    public ResponseEntity<ApiResponse<LineConfigResponse>> deactivate() {
        log.debug("停用 LINE Bot");

        LineConfigResponse response = lineConfigService.deactivate();

        return ResponseEntity.ok(ApiResponse.ok("LINE Bot 已停用", response));
    }
}
